package org.rsmod.api.equipment.instance

public enum class EquipmentStat {
    AttackStab, AttackSlash, AttackCrush, AttackMagic, AttackRanged,
    DefenceStab, DefenceSlash, DefenceCrush, DefenceMagic, DefenceRanged,
    DefenceSummoning, AbsorbMelee, AbsorbMagic, AbsorbRanged,
    Strength, RangedStrength, Prayer, MagicDamage,
    AttackSpeedTicks, AttackSpeedPercent, DamageMin, DamageMax, AverageHit,
    DamagePerSecond, AttacksPerSecond, SelectedStyleAccuracy, CriticalRate,
    CriticalDamage, MeleePower, RangedPower, MagicPower, MaximumHealth,
    EffectiveHealth, LifeSteal, SoulSplitEffectiveness, SpecialEnergyCost,
    SpecialEnergyRegeneration, Cooldown, BossDamage, SlayerDamage, RunEnergy, DropRate,
}

public enum class ModifierUnit { Flat, BasisPoints, Ticks, ProcBasisPoints }
public enum class ModifierPolarity { Boon, Curse, Mixed }
