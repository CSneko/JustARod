package org.cneko.justarod.item

import org.cneko.justarod.api.NetWorkingRodData

class NetWorkingRodItem: SelfUsedItem(Settings().maxCount(1).maxDamage(NetWorkingRodData.MAX_DAMAGE).component(JRComponents.SPEED, NetWorkingRodData.SPEED).component(JRComponents.USED_TIME_MARK, 0)) {

}