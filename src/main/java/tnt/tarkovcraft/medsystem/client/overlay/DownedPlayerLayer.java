package tnt.tarkovcraft.medsystem.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.client.gui.GuiLayer;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.init.MedSystemDataAttachments;

public class DownedPlayerLayer implements GuiLayer {

    public static final ResourceLocation LAYER_ID = MedicalSystem.resource("downed_player");

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        HealthContainer healthContainer = mc.player.getData(MedSystemDataAttachments.HEALTH_CONTAINER);
        if (healthContainer == null) return;

        // Check if player is downed
        DownedPlayerStatusEffect downedEffect = healthContainer.getDownedEffect();
        if (downedEffect != null) {
            renderDownedPlayerOverlay(graphics, downedEffect);
        }

        // Removed blue progress bar rendering - action bar will handle all rescue feedback
    }

    private void renderDownedPlayerOverlay(GuiGraphics graphics, DownedPlayerStatusEffect downedEffect) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Red tint overlay to indicate downed state
        graphics.fill(0, 0, screenWidth, screenHeight, 0x44AA0000);

        // Death countdown timer
        int secondsLeft = downedEffect.getSecondsLeft();
        Component deathTimer = Component.translatable("medsystem.downed.death_timer", secondsLeft)
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        
        int textWidth = Minecraft.getInstance().font.width(deathTimer);
        graphics.drawString(Minecraft.getInstance().font, deathTimer, 
                (screenWidth - textWidth) / 2, 50, 0xFFFFFF);

        // Downed status message
        Component downedMessage = Component.translatable("medsystem.downed.message")
                .withStyle(ChatFormatting.YELLOW);
        
        int messageWidth = Minecraft.getInstance().font.width(downedMessage);
        graphics.drawString(Minecraft.getInstance().font, downedMessage,
                (screenWidth - messageWidth) / 2, 70, 0xFFFFFF);

        // Help text
        Component helpText = Component.translatable("medsystem.downed.help")
                .withStyle(ChatFormatting.GRAY);
        
        int helpWidth = Minecraft.getInstance().font.width(helpText);
        graphics.drawString(Minecraft.getInstance().font, helpText,
                (screenWidth - helpWidth) / 2, 90, 0xFFFFFF);
    }

    /*
    // Removed - blue progress bar is no longer used, action bar handles all rescue feedback
    private void renderRescueProgress(GuiGraphics graphics, float progress, int remainingSeconds, boolean isBeingRescued) {
        // This method is no longer used - removed to eliminate duplicate rescue progress bars
    }
    */
}