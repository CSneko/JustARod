package org.cneko.justarod.client.screen;

import net.minecraft.text.Text;

import java.util.List;
import java.util.Random;

// 做吗？做！
public class Questions {
    public static final List<QuestionScreen.Question> QUESTIONS = List.of(
            new QuestionScreen.Question(
                    Text.of("正常人类体细胞中的染色体数量为"),
                    2,
                    Text.of("23条"),
                    Text.of("23对"),
                    Text.of("21对"),
                    Text.of("46对")
            ),
            new QuestionScreen.Question(
                    Text.of("若Aa与AA杂交，后代为AA的概率为"),
                    2,
                    Text.of("25%"),
                    Text.of("50%"),
                    Text.of("75%"),
                    Text.of("100%")
            ),
            new QuestionScreen.Question(Text.of("若AaBb与AABB杂交，子代有多少表现型"),
                    1,
                    Text.of("1种"),
                    Text.of("2种"),
                    Text.of("3种"),
                    Text.of("4种")
            ),
            new QuestionScreen.Question(Text.of("豌豆的高茎（D）对矮茎（d）为显性。将两株高茎豌豆杂交，子代中高茎与矮茎的比例为3:1。请问亲本的基因型是"),
                    3,
                    Text.of("DD x DD"),
                    Text.of("DD x dd"),
                    Text.of("Dd x Dd"),
                    Text.of("Dd x dd")
            ),
            new QuestionScreen.Question(Text.of("光合作用的光反应阶段为暗反应阶段直接提供的物质是"),
                    1,
                    Text.of("ATP和NADPH"),
                    Text.of("CO₂和H₂O"),
                    Text.of("O₂和葡萄糖"),
                    Text.of("叶绿素和酶")
            ),
            new QuestionScreen.Question(Text.of("兴奋在神经元之间传递时，信号转换的形式是"),
                    1,
                    Text.of("电信号→化学信号→电信号"),
                    Text.of("化学信号→电信号→化学信号"),
                    Text.of("电信号→电信号"),
                    Text.of("化学信号→化学信号")
            ),
            new QuestionScreen.Question(Text.of("在基因组中，基因的编码起始位点为"),
                    4,
                    Text.of("GAG"),
                    Text.of("GCT"),
                    Text.of("GAY"),
                    Text.of("ATG")
            ),
            new QuestionScreen.Question(Text.of("下列属于生态系统次级消费者的是"),
                    3,
                    Text.of("杂草"),
                    Text.of("蚂蚱"),
                    Text.of("青蛙"),
                    Text.of("分解者")
            ),
            new QuestionScreen.Question(Text.of("下列细胞器中，不含DNA的是"),
                    3,
                    Text.of("线粒体"),
                    Text.of("叶绿体"),
                    Text.of("核糖体"),
                    Text.of("细胞核")
            ),
            new QuestionScreen.Question(Text.of("在酶促反应中，温度升高10℃时，反应速率通常会提高1-2倍。这一现象主要与下列哪个理化因素有关"),
                    2,
                    Text.of("酶的空间结构永久性改变"),
                    Text.of("反应物分子热运动加快，有效碰撞频率增加"),
                    Text.of("酶的最适pH发生偏移"),
                    Text.of("反应体系的熵减小")
            ),
            new QuestionScreen.Question(Text.of("将红细胞置于0.9% NaCl溶液中形态不变，若改用1.5% NaCl溶液，细胞会发生"),
                    2,
                    Text.of("吸水胀破"),
                    Text.of("失水皱缩"),
                    Text.of("自由扩散速率加快"),
                    Text.of("主动运输增强")
            ),
            new QuestionScreen.Question(Text.of("在密闭容器中，酵母菌进行有氧呼吸时，下列哪项比值会逐渐减小？"),
                    1,
                    Text.of("O₂分压 / CO₂分压"),
                    Text.of("容器内温度 / 气体摩尔数"),
                    Text.of("气体总质量 / 容器体积"),
                    Text.of("CO₂体积 / O₂体积")
            ),
            new QuestionScreen.Question(Text.of("神经纤维受刺激时，Na⁺内流引发动作电位。此过程依赖于"),
                    1,
                    Text.of("细胞内外Na⁺浓度差和膜电位差"),
                    Text.of("载体蛋白的主动运输"),
                    Text.of("线粒体直接供能"),
                    Text.of("细胞膜磷脂的主动翻转")
            ),
            new QuestionScreen.Question(Text.of("某常染色体隐性遗传病在人群中的发病率为1/100。一对表型正常的夫妇已生育一个患病孩子，他们再生育一个正常孩子的概率为"),
                    2,
                    Text.of("1/4"),
                    Text.of("3/4"),
                    Text.of("3/8"),
                    Text.of("9/16")
            ),
                        new QuestionScreen.Question(Text.of("函数 f(x)=x^2−4x+3的最小值是（ ）"),
                    1,
                    Text.of("-1"),
                    Text.of("0"),
                    Text.of("1"),
                    Text.of("3")
            ),
            new QuestionScreen.Question(Text.of("过点 (1,2) 且与直线 2x−3y+1=0 平行的直线方程是（ ）"),
                    1,
                    Text.of("2x−3y+4=0"),
                    Text.of("3x+2y−7=0"),
                    Text.of("2x−3y−1=0"),
                    Text.of("2x+3y−8=0")
            ),
            new QuestionScreen.Question(Text.of("若 sin(θ)=3/5且 θ 为第二象限角，则tan(θ)=()"),
                    2,
                    Text.of("3/4"),
                    Text.of("-3/4"),
                    Text.of("4/3"),
                    Text.of("-4/3")
            ),
            new QuestionScreen.Question(Text.of("已知等差数列{a_n}中，a_3=5,a_7=13，则公差d=()"),
                    2,
                    Text.of("1"),
                    Text.of("2"),
                    Text.of("3"),
                    Text.of("4")
            ),
            new QuestionScreen.Question(Text.of("函数 f(x)=e^x+x 在x=0 处的导数是（ ）"),
                    3,
                    Text.of("0"),
                    Text.of("1"),
                    Text.of("2"),
                    Text.of("e")
            ),
            new QuestionScreen.Question(Text.of("复数 z=1+i（i 为虚数单位）的模是（ ）"),
                    1,
                    Text.of("√2"),
                    Text.of("2"),
                    Text.of("1"),
                    Text.of("0")
            ),
            new QuestionScreen.Question(Text.of("已知函数 f(x)=log_2 (4^x−2^(x+1)+3)的定义域为全体实数，则实数 k 的取值范围是（ ）"),
                    4,
                    Text.of(" (−∞,1]"),
                    Text.of("[1,+∞)"),
                    Text.of("(−∞,2]"),
                    Text.of("[2,+∞)")
            ),
            new QuestionScreen.Question(Text.of("向量 u=(1,2) 和 v=(3,−1) 的点积 u⋅v 等于"),
                    1,
                    Text.of("1"),
                    Text.of("-1"),
                    Text.of("5"),
                    Text.of("0")
            ),
            new QuestionScreen.Question(Text.of("设 A 是 3×2 矩阵，B 是 2×4 矩阵，则 AB 的维度是"),
                    3,
                    Text.of("2×4"),
                    Text.of("3×2"),
                    Text.of("3×4"),
                    Text.of("无法计算")
            ),
            new QuestionScreen.Question(Text.of("下列哪个集合是 R^2 的子空间？"),
                    3,
                    Text.of("{(x,y)∣x+y=1}"),
                    Text.of("{(x,y)∣x≥0,y≥0}"),
                    Text.of("{(x,y)∣y=2x}"),
                    Text.of("{(x,y)∣x^2+y^2=1}")
            ),
            new QuestionScreen.Question(Text.of("向量空间 R^3 的标准基是"),
                    2,
                    Text.of("{(1,0),(0,1)}"),
                    Text.of("{(1,0,0),(0,1,0),(0,0,1)}"),
                    Text.of("{(1,1,1),(0,1,1),(0,0,1)}"),
                    Text.of("{(1,2,3),(0,1,0)}")
            ),
             new QuestionScreen.Question(Text.of("若 u=(1,2)和 v=(3,k) 线性相关，则 k的值为"),
                    3,
                    Text.of("2"),
                    Text.of("4"),
                    Text.of("6"),
                    Text.of("8")
            ),
            new QuestionScreen.Question(Text.of("函数 f(x)=(x^2−1)/(x−1)在 x=1 处的极限是"),
                    3,
                    Text.of("0"),
                    Text.of("1"),
                    Text.of("2"),
                    Text.of("不存在")
            ),
            new QuestionScreen.Question(Text.of("设 f(x)=x^3+ax+b 与 x 轴相切于点 (1,0)，则 a+b 的值为（ ）"),
                    2,
                    Text.of("-2"),
                    Text.of("-1"),
                    Text.of("0"),
                    Text.of("1")
            ),
            new QuestionScreen.Question(Text.of("若 F(x) 是 f(x)=2x的“原函数”（即 F′(x)=f(x)），且 F(0)=1，则 F(2)的值是？"),
                    2,
                    Text.of("3"),
                    Text.of("5"),
                    Text.of("7"),
                    Text.of("9")
            ),
            new QuestionScreen.Question(Text.of("设 y 由方程 x^2+y^2=1隐式定义，则dy/dx为："),
                    2,
                    Text.of("x/y"),
                    Text.of("-x/y"),
                    Text.of("y/x"),
                    Text.of("-y/x")
            )
    );

    public static QuestionScreen.Question randomQuestion() {
        return QUESTIONS.get(new Random().nextInt(QUESTIONS.size()));
    }
}
