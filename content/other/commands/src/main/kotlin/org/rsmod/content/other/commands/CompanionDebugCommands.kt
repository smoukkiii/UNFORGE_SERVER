package org.rsmod.content.other.commands

import jakarta.inject.Inject
import org.rsmod.api.companion.CompanionClass
import org.rsmod.api.companion.CompanionFrameLayout
import org.rsmod.api.companion.CompanionFrameLayoutStore
import org.rsmod.api.companion.CompanionService
import org.rsmod.api.player.output.mes
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Admin-only smoke-test commands; recruitment in normal play must be wired to Contract Scrolls. */
public class CompanionDebugCommands @Inject constructor(
    private val companions: CompanionService,
    private val layouts: CompanionFrameLayoutStore,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("companioncreate", "Create a test companion", ::create) {
            invalidArgs = "Use as ::companioncreate name class npcId hp"
        }
        onCommand("companionlist", "List owned companions", ::list)
        onCommand("companionactive", "Activate an owned companion", ::activate) {
            invalidArgs = "Use as ::companionactive companionId"
        }
        onCommand("companionxp", "Add companion experience", ::addExperience) {
            invalidArgs = "Use as ::companionxp companionId amount"
        }
        onCommand("companiontalent", "Spend one companion talent point", ::talent) {
            invalidArgs = "Use as ::companiontalent companionId talentId"
        }
        onCommand("companionframe", "Set companion frame layout", ::frame) {
            invalidArgs = "Use as ::companionframe x y scale"
        }
    }

    private fun create(cheat: Cheat) = with(cheat) {
        val name = args[0]
        val role = runCatching { CompanionClass.valueOf(args[1].uppercase()) }.getOrElse {
            player.mes("Class must be TANK, SUPPORT or DPS")
            return
        }
        val npcId = args[2].toIntOrNull() ?: run { player.mes("Invalid NPC id"); return }
        val hp = args[3].toIntOrNull() ?: run { player.mes("Invalid hitpoints"); return }
        val ownerId = player.characterId.toLong()
        val companion = companions.recruit(ownerId, 1, name, role, npcId, hp)
        player.mes("Created ${companion.name} #${companion.id} (${companion.companionClass})")
    }

    private fun list(cheat: Cheat) = with(cheat) {
        val owned = companions.owned(player.characterId.toLong())
        if (owned.isEmpty()) player.mes("No companions")
        owned.forEach { player.mes("#${it.id} slot ${it.slot}: ${it.name} ${it.companionClass} Lv${it.level} ${it.state}") }
    }

    private fun activate(cheat: Cheat) = with(cheat) {
        val id = args[0].toLongOrNull() ?: run { player.mes("Invalid companion id"); return }
        val companion = companions.activate(player.characterId.toLong(), id)
        player.mes("Active companion: ${companion.name}")
    }

    private fun addExperience(cheat: Cheat) = with(cheat) {
        val id = args[0].toLongOrNull() ?: run { player.mes("Invalid companion id"); return }
        val amount = args[1].toLongOrNull() ?: run { player.mes("Invalid experience"); return }
        val companion = companions.addExperience(player.characterId.toLong(), id, amount)
        player.mes("${companion.name}: level ${companion.level}, ${companion.talentPoints} talent points")
    }

    private fun talent(cheat: Cheat) = with(cheat) {
        val id = args[0].toLongOrNull() ?: run { player.mes("Invalid companion id"); return }
        val companion = companions.allocateTalent(player.characterId.toLong(), id, args[1])
        player.mes("${companion.name}: ${args[1]} allocated")
    }

    private fun frame(cheat: Cheat) = with(cheat) {
        val x = args[0].toIntOrNull() ?: run { player.mes("Invalid x"); return }
        val y = args[1].toIntOrNull() ?: run { player.mes("Invalid y"); return }
        val scale = args[2].toIntOrNull() ?: run { player.mes("Invalid scale"); return }
        val layout = layouts.set(player.characterId.toLong(), CompanionFrameLayout(x, y, scalePercent = scale))
        player.mes("Companion frame saved at ${layout.x},${layout.y} scale ${layout.scalePercent}%")
    }
}
