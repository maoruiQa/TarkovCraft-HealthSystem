package tnt.tarkovcraft.medsystem.common.health;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import tnt.tarkovcraft.core.util.Codecs;
import tnt.tarkovcraft.core.util.context.WritableContext;
import tnt.tarkovcraft.medsystem.common.MedicalSystemContextKeys;
import tnt.tarkovcraft.medsystem.common.effect.StatusEffectMap;

import java.util.Objects;

public final class BodyPart {

    public static final Codec<BodyPart> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(t -> t.name),
            Codec.BOOL.fieldOf("vital").forGetter(t -> t.vital),
            Codec.FLOAT.fieldOf("health").forGetter(t -> t.health),
            Codec.FLOAT.fieldOf("maxHealth").forGetter(t -> t.maxHealth),
            Codec.FLOAT.fieldOf("originalMaxHealth").forGetter(t -> t.originalMaxHealth),
            Codec.FLOAT.fieldOf("parentDamageScale").forGetter(t -> t.parentDamageScale),
            Codec.FLOAT.fieldOf("damageScale").forGetter(t -> t.damageScale),
            Codecs.simpleEnumCodec(BodyPartGroup.class).fieldOf("group").forGetter(t -> t.group),
            StatusEffectMap.CODEC.fieldOf("statusEffects").forGetter(t -> t.statusEffects)
    ).apply(instance, BodyPart::new));

    private BodyPartDefinition definition;
    private final String name;
    private final boolean vital;
    private final float originalMaxHealth;
    private float health;
    private float maxHealth;
    private final float parentDamageScale;
    private final float damageScale;
    private final BodyPartGroup group;
    private final Component displayName;
    private final StatusEffectMap statusEffects;

    public BodyPart(String name, boolean vital, float maxHealth, float parentDamageScale, float damageScale, BodyPartGroup group) {
        this(name, vital, maxHealth, maxHealth, maxHealth, parentDamageScale, damageScale, group, new StatusEffectMap());
    }

    private BodyPart(String name, boolean vital, float health, float maxHealth, float originalMaxHealth, float parentDamageScale, float damageScale, BodyPartGroup group, StatusEffectMap statusEffects) {
        this.name = name;
        this.vital = vital;
        this.health = health;
        this.maxHealth = maxHealth;
        this.originalMaxHealth = originalMaxHealth;
        this.parentDamageScale = parentDamageScale;
        this.damageScale = damageScale;
        this.group = group;
        this.displayName = Component.translatable("medsystem.bodypart." + name);
        this.statusEffects = statusEffects;
    }

    public void setDefinition(BodyPartDefinition definition) {
        this.definition = definition;
    }

    public String getName() {
        return name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public float getParentDamageScale() {
        return parentDamageScale;
    }

    public float getDamageScale() {
        return damageScale;
    }

    public boolean shouldOwnerDie() {
        return this.vital && this.health <= 0.0F;
    }

    public boolean isDead() {
        return this.health <= 0.0F;
    }

    public boolean isVital() {
        return vital;
    }

    public BodyPartGroup getGroup() {
        return group;
    }

    public float getHealth() {
        return health;
    }

    public float getHealthPercent() {
        return this.health / this.maxHealth;
    }
    
    /**
     * Get health percentage for display purposes using original health ratios.
     * This ensures UI colors are based on original health ranges, not multiplied values.
     */
    /**
     * Get health percentage for display purposes with improved logic:
     * - If player is downed: return very low value (0.05) for very deep red
     * - Head/Chest near downed threshold: use enhanced sensitivity for early red warning
     * - Limbs: use original logic
     */
    public float getDisplayHealthPercent() {
        // For head and chest, use enhanced color calculation
        if (this.group == BodyPartGroup.HEAD || this.group == BodyPartGroup.TORSO) {
            // Get config thresholds
            tnt.tarkovcraft.medsystem.common.config.MedSystemConfig config = tnt.tarkovcraft.medsystem.MedicalSystem.getConfig();
            float downedThreshold = (this.group == BodyPartGroup.HEAD) ? config.headDownedThreshold : config.chestDownedThreshold;
            
            // Check if player is currently downed (if we can access the container)
            // For now, we'll detect near-downed state more aggressively
            
            // Calculate current health percentage
            float currentHealthPercent = this.health / this.maxHealth;
            
            // If already below downed threshold, return very low value for deep red
            if (currentHealthPercent <= downedThreshold) {
                return 0.05f; // Very deep red color
            }
            
            // Enhanced sensitivity: make colors turn red much earlier
            // Map the range [downedThreshold, 1.0] to [0.0, 1.0] with exponential curve
            float normalizedHealth = (currentHealthPercent - downedThreshold) / (1.0f - downedThreshold);
            
            // Apply exponential curve to make red appear earlier
            // Using square root to make the transition more aggressive
            return (float) Math.sqrt(normalizedHealth);
        } else {
            // For limbs, use original simple logic
            float multiplier = this.maxHealth / this.originalMaxHealth;
            float effectiveMaxHealth = this.maxHealth / multiplier;
            return this.health / effectiveMaxHealth;
        }
    }
    
    /**
     * Get the display health value for UI purposes based on original health scale
     */
    public float getDisplayHealth() {
        // Return health scaled back to original range for UI display
        float multiplier = this.maxHealth / this.originalMaxHealth;
        return this.health / multiplier;
    }

    public void setHealth(float health) {
        this.health = Mth.clamp(health, 0, maxHealth);
    }

    public void heal(float amount) {
        this.setHealth(this.health + amount);
    }

    public void hurt(float amount) {
        this.setHealth(this.health - amount);
    }

    public float getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(float maxHealth) {
        this.maxHealth = maxHealth;
        this.setHealth(this.health);
    }

    public float getMaxHealAmount() {
        return this.maxHealth - this.health;
    }

    public float getOriginalMaxHealth() {
        return originalMaxHealth;
    }

    public StatusEffectMap getStatusEffects() {
        return this.statusEffects;
    }

    public void trigger(WritableContext context) {
        context.set(MedicalSystemContextKeys.BODY_PART, this);
        this.definition.getReactions().forEach(def -> def.react(context));
    }

    public void tick(WritableContext context) {
        context.set(MedicalSystemContextKeys.BODY_PART, this);
        this.statusEffects.tick(context);
        this.definition.getReactions().forEach(def -> def.react(context));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BodyPart part)) return false;
        return Objects.equals(name, part.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
