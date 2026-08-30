package org.rsmod.content.other.devhub.configs

import org.rsmod.api.type.builders.interf.InterfaceBuilder
import org.rsmod.game.type.comp.ComponentTypeBuilder

/**
 * The dev-hub window, authored from scratch and packed into the cache (interface 1000).
 *
 * Layout: a 512x334 centered modal with a title bar (search button + close X on the right), a row
 * of six boxed tabs, a left-hand category list (12 rows), an 8x6 item grid on the right (populated
 * at runtime through clientscript 149 - see `DevHubScript`), and a pagination row underneath the
 * grid.
 *
 * Each tab is a button layer holding three children whose ids are deliberately ascending - box
 * rect, selected-state rect (`hide` baked on), label text - because children draw in component id
 * order, so the rects must take lower ids than the text to sit under it.
 *
 * Every constant below was harvested from decoded rev-239 vanilla components rather than guessed
 * (project method: decode the cache, never guess):
 * - Root sizing/centering copies `skill_guide_v2` 860:1 (512x334, both modes centered,
 *   `noClickThrough`).
 * - The close button copies the common vanilla close X: sprite 539 (hover 540), 26x23, baked op1,
 *   with clientscript 44 swapping the sprite on hover - e.g. 346:48, 362:84, 290:1.
 * - Buttons are type-0 layers with baked `events = 2` (op1) plus an `op` string, and text lives in
 *   a separate text component recolored on hover via clientscript 45 - the shape shopmain 300:8/9
 *   uses for its quantity buttons.
 * - Fonts/colors: b12 font 495, standard orange 0xFF981F, white hover 0xFFFFFF, border shade
 *   0x0E0E0C (harvested from 860:12).
 */
internal object DevHubInterfaceBuilder : InterfaceBuilder() {
    private const val TYPE_LAYER: Int = 0
    private const val TYPE_RECT: Int = 3
    private const val TYPE_TEXT: Int = 4
    private const val TYPE_GRAPHIC: Int = 5

    /** v1-style event mask with only op1 set - what every baked vanilla button carries. */
    private const val EVENTS_OP1: Int = 2

    /** Anchor modes harvested from vanilla comps: 0 = absolute, 1 = centered, 2 = far edge. */
    private const val MODE_CENTER: Int = 1
    private const val MODE_FAR: Int = 2

    /** Size modes: 0 = fixed, 1 = fill parent. */
    private const val SIZE_FILL: Int = 1

    private const val FONT_B12: Int = 495
    private const val COLOUR_ORANGE: Int = 0xFF981F
    private const val COLOUR_WHITE: Int = 0xFFFFFF
    private const val COLOUR_BORDER: Int = 0x0E0E0C
    private const val COLOUR_BACKGROUND: Int = 0x332E25
    private const val COLOUR_DIVIDER: Int = 0x5A5245
    private const val COLOUR_TAB: Int = 0x3A342A
    private const val COLOUR_TAB_SELECTED: Int = 0x554C3D

    private const val SPRITE_CLOSE: Int = 539
    private const val SPRITE_CLOSE_HOVER: Int = 540

    /** cs2 44: swaps a graphic component's sprite. `(component, graphic)` */
    private const val CS_SWAP_GRAPHIC: Int = 44

    /** cs2 45: recolors a text component. `(component, colour)` */
    private const val CS_RECOLOUR_TEXT: Int = 45

    /** The "this component" sentinel vanilla hover hooks pass as the component arg. */
    private const val SELF: Int = -2147483645

    private const val TAB_Y: Int = 34
    private const val TAB_WIDTH: Int = 78
    private const val TAB_HEIGHT: Int = 24
    private const val TAB_PITCH: Int = 82

    private const val CAT_X: Int = 8
    private const val CAT_Y: Int = 64
    private const val CAT_WIDTH: Int = 140
    private const val CAT_HEIGHT: Int = 20
    private const val CAT_PITCH: Int = 22

