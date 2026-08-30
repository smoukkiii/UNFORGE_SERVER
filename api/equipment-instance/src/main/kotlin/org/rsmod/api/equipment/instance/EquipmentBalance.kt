package org.rsmod.api.equipment.instance

public object EquipmentBalance {
    public const val VERSION: Int = 2
    private val percentRanges = mapOf(
        EquipmentRarity.Uncommon to (100..2000), EquipmentRarity.Rare to (1000..4000),
        EquipmentRarity.Epic to (2000..6000), EquipmentRarity.Legendary to (3000..9000),
        EquipmentRarity.Mythic to (4000..10000), EquipmentRarity.Jackpot to (5000..15000),
    )
    public fun bonusPercentRange(rarity: EquipmentRarity): IntRange = percentRanges.getValue(rarity)
    public fun armorHealth(tier: EquipmentTier, rarity: EquipmentRarity): Int = tier.value * rarity.ordinal.coerceAtLeast(1) * 10
}
