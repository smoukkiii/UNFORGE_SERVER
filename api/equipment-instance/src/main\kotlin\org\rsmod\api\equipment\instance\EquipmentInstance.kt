package org.rsmod.api.equipment.instance

public data class EquipmentAffixDefinition(
    public val id: String,
    public val family: String,
    public val stat: EquipmentStat,
    public val unit: ModifierUnit,
    public val polarity: ModifierPolarity,
    public val minMagnitude: Int,
    public val maxMagnitude: Int,
    public val minRarity: EquipmentRarity = EquipmentRarity.Uncommon,
    public val categories: Set<EquipmentCategory>,
) {
    init {
        require(id.isNotBlank() && family.isNotBlank())
        require(minMagnitude <= maxMagnitude)
        require(categories.isNotEmpty())
    }
}

public data class EquipmentAffixRoll(
    public val slot: Int,
    public val definitionId: String,
    public val family: String,
    public val stat: EquipmentStat,
    public val unit: ModifierUnit,
    public val polarity: ModifierPolarity,
    public val magnitude: Int,
)

public data class EquipmentSocket(
    public val slot: Int,
    public val type: String,
    public val socketedObj: Int? = null,
    public val magnitude: Int = 0,
)

public enum class EquipmentTier(public val value: Int) {
    Bronze(1), Iron(2), Steel(3), Black(4), Mithril(5), Adamant(6), Rune(7), Dragon(8), Bandos(9), Torva(10);
}

public data class EquipmentInstance(
    public val instanceId: Long,
    public val templateObj: Int,
    public val category: EquipmentCategory,
    public val rarity: EquipmentRarity,
    public val rollSeed: Long,
    public val affixes: List<EquipmentAffixRoll>,
    public val sockets: List<EquipmentSocket>,
    public val uniqueEffectIds: List<String>,
    public val source: String,
    public val tier: EquipmentTier = EquipmentTier.Bronze,
    public val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    public val balanceVersion: Int = CURRENT_BALANCE_VERSION,
    public val itemLevel: Int = 1,
    public val quality: Int = 100,
    public val lockedAffixSlots: Set<Int> = emptySet(),
    public val reforgeCount: Int = 0,
    public val reforgeHistory: List<String> = emptyList(),
) {
    init {
        require(instanceId >= 0L && templateObj >= 0 && source.isNotBlank())
        require(itemLevel >= 1 && quality in 0..100)
        require(lockedAffixSlots.all { it in affixes.indices })
        require(reforgeCount >= 0)
        require(affixes.map(EquipmentAffixRoll::family).distinct().size == affixes.size)
        require(affixes.size in rarity.affixMin..rarity.affixMax)
        require(sockets.size in rarity.socketMin..rarity.socketMax)
        require(uniqueEffectIds.size == rarity.uniqueEffectCount)
        require(uniqueEffectIds.none(String::isBlank) && uniqueEffectIds.distinct().size == uniqueEffectIds.size)
    }

    public companion object {
        public const val CURRENT_SCHEMA_VERSION: Int = 2
        public const val CURRENT_BALANCE_VERSION: Int = 2
        public const val UNPERSISTED_INSTANCE_ID: Long = 0L
    }
}
