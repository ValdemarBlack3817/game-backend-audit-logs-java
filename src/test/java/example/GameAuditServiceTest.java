package example;

import java.io.IOException;
import java.net.http.HttpClient;

public final class GameAuditServiceTest {
    public static void main(String[] args) throws Exception {
        boolean rejected = false;
        try { new GameAuditService(new InfraiClient("test", "http://127.0.0.1")).publishAsset("", "asset-1", "review"); }
        catch (IllegalArgumentException expected) { rejected = true; }
        if (!rejected) throw new AssertionError("missing player id must be rejected before publish");
        System.out.println("GameAuditServiceTest passed: incomplete asset events are not published");
    }
}
