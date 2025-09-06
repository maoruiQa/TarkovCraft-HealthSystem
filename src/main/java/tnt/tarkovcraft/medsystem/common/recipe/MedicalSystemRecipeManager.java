package tnt.tarkovcraft.medsystem.common.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;

import java.util.Map;

@EventBusSubscriber(modid = MedicalSystem.MOD_ID)
public class MedicalSystemRecipeManager {
    
    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        // This event is called before the server starts, after recipes are loaded
        // We can modify recipe outputs here based on config values
        modifyRecipeOutputs(event.getServer().getRecipeManager());
    }
    
    private static void modifyRecipeOutputs(RecipeManager recipeManager) {
        // Get the config values
        MedSystemConfig config = MedicalSystem.getConfig();
        int bandageCraftAmount = config.bandageCraftAmount;
        int splintCraftAmount = config.splintCraftAmount;
        int tourniquetCraftAmount = config.tourniquetCraftAmount;
        
        // Note: In Minecraft 1.21+, direct modification of recipes is not straightforward
        // We would need to use a custom recipe serializer or override the recipe results
        // For now, we'll rely on the JSON recipes having the correct count values
        MedicalSystem.LOGGER.info(MedicalSystem.MARKER, "Recipe output amounts - Bandage: {}, Splint: {}, Tourniquet: {}", 
            bandageCraftAmount, splintCraftAmount, tourniquetCraftAmount);
    }
}