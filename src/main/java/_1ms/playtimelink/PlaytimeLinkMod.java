package _1ms.playtimelink;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlaytimeLinkMod implements DedicatedServerModInitializer {
    public static final String MOD_ID = "playtimelink-fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final PlaytimeCache CACHE = new PlaytimeCache();

    @Override
    public void onInitializeServer() {
        PayloadRegistrations.register();
        VelocityProtocolHandler protocolHandler = new VelocityProtocolHandler(CACHE);
        protocolHandler.register();
        JoinEventHandlers.register(protocolHandler);
        PlaytimePlaceholders.register(CACHE);

        LOGGER.info("PlaytimeLink Fabric initialized");
    }
}

final class PayloadRegistrations {
    private PayloadRegistrations() {
    }

    static void register() {
        PayloadTypeRegistry.playC2S().register(VelocityPlaytimePayload.ID, VelocityPlaytimePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VelocityPlaytimePayload.ID, VelocityPlaytimePayload.CODEC);
    }
}

final class JoinEventHandlers {
    private static final java.util.concurrent.atomic.AtomicInteger TICK_COUNTER = new java.util.concurrent.atomic.AtomicInteger();

    private JoinEventHandlers() {
    }

    static void register(VelocityProtocolHandler protocolHandler) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                {
                    if (server.getPlayerManager().getCurrentPlayerCount() == 1) {
                        protocolHandler.sendRpt(server);
                        protocolHandler.sendRtl(server);
                    }
                }
        );
        ServerTickEvents.END_SERVER_TICK.register(server -> refreshPeriodicSync(protocolHandler, server));
    }

    private static void refreshPeriodicSync(VelocityProtocolHandler protocolHandler, MinecraftServer server) {
        if (server.getPlayerManager().getCurrentPlayerCount() <= 0) {
            TICK_COUNTER.set(0);
            return;
        }

        if (TICK_COUNTER.incrementAndGet() >= 20) {
            TICK_COUNTER.set(0);
            protocolHandler.sendRpt(server);
            protocolHandler.sendRtl(server);
        }
    }
}

final class PlaytimePlaceholders {
    private static final Identifier TOTAL_HOURS = Identifier.of("vptlink", "totalhours");
    private static final Identifier MINUTES = Identifier.of("vptlink", "minutes");
    private static final Identifier FORMATTED = Identifier.of("vptlink", "formatted");
    private static final Identifier PLACE = Identifier.of("vptlink", "place");
    private static final Identifier TOP_NAME = Identifier.of("vptlink", "topname");
    private static final Identifier TOP_TIME_TOTAL_HOURS = Identifier.of("vptlink", "toptime_totalhours");

    private PlaytimePlaceholders() {
    }

    static void register(PlaytimeCache cache) {
        Placeholders.register(TOTAL_HOURS, (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("Player context required");
            }

            return cache.getPlaytime(ctx.player().getGameProfile().getName())
                    .map(value -> PlaceholderResult.value(TimeConverter.calcTotalPT(value, "hours")))
                    .orElseGet(() -> PlaceholderResult.value(Constants.LOADING_MESSAGE));
        });

        Placeholders.register(MINUTES, (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("Player context required");
            }

            return cache.getPlaytime(ctx.player().getGameProfile().getName())
                    .map(value -> PlaceholderResult.value(TimeConverter.convert(value, "minutes")))
                    .orElseGet(() -> PlaceholderResult.value(Constants.LOADING_MESSAGE));
        });

        Placeholders.register(FORMATTED, (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("Player context required");
            }

            return cache.getPlaytime(ctx.player().getGameProfile().getName())
                    .map(value -> PlaceholderResult.value(TimeConverter.formatCompact(value)))
                    .orElseGet(() -> PlaceholderResult.value(Constants.LOADING_MESSAGE));
        });

        Placeholders.register(PLACE, (ctx, arg) -> {
            if (!ctx.hasPlayer()) {
                return PlaceholderResult.invalid("Player context required");
            }

            return cache.getPlace(ctx.player().getGameProfile().getName())
                    .map(place -> PlaceholderResult.value(Integer.toString(place)))
                    .orElseGet(() -> PlaceholderResult.value(Constants.NOT_IN_TOPLIST_MESSAGE));
        });

        Placeholders.register(TOP_NAME, (ctx, arg) -> resolveTopPlace(arg)
                .flatMap(cache::getTopEntry)
                .map(entry -> PlaceholderResult.value(entry.getKey()))
                .orElseGet(() -> PlaceholderResult.value(Constants.LOADING_MESSAGE))
        );

        Placeholders.register(TOP_TIME_TOTAL_HOURS, (ctx, arg) -> resolveTopPlace(arg)
                .flatMap(cache::getTopEntry)
                .map(entry -> PlaceholderResult.value(TimeConverter.calcTotalPT(entry.getValue(), "hours")))
                .orElseGet(() -> PlaceholderResult.value(Constants.LOADING_MESSAGE))
        );
    }

    private static java.util.Optional<Integer> resolveTopPlace(String arg) {
        if (arg == null || arg.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            return java.util.Optional.of(Integer.parseInt(arg.trim()));
        } catch (NumberFormatException ignored) {
            return java.util.Optional.empty();
        }
    }
}
