package tnt.tarkovcraft.medsystem.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;
import tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;

public class GiveUpPromptLayer implements GuiLayer {
    
    private static int giveUpProgress = 0;
    private static final int GIVE_UP_REQUIRED_TIME = 90; // 4.5 seconds at 20 ticks/second
    private static int rescueProgress = 0;
    private static final int RESCUE_REQUIRED_TIME = 160; // 8 seconds at 20 ticks/second
    private static Player nearbyDownedPlayer = null;

    @Override
    public void render(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        
        if (player == null || !HealthSystem.hasCustomHealth(player)) {
            return;
        }
        
        HealthContainer container = HealthSystem.getHealthData(player);
        
        if (container.isPlayerDowned()) {
            handleDownedPlayerDisplay(guiGraphics, player, container);
        } else {
            handleHealthyPlayerDisplay(guiGraphics, player);
        }
    }
    
    private void handleDownedPlayerDisplay(GuiGraphics guiGraphics, Player player, HealthContainer container) {
        DownedPlayerStatusEffect downedEffect = container.getDownedEffect();
        if (downedEffect == null) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        Font font = minecraft.font;
        
        // Get actual death timer from the status effect
        int secondsLeft = downedEffect.getActualSecondsLeft(player);
        
        // Check if R key is being held
        boolean isRKeyHeld = false;
        try {
            long window = minecraft.getWindow().getWindow();
            isRKeyHeld = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_R) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        } catch (Exception e) {
            isRKeyHeld = false;
        }
        
        // Update give up progress
        if (isRKeyHeld) {
            giveUpProgress++;
        } else {
            giveUpProgress = Math.max(0, giveUpProgress - 2); // Decay faster
        }
        
        // Display action bar message
        Component message;
        if (giveUpProgress > 0) {
            float giveUpPercent = Math.min(1.0f, (float) giveUpProgress / GIVE_UP_REQUIRED_TIME);
            int progressBars = (int) (giveUpPercent * 20);
            StringBuilder progressBar = new StringBuilder("§c[");
            for (int i = 0; i < 20; i++) {
                progressBar.append(i < progressBars ? "█" : "░");
            }
            progressBar.append("] §fGiving up... " + (int)(giveUpPercent * 100) + "%");
            message = Component.literal(progressBar.toString());
            
            // Check if should give up
            if (giveUpProgress >= GIVE_UP_REQUIRED_TIME) {
                DownedPlayerStatusEffect.handleGiveUp(player);
                giveUpProgress = 0;
            }
        } else {
            message = Component.literal("§4Death in " + secondsLeft + "s §7| §6Hold [R] to give up (4.5s)");
        }
        
        // Display action bar message
        player.displayClientMessage(message, true); // true = action bar
    }
    
    private void handleHealthyPlayerDisplay(GuiGraphics guiGraphics, Player player) {
        // Check for nearby downed players
        nearbyDownedPlayer = findNearbyDownedPlayer(player);
        
        if (nearbyDownedPlayer != null) {
            double distance = player.distanceToSqr(nearbyDownedPlayer);
            
            if (distance <= 4.0) { // Within 2 blocks
                // Check if shift is being held
                boolean isShiftHeld = false;
                try {
                    Minecraft minecraft = Minecraft.getInstance();
                    long window = minecraft.getWindow().getWindow();
                    isShiftHeld = org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS ||
                                 org.lwjgl.glfw.GLFW.glfwGetKey(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                } catch (Exception e) {
                    isShiftHeld = false;
                }
                
                Component message;
                if (isShiftHeld) {
                    rescueProgress++;
                    float rescuePercent = Math.min(1.0f, (float) rescueProgress / RESCUE_REQUIRED_TIME);
                    int progressBars = (int) (rescuePercent * 20);
                    StringBuilder progressBar = new StringBuilder("§a[");
                    for (int i = 0; i < 20; i++) {
                        progressBar.append(i < progressBars ? "█" : "░");
                    }
                    progressBar.append("] §fRescuing... " + (int)(rescuePercent * 100) + "%");
                    message = Component.literal(progressBar.toString());
                    
                    // Check if rescue is complete
                    if (rescueProgress >= RESCUE_REQUIRED_TIME) {
                        completeRescue(nearbyDownedPlayer);
                        rescueProgress = 0;
                        nearbyDownedPlayer = null;
                    }
                } else {
                    rescueProgress = Math.max(0, rescueProgress - 3); // Decay faster when not holding
                    message = Component.literal("§e" + nearbyDownedPlayer.getName().getString() + " is down! §6Hold [Shift] to rescue");
                }
                
                player.displayClientMessage(message, true); // true = action bar
            } else {
                rescueProgress = 0;
            }
        } else {
            rescueProgress = 0;
        }
    }
    
    private Player findNearbyDownedPlayer(Player player) {
        return player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0))
                .stream()
                .filter(p -> p != player)
                .filter(p -> HealthSystem.hasCustomHealth(p))
                .filter(p -> HealthSystem.getHealthData(p).isPlayerDowned())
                .min((p1, p2) -> Double.compare(player.distanceToSqr(p1), player.distanceToSqr(p2)))
                .orElse(null);
    }
    
    private void completeRescue(Player downedPlayer) {
        // Handle rescue immediately on client (simplified approach)
        DownedPlayerStatusEffect.handleRescue(downedPlayer);
        
        // Show success message to rescuer
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(
                Component.literal("§a✓ Successfully rescued " + downedPlayer.getName().getString() + "!"), 
                true
            );
        }
    }
}