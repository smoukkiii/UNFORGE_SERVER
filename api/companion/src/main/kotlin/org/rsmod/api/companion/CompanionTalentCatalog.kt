package org.rsmod.api.companion

public object CompanionTalentCatalog {
    public val definitions: List<CompanionTalentDefinition> = listOf(
        CompanionTalentDefinition("guardian-vitality", CompanionClass.TANK, 1, effectKey = "max-health-percent"),
        CompanionTalentDefinition("iron-stance", CompanionClass.TANK, 2, effectKey = "damage-reduction-percent"),
        CompanionTalentDefinition("taunting-roar", CompanionClass.TANK, 3, effectKey = "taunt-radius"),
        CompanionTalentDefinition("bulwark", CompanionClass.TANK, 4, effectKey = "owner-damage-shield"),
        CompanionTalentDefinition("last-stand", CompanionClass.TANK, 5, effectKey = "incapacitated-delay"),
        CompanionTalentDefinition("unyielding", CompanionClass.TANK, 6, effectKey = "boss-damage-reduction"),
        CompanionTalentDefinition("guardian-aegis", CompanionClass.TANK, 7, effectKey = "owner-protection"),
        CompanionTalentDefinition("immortal-oath", CompanionClass.TANK, 8, maxRanks = 1, effectKey = "death-prevention", capstone = true),
        CompanionTalentDefinition("mending-light", CompanionClass.SUPPORT, 1, effectKey = "heal-power"),
        CompanionTalentDefinition("battle-hymn", CompanionClass.SUPPORT, 2, effectKey = "owner-stat-buff"),
        CompanionTalentDefinition("cleanse", CompanionClass.SUPPORT, 3, effectKey = "cleanse-debuff"),
        CompanionTalentDefinition("warding-sigil", CompanionClass.SUPPORT, 4, effectKey = "shield-power"),
        CompanionTalentDefinition("renewal", CompanionClass.SUPPORT, 5, effectKey = "heal-over-time"),
        CompanionTalentDefinition("mana-thread", CompanionClass.SUPPORT, 6, effectKey = "ability-cooldown"),
        CompanionTalentDefinition("rescue", CompanionClass.SUPPORT, 7, effectKey = "emergency-heal"),
        CompanionTalentDefinition("life-anchor", CompanionClass.SUPPORT, 8, maxRanks = 1, effectKey = "owner-death-save", capstone = true),
        CompanionTalentDefinition("vicious-strikes", CompanionClass.DPS, 1, effectKey = "damage-percent"),
        CompanionTalentDefinition("executioner", CompanionClass.DPS, 2, effectKey = "low-health-damage"),
        CompanionTalentDefinition("rapid-assault", CompanionClass.DPS, 3, effectKey = "attack-speed"),
        CompanionTalentDefinition("critical-focus", CompanionClass.DPS, 4, effectKey = "critical-rate"),
        CompanionTalentDefinition("elemental-edge", CompanionClass.DPS, 5, effectKey = "ability-power"),
        CompanionTalentDefinition("blood-price", CompanionClass.DPS, 6, effectKey = "life-steal"),
        CompanionTalentDefinition("relentless", CompanionClass.DPS, 7, effectKey = "proc-chance"),
        CompanionTalentDefinition("annihilation", CompanionClass.DPS, 8, maxRanks = 1, effectKey = "burst-ability", capstone = true),
    )

    public val byId: Map<String, CompanionTalentDefinition> = definitions.associateBy { it.id }

    public fun definition(id: String): CompanionTalentDefinition = byId[id] ?: error("Unknown companion talent: $id")
}
