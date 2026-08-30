package org.rsmod.api.equipment.instance

import org.rsmod.game.type.obj.Wearpos

public object EquipmentCategoryResolver {
    public fun resolve(wearpos: Wearpos?): EquipmentCategory =
        when (wearpos) {
            Wearpos.Hat -> EquipmentCategory.Head
            Wearpos.Back -> EquipmentCategory.Back
            Wearpos.Front -> EquipmentCategory.Neck
            Wearpos.RightHand -> EquipmentCategory.RightHand
            Wearpos.Torso -> EquipmentCategory.Torso
            Wearpos.LeftHand -> EquipmentCategory.LeftHand
            Wearpos.Legs -> EquipmentCategory.Legs
            Wearpos.Hands -> EquipmentCategory.Hands
            Wearpos.Feet -> EquipmentCategory.Feet
            Wearpos.Ring -> EquipmentCategory.Ring
            Wearpos.Quiver -> EquipmentCategory.Quiver
            Wearpos.Arms, Wearpos.Head, Wearpos.Jaw, null -> EquipmentCategory.Custom
        }
}