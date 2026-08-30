package org.rsmod.api.equipment.instance

import java.util.SplittableRandom

public class EquipmentRollPolicy(
    public val affixes: List<EquipmentAffixDefinition>,
    public val jackpotEffects: List<String>,
    public val socketType: String = "Universal",
) {
    init {
        require(EquipmentRarity.entries.sumOf(EquipmentRarity::weightBasisPoints) == EquipmentRarity.TOTAL_WEIGHT_BASIS_POINTS)
        require(affixes.map(EquipmentAffixDefinition::id).distinct().size == affixes.size)
        require(jackpotEffects.isNotEmpty() && jackpotEffects.none(String::isBlank))
        require(jackpotEffects.none(String::isBlank) && socketType.isNotBlank())
    }

    public fun roll(templateObj: Int, category: EquipmentCategory, seed: Long, source: String, tier: EquipmentTier = EquipmentTier.Bronze): EquipmentInstance {
        val random = SplittableRandom(seed)
        val rarity = selectRarity(random)
        val eligible = affixes.filter { category in it.categories && rarity.ordinal >= it.minRarity.ordinal }
        val count = random.nextInt(rarity.affixMin, rarity.affixMax + 1)
        val selected = selectAffixes(random, eligible, count, rarity, category, tier)
        val sockets = List(random.nextInt(rarity.socketMin, rarity.socketMax + 1)) { EquipmentSocket(it, socketType) }
        val effectCandidates = jackpotEffects.toMutableList()
        val effects = buildList {
            repeat(minOf(rarity.uniqueEffectCount, effectCandidates.size)) {
                add(effectCandidates.removeAt(random.nextInt(effectCandidates.size)))
            }
        }
        return EquipmentInstance(0L, templateObj, category, rarity, seed, selected, sockets, effects, source, tier)
    }

    private fun selectRarity(random: SplittableRandom): EquipmentRarity {
        val roll = random.nextInt(EquipmentRarity.TOTAL_WEIGHT_BASIS_POINTS)
        var total = 0
        return EquipmentRarity.entries.first { total += it.weightBasisPoints; roll < total }
    }

    private fun selectAffixes(random: SplittableRandom, eligible: List<EquipmentAffixDefinition>, count: Int, rarity: EquipmentRarity, category: EquipmentCategory, tier: EquipmentTier): List<EquipmentAffixRoll> {
        val candidates = eligible.toMutableList()
        val families = HashSet<String>(count)
        val result = ArrayList<EquipmentAffixRoll>(count)
        while (candidates.isNotEmpty() && result.size < count) {
            val definition = candidates.removeAt(random.nextInt(candidates.size))
            if (!families.add(definition.family)) continue
            val magnitude = if (definition.stat == EquipmentStat.MaximumHealth) {
                EquipmentBalance.armorHealth(tier, rarity)
            } else random.nextInt(definition.minMagnitude, definition.maxMagnitude + 1)
            result += EquipmentAffixRoll(result.size, definition.id, definition.family, definition.stat, definition.unit, definition.polarity, magnitude)
        }
        require(result.size == count) { "Not enough unique affix families for rarity roll." }
        return result
    }
}
