package org.rsmod.api.account.character.equipment

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.equipment.instance.EquipmentInstanceRegistry
import org.rsmod.api.equipment.instance.EquipmentInstanceRepository
import org.rsmod.game.entity.Player

public class EquipmentInstanceLoadPipeline
@Inject
constructor(
    private val registry: EquipmentInstanceRegistry,
    private val repository: EquipmentInstanceRepository,
) : CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        connection.prepareStatement(
            """
                SELECT DISTINCT io.equipment_instance_id
                FROM inventory_objs io
                JOIN inventories i ON i.id = io.inventories_id
                WHERE i.character_id = ? AND io.equipment_instance_id > 0
            """.trimIndent()
        ).use { statement ->
            statement.setInt(1, metadata.characterId)
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val id = resultSet.getLong(1)
                    repository.load(connection, id)?.let(registry::put)
                }
            }
        }
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int): Unit = Unit
}