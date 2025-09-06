package tnt.tarkovcraft.medsystem.client.overlay;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.gui.GuiLayer;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.init.MedSystemDataAttachments;

import java.util.List;

public class RescuePromptLayer implements GuiLayer {

    public static final net.minecraft.resources.ResourceLocation LAYER_ID = MedicalSystem.resource("rescue_prompt");

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // Find nearby downed players
        Player nearbyDownedPlayer = findNearbyDownedPlayer(mc.player);
        
        if (nearbyDownedPlayer != null) {
            renderRescuePrompt(graphics, nearbyDownedPlayer);
        }
    }

    private Player findNearbyDownedPlayer(Player player) {
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

    private void renderRescuePrompt(GuiGraphics graphics, Player downedPlayer) {
        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Create rescue prompt message
        Component promptText = Component.translatable("medsystem.rescue.prompt", downedPlayer.getDisplayName())
                .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        
        Component instructionText = Component.translatable("medsystem.rescue.instruction")
                .withStyle(ChatFormatting.WHITE);

        // Position text at the bottom center of screen
        int promptWidth = Minecraft.getInstance().font.width(promptText);
        int instructionWidth = Minecraft.getInstance().font.width(instructionText);
        
        int promptX = (screenWidth - promptWidth) / 2;
        int instructionX = (screenWidth - instructionWidth) / 2;
        int baseY = screenHeight - 120;

        // Semi-transparent background
        graphics.fill(promptX - 10, baseY - 5, promptX + promptWidth + 10, baseY + 25, 0x80000000);

        // Render text
        graphics.drawString(Minecraft.getInstance().font, promptText, promptX, baseY, 0xFFFFFF);
        graphics.drawString(Minecraft.getInstance().font, instructionText, instructionX, baseY + 12, 0xFFFFFF);
    }
}