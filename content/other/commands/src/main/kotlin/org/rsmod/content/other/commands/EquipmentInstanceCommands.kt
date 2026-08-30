package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.rsmod.api.db.gateway.GameDbManager
import org.rsmod.api.db.gateway.model.GameDbResult
import org.rsmod.api.equipment.instance.EquipmentAffixCatalog
import org.rsmod.api.equipment.instance.EquipmentCategoryResolver
import org.rsmod.api.equipment.instance.EquipmentInstanceRegistry
import org.rsmod.api.equipment.instance.EquipmentInstanceRepository
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.type.symbols.name.NameMapping
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.Wearpos
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class EquipmentInstanceCommands @Inject constructor(
    private val db: GameDbManager,
    private val repository: EquipmentInstanceRepository,
    private val registry: EquipmentInstanceRegistry,
    private val players: PlayerList,
    private val objTypes: ObjTypeList,
    private val names: NameMapping,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("invequiproll", "Create deterministic wearable instance", ::roll)
    }

    private fun roll(cheat: Cheat) = with(cheat) {
        val rawName = args.getOrNull(0) ?: "bronze_sword"
        val seed = args.getOrNull(1)?.toLongOrNull() ?: 239L
        val normalized = rawName.replace("-", "_")
        val typeId = normalized.toIntOrNull() ?: names.objs[normalized]
        if (typeId == null) { player.mes("Unknown object: $rawName"); return }
        val type = objTypes[typeId] ?: run { player.mes("Object does not exist: $typeId"); return }
        val wearpos = Wearpos[type.wearpos1] ?: run { player.mes("Object is not wearable: $rawName"); return }
        val uid = player.uid
        val rolled = EquipmentAffixCatalog.policy().roll(type.id, EquipmentCategoryResolver.resolve(wearpos), seed, "owner-command")
        db.request({ connection -> GameDbResult.Ok(repository.create(connection, rolled)) }) { result ->
            when (result) {
                is GameDbResult.Ok -> {
                    registry.put(result.value)
                    uid.resolve(players)?.let { target ->
                        target.invAdd(target.inv, type, count = 1, instanceId = result.value.instanceId, strict = false)
                        target.mes("Created ${result.value.rarity.name} ${type.name} instance #${result.value.instanceId}")
                    }
                }
                is GameDbResult.Err -> uid.resolve(players)?.mes("Equipment roll failed")
            }
        }
    }
}