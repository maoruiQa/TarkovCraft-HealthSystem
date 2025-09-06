package tnt.tarkovcraft.medsystem.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.network.message.C2S_GiveUp;

public class GiveUpPromptLayer implements GuiLayer {
    
    private static int giveUpProgress = 0;
    private static final int GIVE_UP_REQUIRED_TIME = 90; // 4.5 seconds at 20 ticks/second

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
        }
        // Removed rescue logic - original RescueSystem.java will handle rescues
    }
    
    private void handleDownedPlayerDisplay(GuiGraphics guiGraphics, Player player, HealthContainer container) {
        DownedPlayerStatusEffect downedEffect = container.getDownedEffect();
        if (downedEffect == null) return;
        
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        
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
            
            // Check if should give up - send network packet to server
            if (giveUpProgress >= GIVE_UP_REQUIRED_TIME) {
                ClientPacketDistributor.sendToServer(new C2S_GiveUp());
                giveUpProgress = 0;
            }
        } else {
            message = Component.literal("§4Death in " + secondsLeft + "s §7| §6Hold [R] to give up (4.5s)");
        }
        
        // Display action bar message
        player.displayClientMessage(message, true); // true = action bar
    }
}