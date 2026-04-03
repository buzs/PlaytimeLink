package _1ms.playtimelink;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public final class PlaytimeCache {
    private static final long TTL_MS = 5000L;

    private final ConcurrentHashMap<String, Long> playtimes = new ConcurrentHashMap<>();
    private final AtomicReference<List<Map.Entry<String, Long>>> toplist =
            new AtomicReference<>(List.of());

    private volatile long lastUpdateTime;

    public void updatePlaytimes(Map<String, Long> values) {
        playtimes.clear();
        playtimes.putAll(values);
        lastUpdateTime = System.currentTimeMillis();
    }

    public void updateToplist(Map<String, Long> values) {
        List<Map.Entry<String, Long>> snapshot = new ArrayList<>(values.entrySet());
        snapshot.sort(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry.comparingByKey()));

        List<Map.Entry<String, Long>> immutableSnapshot = new ArrayList<>(snapshot.size());
        for (Map.Entry<String, Long> entry : snapshot) {
            immutableSnapshot.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), entry.getValue()));
        }

        toplist.set(Collections.unmodifiableList(immutableSnapshot));
        lastUpdateTime = System.currentTimeMillis();
    }

    public Optional<Long> getPlaytime(String player) {
        return Optional.ofNullable(playtimes.get(player));
    }

    public Optional<Integer> getPlace(String player) {
        List<Map.Entry<String, Long>> snapshot = toplist.get();
        for (int i = 0; i < snapshot.size(); i++) {
            if (snapshot.get(i).getKey().equals(player)) {
                return Optional.of(i + 1);
            }
        }
        return Optional.empty();
    }

    public Optional<Map.Entry<String, Long>> getTopEntry(int place) {
        if (place < 1) {
            return Optional.empty();
        }

        List<Map.Entry<String, Long>> snapshot = toplist.get();
        if (place > snapshot.size()) {
            return Optional.empty();
        }

        Map.Entry<String, Long> entry = snapshot.get(place - 1);
        return Optional.of(new AbstractMap.SimpleImmutableEntry<>(entry));
    }

    public boolean isFresh() {
        return System.currentTimeMillis() - lastUpdateTime < TTL_MS;
    }
}
