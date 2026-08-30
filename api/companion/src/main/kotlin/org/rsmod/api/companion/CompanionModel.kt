package org.rsmod.api.companion

public enum class CompanionClass {
    TANK,
    SUPPORT,
    DPS,
}

public enum class CompanionState {
    FOLLOWING,
    COMBAT,
    INCAPACITATED,
    DESPAWNED,
}

public data class CompanionTalentDefinition(
    public val id: String,
    public val companionClass: CompanionClass,
    public val tier: Int,
    public val maxRanks: Int = 5,
    public val pointsPerRank: Int = 1,
    public val effectKey: String,
    public val capstone: Boolean = false,
) {
    init {
        require(id.matches(Regex("[a-z0-9-]{2,48}")))
        require(tier in 1..8)
        require(maxRanks in 1..5)
        require(pointsPerRank > 0)
        require(effectKey.isNotBlank())
        require(!capstone || tier == 8)
    }
}

public data class CompanionTalent(
    public val definitionId: String,
    public val ranks: Int = 0,
) {
    init {
        require(definitionId.isNotBlank())
        require(ranks >= 0)
    }
}

public data class Companion(
    public val id: Long,
    public val ownerCharacterId: Long,
    public val slot: Int,
    public val name: String,
    public val companionClass: CompanionClass,
    public val npcId: Int,
    public val level: Int = 1,
    public val experience: Long = 0,
    public val talentPoints: Int = 3,
    public val talents: List<CompanionTalent> = emptyList(),
    public val active: Boolean = false,
    public val state: CompanionState = CompanionState.FOLLOWING,
    public val hitpoints: Int = 1,
    public val maximumHitpoints: Int = 1,
    public val incapacitatedUntilEpochMillis: Long? = null,
    public val encounterId: Long? = null,
    public val gearInstanceIds: List<Long> = emptyList(),
) {
    init {
        require(id >= 0L)
        require(ownerCharacterId > 0L)
        require(slot in 1..CompanionRules.MAX_SLOTS)
        require(name.length in CompanionRules.NAME_MIN_LENGTH..CompanionRules.NAME_MAX_LENGTH)
        require(npcId >= 0)
        require(level >= 1)
        require(experience >= 0L)
        require(talentPoints >= CompanionRules.BASE_TALENT_POINTS)
        require(hitpoints >= 0 && maximumHitpoints >= 1 && hitpoints <= maximumHitpoints)
        require(gearInstanceIds.distinct().size == gearInstanceIds.size)
        require(state != CompanionState.INCAPACITATED || hitpoints == 0)
        require(state != CompanionState.DESPAWNED || !active)
    }

    public fun withHitpoints(value: Int): Companion = copy(
        hitpoints = value.coerceIn(0, maximumHitpoints),
        state = if (value <= 0) CompanionState.INCAPACITATED else state,
        active = if (value <= 0) false else active,
    )
}

public object CompanionRules {
    public const val MAX_SLOTS: Int = 4
    public const val ACTIVE_LIMIT: Int = 1
    public const val BASE_TALENT_POINTS: Int = 3
    public const val NAME_MIN_LENGTH: Int = 1
    public const val NAME_MAX_LENGTH: Int = 12
    public const val INCAPACITATED_COOLDOWN_MILLIS: Long = 30_000L
    public val TIER_POINT_GATES: List<Int> = listOf(0, 10, 20, 30, 40, 50, 60, 70)

    public fun talentPointsForLevel(level: Int): Int {
        require(level >= 1)
        val early = if (level < 5) 0 else ((minOf(level, 50) - 5) / 5) + 1
        val mid = if (level < 51) 0 else ((minOf(level, 100) - 51) / 3) + 1
        val late = (level - 100).coerceAtLeast(0)
        return BASE_TALENT_POINTS + early + mid + late
    }

    public fun levelFromExperience(experience: Long): Int {
        require(experience >= 0L)
        var level = 1
        var remaining = experience
        while (level < 200 && remaining >= experienceToNextLevel(level)) {
            remaining -= experienceToNextLevel(level)
            level++
        }
        return level
    }

    public fun experienceToNextLevel(level: Int): Long {
        require(level >= 1)
        return 100L * level
    }

    public fun tierUnlocked(tier: Int, spentPoints: Int): Boolean {
        require(tier in 1..8 && spentPoints >= 0)
        return spentPoints >= TIER_POINT_GATES[tier - 1]
    }
}

public data class CompanionTarget(
    public val id: Int,
    public val isPlayer: Boolean,
    public val alive: Boolean,
    public val reachable: Boolean,
    public val distance: Int,
)

public data class CompanionCombatContext(
    public val ownerOnline: Boolean,
    public val ownerAlive: Boolean,
    public val sameRegion: Boolean,
    public val inWilderness: Boolean,
    public val ownerTarget: CompanionTarget?,
)

public data class CompanionAction(
    public val targetIds: List<Int>,
    public val role: CompanionClass,
    public val action: String,
    public val damageMultiplier: Double = 0.0,
    public val supportPower: Double = 0.0,
)
