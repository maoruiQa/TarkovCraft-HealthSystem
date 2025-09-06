package tnt.tarkovcraft.medsystem.common.config;

import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.UpdateRestrictions;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.health.DefaultArmorComponent;

@Config(id = MedicalSystem.MOD_ID, filename = "medicalsystem")
public final class MedSystemConfig {

    @Configurable
    @Configurable.Comment("Includes all equipped armors for damage reduction calculation")
    @Configurable.Validate(DefaultArmorComponent.ConfigValidator.class)
    public boolean simpleArmorCalculation = false;

    @Configurable
    @Configurable.DecimalRange(min = 0.15, max = 3.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Damage scale for explosions")
    public float explosionDamageScale = 0.6F;

    @Configurable
    @Configurable.DecimalRange(min = 0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Losing limb has small chance to cause immediate death")
    public float limbLossDeathCauseChance = 0.05F;

    @Configurable
    @Configurable.Comment("Health will be primarily recovered into vital parts")
    public boolean prioritizeVitalHealing = true;

    @Configurable
    @Configurable.DecimalRange(min = 0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.0##")
    @Configurable.Comment("Threshold for prioritized vital body part health recovery")
    public float vitalBodyPartHealthTrigger = 0.75F;

    @Configurable
    @Configurable.Comment("Enables hit effects such as bleeds, fractures and other effects")
    @Configurable.Synchronized
    public boolean enableHitEffects = true;

    @Configurable
    @Configurable.Comment("Allows scaling of injury recovery status effects when getting the effect repeatedly")
    public boolean allowInjuryRecoveryScaling = true;

    @Configurable
    @Configurable.Comment("Vanilla tools will have chance to cause some negative effects such as bleeds or fractures")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public boolean addHitEffectsToVanillaItems = true;

    // =========================
    // Server-configurable chances
    // =========================

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 10.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.0##")
    @Configurable.Comment("Global multiplier for chances of negative effects caused by FALL damage (applies to data-driven reactions). 1.0 = default")
    @Configurable.Synchronized
    public float fallEffectChanceMultiplier = 0.65F;

    // Vanilla tool hit effect chances (server-side). These replace hardcoded defaults.
    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Sword hit: chance for Light Bleed")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float swordLightBleedChance = 0.10F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Sword hit: chance for Heavy Bleed")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float swordHeavyBleedChance = 0.04F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Axe hit: chance for Light Bleed")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float axeLightBleedChance = 0.10F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Axe hit: chance for Heavy Bleed")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float axeHeavyBleedChance = 0.02F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Axe hit: chance for Fracture")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float axeFractureChance = 0.10F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Blunt tool hit (shovel/pickaxe/hoe/mace): chance for Fracture")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float bluntFractureChance = 0.10F;

    @Configurable
    @Configurable.DecimalRange(min = 0.0, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Blunt tool hit (shovel/pickaxe/hoe/mace): chance for Light Bleed")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float bluntLightBleedChance = 0.05F;

    // =========================
    // Body Part Health Configuration
    // =========================

    @Configurable
    @Configurable.DecimalRange(min = 0.1, max = 5.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Health multiplier for head body part (1.9 = +90%)")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float headHealthMultiplier = 1.9F;

    @Configurable
    @Configurable.DecimalRange(min = 0.1, max = 5.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Health multiplier for chest/torso body part (1.7 = +70%)")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float chestHealthMultiplier = 1.7F;

    @Configurable
    @Configurable.DecimalRange(min = 0.1, max = 5.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Health multiplier for arm body parts (1.0 = no change)")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float armHealthMultiplier = 1.0F;

    @Configurable
    @Configurable.DecimalRange(min = 0.1, max = 5.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Health multiplier for leg body parts (1.0 = no change)")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public float legHealthMultiplier = 1.0F;

    // =========================
    // Downed Player System Configuration
    // =========================

    @Configurable
    @Configurable.Comment("Enable the downed player system")
    @Configurable.Synchronized
    public boolean enableDownedSystem = true;

    @Configurable
    @Configurable.DecimalRange(min = 0.01, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Head health percentage threshold for downed state (0.45 = 45%)")
    @Configurable.Synchronized
    public float headDownedThreshold = 0.45F;

    @Configurable
    @Configurable.DecimalRange(min = 0.01, max = 1.0)
    @Configurable.Gui.Slider
    @Configurable.Gui.NumberFormat("0.00#")
    @Configurable.Comment("Chest health percentage threshold for downed state (0.48 = 48%)")
    @Configurable.Synchronized
    public float chestDownedThreshold = 0.48F;

    @Configurable
    @Configurable.Comment("Time in seconds before downed player dies")
    @Configurable.Synchronized
    public int downedDeathTimer = 45;

    @Configurable
    @Configurable.Comment("Time in seconds required to rescue a downed player by crouching")
    @Configurable.Synchronized
    public int rescueTime = 8;

    @Configurable
    @Configurable.Comment("Slowness effect level for downed players")
    @Configurable.Synchronized
    public int downedSlownessLevel = 4;

    @Configurable
    @Configurable.Comment("Resistance effect level for downed players")
    @Configurable.Synchronized
    public int downedResistanceLevel = 4;

    @Configurable
    @Configurable.Comment("Weakness effect level for downed players")
    @Configurable.Synchronized
    public int downedWeaknessLevel = 1;
    
    // =========================
    // Medical Item Durability Configuration
    // =========================
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 100)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Durability (uses) for Emergency Surgery Kit")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int emergencySurgeryKitDurability = 10;
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 200)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Durability (uses) for First Aid Kit")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int firstAidKitDurability = 30;
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 20)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Durability (uses) for Painkillers")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int painkillersKitDurability = 4;
    
    // =========================
    // Medical Item Craft Amount Configuration
    // =========================
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 10)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Number of bandages crafted per recipe")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int bandageCraftAmount = 2;
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 10)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Number of splints crafted per recipe")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int splintCraftAmount = 2;
    
    @Configurable
    @Configurable.DecimalRange(min = 1, max = 10)
    @Configurable.Gui.NumberFormat("0")
    @Configurable.Comment("Number of tourniquets crafted per recipe")
    @Configurable.UpdateRestriction(UpdateRestrictions.GAME_RESTART)
    public int tourniquetCraftAmount = 2;
}
