package _1ms.playtimelink;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VelocityProtocolHandler {
    private static final Gson GSON = new Gson();
    private static final Type PLAYTIME_MAP_TYPE = new TypeToken<HashMap<String, Long>>() {
    }.getType();
    private static final Type TOPLIST_MAP_TYPE = new TypeToken<LinkedHashMap<String, Long>>() {
    }.getType();

    private final PlaytimeCache cache;
    private final AtomicBoolean preloadEnabled = new AtomicBoolean(false);
    private final AtomicBoolean firstConfLogged = new AtomicBoolean(false);
    private final AtomicBoolean firstPlaytimesLogged = new AtomicBoolean(false);
    private final AtomicBoolean firstToplistLogged = new AtomicBoolean(false);

    public VelocityProtocolHandler(PlaytimeCache cache) {
        this.cache = cache;
    }

    public void register() {
        ServerPlayNetworking.registerGlobalReceiver(VelocityPlaytimePayload.ID, (payload, context) ->
                context.server().execute(() -> handlePayload(payload, context.server()))
        );
    }

    public boolean isPreloadEnabled() {
        return preloadEnabled.get();
    }

    public void sendRpt(MinecraftServer server) {
        PlaytimeLinkMod.LOGGER.debug("Sending rpt handshake to Velocity proxy");
        send(server, "rpt", output -> output.writeUTF("rpt"));
    }

    public void sendRtl(MinecraftServer server) {
        PlaytimeLinkMod.LOGGER.debug("Sending rtl handshake to Velocity proxy");
        send(server, "rtl", output -> output.writeUTF("rtl"));
    }

    public void sendCc(MinecraftServer server) {
        send(server, "cc", output -> output.writeUTF("cc"));
    }

    private void handlePayload(VelocityPlaytimePayload payload, MinecraftServer server) {
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(payload.data()))) {
            String opcode = input.readUTF();

            switch (opcode) {
                case "pt" -> handlePlaytimes(input);
                case "ptt" -> handleToplist(input);
                case "rs" -> handleProxyRestart(server);
                case "conf" -> handleConfirmation(input);
                default -> PlaytimeLinkMod.LOGGER.warn("Ignoring unknown velocity:playtime opcode {}", opcode);
            }
        } catch (IOException | RuntimeException exception) {
            PlaytimeLinkMod.LOGGER.warn("Failed to decode velocity:playtime payload", exception);
        }
    }

    private void handlePlaytimes(DataInputStream input) throws IOException {
        String json = input.readUTF();
        Map<String, Long> playtimes = GSON.fromJson(json, PLAYTIME_MAP_TYPE);
        cache.updatePlaytimes(playtimes == null ? Map.of() : playtimes);
        if (firstPlaytimesLogged.compareAndSet(false, true)) {
            PlaytimeLinkMod.LOGGER.info("Received first playtime sync from Velocity proxy");
        }
    }

    private void handleToplist(DataInputStream input) throws IOException {
        String json = input.readUTF();
        Map<String, Long> toplist = GSON.fromJson(json, TOPLIST_MAP_TYPE);
        cache.updateToplist(toplist == null ? Map.of() : toplist);
        if (firstToplistLogged.compareAndSet(false, true)) {
            PlaytimeLinkMod.LOGGER.info("Received first toplist sync from Velocity proxy");
        }
    }

    private void handleProxyRestart(MinecraftServer server) {
        PlaytimeLinkMod.LOGGER.info("Velocity proxy restart detected, acknowledging and re-handshaking");
        sendCc(server);
        sendRpt(server);
        sendRtl(server);
    }

    private void handleConfirmation(DataInputStream input) throws IOException {
        boolean enabled = input.readBoolean();
        preloadEnabled.set(enabled);
        if (firstConfLogged.compareAndSet(false, true)) {
            PlaytimeLinkMod.LOGGER.info("Received first config sync from Velocity proxy (preload={})", enabled);
        }
    }

    private void send(MinecraftServer server, String opcode, PayloadWriter writer) {
        Optional<ServerPlayerEntity> conduit = getOnlinePlayer(server);
        if (conduit.isEmpty()) {
            return;
        }

        PlaytimeLinkMod.LOGGER.debug("Sending velocity:playtime outbound opcode {} via player connection", opcode);
        conduit.get().networkHandler.sendPacket(new CustomPayloadS2CPacket(new VelocityPlaytimePayload(encode(writer))));
    }

    private Optional<ServerPlayerEntity> getOnlinePlayer(MinecraftServer server) {
        List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if (players.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(players.get(0));
    }

    private byte[] encode(PayloadWriter writer) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writer.write(output);
            output.flush();
            return bytes.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to encode velocity:playtime payload", exception);
        }
    }

    @FunctionalInterface
    private interface PayloadWriter {
        void write(DataOutputStream output) throws IOException;
    }
}