    const val CATEGORY_ROWS: Int = 12

    const val GRID_COLUMNS: Int = 8
    const val GRID_ROWS: Int = 6

    init {
        build("dev_hub:root") {
            type = TYPE_LAYER
            width = 512
            height = 334
            xMode = MODE_CENTER
            yMode = MODE_CENTER
            noClickThrough = true
        }
        build("dev_hub:bg") {
            type = TYPE_RECT
            layer = DevHubComponents.root.packed
            widthMode = SIZE_FILL
            heightMode = SIZE_FILL
            fill = true
            colour1 = COLOUR_BACKGROUND
        }
        build("dev_hub:border") {
            type = TYPE_RECT
            layer = DevHubComponents.root.packed
            widthMode = SIZE_FILL
            heightMode = SIZE_FILL
            fill = false
            colour1 = COLOUR_BORDER
        }
        build("dev_hub:title") {
            type = TYPE_TEXT
            layer = DevHubComponents.root.packed
            y = 8
            height = 24
            widthMode = SIZE_FILL
            textFont = FONT_B12
            textAlignH = 1
            textAlignV = 1
            textShadow = true
            colour1 = COLOUR_ORANGE
            text = "Developer Hub"
        }
        build("dev_hub:close") {
            type = TYPE_GRAPHIC
            layer = DevHubComponents.root.packed
            x = 6
            y = 6
            width = 26
            height = 23
            xMode = MODE_FAR
            graphic = SPRITE_CLOSE
            events = EVENTS_OP1
            op = arrayOf("Close")
            onMouseOver = arrayOf(CS_SWAP_GRAPHIC, SELF, SPRITE_CLOSE_HOVER)
            onMouseLeave = arrayOf(CS_SWAP_GRAPHIC, SELF, SPRITE_CLOSE)
        }

        build("dev_hub:search_button") {
            buttonLayer(
                parent = DevHubComponents.root.packed,
                xPos = 38,
                yPos = 8,
                w = 64,
                h = 23,
                opText = "Search",
                hoverTextPacked = DevHubComponents.search_button_text.packed,
            )
            xMode = MODE_FAR
        }
        build("dev_hub:search_button_text") {
            buttonText(parent = DevHubComponents.search_button.packed, label = "Search")
        }

        val tabs =
            listOf(
                "items" to "Items",
                "equipment" to "Equipment",
                "gear" to "Gear",
                "teleports" to "Teleports",
                "skills" to "Skills",
                "misc" to "Misc",
            )
        for ((index, tab) in tabs.withIndex()) {
            val (slug, label) = tab
            val buttonPacked = DevHubComponents.tabButtons[index].packed
            val textPacked = DevHubComponents.tabTexts[index].packed
            build("dev_hub:tab_$slug") {
                buttonLayer(
                    parent = DevHubComponents.root.packed,
                    xPos = 8 + index * TAB_PITCH,
                    yPos = TAB_Y,
                    w = TAB_WIDTH,
                    h = TAB_HEIGHT,
                    opText = "Select",
                    hoverTextPacked = textPacked,
                )
            }
            build("dev_hub:tab_${slug}_box") { fillRect(parent = buttonPacked, shade = COLOUR_TAB) }
            build("dev_hub:tab_${slug}_sel") {
                fillRect(parent = buttonPacked, shade = COLOUR_TAB_SELECTED, hidden = true)
            }
            build("dev_hub:tab_${slug}_text") { buttonText(parent = buttonPacked, label = label) }
        }

        build("dev_hub:divider") {
            type = TYPE_RECT
            layer = DevHubComponents.root.packed
            x = 152
            y = 62
            width = 1
            height = 264
            fill = true
            colour1 = COLOUR_DIVIDER
        }

        for (index in 0 until CATEGORY_ROWS) {
            val buttonPacked = DevHubComponents.catButtons[index].packed
            val textPacked = DevHubComponents.catTexts[index].packed
            build("dev_hub:cat$index") {
                buttonLayer(
                    parent = DevHubComponents.root.packed,
                    xPos = CAT_X,
                    yPos = CAT_Y + index * CAT_PITCH,
                    w = CAT_WIDTH,
                    h = CAT_HEIGHT,
                    opText = "Select",
                    hoverTextPacked = textPacked,
                )
            }
            build("dev_hub:cat${index}_box") { fillRect(parent = buttonPacked, shade = COLOUR_TAB) }
            build("dev_hub:cat${index}_sel") {
                fillRect(parent = buttonPacked, shade = COLOUR_TAB_SELECTED, hidden = true)
            }
            build("dev_hub:cat${index}_text") {
                buttonText(parent = buttonPacked, label = "", alignLeft = true)
            }
        }

        build("dev_hub:grid") {
            type = TYPE_LAYER
            layer = DevHubComponents.root.packed
            x = 160
            y = 68
            width = 336
            height = 216
        }

        build("dev_hub:page_prev") {
            buttonLayer(
                parent = DevHubComponents.root.packed,
                xPos = 160,
                yPos = 292,
                w = 40,
                h = 24,
                opText = "Previous page",
                hoverTextPacked = DevHubComponents.page_prev_text.packed,
            )
        }
        build("dev_hub:page_prev_text") {
            // A literal `<` starts a formatting tag in the client's text renderer and draws
            // nothing; `<lt>` is the escape for it. (Verified in-client: the button hovered and
            // clicked fine, but its label was invisible.)
            buttonText(parent = DevHubComponents.page_prev.packed, label = "<lt>")
        }
        build("dev_hub:page_next") {
            buttonLayer(
                parent = DevHubComponents.root.packed,
                xPos = 456,
                yPos = 292,
                w = 40,
                h = 24,
                opText = "Next page",
                hoverTextPacked = DevHubComponents.page_next_text.packed,
            )
        }
        build("dev_hub:page_next_text") {
            buttonText(parent = DevHubComponents.page_next.packed, label = "<gt>")
        }
        build("dev_hub:page_label") {
            type = TYPE_TEXT
            layer = DevHubComponents.root.packed
            x = 200
            y = 292
            width = 256
            height = 24
            textFont = FONT_B12
            textAlignH = 1
            textAlignV = 1
            textShadow = true
            colour1 = COLOUR_ORANGE
        }
    }

