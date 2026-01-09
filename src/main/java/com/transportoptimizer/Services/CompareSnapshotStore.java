package com.transportoptimizer.Services;

import com.transportoptimizer.entity.FareEstimate;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Component
public class CompareSnapshotStore {

    private static final long TTL_SECONDS = 300;

    private static class Snapshot {
        FareEstimate estimate;
        Instant expiresAt;

        Snapshot(FareEstimate estimate, Instant expiresAt) {
            this.estimate = estimate;
            this.expiresAt = expiresAt;
        }
    }
    private final Map<String, Snapshot> store = new ConcurrentHashMap<>();

    public void save(String snapshotId, FareEstimate estimate) {
        store.put(
                snapshotId,
                new Snapshot(
                        estimate,
                        Instant.now().plusSeconds(TTL_SECONDS)
                )
        );
    }

    public FareEstimate get(String snapshotId) {
        Snapshot snapshot = store.get(snapshotId);
        if (snapshot == null) return null;

        if (Instant.now().isAfter(snapshot.expiresAt)) {
            store.remove(snapshotId);
            return null;
        }

        return snapshot.estimate;
    }

    public void remove(String snapshotId) {
        store.remove(snapshotId);
    }
}
