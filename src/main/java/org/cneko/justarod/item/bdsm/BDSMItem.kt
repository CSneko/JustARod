package org.cneko.justarod.item.bdsm

import net.minecraft.item.Item

/*
这个的话... 名字叫禁言口罩吧... 其实就是... 口求
戴上去的话... 根本说不出话诶，哼哼唧唧的♡
对方看来的话，甚至还有点... 可爱呢？
哎呀，戴久了要流口水的喵~
 */
class BallMouthItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.ballMouth },
    { e, v -> e.ballMouth = v },
    "对方已经有禁言口罩了哦~",
    "成功插入禁言口罩~"
)

/*
喂喂，你还在想什么不正经的用途呢
 */
class EarplugItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.earplug },
    { e, v -> e.earplug = v },
    "对方已经有耳塞了哦~",
    "成功插入耳塞~"
)

/*
我没试过...
 */
class BindingRopeItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.bundled },
    { e, v -> e.bundled = v },
    "对方已经有封禁绳了哦~",
    "成功插入封禁绳~"
)

class ElectricShockDeviceItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.electricShock },
    { e, v -> e.electricShock = v },
    "对方已经有电击器了哦~",
    "成功插入电击器~",
    20 * 60 * 20
)

class EyePatchItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.eyePatch },
    { e, v -> e.eyePatch = v },
    "对方已经有眼罩了哦~",
    "成功插入眼罩~"
)

/*
大哥哥，喝茶~
 */
class HandcuffesItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.handcuffed },
    { e, v -> e.handcuffed = v },
    "对方已经有手铐了哦~",
    "成功插入手铐~"
)

class ShacklesItem(settings: Settings) : AbstractBDSMItem(
    settings,
    { it.shackled },
    { e, v -> e.shackled = v },
    "对方已经有脚镣了哦~",
    "成功插入脚镣~"
)

/*
听说你们小南梁喜欢这个？
 */
class NoMatingPlz(settings: Item.Settings): AbstractBDSMItem(
    settings,
    { it.noMatingPlz },
    { e, v -> e.noMatingPlz = v },
    "对方已经有繁殖锁定~",
    "成功插入禁止繁殖锁定~"
)
