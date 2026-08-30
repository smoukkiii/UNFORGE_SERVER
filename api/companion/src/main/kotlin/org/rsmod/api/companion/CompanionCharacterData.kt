package org.rsmod.api.companion

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

public data class CompanionCharacterData(public val companions: List<Companion>) : CharacterDataStage.Segment

public class CompanionCharacterApplier @Inject constructor(
    private val service: CompanionService,
) : CharacterDataStage.Applier<CompanionCharacterData> {
    override fun apply(player: Player, data: CompanionCharacterData) {
        service.restore(player.characterId.toLong(), data.companions)
    }
}

public class CompanionCharacterPipeline @Inject constructor(
    private val applier: CompanionCharacterApplier,
    private val service: CompanionService,
) : CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        metadata.add(applier, CompanionCharacterData(CompanionRepository(connection).load(metadata.characterId.toLong())))
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        val repository = CompanionRepository(connection)
        service.owned(characterId.toLong()).forEach(repository::save)
    }
}