    private fun ComponentTypeBuilder.buttonLayer(
        parent: Int,
        xPos: Int,
        yPos: Int,
        w: Int,
        h: Int,
        opText: String,
        hoverTextPacked: Int,
    ) {
        type = TYPE_LAYER
        layer = parent
        x = xPos
        y = yPos
        width = w
        height = h
        events = EVENTS_OP1
        op = arrayOf(opText)
        onMouseOver = arrayOf(CS_RECOLOUR_TEXT, hoverTextPacked, COLOUR_WHITE)
        onMouseLeave = arrayOf(CS_RECOLOUR_TEXT, hoverTextPacked, COLOUR_ORANGE)
    }

    private fun ComponentTypeBuilder.fillRect(parent: Int, shade: Int, hidden: Boolean = false) {
        type = TYPE_RECT
        layer = parent
        widthMode = SIZE_FILL
        heightMode = SIZE_FILL
        fill = true
        colour1 = shade
        if (hidden) {
            hide = true
        }
    }

    private fun ComponentTypeBuilder.buttonText(
        parent: Int,
        label: String,
        alignLeft: Boolean = false,
    ) {
        type = TYPE_TEXT
        layer = parent
        widthMode = SIZE_FILL
        heightMode = SIZE_FILL
        textFont = FONT_B12
        textAlignH = if (alignLeft) 0 else 1
        textAlignV = 1
        textShadow = true
        colour1 = COLOUR_ORANGE
        text = label
    }
}
