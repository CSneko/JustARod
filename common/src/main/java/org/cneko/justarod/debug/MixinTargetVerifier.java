package org.cneko.justarod.debug;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mixin 注入目标的离线校验：把每个 {@code @Mixin} 里声明的类取出来，
 * 逐个检查 {@code @Inject} 的方法名在「该类的继承链」上是否真的存在，
 * 以及 {@code @Invoker} / {@code @Accessor} 的目标成员是否存在、**签名是否一致**。
 *
 * <p>为什么需要它：对 {@code Slime} 注入 {@code mobInteract} 时编译完全通过，
 * 但运行期类加载会直接崩（{@code InvalidInjectionException: could not find any targets}），
 * 因为 {@code mobInteract} 是 {@code Mob} 声明的、{@code Slime} 没覆写，
 * 而 Mixin 的默认目标选择器**不搜父类**。本任务就是提前把这类问题抓出来。
 *
 * <p>访问器/调用器同理：{@code @Invoker("scale")} 只要名字或签名写错（泛型擦除后的
 * 描述符必须逐字一致），运行期同样在类加载阶段直接崩，编译器一点提示都没有。
 *
 * <p>用法：{@code ./gradlew :common:verifyMixins}（构建产物里已编译的 class 上运行）
 */
public final class MixinTargetVerifier {

    /** 已知的 Mixin「默认不搜父类」行为，这里按 Mixin 的真实语义校验：目标方法必须在被声明的层级上 */
    private static final Map<String, String> KNOWN_DEFAULTS = Map.of();

    public static void main(String[] args) throws Exception {
        Path classesDir = Path.of(args[0]);
        // 只校验「配置里真正注册」的 mixin：未注册的类是死代码，不需要担心
        Set<String> registered = new HashSet<>();
        Path mixinConfig = Path.of(args.length > 1 ? args[1] : "");
        if (!Files.isRegularFile(mixinConfig)) {
            // 兜底：从 classes 目录反推 build/resources/main
            mixinConfig = classesDir.getParent().getParent().getParent()
                    .resolve("resources/main/justarod.mixins.json");
        }
        System.out.println("mixin 配置: " + mixinConfig + " (存在=" + Files.isRegularFile(mixinConfig) + ")");
        if (Files.isRegularFile(mixinConfig)) {
            String json = Files.readString(mixinConfig);
            // 包名（mixin 列表里的名字是简化名，要拼上 package）
            String pkg = "";
            var pkgMatcher = java.util.regex.Pattern
                    .compile("\"package\"\\s*:\\s*\"([^\"]+)\"")
                    .matcher(json);
            if (pkgMatcher.find()) pkg = pkgMatcher.group(1).replace('.', '/');
            for (String section : new String[]{"mixins", "client", "server"}) {
                var sectionMatcher = java.util.regex.Pattern
                        .compile("\"" + section + "\"\\s*:\\s*\\[(.*?)\\]", java.util.regex.Pattern.DOTALL)
                        .matcher(json);
                while (sectionMatcher.find()) {
                    var itemMatcher = java.util.regex.Pattern.compile("\"([A-Za-z0-9_$.]+)\"").matcher(sectionMatcher.group(1));
                    while (itemMatcher.find()) {
                        String value = itemMatcher.group(1);
                        registered.add(pkg.isEmpty() ? value : pkg + "/" + value.replace('.', '/'));
                    }
                }
            }
        }
        List<String> errors = new ArrayList<>();
        List<String> unresolved = new ArrayList<>();
        int checked = 0;
        int generated = 0;
        int skipped = 0;

        Map<String, ClassNode> nodes = new HashMap<>();
        try (var stream = Files.walk(classesDir)) {
            for (Path path : stream.filter(p -> p.toString().endsWith(".class")).toList()) {
                try (InputStream in = Files.newInputStream(path)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, 0);
                    nodes.put(node.name, node);
                }
            }
        }
        System.out.println("扫描到 " + nodes.size() + " 个已编译类");

