package tnt.tarkovcraft.medsystem.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tnt.tarkovcraft.medsystem.network.MedicalSystemNetwork;

public record S2C_ClearDownedPose() implements CustomPacketPayload {

    public static final Type<S2C_ClearDownedPose> TYPE = new Type<>(MedicalSystemNetwork.createId(S2C_ClearDownedPose.class));
    
    public static final StreamCodec<ByteBuf, S2C_ClearDownedPose> CODEC = StreamCodec.of(
            (buf, packet) -> {
                // No data to write
            },
            (buf) -> new S2C_ClearDownedPose()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleMessage(S2C_ClearDownedPose packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().getReceptionSide().isClient()) {
                Player player = context.player();
                if (player != null) {
                    // Force clear the pose on client side
                    player.setForcedPose(null);
                    player.setPose(net.minecraft.world.entity.Pose.STANDING);
                    player.refreshDimensions();
                }
            }
        });
    }
}