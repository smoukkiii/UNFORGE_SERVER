package org.rsmod.api.companion

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

public interface CompanionContractSource {
    public fun consume(ownerCharacterId: Long, slot: Int): Boolean
}

public object NoopCompanionContractSource : CompanionContractSource {
    override fun consume(ownerCharacterId: Long, slot: Int): Boolean = true
}

public class CompanionService(
    private val contracts: CompanionContractSource = NoopCompanionContractSource,
    private val persistence: CompanionPersistence = NoopCompanionPersistence,
) {
    private val values = ConcurrentHashMap<Long, MutableList<Companion>>()

    public fun recruit(
        ownerCharacterId: Long,
        slot: Int,
        name: String,
        companionClass: CompanionClass,
        npcId: Int,
        maximumHitpoints: Int,
    ): Companion {
        validateName(name)
        require(slot in 1..CompanionRules.MAX_SLOTS)
        require(maximumHitpoints > 0)
        val owned = values.computeIfAbsent(ownerCharacterId) { mutableListOf() }
        require(owned.none { it.slot == slot }) { "Companion slot $slot is already occupied" }
        require(slot == 1 || owned.any { it.slot == slot - 1 }) { "Companion slots must be unlocked in order" }
        check(contracts.consume(ownerCharacterId, slot)) { "Companion Contract Scroll is required" }
        val companion = Companion(
            id = nextId(ownerCharacterId, slot),
            ownerCharacterId = ownerCharacterId,
            slot = slot,
            name = name.trim(),
            companionClass = companionClass,
            npcId = npcId,
            hitpoints = maximumHitpoints,
            maximumHitpoints = maximumHitpoints,
        )
        owned += companion
        persistence.save(companion)
        return companion
    }

    public fun activate(ownerCharacterId: Long, companionId: Long): Companion {
        val owned = owned(ownerCharacterId)
        val selected = owned.first { it.id == companionId }
        require(selected.state != CompanionState.INCAPACITATED && selected.state != CompanionState.DESPAWNED)
        val updated = owned.map { it.copy(active = it.id == companionId) }
        replace(ownerCharacterId, updated)
        return updated.first { it.id == companionId }
    }

    public fun addExperience(ownerCharacterId: Long, companionId: Long, amount: Long): Companion {
        require(amount >= 0L)
        val current = find(ownerCharacterId, companionId)
        val xp = current.experience + amount
        val level = CompanionRules.levelFromExperience(xp)
        val updated = current.copy(level = level, experience = xp, talentPoints = CompanionRules.talentPointsForLevel(level))
        replace(ownerCharacterId, owned(ownerCharacterId).map { if (it.id == companionId) updated else it })
        persistence.save(updated)
        return updated
    }

    public fun equipGear(ownerCharacterId: Long, companionId: Long, instanceIds: List<Long>): Companion {
        require(instanceIds.size <= 8)
        require(instanceIds.all { it > 0L })
        require(instanceIds.distinct().size == instanceIds.size)
        val current = find(ownerCharacterId, companionId)
        val updated = current.copy(gearInstanceIds = instanceIds)
        replace(ownerCharacterId, owned(ownerCharacterId).map { if (it.id == companionId) updated else it })
        persistence.save(updated)
        return updated
    }

    public fun allocateTalent(ownerCharacterId: Long, companionId: Long, talentId: String): Companion {
        val current = find(ownerCharacterId, companionId)
        val definition = CompanionTalentCatalog.definition(talentId)
        require(definition.companionClass == current.companionClass)
        val spent = current.talents.sumOf { talent ->
            CompanionTalentCatalog.definition(talent.definitionId).pointsPerRank * talent.ranks
        }
        require(CompanionRules.tierUnlocked(definition.tier, spent))
        require(spent < current.talentPoints)
        val old = current.talents.firstOrNull { it.definitionId == talentId }
        require(old == null || old.ranks < definition.maxRanks)
        val talents = current.talents.toMutableList()
        if (old == null) talents += CompanionTalent(talentId, 1)
        else talents[talents.indexOf(old)] = old.copy(ranks = old.ranks + 1)
        val updated = current.copy(talents = talents)
        replace(ownerCharacterId, owned(ownerCharacterId).map { if (it.id == companionId) updated else it })
        persistence.save(updated)
        return updated
    }

    public fun tick(ownerCharacterId: Long, companionId: Long, context: CompanionCombatContext, nearbyTargets: List<CompanionTarget>): CompanionAction? {
        val companion = find(ownerCharacterId, companionId)
        if (!companion.active || companion.state == CompanionState.INCAPACITATED || companion.state == CompanionState.DESPAWNED) return null
        val ownerTarget = context.ownerTarget
        val validOwnerTarget = ownerTarget != null && ownerTarget.alive && !ownerTarget.isPlayer && ownerTarget.reachable && ownerTarget.distance <= 8
        val inCombat = context.ownerOnline && context.ownerAlive && context.sameRegion && !context.inWilderness && validOwnerTarget
        val state = if (inCombat) CompanionState.COMBAT else CompanionState.FOLLOWING
        val updated = companion.copy(state = state)
        replace(ownerCharacterId, owned(ownerCharacterId).map { if (it.id == companionId) updated else it })
        if (!inCombat) return null
        val targets = (listOfNotNull(ownerTarget) + nearbyTargets)
            .filter { it.alive && !it.isPlayer && it.reachable && it.distance <= 8 }
            .distinctBy { it.id }
            .take(3)
        if (targets.isEmpty()) return null
        return when (companion.companionClass) {
            CompanionClass.TANK -> CompanionAction(targets.take(1).map { it.id }, CompanionClass.TANK, "TAUNT_AND_ATTACK", damageMultiplier = 0.75)
            CompanionClass.SUPPORT -> CompanionAction(targets.take(1).map { it.id }, CompanionClass.SUPPORT, "SUPPORT_THEN_ATTACK", damageMultiplier = 0.60, supportPower = 1.0)
            CompanionClass.DPS -> CompanionAction(targets.map { it.id }, CompanionClass.DPS, "ATTACK_OWNER_TARGET", damageMultiplier = 1.25)
        }
    }

    public fun damage(ownerCharacterId: Long, companionId: Long, amount: Int, nowEpochMillis: Long): Companion {
        require(amount >= 0)
        val current = find(ownerCharacterId, companionId)
        if (current.state == CompanionState.INCAPACITATED || current.state == CompanionState.DESPAWNED) return current
        val hp = max(0, current.hitpoints - amount)
        val updated = if (hp == 0) current.copy(
            hitpoints = 0,
            active = false,
            state = CompanionState.INCAPACITATED,
            incapacitatedUntilEpochMillis = nowEpochMillis + CompanionRules.INCAPACITATED_COOLDOWN_MILLIS,
        ) else current.copy(hitpoints = hp)
        replace(ownerCharacterId, owned(ownerCharacterId).map { if (it.id == companionId) updated else it })
        persistence.save(updated)
        return updated
    }

    public fun resummon(ownerCharacterId: Long, companionId: Long, nowEpochMillis: Long): Companion {
        val current = find(ownerCharacterId, companionId)
        require(current.state == CompanionState.INCAPACITATED)
        require((current.incapacitatedUntilEpochMillis ?: Long.MAX_VALUE) <= nowEpochMillis)
        val updated = current.copy(
            active = true,
            state = CompanionState.FOLLOWING,
            hitpoints = current.maximumHitpoints,
            incapacitatedUntilEpochMillis = null,
        )
        replace(ownerCharacterId, owned(ownerCharacterId).map { when {
            it.id == companionId -> updated
            it.active -> it.copy(active = false)
            else -> it
        } })
        persistence.save(updated)
        return updated
    }

    public fun owned(ownerCharacterId: Long): List<Companion> = values[ownerCharacterId]?.toList().orEmpty()

    /** Loads persisted companions once for a logged-in owner without changing stable ids. */
    public fun load(ownerCharacterId: Long) {
        if (values.containsKey(ownerCharacterId)) return
        val loaded = persistence.load(ownerCharacterId)
        if (loaded.isNotEmpty()) values[ownerCharacterId] = loaded.toMutableList()
    }

    /** Restores a character segment produced by the account loading pipeline. */
    public fun restore(ownerCharacterId: Long, companions: List<Companion>) {
        require(companions.all { it.ownerCharacterId == ownerCharacterId })
        require(companions.map { it.slot }.distinct().size == companions.size)
        require(companions.count { it.active } <= CompanionRules.ACTIVE_LIMIT)
        values[ownerCharacterId] = companions.toMutableList()
    }

    private fun find(ownerCharacterId: Long, companionId: Long): Companion = owned(ownerCharacterId).first { it.id == companionId }
    private fun replace(ownerCharacterId: Long, companions: List<Companion>) { values[ownerCharacterId] = companions.toMutableList() }
    private fun nextId(owner: Long, slot: Int): Long = (owner shl 8) or slot.toLong()

    private fun validateName(name: String) {
        require(name.trim().length in CompanionRules.NAME_MIN_LENGTH..CompanionRules.NAME_MAX_LENGTH)
        require(name.trim().matches(Regex("[A-Za-z0-9 _-]+")))
    }
}
