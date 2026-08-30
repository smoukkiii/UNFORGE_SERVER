package org.rsmod.content.other.devhub.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias devhub_interfaces = DevHubInterfaces

typealias devhub_components = DevHubComponents

object DevHubInterfaces : InterfaceReferences() {
    val dev_hub = find("dev_hub")
}

object DevHubComponents : ComponentReferences() {
    val root = find("dev_hub:root")
    val title = find("dev_hub:title")
    val close = find("dev_hub:close")
    val grid = find("dev_hub:grid")
    val page_prev = find("dev_hub:page_prev")
    val page_prev_text = find("dev_hub:page_prev_text")
    val page_next = find("dev_hub:page_next")
    val page_next_text = find("dev_hub:page_next_text")
    val page_label = find("dev_hub:page_label")
    val search_button = find("dev_hub:search_button")
    val search_button_text = find("dev_hub:search_button_text")

    /** One entry per [org.rsmod.content.other.devhub.DevHubTab], in enum order. */
    val tabButtons =
        listOf(
            find("dev_hub:tab_items"),
            find("dev_hub:tab_equipment"),
            find("dev_hub:tab_gear"),
            find("dev_hub:tab_teleports"),
            find("dev_hub:tab_skills"),
            find("dev_hub:tab_misc"),
        )

    val tabBoxes =
        listOf(
            find("dev_hub:tab_items_box"),
            find("dev_hub:tab_equipment_box"),
            find("dev_hub:tab_gear_box"),
            find("dev_hub:tab_teleports_box"),
            find("dev_hub:tab_skills_box"),
            find("dev_hub:tab_misc_box"),
        )

    val tabSels =
        listOf(
            find("dev_hub:tab_items_sel"),
            find("dev_hub:tab_equipment_sel"),
            find("dev_hub:tab_gear_sel"),
            find("dev_hub:tab_teleports_sel"),
            find("dev_hub:tab_skills_sel"),
            find("dev_hub:tab_misc_sel"),
        )

    val tabTexts =
        listOf(
            find("dev_hub:tab_items_text"),
            find("dev_hub:tab_equipment_text"),
            find("dev_hub:tab_gear_text"),
            find("dev_hub:tab_teleports_text"),
            find("dev_hub:tab_skills_text"),
            find("dev_hub:tab_misc_text"),
        )

    val catButtons = List(DevHubInterfaceBuilder.CATEGORY_ROWS) { find("dev_hub:cat$it") }

    val catBoxes = List(DevHubInterfaceBuilder.CATEGORY_ROWS) { find("dev_hub:cat${it}_box") }

    val catSels = List(DevHubInterfaceBuilder.CATEGORY_ROWS) { find("dev_hub:cat${it}_sel") }

    val catTexts = List(DevHubInterfaceBuilder.CATEGORY_ROWS) { find("dev_hub:cat${it}_text") }
}
