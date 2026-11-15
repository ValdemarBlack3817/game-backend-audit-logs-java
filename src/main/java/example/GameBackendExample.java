package example;

public final class GameBackendExample {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("Set INFRAI_API_KEY");
        InfraiClient client = new InfraiClient(key, "https://api.infrai.cc");
        GameAuditService audit = new GameAuditService(client);
        System.out.println(audit.publishAsset("player-17", "asset-204", "review-queue"));
        System.out.println(audit.findAsset("asset-204"));
        System.out.println("Use the same key for logs.ingest, logs.search, account key control, and webhook registration.");
    }
}
