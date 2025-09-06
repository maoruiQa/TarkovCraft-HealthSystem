package tnt.tarkovcraft.medsystem.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.configuration.ServerConfigurationPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.network.message.C2S_GiveUp;
import tnt.tarkovcraft.medsystem.network.message.C2S_SelectBodyPart;
import tnt.tarkovcraft.medsystem.network.message.S2C_ClearDownedPose;
import tnt.tarkovcraft.medsystem.network.message.S2C_OpenBodyPartSelectScreen;
import tnt.tarkovcraft.medsystem.network.message.S2C_RescueProgress;
import tnt.tarkovcraft.medsystem.network.message.S2C_SendHealthDefinitions;
import tnt.tarkovcraft.medsystem.network.message.S2C_SendServerEffectChances;
import tnt.tarkovcraft.medsystem.common.config.MedSystemConfig;

import java.util.Locale;
import java.util.function.Consumer;

public final class MedicalSystemNetwork {

    public static final int VERSION = 1;
    public static final String NETWORK_ID = "MedicalSystemNetwork@" + VERSION;

    public static ResourceLocation createId(Class<? extends CustomPacketPayload> type) {
        String name = type.getSimpleName().toLowerCase(Locale.ROOT);
        return MedicalSystem.resource("net/" + name);
    }

    @SubscribeEvent
    private void onRegistration(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registry = event.registrar(NETWORK_ID).executesOn(HandlerThread.MAIN);

        registry.playToClient(S2C_ClearDownedPose.TYPE, S2C_ClearDownedPose.CODEC, S2C_ClearDownedPose::handleMessage);
        registry.playToClient(S2C_OpenBodyPartSelectScreen.TYPE, S2C_OpenBodyPartSelectScreen.CODEC, S2C_OpenBodyPartSelectScreen::handleMessage);
        registry.playToClient(S2C_RescueProgress.TYPE, S2C_RescueProgress.CODEC, S2C_RescueProgress::handle);

        registry.playToServer(C2S_GiveUp.TYPE, C2S_GiveUp.CODEC, C2S_GiveUp::handleMessage);
        registry.playToServer(C2S_SelectBodyPart.TYPE, C2S_SelectBodyPart.CODEC, C2S_SelectBodyPart::handleMessage);

        registry.configurationToClient(S2C_SendHealthDefinitions.TYPE, S2C_SendHealthDefinitions.CODEC, S2C_SendHealthDefinitions::handleMessage);
        registry.configurationToClient(S2C_SendServerEffectChances.TYPE, S2C_SendServerEffectChances.CODEC, S2C_SendServerEffectChances::handleMessage);
    }

    @SubscribeEvent
    private void registerConfigurationTasks(RegisterConfigurationTasksEvent event) {
        event.register(new HealthContainerSynchronizationTask(event.getListener()));
    }

    private record HealthContainerSynchronizationTask(ServerConfigurationPacketListener listener) implements ICustomConfigurationTask {

        public static final Type TYPE = new Type(MedicalSystem.resource("health_container_sync"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            // send health system definitions
            sender.accept(MedicalSystem.HEALTH_SYSTEM.getConfigurationPayload());
            // send server-configured weapon effect chances
            MedSystemConfig cfg = MedicalSystem.getConfig();
            sender.accept(new S2C_SendServerEffectChances(
                    cfg.swordLightBleedChance,
                    cfg.swordHeavyBleedChance,
                    cfg.axeLightBleedChance,
                    cfg.axeHeavyBleedChance,
                    cfg.axeFractureChance,
                    cfg.bluntFractureChance,
                    cfg.bluntLightBleedChance
            ));
            this.listener.finishCurrentTask(TYPE);
        }

        @Override
        public Type type() {
            return TYPE;
        }
    }
}
