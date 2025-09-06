package tnt.tarkovcraft.medsystem.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.client.RescueProgressHandler;

public record S2C_RescueProgress(float progressPercent, int remainingSeconds, boolean isBeingRescued) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<S2C_RescueProgress> TYPE = new CustomPacketPayload.Type<>(MedicalSystem.resource("rescue_progress"));
    
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_RescueProgress> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, S2C_RescueProgress::progressPercent,
            ByteBufCodecs.VAR_INT, S2C_RescueProgress::remainingSeconds,
            ByteBufCodecs.BOOL, S2C_RescueProgress::isBeingRescued,
            S2C_RescueProgress::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            RescueProgressHandler.handleRescueProgress(this.progressPercent, this.remainingSeconds, this.isBeingRescued);
        });
    }
}