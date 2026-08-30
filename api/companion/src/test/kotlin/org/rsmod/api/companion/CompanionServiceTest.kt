package org.rsmod.api.companion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

public class CompanionServiceTest {
    @Test public fun `progression matches design checkpoints`() {
        assertEquals(3, CompanionRules.talentPointsForLevel(1))
        assertEquals(13, CompanionRules.talentPointsForLevel(50))
        assertEquals(30, CompanionRules.talentPointsForLevel(100))
        assertEquals(50, CompanionRules.talentPointsForLevel(120))
        assertEquals(80, CompanionRules.talentPointsForLevel(150))
    }

    @Test public fun `recruitment enforces ordered slots and one active companion`() {
        val service = CompanionService(NoopCompanionContractSource, NoopCompanionPersistence)
        val first = service.recruit(42, 1, "Aegis", CompanionClass.TANK, 1000, 250)
        assertThrows(IllegalArgumentException::class.java) { service.recruit(42, 3, "Missing", CompanionClass.DPS, 1001, 200) }
        val second = service.recruit(42, 2, "Mender", CompanionClass.SUPPORT, 1001, 200)
        service.activate(42, first.id)
        service.activate(42, second.id)
        assertFalse(service.owned(42).first { it.id == first.id }.active)
        assertTrue(service.owned(42).first { it.id == second.id }.active)
    }

    @Test public fun `combat is only selected from owners target`() {
        val service = CompanionService(NoopCompanionContractSource, NoopCompanionPersistence)
        val dps = service.recruit(42, 1, "Blade", CompanionClass.DPS, 1000, 200)
        service.activate(42, dps.id)
        val action = service.tick(42, dps.id, CompanionCombatContext(true, true, true, false, CompanionTarget(9, false, true, true, 2)), listOf(CompanionTarget(10, false, true, true, 3)))!!
        assertEquals(listOf(9, 10), action.targetIds)
        assertNull(service.tick(42, dps.id, CompanionCombatContext(true, true, true, false, null), emptyList()))
    }

    @Test public fun `damage incapacitates and resummon requires cooldown`() {
        val service = CompanionService(NoopCompanionContractSource, NoopCompanionPersistence)
        val tank = service.recruit(42, 1, "Aegis", CompanionClass.TANK, 1000, 100)
        service.activate(42, tank.id)
        val down = service.damage(42, tank.id, 100, 1_000L)
        assertEquals(CompanionState.INCAPACITATED, down.state)
        assertFalse(down.active)
        assertThrows(IllegalArgumentException::class.java) { service.resummon(42, tank.id, 1_000L) }
        assertEquals(CompanionState.FOLLOWING, service.resummon(42, tank.id, 31_000L).state)
        assertTrue(service.owned(42).single { it.id == tank.id }.active)
    }

    @Test public fun `talent tree gates by points and class`() {
        val service = CompanionService(NoopCompanionContractSource, NoopCompanionPersistence)
        val support = service.recruit(42, 1, "Mender", CompanionClass.SUPPORT, 1000, 100)
        assertThrows(IllegalArgumentException::class.java) { service.allocateTalent(42, support.id, "annihilation") }
        assertEquals(1, service.allocateTalent(42, support.id, "mending-light").talents.single().ranks)
    }

    @Test public fun `restore keeps companion identity and gear`() {
        val service = CompanionService(NoopCompanionContractSource, NoopCompanionPersistence)
        val original = service.recruit(42, 1, "Aegis", CompanionClass.TANK, 1000, 100)
        val restored = original.copy(id = original.id, gearInstanceIds = listOf(77L))
        service.restore(42, listOf(restored))
        assertEquals(original.id, service.owned(42).single().id)
        assertEquals(listOf(77L), service.owned(42).single().gearInstanceIds)
    }

    @Test public fun `frame layout stays within server limits`() {
        val store = CompanionFrameLayoutStore()
        val layout = store.set(42L, CompanionFrameLayout(120, 80, scalePercent = 150))
        assertEquals(layout, store.get(42L))
    }
}
