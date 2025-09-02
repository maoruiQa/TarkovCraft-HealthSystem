package tnt.tarkovcraft.medsystem.network.message;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import tnt.tarkovcraft.medsystem.MedicalSystem;
import tnt.tarkovcraft.medsystem.client.ClientServerConfigState;
import tnt.tarkovcraft.medsystem.network.MedicalSystemNetwork;

public record S2C_SendServerEffectChances(
        float swordLightBleed,
        float swordHeavyBleed,
        float axeLightBleed,
        float axeHeavyBleed,
        float axeFracture,
        float bluntFracture,
        float bluntLightBleed
) implements CustomPacketPayload {

    public static final ResourceLocation PACKET_ID = MedicalSystemNetwork.createId(S2C_SendServerEffectChances.class);
    public static final Type<S2C_SendServerEffectChances> TYPE = new Type<>(PACKET_ID);
    public static final StreamCodec<FriendlyByteBuf, S2C_SendServerEffectChances> CODEC = StreamCodec.of(
            (buffer, value) -> value.encode(buffer),
            S2C_SendServerEffectChances::decode
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.swordLightBleed);
        buf.writeFloat(this.swordHeavyBleed);
        buf.writeFloat(this.axeLightBleed);
        buf.writeFloat(this.axeHeavyBleed);
        buf.writeFloat(this.axeFracture);
        buf.writeFloat(this.bluntFracture);
        buf.writeFloat(this.bluntLightBleed);
    }

    private static S2C_SendServerEffectChances decode(FriendlyByteBuf buf) {
        float swordL = buf.readFloat();
        float swordH = buf.readFloat();
        float axeL = buf.readFloat();
        float axeH = buf.readFloat();
        float axeFx = buf.readFloat();
        float bluntFx = buf.readFloat();
        float bluntL = buf.readFloat();
        return new S2C_SendServerEffectChances(swordL, swordH, axeL, axeH, axeFx, bluntFx, bluntL);
    }

    public void handleMessage(IPayloadContext context) {
        // client thread
        ClientServerConfigState.update(
                this.swordLightBleed,
                this.swordHeavyBleed,
                this.axeLightBleed,
                this.axeHeavyBleed,
                this.axeFracture,
                this.bluntFracture,
                this.bluntLightBleed
        );
        MedicalSystem.LOGGER.debug(MedicalSystem.MARKER,
                "Received server effect chances: sword(L={},H={}), axe(L={},H={},Fx={}), blunt(Fx={},L={})",
                swordLightBleed, swordHeavyBleed, axeLightBleed, axeHeavyBleed, axeFracture, bluntFracture, bluntLightBleed);
    }
}
