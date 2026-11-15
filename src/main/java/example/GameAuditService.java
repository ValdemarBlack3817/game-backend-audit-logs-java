package example;

import java.io.IOException;
import java.time.Instant;

public final class GameAuditService {
    private final InfraiClient client;

    public GameAuditService(InfraiClient client) { this.client = client; }

    public String publishAsset(String playerId, String assetId, String moderationQueue)
            throws IOException, InterruptedException {
        if (playerId == null || playerId.isBlank() || assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("playerId and assetId are required");
        }
        String payload = "{\"entries\":[{" +
                "\"event\":\"asset_submitted\",\"player_id\":\"" + playerId + "\"," +
                "\"asset_id\":\"" + assetId + "\",\"queue\":\"" + moderationQueue + "\"," +
                "\"occurred_at\":\"" + Instant.now() + "\"}]}";
        return client.ingest(payload);
    }

    public String findAsset(String assetId) throws IOException, InterruptedException {
        return client.search("?q=asset_id%3A" + assetId);
    }
}
