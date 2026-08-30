package org.rsmod.content.areas.city.lumbridge.configs

import org.rsmod.api.type.builders.npc.NpcBuilder
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.util.CompactableIntArray

internal object LumbridgeR239BossNpc : NpcBuilder() {
    init {
        build("r239_test_boss") {
            name = "R239 Test Boss"
            desc = "A configurable r239 boss encounter."
            size = 2
            models = CompactableIntArray(24457, 24488, 24434, 24478, 24441, 24448)
            head = CompactableIntArray(intArrayOf(24210))
            readyAnim = 6181
            walkAnim = 6180
            resizeH = 256
            resizeV = 256
            op[0] = "Attack"
            attack = 60
            strength = 70
            defence = 55
            hitpoints = 250
            attackRange = 1
            huntRange = 8
            respawnRate = 60
        }
    }
}

internal object LumbridgeR239BossRefs : NpcReferences() {
    val r239_test_boss = find("r239_test_boss")
}
