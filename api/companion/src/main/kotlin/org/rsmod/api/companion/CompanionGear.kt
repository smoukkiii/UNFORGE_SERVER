package org.rsmod.api.companion

import org.rsmod.api.equipment.instance.EquipmentInstance
import org.rsmod.api.equipment.instance.EquipmentInstanceRegistry
import org.rsmod.api.equipment.instance.EquipmentStat

public data class CompanionCombatStats(
    public val maximumHitpoints: Int,
    public val damageMultiplier: Double,
    public val supportPower: Double,
    public val attackDelay: Int,
)

/** Converts the same EquipmentInstance affixes used by players into companion combat bonuses. */
public object CompanionGearCalculator {
    public fun calculate(companion: Companion, registry: EquipmentInstanceRegistry): CompanionCombatStats {
        val items = companion.gearInstanceIds.mapNotNull(registry::get)
        val maximumHealth = items.sumAffixes(EquipmentStat.MaximumHealth)
        val damage = items.sumAffixes(EquipmentStat.DamageMax) +
            items.sumAffixes(EquipmentStat.AverageHit) + items.sumAffixes(EquipmentStat.Strength)
        val support = items.sumAffixes(EquipmentStat.LifeSteal) + items.sumAffixes(EquipmentStat.EffectiveHealth)
        val cooldown = items.sumAffixes(EquipmentStat.Cooldown)
        return CompanionCombatStats(
            maximumHitpoints = (companion.maximumHitpoints + maximumHealth).coerceAtLeast(1),
            damageMultiplier = (1.0 + damage / 100.0).coerceAtLeast(0.1),
            supportPower = (1.0 + support / 100.0).coerceAtLeast(0.0),
            attackDelay = (4 - cooldown / 10).coerceIn(2, 10),
        )
    }

    private fun List<EquipmentInstance>.sumAffixes(stat: EquipmentStat): Int =
        sumOf { item -> item.affixes.filter { it.stat == stat }.sumOf { it.magnitude } }
}
