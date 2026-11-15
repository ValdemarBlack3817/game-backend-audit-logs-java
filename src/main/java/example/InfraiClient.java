package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** Small REST client: decode Infrai's envelope before interpreting HTTP status. */
public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key, String baseUrl) {
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public String call(String method, String path, String json) throws IOException, InterruptedException {
        int attempt = 0;
        while (true) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            if (!envelopeIsOk(body)) {
                throw new IllegalStateException("Infrai rejected the request: " + errorText(body));
            }
            if (response.statusCode() == 429 && attempt < 3) {
                long delay = retryAfter(response, attempt);
                Thread.sleep(delay);
                attempt++;
                continue;
            }
            if (response.statusCode() >= 500) {
                throw new IOException("Infrai transport status " + response.statusCode());
            }
            return body;
        }
    }

    private static boolean envelopeIsOk(String body) {
        return body.contains("\"ok\":true") || body.contains("\"ok\": true");
    }

    private static String errorText(String body) {
        int start = body.indexOf("\"error\"");
        return start < 0 ? body : body.substring(start, Math.min(body.length(), start + 180));
    }

    private static long retryAfter(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(InfraiClient::secondsToMillis)
                .orElse(250L * (1L << attempt));
    }

    private static long secondsToMillis(String value) {
        try { return Math.max(100L, Long.parseLong(value) * 1000L); }
        catch (NumberFormatException ignored) { return 250L; }
    }

    public String ingest(String payload) throws IOException, InterruptedException {
        return call("POST", "/v1/logs/ingest", payload); // logs.ingest
    }

    public String search(String query) throws IOException, InterruptedException {
        return call("GET", "/v1/logs/search" + query, null); // logs.search
    }

    public String createTemporaryKey(String name) throws IOException, InterruptedException {
        return call("POST", "/v1/account/keys/create", "{\"name\":\"" + name + "\"}");
    }

    public String registerWebhook(String url, String events, String secret)
            throws IOException, InterruptedException {
        return call("POST", "/v1/account/webhooks/register",
                "{\"url\":\"" + url + "\",\"events\":[\"" + events + "\"],\"secret\":\"" + secret + "\"}");
    }
}
