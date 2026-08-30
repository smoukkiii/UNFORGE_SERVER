package org.rsmod.content.areas.tutorialisland

import jakarta.inject.Inject
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.boolVarp
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onEvent
import org.rsmod.content.areas.tutorialisland.configs.tutorial_components
import org.rsmod.content.areas.tutorialisland.configs.tutorial_interfaces
import org.rsmod.content.areas.tutorialisland.configs.tutorial_varbits
import org.rsmod.content.areas.tutorialisland.configs.tutorial_varps
import org.rsmod.content.areas.tutorialisland.progression.TutorialFocusGuidance
import org.rsmod.content.areas.tutorialisland.progression.TutorialProgression
import org.rsmod.content.areas.tutorialisland.progression.TutorialStep
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Starts or resumes Learning the Ropes for accounts that have not completed Tutorial Island.
 *
 * Runs on [SessionStateEvent.EngineLoginReady] so the gameframe toplevel is already open before the
 * character creator modal or tutorial helper overlay attaches. Brand-new accounts open
 * `player_design` (679) after `tutorial_overlay` (614), matching live OSRS first-login order. Past
 * Experience uses `tutorial_player_experience` (929).
 * [org.rsmod.content.areas.tutorialisland.scripts.ui.TutorialCharacterScript] advances the flow
 * once appearance is confirmed.
 */
class TutorialIslandBootstrap
@Inject
constructor(
    private val progression: TutorialProgression,
    private val focus: TutorialFocusGuidance,
    private val eventBus: EventBus,
) : PluginScript() {
    private var Player.tutCompleted by boolVarp(tutorial_varps.tut2_completed)
    private var Player.designBodyType by intVarBit(tutorial_varbits.player_design_bodytype)
    private var Player.transmitPronouns by intVarBit(tutorial_varbits.settings_transmit_pronouns)

    override fun ScriptContext.startup() {
        onEvent<SessionStateEvent.EngineLoginReady> { player.onLoginReady() }
    }

    private fun Player.onLoginReady() {
        if (tutCompleted) {
            return
        }
        ClientScripts.tutorialDefaultSettings(this)
        when (progression.current(this)) {
            TutorialStep.CharCreate -> {
                // Overlay + side-panel lock + guide chrome first (OSRS: if_opensub 614, then
                // closesubs).
                progression.setStep(this, TutorialStep.CharCreate)
                // Live keeps the local player soft-hidden while the creator is open.
                appearance.softHidden = true
                // Seed UI varbits so Body Type A / He start depressed (client CS2 reads these).
                designBodyType = appearance.bodyType
                transmitPronouns = appearance.pronoun
                // Live: hide noclick → midi_song_v2 62 → show noclick → CS2 2524 → open 679 → CS2
                // 1974.
                focus.playCharacterCreateSting(this)
                ifOpenMainModal(tutorial_interfaces.player_design, eventBus)
                ifSetEvents(tutorial_components.player_design_pronouns, 1..3, IfEvent.Op1)
                // Capture sends mesoverlay after the modal is open.
                focus.setControlGuide(this, TutorialStep.CharCreate)
            }
            TutorialStep.ExpSelect -> {
                progression.resume(this)
                focus.openPastExperience(this)
            }
            else -> progression.resume(this)
        }
    }
}
