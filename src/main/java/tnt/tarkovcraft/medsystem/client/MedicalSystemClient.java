package tnt.tarkovcraft.medsystem.client;

import dev.toma.configuration.Configuration;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import tnt.tarkovcraft.core.client.overlay.StaminaLayer;
import tnt.tarkovcraft.core.client.screen.navigation.CoreNavigators;
import tnt.tarkovcraft.core.client.screen.navigation.NavigationEntry;
import tnt.tarkovcraft.core.client.screen.navigation.OptionalNavigationEntry;
import tnt.tarkovcraft.core.util.context.ContextKeys;
import tnt.tarkovcraft.core.util.helper.TextHelper;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.client.config.MedSystemClientConfig;
import tnt.tarkovcraft.medsystem.client.ClientServerConfigState;
import tnt.tarkovcraft.medsystem.client.overlay.DownedPlayerLayer;
import tnt.tarkovcraft.medsystem.client.overlay.HealthLayer;
import tnt.tarkovcraft.medsystem.client.overlay.RescuePromptLayer;
import tnt.tarkovcraft.medsystem.client.GiveUpPromptLayer;
import tnt.tarkovcraft.medsystem.client.screen.HealthScreen;
import tnt.tarkovcraft.medsystem.common.init.MedSystemStatusEffects;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

@Mod(value = MedicalSystem.MOD_ID, dist = Dist.CLIENT)
public final class MedicalSystemClient {

    private static MedSystemClientConfig config;

    public static final NavigationEntry HEALTH = new OptionalNavigationEntry(
            TextHelper.createScreenTitle(MedicalSystem.MOD_ID, "health"),
            ctx -> {
                if (Minecraft.getInstance().player == null) return false;
                UUID clientId = Minecraft.getInstance().player.getUUID();
                return ctx.get(ContextKeys.UUID).filter(uuid -> uuid.equals(clientId)).isPresent();
            },
            HealthScreen::new,
            25
    );

    public MedicalSystemClient(IEventBus modEventBus, ModContainer container) {
        config = Configuration.registerSimpleYmlConfig(MedSystemClientConfig.class);

        modEventBus.addListener(this::registerGuiLayer);

        NeoForge.EVENT_BUS.addListener(this::prepareLayerRender);
        NeoForge.EVENT_BUS.addListener(this::onItemTooltip);

        CoreNavigators.CHARACTER_NAVIGATION_PROVIDER.register(HEALTH);
    }

    public static MedSystemClientConfig getConfig() {
        return config;
    }

    private void registerGuiLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(StaminaLayer.LAYER_ID, HealthLayer.LAYER_ID, new HealthLayer());
        event.registerAbove(HealthLayer.LAYER_ID, DownedPlayerLayer.LAYER_ID, new DownedPlayerLayer());
        event.registerAbove(DownedPlayerLayer.LAYER_ID, RescuePromptLayer.LAYER_ID, new RescuePromptLayer());
        event.registerAbove(RescuePromptLayer.LAYER_ID, MedicalSystem.resource("give_up_prompt"), new GiveUpPromptLayer());
    }

    private void prepareLayerRender(RenderGuiLayerEvent.Pre event) {
        if (!config.renderHealth && event.getName().equals(VanillaGuiLayers.PLAYER_HEALTH)) {
            event.setCanceled(true);
        }
    }

    private void onItemTooltip(ItemTooltipEvent event) {
        if (!ClientServerConfigState.isSynced()) return;
        ItemStack stack = event.getItemStack();
        if (stack == null || stack.isEmpty()) return;

        boolean isSword = stack.is(ItemTags.SWORDS);
        boolean isAxe = stack.is(ItemTags.AXES);
        boolean isBlunt = stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.HOES);
        if (!(isSword || isAxe || isBlunt)) return;

        event.getToolTip().add(Component.literal(" "));
        event.getToolTip().add(Component.literal("Server-side chances:").withStyle(ChatFormatting.GRAY));

        if (isSword) {
            float l = ClientServerConfigState.getSwordLightBleed();
            float h = ClientServerConfigState.getSwordHeavyBleed();
            if (l > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", l * 100)).append(MedSystemStatusEffects.LIGHT_BLEED.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
            if (h > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", h * 100)).append(MedSystemStatusEffects.HEAVY_BLEED.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
        } else if (isAxe) {
            float l = ClientServerConfigState.getAxeLightBleed();
            float h = ClientServerConfigState.getAxeHeavyBleed();
            float f = ClientServerConfigState.getAxeFracture();
            if (l > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", l * 100)).append(MedSystemStatusEffects.LIGHT_BLEED.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
            if (h > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", h * 100)).append(MedSystemStatusEffects.HEAVY_BLEED.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
            if (f > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", f * 100)).append(MedSystemStatusEffects.FRACTURE.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
        } else if (isBlunt) {
            float f = ClientServerConfigState.getBluntFracture();
            float l = ClientServerConfigState.getBluntLightBleed();
            if (f > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", f * 100)).append(MedSystemStatusEffects.FRACTURE.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
            if (l > 0.0f) event.getToolTip().add(Component.literal(String.format("> %.1f%% ", l * 100)).append(MedSystemStatusEffects.LIGHT_BLEED.value().getDisplayName()).withStyle(ChatFormatting.YELLOW));
        }
    }
}
