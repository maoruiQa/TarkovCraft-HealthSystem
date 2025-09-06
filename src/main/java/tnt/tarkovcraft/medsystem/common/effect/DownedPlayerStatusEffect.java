package tnt.tarkovcraft.medsystem.common.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import tnt.tarkovcraft.core.util.context.Context;
import tnt.tarkovcraft.core.util.context.ContextKeys;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;
import tnt.tarkovcraft.medsystem.common.health.BodyPartGroup;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.common.MedicalSystemContextKeys;
import tnt.tarkovcraft.medsystem.common.init.MedSystemStatusEffects;

import java.util.function.Consumer;

public class DownedPlayerStatusEffect extends StatusEffect {

    public static final MapCodec<DownedPlayerStatusEffect> CODEC = MapCodec.unit(DownedPlayerStatusEffect::new);

    private static final Component HINT = Component.translatable("status_effect.medsystem.downed_player.info").withStyle(ChatFormatting.RED);

    // NBT keys stored under player.getPersistentData() -> MODID compound
    private static final String NBT_DEATH = "downed_death_timer";
    private static final String NBT_GIVEUP = "downed_giveup_progress";

    public DownedPlayerStatusEffect(int duration, int delay) {
        super(duration, delay);
    }

    public DownedPlayerStatusEffect() {
        this(-1, 0); // Infinite duration
    }

    @Override
    public StatusEffectType<?> getType() {
        return MedSystemStatusEffects.DOWNED_PLAYER.value();
    }

