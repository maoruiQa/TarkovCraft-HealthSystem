package tnt.tarkovcraft.medsystem.network.message;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tnt.tarkovcraft.medsystem.common.effect.DownedPlayerStatusEffect;
import tnt.tarkovcraft.medsystem.common.health.HealthContainer;
import tnt.tarkovcraft.medsystem.common.health.HealthSystem;
import tnt.tarkovcraft.medsystem.network.MedicalSystemNetwork;

public record C2S_GiveUp() implements CustomPacketPayload {

    public static final Type<C2S_GiveUp> TYPE = new Type<>(MedicalSystemNetwork.createId(C2S_GiveUp.class));
    
    public static final StreamCodec<ByteBuf, C2S_GiveUp> CODEC = StreamCodec.of(
            (buf, packet) -> {
                // No data to write
            },
            (buf) -> new C2S_GiveUp()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleMessage(C2S_GiveUp packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().getReceptionSide().isServer()) {
                ServerPlayer player = (ServerPlayer) context.player();
                
                // Verify player is actually downed before allowing give up
                if (HealthSystem.hasCustomHealth(player)) {
                    HealthContainer container = HealthSystem.getHealthData(player);
                    if (container.isPlayerDowned()) {
                        // Don't remove downed effect first - let normal death process handle it
                        // This preserves the death screen
                        player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
                    }
                }
            }
        });
    }
}