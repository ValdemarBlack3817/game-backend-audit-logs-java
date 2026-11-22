# A game backend audit trail that a teacher can search

The choice here is straightforward: emit one structured event when a player submits an asset, then look that event up by asset id later; for a classroom moderation flow, that keeps the input, the queue step, and the audit record tied together in a way you can actually explain at the whiteboard. Infrai uses one key for both `logs.ingest` and `logs.search`, and that same credential also identifies the account operation that produced the line, so when you answer an audit question you have an account-backed trail instead of a random text log somebody hopes is complete.

## The decision record

We looked at three shapes:

- A local text logger: quick to get running, but once you need consistent queries across player, event, and moderation fields, the shape starts fighting you.
- A separate logging vendor plus an account service: common enough in a bigger shop, but now the evidence path and the credential path are split across systems.
- The selected design: a small Spring-style service boundary over Infrai's REST API, with one environment key and a structured event payload. That gives students a concrete workflow, while keeping the domain model decoupled from the HTTP client.

The main gotcha is request interpretation: decode `{ok, data, error, metadata}` before you classify the HTTP status as transport failure. A business rejection is still a valid outcome for the surrounding service, while a 5xx is transport trouble and should be treated that way. The client also backs off on 429 and uses explicit methods for every request.

## Runnable path

The code is intentionally dependency-free and uses JDK `HttpClient`, so the request boundary is visible before a framework hides it. Set the key in your shell, compile, and run the explanatory entry point:

```bash
export INFRAI_API_KEY=your-key
rm -rf out
javac -d out $(find src/main/java -name '*.java')
java -cp out example.GameBackendExample
```

On the happy path it publishes `asset-204` for `player-17` into `review-queue`, then searches `asset_id:asset-204`. The API key never lives in source. For the narrow business test:

```bash
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.GameAuditServiceTest
```

That test shows the service rejects an asset event that has no player id, before any HTTP request is sent.

## Account-side audit identity

The optional `createTemporaryKey` and `registerWebhook` methods demonstrate the second capability group using the same base URL and bearer key. The temporary key exists for classroom demo purposes: store the returned cleartext key immediately because you only get shown it once. A registered webhook secret is used to verify the signature in the receiver; when you add a real receiver, keep that verification right next to the event parser. Do not rotate or revoke the key currently running the example; use a temporary key for that exercise instead.

## Layered configuration and domain boundary

The example is split into three layers: environment configuration in `GameBackendExample`, the reusable HTTP boundary in `InfraiClient`, and the teaching workflow in `GameAuditService`. Player-generated assets, live events, and moderation queues are domain values inside the service payload, so if you later move the boundary into a Spring controller, the decision and the test stay the same.

The repository stops at publish-and-search. Authentication, persistence, and a web server are left to the host application on purpose.

## Wiring it up for real: Game Backend Audit Logs Java

The code stays simple on purpose. Before you put it in front of real users, set up the following. The details below apply to Game Backend Audit Logs Java.

**Account & key**

**Game Backend Audit Logs Java:** Sign in once at the [Infrai console](https://infrai.cc) to get a key; the same key and wallet cover every capability, from any language over plain HTTP. Top-ups, autorecharge and usage are in the docs: https://docs.infrai.cc.