    @Override
    public void apply(Context context) {
        LivingEntity entity = context.get(ContextKeys.LIVING_ENTITY).orElse(null);
        if (!(entity instanceof Player player)) {
            return;
        }

        // obtain player's mod-namespaced persistent compound
        CompoundTag playerRoot = player.getPersistentData();
        CompoundTag modTag = playerRoot.contains(MedicalSystem.MOD_ID) ? 
            playerRoot.getCompound(MedicalSystem.MOD_ID).orElse(new CompoundTag()) : 
            new CompoundTag();

        // initialize death timer on first tick if absent
        if (!modTag.contains(NBT_DEATH)) {
            int initTicks = MedicalSystem.getConfig().downedDeathTimer * 20;
            modTag.putInt(NBT_DEATH, initTicks);
        }
        int deathTimer = modTag.getInt(NBT_DEATH).orElse(45 * 20); // Default 45 seconds
        int giveUpProgress = modTag.getInt(NBT_GIVEUP).orElse(0);

        // --- FORCE SWIMMING POSE every tick (more aggressive approach) ---
        player.setForcedPose(Pose.SWIMMING);
        // Force both client and server side
        if (player.level().isClientSide()) {
            // On client side, also force the pose and override eye height
            player.setPose(Pose.SWIMMING);
            // Force refresh the entity dimensions to make sure hitbox changes
            player.refreshDimensions();
        } else {
            // On server side, also set the pose and sync to client
            player.setPose(Pose.SWIMMING);
            // Force dimension refresh on server too
            player.refreshDimensions();
        }

        // --- PREVENT JUMPING by clamping upward velocity on server & client ticks ---
        Vec3 vel = player.getDeltaMovement();
        if (vel.y > 0.0D) {
            player.setDeltaMovement(vel.x, 0.0D, vel.z);
        }
        // (optional) client-side you can also prevent jump input — see client key handler below

        // --- Apply periodic negative effects every 40 ticks ---
        if (!player.level().isClientSide() && (deathTimer % 40 == 0)) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 4, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 60, 2, false, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, false));
        }

        // --- Countdown to death (server-only) ---
        if (!player.level().isClientSide()) {
            deathTimer--;
            if (deathTimer <= 0 && player.level() instanceof ServerLevel) {
                // Remove the downed effect first to prevent interference with death handling
                HealthContainer healthContainer = HealthSystem.getHealthData(player);
                healthContainer.getGlobalStatusEffects().remove(
                    MedSystemStatusEffects.DOWNED_PLAYER.value(), 
                    tnt.tarkovcraft.core.util.context.ContextImpl.of(
                        tnt.tarkovcraft.core.util.context.ContextKeys.LIVING_ENTITY, player,
                        MedicalSystemContextKeys.HEALTH_CONTAINER, healthContainer
                    )
                );
                
                // Allow normal death process - using direct health manipulation for better death screen preservation
                // First clear the forced pose to prevent visual issues
                player.setForcedPose(null);
                player.setPose(net.minecraft.world.entity.Pose.STANDING);
                
                // Set health to 0 to trigger natural death sequence
                player.setHealth(0.0F);
                
                // If that doesn't work, use damage source as backup
                if (!player.isDeadOrDying()) {
                    player.hurt(player.damageSources().starve(), Float.MAX_VALUE);
                }
                return; // Don't save NBT since player is dying
            }
        }

        // save back
        modTag.putInt(NBT_DEATH, deathTimer);
        modTag.putInt(NBT_GIVEUP, giveUpProgress);
        playerRoot.put(MedicalSystem.MOD_ID, modTag);
    }

    @Override
    public StatusEffect onRemoved(Context context) {
        LivingEntity entity = context.get(ContextKeys.LIVING_ENTITY).orElse(null);
        if (entity instanceof Player player) {
            // clear forced pose when rescued or effect removed
            player.setForcedPose(null);

            // optionally clear the mod keys
            CompoundTag root = player.getPersistentData();
            if (root.contains(MedicalSystem.MOD_ID)) {
                CompoundTag modTag = root.getCompound(MedicalSystem.MOD_ID).orElse(new CompoundTag());
                modTag.remove(NBT_DEATH);
                modTag.remove(NBT_GIVEUP);
                root.put(MedicalSystem.MOD_ID, modTag);
            }
        }
        return null;
    }

    @Override
    public StatusEffect copy() {
        return new DownedPlayerStatusEffect(this.getDuration(), this.getDelay());
    }

    @Override
    public void addAdditionalInfo(Consumer<Component> tooltip) {
        tooltip.accept(HINT);
        // remaining time shown will be from user's local side value (if synced)
        // Keep the simple hint:
        Component giveUpHint = Component.literal("Hold [R] to give up").withStyle(ChatFormatting.GRAY);
        tooltip.accept(giveUpHint);
    }
    
    // Helper methods for compatibility with existing code
    public int getSecondsLeft() {
        // Try to get actual time from player's NBT if available
        return 45; // Simplified for now - would read from NBT in full implementation
    }
    
    public int getActualDeathTimer(Player player) {
        if (player != null) {
            CompoundTag playerRoot = player.getPersistentData();
            if (playerRoot.contains(MedicalSystem.MOD_ID)) {
                CompoundTag modTag = playerRoot.getCompound(MedicalSystem.MOD_ID).orElse(new CompoundTag());
                return modTag.getInt(NBT_DEATH).orElse(45 * 20);
            }
        }
        return 45 * 20;
    }
    
    public int getActualSecondsLeft(Player player) {
        return Math.max(0, getActualDeathTimer(player) / 20);
    }
    
    public float getGiveUpPercent() {
        // Simplified for now
        return 0.0f; 
    }
    
    public void tickGiveUp(boolean isKeyHeld) {
        // Simplified - the actual implementation would be in NBT
    }
    
    public boolean shouldGiveUp() {
        // Simplified - would check NBT progress
        return false;
    }
    
    public void setGiveUpProgress(int progress) {
        // Simplified - would update NBT
    }
    
    // New method to handle giving up
    public static void handleGiveUp(Player player) {
        if (player != null) {
            // Set health to 0 to trigger normal death sequence
            player.setHealth(0.0F);
        }
    }
    
    // New method to safely remove downed effect before death
    public static void removeDownedEffect(Player player, HealthContainer container) {
        if (player != null && container != null) {
            // Clear the forced pose immediately
            player.setForcedPose(null);
            player.setPose(net.minecraft.world.entity.Pose.STANDING);
            player.refreshDimensions();
            
            // Clear NBT data
            CompoundTag root = player.getPersistentData();
            if (root.contains(MedicalSystem.MOD_ID)) {
                CompoundTag modTag = root.getCompound(MedicalSystem.MOD_ID).orElse(new CompoundTag());
                modTag.remove(NBT_DEATH);
                modTag.remove(NBT_GIVEUP);
                root.put(MedicalSystem.MOD_ID, modTag);
            }
            
            // Remove the status effect
            container.getGlobalStatusEffects().remove(
                MedSystemStatusEffects.DOWNED_PLAYER.value(), 
                tnt.tarkovcraft.core.util.context.ContextImpl.of(
                    tnt.tarkovcraft.core.util.context.ContextKeys.LIVING_ENTITY, player,
                    MedicalSystemContextKeys.HEALTH_CONTAINER, container
                )
            );
        }
    }
    
    // New method to handle rescue
    public static void handleRescue(Player downedPlayer) {
        if (downedPlayer != null) {
            // Work on both client and server side
            if (HealthSystem.hasCustomHealth(downedPlayer)) {
                HealthContainer container = HealthSystem.getHealthData(downedPlayer);
                
                // Remove downed effect - this will trigger onRemoved() which clears the pose
                container.getGlobalStatusEffects().remove(
                    MedSystemStatusEffects.DOWNED_PLAYER.value(), 
                    tnt.tarkovcraft.core.util.context.ContextImpl.of(
                        tnt.tarkovcraft.core.util.context.ContextKeys.LIVING_ENTITY, downedPlayer,
                        MedicalSystemContextKeys.HEALTH_CONTAINER, container
                    )
                );
                
                // Restore critical health
                restoreHealthAfterRescue(container);
                
                // Update player's vanilla health to match container
                container.updateHealth(downedPlayer);
                
                // Ensure pose is cleared immediately
                downedPlayer.setForcedPose(null);
                downedPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
                downedPlayer.refreshDimensions();
                
                // Sync to client if on server
                if (!downedPlayer.level().isClientSide()) {
                    HealthSystem.synchronizeEntity(downedPlayer);
                }
            }
        }
    }
    
    private static void restoreHealthAfterRescue(HealthContainer container) {
        // Get current config thresholds (36% for both)
        MedSystemConfig config = MedicalSystem.getConfig();
        float headThreshold = config.headDownedThreshold; 
        float chestThreshold = config.chestDownedThreshold;
        
        // Restore head health if below threshold
        container.getBodyPartStream()
            .filter(part -> part.getGroup() == BodyPartGroup.HEAD)
            .forEach(part -> {
                float currentHealth = part.getHealth();
                float maxHealth = part.getMaxHealth();
                float healthPercent = currentHealth / maxHealth;
                
                if (healthPercent < headThreshold) {
                    // Restore to 120% of downed threshold to ensure well above threshold
                    float targetHealth = maxHealth * headThreshold * 1.20f; 
                    part.heal(targetHealth - currentHealth);
                }
            });
        
        // Restore chest health if below threshold  
        container.getBodyPartStream()
            .filter(part -> part.getGroup() == BodyPartGroup.TORSO)
            .forEach(part -> {
                float currentHealth = part.getHealth();
                float maxHealth = part.getMaxHealth();
                float healthPercent = currentHealth / maxHealth;
                
                if (healthPercent < chestThreshold) {
                    // Restore to 120% of downed threshold to ensure well above threshold
                    float targetHealth = maxHealth * chestThreshold * 1.20f;
                    part.heal(targetHealth - currentHealth);
                }
            });
    }
}
