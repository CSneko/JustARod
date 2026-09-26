package org.cneko.justarod.item

import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/*
帕秋莉手册（Patchouli）联动：
手册物品 justarod:justarod_guide 由 Patchouli 根据 book.json 自动注册，
这里只负责构造与识别它，全程不直接引用 Patchouli 的类（仅用反射探测是否安装），
因此在未安装 Patchouli 时也能安全运行。
 */
object JRGuideBook {
    const val BOOK_PATH: String = "justarod_guide"

    /** 手册 ID：对应 data/justarod/patchouli_books/justarod_guide/book.json */
    val BOOK_ID: Identifier = Identifier.fromNamespaceAndPath("justarod", BOOK_PATH)

    // Patchouli 通用的指南书物品与其「书」数据组件
    private val PATCHOULI_BOOK_ITEM_ID = Identifier.fromNamespaceAndPath("patchouli", "guide_book")
    private val PATCHOULI_BOOK_COMPONENT_ID = Identifier.fromNamespaceAndPath("patchouli", "book")

    /** 反射探测 Patchouli 是否已安装，避免类加载依赖 */
    val PATCHOULI_LOADED: Boolean by lazy {
        try {
            Class.forName("vazkii.patchouli.api.PatchouliAPI")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private fun patchouliBookComponent(): DataComponentType<Identifier>? {
        if (!PATCHOULI_LOADED) return null
        @Suppress("UNCHECKED_CAST")
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(PATCHOULI_BOOK_COMPONENT_ID)
                as? DataComponentType<Identifier>
    }

    /**
     * 构造一本属于本模组的指南书。
     * Patchouli 未安装时返回空物品堆。
     */
    fun createGuideBookStack(): ItemStack {
        if (!PATCHOULI_LOADED) return ItemStack.EMPTY
        val guideBookItem: Item = BuiltInRegistries.ITEM.getValue(PATCHOULI_BOOK_ITEM_ID)
        if (guideBookItem == Items.AIR) return ItemStack.EMPTY
        val bookComponent = patchouliBookComponent() ?: return ItemStack.EMPTY
        val stack = ItemStack(guideBookItem)
        stack.set(bookComponent, BOOK_ID)
        return stack
    }

    /** 判断该物品堆是否为本模组的指南书 */
    fun isOurGuideBook(stack: ItemStack): Boolean {
        if (!PATCHOULI_LOADED || stack.isEmpty) return false
        val guideBookItem = BuiltInRegistries.ITEM.getValue(PATCHOULI_BOOK_ITEM_ID)
        if (guideBookItem == Items.AIR) return false
        if (!stack.`is`(guideBookItem)) return false
        val bookComponent = patchouliBookComponent() ?: return false
        return stack.get(bookComponent) == BOOK_ID
    }
}
