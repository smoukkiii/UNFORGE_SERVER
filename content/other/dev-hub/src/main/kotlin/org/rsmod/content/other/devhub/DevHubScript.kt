package org.rsmod.content.other.devhub

import jakarta.inject.Inject
import java.util.WeakHashMap
import kotlin.math.max
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.UpdateInventory
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.other.devhub.configs.DevHubInterfaceBuilder.GRID_COLUMNS
import org.rsmod.content.other.devhub.configs.DevHubInterfaceBuilder.GRID_ROWS
import org.rsmod.content.other.devhub.configs.devhub_components
import org.rsmod.content.other.devhub.configs.devhub_interfaces
import org.rsmod.content.other.devhub.configs.devhub_invs
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.obj.isAssociatedWith
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.objtx.TransactionResult
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Per-player hub view state. Nothing here persists, and nothing client-side reads it. */
private class DevHubSession(
    var tab: DevHubTab,
    var category: DevHubCategory,
    var page: Int,
    var searchQuery: String? = null,
    var searchResults: List<Int>? = null,
)

class DevHubScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val objTypes: ObjTypeList,
    private val invTypes: InvTypeList,
    private val statTypes: StatTypeList,
) : PluginScript() {
    private val categorizer by lazy { ObjCategorizer(objTypes) }
    private val curatedGear by lazy { CuratedGear(objTypes) }
    private val teleports by lazy { DevHubTeleports(objTypes) }

    /** Weak keys so a player who logs out mid-session cannot be leaked by this map. */
    private val sessions = WeakHashMap<Player, DevHubSession>()

    override fun ScriptContext.startup() {
        onCommand("hub") {
            desc = "Open the developer hub"
            modLevel = modlevels.admin
            cheat { protectedAccess.launch(player) { openHub() } }
        }
        onIfClose(devhub_interfaces.dev_hub) { player.cleanup() }
        onIfModalButton(devhub_components.close) { ifClose() }
        onIfModalButton(devhub_components.search_button) { searchClick() }
        onIfModalButton(devhub_components.grid) { gridClick(it) }
        onIfModalButton(devhub_components.page_prev) { pageClick(-1) }
        onIfModalButton(devhub_components.page_next) { pageClick(1) }
        for ((index, button) in devhub_components.tabButtons.withIndex()) {
            onIfModalButton(button) { tabClick(index) }
        }
        for ((index, button) in devhub_components.catButtons.withIndex()) {
            onIfModalButton(button) { catClick(index) }
        }
    }

    private suspend fun ProtectedAccess.openHub() {
        val session = DevHubSession(DevHubTab.Items, DevHubCategory.Potions, page = 0)
        sessions[player] = session
        val hubInv = hubInv()
        invTransmit(hubInv)
        ifOpenMainModal(devhub_interfaces.dev_hub)
        ifSetEvents(
            devhub_components.grid,
            0 until hubInv.size,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
        refresh(session, hubInv)
    }

    private fun Player.cleanup() {
        sessions.remove(this)
        stopInvTransmit(invMap.getOrPut(invTypes[devhub_invs.dev_hub_shop]))
    }

    private fun ProtectedAccess.hubInv(): Inventory = inv(devhub_invs.dev_hub_shop)

    private fun ProtectedAccess.refresh(session: DevHubSession, hubInv: Inventory) {
        val tab = session.tab
        ifSetText(devhub_components.title, "Developer Hub - ${tab.label}")
        for ((index, text) in devhub_components.tabTexts.withIndex()) {
            val label = DevHubTab.entries[index].label
            ifSetText(text, if (index == tab.ordinal) label.selected() else label)
        }
        for ((index, sel) in devhub_components.tabSels.withIndex()) {
            ifSetHide(sel, hide = index != tab.ordinal)
        }
        val search = session.searchResults
        val categories = DevHubCategory.forTab(tab)
        for ((index, text) in devhub_components.catTexts.withIndex()) {
            val category = categories.getOrNull(index)
            val label = category?.label ?: ""
            val highlight = category == session.category && search == null
            ifSetText(text, if (highlight) label.selected() else label)
            // Unused rows hide their box outright; the sel rect only shows on the selection.
            ifSetHide(devhub_components.catBoxes[index], hide = category == null)
            ifSetHide(devhub_components.catSels[index], hide = !highlight)
        }
        val objs =
            when {
                search != null -> search
                session.category.tab == DevHubTab.Gear -> curatedGear[session.category]
                else -> categorizer[session.category]
            }
        val pageCount = max(1, (objs.size + PAGE_SIZE - 1) / PAGE_SIZE)
        session.page = session.page.coerceIn(0, pageCount - 1)
        val start = session.page * PAGE_SIZE
        for (slot in 0 until hubInv.size) {
            val objId = objs.getOrNull(start + slot)
            val objType = objId?.let { objTypes[it] }
            hubInv[slot] = objType?.let { InvObj(it, count = STOCK_COUNT) }
        }
        val pageText = "Page ${session.page + 1} / $pageCount"
        val pageLabel =
            if (search != null) {
                "Search: \"${session.searchQuery.orEmpty().escapeTags()}\" - $pageText"
            } else {
                pageText
            }
        ifSetText(devhub_components.page_label, pageLabel)

        // The cs-149 grid is a snapshot: dynamic children do not repaint reliably off the regular
        // end-of-tick partial transmits alone (verified in-client - switching category left the old
        // page rendered). Push the inv state now, then re-run the init script so it rebuilds from
        // the data that just arrived; the immediate writes keep the two in order.
        UpdateInventory.updateInvPartial(player, hubInv)
        hubInv.clearModifiedSlots()
        // Note the arg naming: cs 153 (which cs 149 defers to) treats the FIRST count as columns -
        // the shop side inv passes (4, 7) and renders 4 wide by 7 tall. rsmod's parameter names
        // have them backwards. Decoded pitch is 36x32 per cell.
        interfaceInvInit(
            inv = hubInv,
            target = devhub_components.grid,
            objRowCount = GRID_COLUMNS,
            objColCount = GRID_ROWS,
            op1 = "Take-1",
            op2 = "Take-5",
            op3 = "Take-50",
            op4 = "Take-X",
            op5 = "Examine",
        )
    }

    private fun ProtectedAccess.tabClick(index: Int) {
        val session = sessions[player] ?: return
        val tab = DevHubTab[index] ?: return
        session.tab = tab
        session.category = DevHubCategory.forTab(tab).first()
        session.page = 0
        session.clearSearch()
        refresh(session, hubInv())
    }

    private suspend fun ProtectedAccess.catClick(index: Int) {
        val session = sessions[player] ?: return
        val category = DevHubCategory.forTab(session.tab).getOrNull(index) ?: return
        session.category = category
        session.page = 0
        session.clearSearch()
        refresh(session, hubInv())
        // The menu-driven tabs use their category rows as launchers: the row stays highlighted
        // while its menu is up, and the grid stays empty (the categorizer seeds these as empty).
        when (session.tab) {
            DevHubTab.Teleports -> teleportMenu(category)
            DevHubTab.Skills -> skillEditor(category)
            else -> Unit
        }
    }

    private fun DevHubSession.clearSearch() {
        searchQuery = null
        searchResults = null
    }

    private suspend fun ProtectedAccess.searchClick() {
        val session = sessions[player] ?: return
        val query = stringDialog("Search for:").trim().lowercase()
        if (query.length < 2) {
            mes("Search with at least two characters.")
            return
        }
        val results =
            categorizer.allIds
                .mapNotNull { objTypes[it] }
                .filter { it.lowercaseName.contains(query) }
                .sortedBy { it.lowercaseName }
                .map { it.id }
        if (results.isEmpty()) {
            mes("Nothing in the store matches \"$query\".")
            return
        }
        session.searchQuery = query
        session.searchResults = results
        session.page = 0
        refresh(session, hubInv())
    }

    private fun ProtectedAccess.pageClick(delta: Int) {
        val session = sessions[player] ?: return
        session.page += delta
        refresh(session, hubInv())
    }

    private suspend fun ProtectedAccess.gridClick(button: IfModalButton) {
        if (sessions[player] == null) {
            return
        }
        val invObj = hubInv()[button.comsub] ?: return
        val clientObj = button.obj
        if (clientObj == null || !clientObj.isAssociatedWith(invObj)) {
            return
        }
        val type = objTypes[invObj.id] ?: return
        when (button.op) {
            IfButtonOp.Op1 -> take(type, 1)
            IfButtonOp.Op2 -> take(type, 5)
            IfButtonOp.Op3 -> take(type, 50)
            IfButtonOp.Op4 -> {
                val count = countDialog("Take how many?")
                if (count > 0) {
                    take(type, count)
                }
            }
            else -> mes(type.desc.ifBlank { type.name })
        }
    }

    private fun ProtectedAccess.take(type: UnpackedObjType, count: Int) {
        val result = player.invAdd(player.inv, type, count, strict = false)
        if (result.err is TransactionResult.RestrictedDummyitem) {
            mes("That item can't be spawned.")
            return
        }
        val added = result.completed()
        if (added <= 0) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("Took $added x ${type.name}.")
    }

    /**
     * Settles the client's own menu close before this coroutine opens anything else.
     *
     * The client closes the menu itself after a selection and reports it with a `CloseModal`
     * packet, which the main process consumes _after_ the input phase - i.e. after the selection
     * already resumed this coroutine. That deferred close runs `Player.ifClose`, whose
     * `cancelActiveDialog` **aborts any coroutine suspended on a menu or input dialog** - so a
     * [countDialog] (or second [menu]) opened in the same cycle as the selection is killed the
     * moment the close processes, with no exception logged. (Verified in-client: the skill editor's
     * count dialog did nothing on submit, while the same flow passed in integration tests, which
     * never send `CloseModal`.)
     *
     * Two steps defuse it: close the menu server-side now so the mainmodal state already matches
     * the client, then park on a [delay] - a delay suspension is condition-based, not a dialog, so
     * the deferred `ifClose` cannot abort it, and the pending close is consumed against an
     * already-empty modal stack. The delay must span **two** cycles: coroutines resume at the
     * _start_ of a cycle, before the deferred close is processed, so a one-cycle delay would open
     * the next dialog in the very cycle whose main phase still aborts it.
     */
    private suspend fun ProtectedAccess.settleMenuClose() {
        ifClose()
        delay(2)
    }

    private suspend fun ProtectedAccess.teleportMenu(category: DevHubCategory) {
        val destinations = teleports.forCategory(category) ?: return
        val index = menu(category.label, hotkeys = false, choices = destinations.map { it.label })
        val dest = destinations.getOrNull(index) ?: return
        settleMenuClose()
        telejump(dest.coords)
        mes("Teleported to ${dest.label}.")
    }

    private suspend fun ProtectedAccess.skillEditor(category: DevHubCategory) {
        val stats = DevHubSkillGroups.forCategory(category, statTypes.values.toList()).orEmpty()
        if (stats.isEmpty()) {
            return
        }
        val statIndex =
            menu("Set which skill?", hotkeys = false, choices = stats.map { it.displayName })
        val stat = stats.getOrNull(statIndex) ?: return
        settleMenuClose()
        val minLevel = max(1, stat.minLevel)
        val level = countDialog("Set ${stat.displayName} to level ($minLevel-99):")
        if (level <= 0) {
            return
        }
        val target = level.coerceIn(minLevel, 99)
        player.setDevHubStatLevel(stat, target)
        mes("Set ${stat.displayName} to level $target.")
    }

    private fun String.selected(): String = "<col=ffffff>$this</col>"

    /**
     * Escapes `<`/`>` for the client's text renderer, which treats a literal `<` as an unterminated
     * tag and draws nothing. Single-pass on purpose: chained `replace` calls corrupt themselves
     * because the escape tokens contain the characters being escaped.
     */
    private fun String.escapeTags(): String = buildString {
        for (ch in this@escapeTags) {
            when (ch) {
                '<' -> append("<lt>")
                '>' -> append("<gt>")
                else -> append(ch)
            }
        }
    }

    private companion object {
        private const val PAGE_SIZE: Int = GRID_COLUMNS * GRID_ROWS
        private const val STOCK_COUNT: Int = 100_000_000
    }
}
