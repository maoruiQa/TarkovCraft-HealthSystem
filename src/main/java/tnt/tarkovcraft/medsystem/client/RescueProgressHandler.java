package tnt.tarkovcraft.medsystem.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

public class RescueProgressHandler {
    
    private static float currentProgress = 0.0f;
    private static int remainingSeconds = 0;
    private static boolean isBeingRescued = false;
    private static long lastUpdateTime = 0;
    
    public static void handleRescueProgress(float progressPercent, int remainingSecondsParam, boolean isBeingRescuedParam) {
        currentProgress = progressPercent;
        remainingSeconds = remainingSecondsParam;
        isBeingRescued = isBeingRescuedParam;
        lastUpdateTime = System.currentTimeMillis();
        
        // Show progress message
        if (isBeingRescued) {
            showRescueMessage(String.format("Being rescued... %.0f%% (%ds remaining)", 
                    progressPercent * 100, remainingSecondsParam), ChatFormatting.YELLOW);
        } else {
            showRescueMessage(String.format("Rescuing... %.0f%% (%ds remaining)", 
                    progressPercent * 100, remainingSecondsParam), ChatFormatting.GREEN);
        }
    }
    
    private static void showRescueMessage(String message, ChatFormatting color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                    Component.literal(message).withStyle(color), 
                    true // Show as action bar
            );
        }
    }
    
    public static void clearProgress() {
        currentProgress = 0.0f;
        remainingSeconds = 0;
        isBeingRescued = false;
    }
    
    public static float getCurrentProgress() {
        // Clear progress if it's been too long since last update
        if (System.currentTimeMillis() - lastUpdateTime > 2000) { // 2 second timeout
            clearProgress();
        }
        return currentProgress;
    }
    
    public static int getRemainingSeconds() {
        return remainingSeconds;
    }
    
    public static boolean isBeingRescued() {
        return isBeingRescued;
    }
}