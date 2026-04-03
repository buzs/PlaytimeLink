package _1ms.playtimelink;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record VelocityPlaytimePayload(byte[] data) implements CustomPayload {
    public static final CustomPayload.Id<VelocityPlaytimePayload> ID =
            new CustomPayload.Id<>(Identifier.of("velocity", "playtime"));

    public static final PacketCodec<RegistryByteBuf, VelocityPlaytimePayload> CODEC =
            PacketCodec.of((payload, buf) -> buf.writeBytes(payload.data()),
                    buf -> {
                        byte[] data = new byte[buf.readableBytes()];
                        buf.readBytes(data);
                        return new VelocityPlaytimePayload(data);
                    });

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
