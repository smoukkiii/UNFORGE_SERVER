package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.rsmod.api.companion.Companion
import org.rsmod.api.companion.CompanionCombatContext
import org.rsmod.api.companion.CompanionGearCalculator
import org.rsmod.api.companion.CompanionService
import org.rsmod.api.companion.CompanionTarget
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.registry.npc.NpcRegistry
import org.rsmod.api.equipment.instance.EquipmentInstanceRegistry
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.HitType
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Bridges the owner-bound companion domain to the existing r239 NPC lifecycle.
 * Companions are ordinary server NPCs with no autonomous hunt/wander target.
 */
public class CompanionRuntimeScript @Inject constructor(
    private val companions: CompanionService,
    private val players: PlayerList,
    private val npcList: NpcList,
    private val npcTypes: NpcTypeList,
    private val npcRegistry: NpcRegistry,
    private val npcRepository: NpcRepository,
    private val npcHitModifier: NpcHitModifier,
    private val equipmentInstances: EquipmentInstanceRegistry,
) : PluginScript() {
    private val spawned = mutableMapOf<Long, Npc>()

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.EngineLoginReady> {
            companions.load(player.characterId.toLong())
            sync(player)
        }
        onPlayerLogout { despawn(player.characterId.toLong()) }
        onEvent<GameLifecycle.LateCycle> { players.forEach(::sync) }
    }

    private fun sync(player: Player) {
        if (!player.isValidTarget()) {
            despawn(player.characterId.toLong())
            return
        }
        val ownerId = player.characterId.toLong()
        val active = companions.owned(ownerId).firstOrNull { it.active }
        if (active == null) {
            despawn(ownerId)
            return
        }
        val npc = spawned[active.id] ?: spawn(player, active) ?: return
        if (npc.isInvisible) npcRegistry.reveal(npc)
        if (npc.coords.chebyshevDistance(player.coords) > 6) {
            npc.walk(player.coords.translateX(-1))
        }

        val ownerTarget = player.interaction?.let { interaction ->
            val target = (interaction as? org.rsmod.game.interact.InteractionNpc)?.target
            target?.takeIf { it.isValidTarget() && it.id != npc.id }?.let {
                CompanionTarget(it.slotId, false, it.hitpoints > 0, true, it.coords.chebyshevDistance(player.coords))
            }
        }
        val nearby = npcRegistry.findAll(ZoneKey.from(player.coords))
            .filter { it !== npc && it !== ownerTargetNpc(player) && it.isValidTarget() }
            .map { CompanionTarget(it.slotId, false, it.hitpoints > 0, true, it.coords.chebyshevDistance(player.coords)) }
            .filter { it.distance <= 8 }
            .toList()
        val action = companions.tick(
            ownerId,
            active.id,
            CompanionCombatContext(true, player.isValidTarget(), true, false, ownerTarget),
            nearby,
        ) ?: return
        if (npc.actionDelay > player.currentMapClock) return
        val target = action.targetIds.asSequence().mapNotNull(npcList::get).firstOrNull() ?: return
        val gear = CompanionGearCalculator.calculate(active, equipmentInstances)
        val damage = (npc.strengthLvl * gear.damageMultiplier * action.damageMultiplier / 20.0).toInt().coerceAtLeast(1)
        target.queueHit(npc, delay = 1, type = HitType.Melee, damage = damage, modifier = npcHitModifier)
        npc.actionDelay = player.currentMapClock + gear.attackDelay
    }

    private fun ownerTargetNpc(player: Player): Npc? =
        (player.interaction as? org.rsmod.game.interact.InteractionNpc)?.target

    private fun spawn(player: Player, companion: Companion): Npc? {
        val type = npcTypes[companion.npcId] ?: return null
        val npc = Npc(type, player.coords.translateX(-1)).apply {
            mode = org.rsmod.game.entity.npc.NpcMode.None
            hitpoints = CompanionGearCalculator.calculate(companion, equipmentInstances).maximumHitpoints
            hideAllOps()
        }
        npcRepository.add(npc, Int.MAX_VALUE)
        spawned[companion.id] = npc
        return npc
    }

    private fun despawn(ownerId: Long) {
        val ownedIds = companions.owned(ownerId).map { it.id }.toSet()
        spawned.filterKeys { it in ownedIds }.values.toList().forEach { npc ->
            if (npc.isSlotAssigned) npcRepository.del(npc, Int.MAX_VALUE)
        }
        spawned.entries.removeIf { it.key in ownedIds }
    }
}