        // @Accessor 可以指向「别的 mixin 用 @Unique 注入进来的字段」（例如 SlimeTrustAccessor
        // 读的是 SlimeTrustMixin 加到 Slime 上的 justarod$trust），先把这些字段收集起来
        Map<String, Set<String>> injectedFields = new HashMap<>();
        for (ClassNode node : nodes.values()) {
            AnnotationNode mixin = findMixinAnnotation(node);
            if (mixin == null) continue;
            for (String target : mixinTargets(mixin, node, nodes)) {
                for (var field : node.fields) {
                    AnnotationNode unique = find(field.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Unique;");
                    if (unique == null) unique = find(field.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Unique;");
                    if (unique != null) {
                        injectedFields.computeIfAbsent(target, k -> new HashSet<>()).add(field.name);
                    }
                }
            }
        }

        // 先检查「配置里注册的 mixin 类是否真的存在」——写错包名时运行期才会崩
        List<String> missingMixins = new ArrayList<>();
        for (String name : registered) {
            if (!nodes.containsKey(name)) missingMixins.add(name.replace('/', '.'));
        }

        for (ClassNode node : nodes.values()) {
            AnnotationNode mixin = findMixinAnnotation(node);
            if (mixin == null) continue;
            if (!registered.isEmpty() && !registered.contains(node.name)) {
                skipped++;
                continue;   // 配置里没注册（死代码）
            }
            List<String> targets = mixinTargets(mixin, node, nodes);
            System.out.println("  " + node.name.replace('/', '.') + " → " + targets);
            if (targets.isEmpty()) continue;

            for (MethodNode method : node.methods) {
                AnnotationNode inject = find(method.visibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
                if (inject == null) inject = find(method.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/injection/Inject;");
                if (inject == null) {
                    // @Invoker / @Accessor：名字 + 描述符都要和原方法逐字一致，否则类加载期就崩
                    if (checkGeneratedAccessor(node, method, targets, nodes, injectedFields, errors, unresolved)) {
                        generated++;
                    }
                    continue;
                }
                String spec = stringValue(inject, "method");
                if (spec == null || spec.isEmpty()) continue;
                int paren = spec.indexOf('(');
                String targetName = (paren < 0 ? spec : spec.substring(0, paren)).trim();
                String expectedDescriptor = paren < 0 ? null : spec.substring(paren).trim();
                if (targetName.isEmpty() || targetName.startsWith("<")) continue;

                for (String target : targets) {
                    checked++;
                    // 与 Mixin 的默认目标选择器一致：只看目标类**自身声明**的方法（不搜父类）
                    Lookup lookup = matchMethod(target, targetName, expectedDescriptor, false, nodes);
                    switch (lookup.result()) {
                        case OK -> { }
                        case MISMATCH -> errors.add(node.name.replace('/', '.') + "." + method.name
                                + " → 注入目标 " + target.replace('/', '.') + "#" + spec
                                + " 的描述符不一致：声明的是 " + expectedDescriptor
                                + "，目标是 " + lookup.descriptor());
                        case MISSING -> errors.add(node.name.replace('/', '.') + "." + method.name
                                + " → 注入目标 " + target.replace('/', '.') + "#" + targetName
                                + " 在该类自身声明的成员里不存在（Mixin 默认不搜父类）");
                        case UNRESOLVED -> unresolved.add(target.replace('/', '.') + "#" + targetName);
                    }
                }
            }
        }

        System.out.println("校验了 " + checked + " 个 @Inject 目标、"
                + generated + " 个 @Invoker/@Accessor 目标（跳过未注册的 mixin：" + skipped + " 个）");
        // 注意：registered 里也混有 package 名等字符串，只有能拼出真实类名的才可能是 mixin；
        // 报出来的是「配置里出现过、但编译产物里没有对应类」的候选。
        for (String missing : missingMixins) {
            errors.add("配置里注册的 mixin 类不存在：" + missing + "（检查包名是否与 mixins.json 的 package 一致）");
        }
        if (!unresolved.isEmpty()) {
            System.out.println("⚠ 无法解析的目标（不在扫描范围且反射查不到，通常是第三方模组类）：");
            unresolved.stream().distinct().forEach(u -> System.out.println("   ? " + u));
        }
        if (errors.isEmpty()) {
            System.out.println("✅ 全部目标方法存在");
        } else {
            System.out.println("❌ 发现 " + errors.size() + " 个问题：");
            errors.forEach(e -> System.out.println("   - " + e));
            System.exit(1);
        }
    }

    private static AnnotationNode find(List<AnnotationNode> annotations, String desc) {
        if (annotations == null) return null;
        for (AnnotationNode node : annotations) {
            if (desc.equals(node.desc)) return node;
        }
        return null;
    }

    /** 注意：{@code @Mixin} 是 RetentionPolicy.CLASS → ASM 放在 invisibleAnnotations */
    private static AnnotationNode findMixinAnnotation(ClassNode node) {
        AnnotationNode mixin = find(node.invisibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        if (mixin == null) mixin = find(node.visibleAnnotations, "Lorg/spongepowered/asm/mixin/Mixin;");
        return mixin;
    }

    private static final String INVOKER_DESC = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String ACCESSOR_DESC = "Lorg/spongepowered/asm/mixin/gen/Accessor;";

    /**
     * 校验一个 {@code @Invoker} / {@code @Accessor} 方法。
     *
     * <p>规则（与 Mixin 一致）：
     * <ul>
     *   <li>{@code @Invoker}：目标类（或其父类）必须声明同名方法，且**描述符逐字相同**
     *       （泛型擦除后；写错返回值/参数类型编译器不会报，运行期类加载直接崩）；</li>
     *   <li>{@code @Accessor}：目标字段可以在目标类、其父类，或**别的 mixin 用 {@code @Unique}
     *       注入进目标类的字段**上（{@code justarod$trust} 就是这种）；能找到字段即可，
     *       同时检查取值/写值方法的类型与字段类型一致。</li>
     * </ul>
     */
    private static boolean checkGeneratedAccessor(ClassNode mixinNode, MethodNode method, List<String> targets,
                                                  Map<String, ClassNode> nodes, Map<String, Set<String>> injectedFields,
                                                  List<String> errors, List<String> unresolved) {
        AnnotationNode invoker = find(method.visibleAnnotations, INVOKER_DESC);
        if (invoker == null) invoker = find(method.invisibleAnnotations, INVOKER_DESC);
        AnnotationNode accessor = invoker == null ? find(method.visibleAnnotations, ACCESSOR_DESC) : null;
        if (accessor == null && invoker == null) accessor = find(method.invisibleAnnotations, ACCESSOR_DESC);
        if (invoker == null && accessor == null) return false;

        String name = stringValue(invoker != null ? invoker : accessor, "value");
        if (name == null || name.isEmpty()) name = method.name;
        String owner = mixinNode.name.replace('/', '.');

        for (String target : targets) {
            if (invoker != null) {
                // @Invoker 的目标方法必须与声明**逐字同描述符**；同名重载里只要有一个对上就算过
                Lookup lookup = matchMethod(target, name, method.desc, true, nodes);
                switch (lookup.result()) {
                    case OK -> { }
                    case MISMATCH -> errors.add(owner + "." + method.name + " → @Invoker(\"" + name + "\") 签名不一致："
                            + "声明的是 " + method.desc + "，目标是 " + lookup.descriptor()
                            + "（泛型擦除后必须逐字相同）");
                    case MISSING -> errors.add(owner + "." + method.name + " → @Invoker(\"" + name
                            + "\") 的目标方法在 " + target.replace('/', '.') + " 及其父类里都不存在");
                    case UNRESOLVED -> unresolved.add(target.replace('/', '.') + "#" + name);
                }
            } else {
                String fieldDesc = findFieldDescriptor(target, name, nodes);
                if (fieldDesc == null && !injectedFields.getOrDefault(target, Set.of()).contains(name)) {
                    unresolved.add(target.replace('/', '.') + "#" + name + "（字段）");
                } else if (fieldDesc != null && !accessorTypeMatches(method, fieldDesc)) {
                    errors.add(owner + "." + method.name + " → @Accessor(\"" + name + "\") 类型不一致："
                            + "声明的是 " + method.desc + "，字段是 " + fieldDesc);
                }
            }
        }
        return true;
    }

    /** 取值方法（无参）返回类型 / 写值方法（单参）参数类型应与字段类型一致 */
    private static boolean accessorTypeMatches(MethodNode method, String fieldDesc) {
        Type[] args = Type.getArgumentTypes(method.desc);
        if (args.length == 0) return Type.getReturnType(method.desc).equals(Type.getType(fieldDesc));
        if (args.length == 1) return args[0].equals(Type.getType(fieldDesc));
        return true;   // 多参形态（getter/setter 之外的用法）不校验
    }

    /**
     * 在目标类（可选连父类一起）里找同名方法，并与期望描述符比对。
     *
     * <p>必须「任一重载匹配即算过」：目标类常有同名重载与**桥接方法**
     * （例如 {@code AvatarRenderer#extractRenderState} 既有
     * {@code (Avatar, AvatarRenderState, F)} 又有编译器生成的 {@code (Entity, EntityRenderState, F)}），
     * 只看第一个同名方法会误报。
     */
    private static Lookup matchMethod(String internalName, String methodName, @Nullable String expectedDescriptor,
                                      boolean searchHierarchy, Map<String, ClassNode> nodes) {
        boolean foundName = false;
        String firstOther = null;
        for (ClassNode node = nodes.get(internalName); node != null; node = searchHierarchy ? nodes.get(node.superName) : null) {
            for (MethodNode method : node.methods) {
                if (!method.name.equals(methodName)) continue;
                foundName = true;
                if (expectedDescriptor == null || expectedDescriptor.equals(method.desc)) {
                    return new Lookup(Result.OK, method.desc);
                }
                if (firstOther == null) firstOther = method.desc;
            }
            if (!searchHierarchy) break;
        }
        Class<?> clazz = loadClass(internalName);
        if (clazz == null) return new Lookup(foundName ? Result.MISMATCH : Result.UNRESOLVED, firstOther);
        for (Class<?> c = clazz; c != null && c != Object.class; c = searchHierarchy ? c.getSuperclass() : null) {
            for (var m : c.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                foundName = true;
                String desc = Type.getMethodDescriptor(m);
                if (expectedDescriptor == null || expectedDescriptor.equals(desc)) return new Lookup(Result.OK, desc);
                if (firstOther == null) firstOther = desc;
            }
            if (!searchHierarchy) break;
        }
        if (!foundName) return new Lookup(Result.MISSING, null);
        return new Lookup(Result.MISMATCH, firstOther);
    }

    /** 在目标类（及其父类）里找同名字段的描述符；找不到返回 null */
    private static String findFieldDescriptor(String internalName, String fieldName, Map<String, ClassNode> nodes) {
        for (ClassNode node = nodes.get(internalName); node != null; node = nodes.get(node.superName)) {
            for (var field : node.fields) {
                if (field.name.equals(fieldName)) return field.desc;
            }
        }
        Class<?> clazz = loadClass(internalName);
        if (clazz == null) return null;
        for (Class<?> c = clazz; c != null && c != Object.class; c = c.getSuperclass()) {
            for (var field : c.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) return Type.getDescriptor(field.getType());
            }
        }
        return null;
    }

    /** 不在扫描范围（原版 / 第三方）时用反射兜底；拿不到类返回 null */
    private static Class<?> loadClass(String internalName) {
        try {
            return Class.forName(internalName.replace('/', '.'), false, MixinTargetVerifier.class.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** 注解值取自 ASM：字符串是 String，数组是 List（@Inject#method 就是数组） */
    private static String stringValue(AnnotationNode node, String key) {
        if (node.values == null) return null;
        for (int i = 0; i + 1 < node.values.size(); i += 2) {
            if (!key.equals(node.values.get(i))) continue;
            Object value = node.values.get(i + 1);
            if (value == null) return null;
            if (value instanceof List<?> list) {
                return list.isEmpty() ? null : String.valueOf(list.get(0));
            }
            return String.valueOf(value);
        }
        return null;
    }

    private static List<String> mixinTargets(AnnotationNode mixin, ClassNode self, Map<String, ClassNode> nodes) {
        List<String> targets = new ArrayList<>();
        if (mixin.values == null) return targets;
        for (int i = 0; i + 1 < mixin.values.size(); i += 2) {
            String key = String.valueOf(mixin.values.get(i));
            Object value = mixin.values.get(i + 1);
            if ("value".equals(key) || "targets".equals(key)) {
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        targets.add(toInternalName(item));
                    }
                }
            }
        }
        if (targets.isEmpty()) {
            // @Mixin(TargetClass.class) 已由 value 覆盖；没有显式目标时跳过
            return targets;
        }
        return targets;
    }

    /**
     * ASM 里注解值有两种形态，必须分开处理：
     * <ul>
     *   <li>{@code @Mixin(Target.class)} → 值是 {@link Type}（形如 {@code Lnet/foo/Bar;}）；</li>
     *   <li>{@code @Mixin(targets = "net.foo.Bar$Inner")} → 值是**点号分隔的类名字符串**，
     *       直接丢给 {@link Type#getType(String)} 会抛 {@code Invalid descriptor}
     *       （曾经让整个校验任务崩掉，而不是报告问题）。</li>
     * </ul>
     */
    private static String toInternalName(Object value) {
        if (value instanceof Type type) {
            return type.getInternalName();
        }
        String name = String.valueOf(value);
        if (name.startsWith("L") && name.endsWith(";")) {
            return Type.getType(name).getInternalName();
        }
        return name.replace('.', '/');
    }

    private enum Result { OK, MISSING, MISMATCH, UNRESOLVED }

    /** 查找结果 + 目标方法真实的描述符（用于「写了描述符的注入点」逐字比对） */
    private record Lookup(Result result, String descriptor) {
    }

}
