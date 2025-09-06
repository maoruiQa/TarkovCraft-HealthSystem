package tnt.tarkovcraft.medsystem.common.rescue;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import tnt.tarkovcraft.core.common.init.CoreDataAttachments;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;
import tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect;
import tnt.tarkovcraft.medsystem.common.health.BodyPartGroup;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.init.MedSystemDataAttachments;
import tnt.tarkovcraft.medsystem.common.init.MedSystemStatusEffects;
import tnt.tarkovcraft.medsystem.network.message.S2C_ClearDownedPose;
import tnt.tarkovcraft.medsystem.network.message.S2C_RescueProgress;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RescueSystem {
    
    private static final Map<UUID, RescueProgress> activeRescues = new HashMap<>();
    
    public static class RescueProgress {
        private final UUID rescuerId;
        private final UUID downedPlayerId;
        private int progress; // Progress in ticks
        private final int requiredTime; // Required time in ticks
        
        public RescueProgress(UUID rescuerId, UUID downedPlayerId, int requiredTimeSeconds) {
            this.rescuerId = rescuerId;
            this.downedPlayerId = downedPlayerId;
            this.progress = 0;
            this.requiredTime = requiredTimeSeconds * 20; // Convert to ticks
        }
        
        public boolean tick() {
            progress++;
            return progress >= requiredTime;
        }
        
        public float getProgressPercent() {
            return (float) progress / requiredTime;
        }
        
        public int getRemainingSeconds() {
            return Math.max(0, (requiredTime - progress) / 20);
        }
        
        public UUID getRescuerId() { return rescuerId; }
        public UUID getDownedPlayerId() { return downedPlayerId; }
        public int getProgress() { return progress; }
        public int getRequiredTime() { return requiredTime; }
    }
    
    public static void registerEvents() {
        NeoForge.EVENT_BUS.register(new RescueEventHandler());
    }
    
    private static class RescueEventHandler {
        
        @SubscribeEvent
        public void onPlayerTick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            if (player.level().isClientSide()) return;
            
            ServerPlayer serverPlayer = (ServerPlayer) player;
            MedSystemConfig config = MedicalSystem.getConfig();
            
            if (!config.enableDownedSystem) return;
            
            // Check for nearby downed players
            Player nearbyDownedPlayer = findNearbyDownedPlayer(serverPlayer);
            
            if (nearbyDownedPlayer != null && serverPlayer.isCrouching()) {
                // Start or continue rescue
                UUID rescuerId = serverPlayer.getUUID();
                UUID downedId = nearbyDownedPlayer.getUUID();
                
                RescueProgress rescue = activeRescues.get(rescuerId);
                if (rescue == null || !rescue.getDownedPlayerId().equals(downedId)) {
                    // Start new rescue
                    rescue = new RescueProgress(rescuerId, downedId, config.rescueTime);
                    activeRescues.put(rescuerId, rescue);
                    
                    // Send message to rescuer
                    serverPlayer.sendSystemMessage(
                            Component.translatable("medsystem.rescue.started", nearbyDownedPlayer.getDisplayName())
                                    .withStyle(ChatFormatting.YELLOW)
                    );
                }
                
                // Tick rescue progress
                boolean completed = rescue.tick();
                
                // Send progress update to both players
                if (nearbyDownedPlayer instanceof ServerPlayer downedServerPlayer) {
                    // Send to downed player
                    PacketDistributor.sendToPlayer(downedServerPlayer, new S2C_RescueProgress(
                            rescue.getProgressPercent(), rescue.getRemainingSeconds(), true
                    ));
                    
                    // Send to rescuer
                    PacketDistributor.sendToPlayer(serverPlayer, new S2C_RescueProgress(
                            rescue.getProgressPercent(), rescue.getRemainingSeconds(), false
                    ));
                }
                
                if (completed) {
                    // Complete rescue
                    completeRescue(serverPlayer, nearbyDownedPlayer);
                    activeRescues.remove(rescuerId);
                }
            } else {
                // Cancel any active rescue by this player
                RescueProgress rescue = activeRescues.remove(serverPlayer.getUUID());
                if (rescue != null) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("medsystem.rescue.cancelled")
                                    .withStyle(ChatFormatting.RED)
                    );
                }
            }
        }
    }
    
    private static Player findNearbyDownedPlayer(ServerPlayer player) {
        MedSystemConfig config = MedicalSystem.getConfig();
        
        return player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(3.0))
                .stream()
                .filter(p -> !p.equals(player))
                .filter(p -> {
                    HealthContainer health = p.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
                    return health != null && health.isPlayerDowned();
                })
                .findFirst()
                .orElse(null);
    }
    
    private static void completeRescue(ServerPlayer rescuer, Player downedPlayer) {
        HealthContainer health = downedPlayer.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        if (health == null) return;
        
        // Remove downed effect
        health.getGlobalStatusEffects().remove(MedSystemStatusEffects.DOWNED_PLAYER.value(), 
                tnt.tarkovcraft.core.util.context.ContextImpl.of(
                        tnt.tarkovcraft.medsystem.common.MedicalSystemContextKeys.HEALTH_CONTAINER, health,
                        tnt.tarkovcraft.core.util.context.ContextKeys.LIVING_ENTITY, downedPlayer
                ));
        
        // Ensure pose is cleared immediately on server
        downedPlayer.setForcedPose(null);
        downedPlayer.setPose(net.minecraft.world.entity.Pose.STANDING);
        downedPlayer.refreshDimensions();
        
        // Send pose clear packet to the rescued player's client
        if (downedPlayer instanceof ServerPlayer downedServerPlayer) {
            PacketDistributor.sendToPlayer(downedServerPlayer, new S2C_ClearDownedPose());
        }
        
        // Restore a small amount of health to vital parts - using config-based thresholds
        MedSystemConfig config = MedicalSystem.getConfig();
        health.getBodyPartStream()
                .filter(part -> {
                    BodyPartGroup group = part.getGroup();
                    return group == BodyPartGroup.HEAD || group == BodyPartGroup.TORSO;
                })
                .forEach(part -> {
                    float currentHealth = part.getHealth();
                    float maxHealth = part.getMaxHealth();
                    float healthPercent = currentHealth / maxHealth;
                    
                    if (part.getGroup() == BodyPartGroup.HEAD) {
                        // Restore head to 120% of downed threshold if below it
                        if (healthPercent < config.headDownedThreshold) {
                            float targetHealthPercent = config.headDownedThreshold * 1.2f; // 120% of threshold
                            float targetHealth = maxHealth * targetHealthPercent;
                            part.heal(targetHealth - currentHealth);
                        }
                    } else if (part.getGroup() == BodyPartGroup.TORSO) {
                        // Restore chest to 120% of downed threshold if below it  
                        if (healthPercent < config.chestDownedThreshold) {
                            float targetHealthPercent = config.chestDownedThreshold * 1.2f; // 120% of threshold
                            float targetHealth = maxHealth * targetHealthPercent;
                            part.heal(targetHealth - currentHealth);
                        }
                    }
                });
        
        // Sync health changes
        tnt.tarkovcraft.medsystem.common.health.HealthSystem.synchronizeEntity(downedPlayer);
        
        // Send success messages
        rescuer.sendSystemMessage(
                Component.translatable("medsystem.rescue.completed", downedPlayer.getDisplayName())
                        .withStyle(ChatFormatting.GREEN)
        );
        
        if (downedPlayer instanceof ServerPlayer downedServerPlayer) {
            downedServerPlayer.sendSystemMessage(
                    Component.translatable("medsystem.rescue.rescued_by", rescuer.getDisplayName())
                            .withStyle(ChatFormatting.GREEN)
            );
        }
    }
    
    public static RescueProgress getRescueProgress(UUID playerId) {
        return activeRescues.get(playerId);
    }
    
    public static void clearRescueProgress(UUID playerId) {
        activeRescues.remove(playerId);
    }
}