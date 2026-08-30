package org.rsmod.api.equipment.instance

public object EquipmentAffixCatalog {
    public const val VERSION: Int = 2
    private val wearable = EquipmentCategory.entries.filterTo(linkedSetOf()) { it != EquipmentCategory.Custom && it != EquipmentCategory.Companion }
    private val weapons = setOf(EquipmentCategory.RightHand)
    private val hpSlots = setOf(EquipmentCategory.Head, EquipmentCategory.Back, EquipmentCategory.Neck, EquipmentCategory.Torso, EquipmentCategory.LeftHand, EquipmentCategory.Legs, EquipmentCategory.Hands, EquipmentCategory.Feet, EquipmentCategory.Ring)
    public val definitions: List<EquipmentAffixDefinition> = listOf(
        EquipmentAffixDefinition("stab-power", "attack-stab", EquipmentStat.AttackStab, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("slash-power", "attack-slash", EquipmentStat.AttackSlash, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("crush-power", "attack-crush", EquipmentStat.AttackCrush, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("defence-power", "defence", EquipmentStat.DefenceStab, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("strength-power", "strength", EquipmentStat.Strength, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("ranged-power", "ranged", EquipmentStat.AttackRanged, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("magic-power", "magic", EquipmentStat.AttackMagic, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, categories = wearable),
        EquipmentAffixDefinition("armor-health", "health", EquipmentStat.MaximumHealth, ModifierUnit.Flat, ModifierPolarity.Boon, 0, 0, categories = hpSlots),
        EquipmentAffixDefinition("weapon-speed", "attack-speed", EquipmentStat.AttackSpeedPercent, ModifierUnit.BasisPoints, ModifierPolarity.Boon, 1, 1, minRarity = EquipmentRarity.Epic, categories = weapons),
        // Stable ids retained so already persisted instances remain loadable.
        EquipmentAffixDefinition("keen", "attack-stab-legacy", EquipmentStat.AttackStab, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 4, categories = wearable),
        EquipmentAffixDefinition("savage", "strength-legacy", EquipmentStat.Strength, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 4, categories = wearable),
        EquipmentAffixDefinition("warding", "defence-stab-legacy", EquipmentStat.DefenceStab, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 5, categories = wearable),
        EquipmentAffixDefinition("swift", "attack-speed-legacy", EquipmentStat.AttackSpeedTicks, ModifierUnit.Ticks, ModifierPolarity.Mixed, 1, 1, minRarity = EquipmentRarity.Epic, categories = weapons),
        EquipmentAffixDefinition("blessed", "prayer-legacy", EquipmentStat.Prayer, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 3, categories = wearable),
        EquipmentAffixDefinition("deadeye", "ranged-legacy", EquipmentStat.AttackRanged, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 4, categories = wearable),
        EquipmentAffixDefinition("mystic", "magic-legacy", EquipmentStat.AttackMagic, ModifierUnit.Flat, ModifierPolarity.Boon, 1, 4, categories = wearable),
    )
    public val byId: Map<String, EquipmentAffixDefinition> = definitions.associateBy(EquipmentAffixDefinition::id)
    public fun policy(): EquipmentRollPolicy = EquipmentRollPolicy(definitions, listOf("jackpot-surge", "executioner", "immortal-core"))
}
