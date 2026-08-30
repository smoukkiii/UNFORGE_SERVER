package org.rsmod.api.equipment.instance

import jakarta.inject.Inject
import java.sql.Statement
import org.rsmod.api.db.DatabaseConnection

public class EquipmentInstanceRepository @Inject constructor() {
    public fun create(connection: DatabaseConnection, value: EquipmentInstance): EquipmentInstance {
        require(value.instanceId == 0L)
        val effects = value.uniqueEffectIds.joinToString(",")
        val id = connection.prepareStatement("INSERT INTO equipment_instances (template_obj, rarity, roll_seed, schema_version, balance_version, source, category, unique_effect_id, unique_effect_ids, item_tier, item_level, quality, locked_affix_slots, reforge_count, reforge_history) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS).use { s ->
            s.setInt(1, value.templateObj); s.setString(2, value.rarity.name); s.setLong(3, value.rollSeed); s.setInt(4, value.schemaVersion); s.setInt(5, value.balanceVersion); s.setString(6, value.source); s.setString(7, value.category.name); s.setString(8, value.uniqueEffectIds.firstOrNull()); s.setString(9, effects); s.setInt(10, value.tier.value); s.setInt(11, value.itemLevel); s.setInt(12, value.quality); s.setString(13, value.lockedAffixSlots.sorted().joinToString(",")); s.setInt(14, value.reforgeCount); s.setString(15, value.reforgeHistory.joinToString("\n")); s.executeUpdate(); s.generatedKeys.use { k -> check(k.next()); k.getLong(1) }
        }
        connection.prepareStatement("INSERT INTO equipment_instance_affixes (equipment_instance_id, slot, affix_id, magnitude) VALUES (?, ?, ?, ?)").use { s -> value.affixes.forEach { a -> s.setLong(1,id); s.setInt(2,a.slot); s.setString(3,a.definitionId); s.setInt(4,a.magnitude); s.addBatch() }; s.executeBatch() }
        connection.prepareStatement("INSERT INTO equipment_instance_sockets (equipment_instance_id, slot, socket_type, socketed_obj, magnitude) VALUES (?, ?, ?, ?, ?)").use { s -> value.sockets.forEach { x -> s.setLong(1,id); s.setInt(2,x.slot); s.setString(3,x.type); if (x.socketedObj == null) s.setNull(4, java.sql.Types.INTEGER) else s.setInt(4,x.socketedObj); s.setInt(5,x.magnitude); s.addBatch() }; s.executeBatch() }
        return value.copy(instanceId = id)
    }

    public fun load(connection: DatabaseConnection, id: Long): EquipmentInstance? {
        val core = connection.prepareStatement("SELECT template_obj, rarity, roll_seed, schema_version, balance_version, source, category, unique_effect_id, unique_effect_ids, item_tier, item_level, quality, locked_affix_slots, reforge_count, reforge_history FROM equipment_instances WHERE id = ?").use { s -> s.setLong(1,id); s.executeQuery().use { rs -> if (!rs.next()) return null; Core(rs.getInt(1), EquipmentRarity.valueOf(rs.getString(2)), rs.getLong(3), rs.getInt(4), rs.getInt(5), rs.getString(6), EquipmentCategory.valueOf(rs.getString(7)), rs.getString(8), rs.getString(9), rs.getInt(10), rs.getInt(11), rs.getInt(12), rs.getString(13), rs.getInt(14), rs.getString(15)) } }
        val affixes = connection.prepareStatement("SELECT slot, affix_id, magnitude FROM equipment_instance_affixes WHERE equipment_instance_id = ? ORDER BY slot").use { s -> s.setLong(1,id); s.executeQuery().use { rs -> buildList { while(rs.next()) { val d=EquipmentAffixCatalog.byId.getValue(rs.getString(2)); add(EquipmentAffixRoll(rs.getInt(1),d.id,d.family,d.stat,d.unit,d.polarity,rs.getInt(3))) } } } }
        val sockets = connection.prepareStatement("SELECT slot, socket_type, socketed_obj, magnitude FROM equipment_instance_sockets WHERE equipment_instance_id = ? ORDER BY slot").use { s -> s.setLong(1,id); s.executeQuery().use { rs -> buildList { while(rs.next()) { val obj=rs.getInt(3).takeUnless { rs.wasNull() }; add(EquipmentSocket(rs.getInt(1),rs.getString(2),obj,rs.getInt(4))) } } } }
        val effects = if (core.effectIds.isBlank()) core.legacyEffect?.let(::listOf).orEmpty() else core.effectIds.split(',').filter(String::isNotBlank)
        return EquipmentInstance(id,core.templateObj,core.category,core.rarity,core.seed,affixes,sockets,effects,core.source,EquipmentTier.entries.firstOrNull { it.value==core.tier } ?: EquipmentTier.Bronze,core.schemaVersion,core.balanceVersion,core.itemLevel,core.quality,core.locked.split(',').filter(String::isNotBlank).map(String::toInt).toSet(),core.reforgeCount,core.history.split('\n').filter(String::isNotBlank))
    }
    private data class Core(val templateObj:Int,val rarity:EquipmentRarity,val seed:Long,val schemaVersion:Int,val balanceVersion:Int,val source:String,val category:EquipmentCategory,val legacyEffect:String?,val effectIds:String,val tier:Int,val itemLevel:Int,val quality:Int,val locked:String,val reforgeCount:Int,val history:String)
}
