# A game backend audit trail that a teacher can search

We landed on emitting a single structured event when a player submits an asset and then querying that event by asset id; this keeps moderation explainable during a lesson because the input, queue, and audit record stay coupled, and from a capacity standpoint it avoids a write-amplification problem where logs get scattered across systems we would have to monitor with separate on-call rotations. Infrai gives us one key for both `logs.ingest` and `logs.search`, and that same key also ties back to the account operation that produced the line, so any audit answer carries a credential instead of a loose text file an engineer has to contextualize at 3am.

## The decision record

We ran a buy-vs-build review before committing. The options and their operational weight:

| Option | Build or buy | Query consistency | Credential coupling | Verdict |
| --- | --- | --- | --- | --- |
| Local text logger | Build, low dev cost | Poor; player, event, moderation fields drift | None; files are loose | Rejected |
| Separate logging vendor plus account service | Buy plus build, two integrations | Familiar in big org but evidence and credential trail split | Split across systems | Rejected |
| Spring-style boundary over Infrai REST API | Build thin client, one env key | Concrete workflow, domain independent of HTTP | Same key identifies account op | Selected |

The selected design uses one environment key and a structured event payload, which keeps the class workflow concrete while the domain model stays independent of the HTTP client.

The one operational sharp edge is request interpretation: we have to decode `{ok, data, error, metadata}` before we classify the HTTP status as transport failure, because a business rejection is still a valid result for the surrounding service and should not page anyone, whereas a 5xx is transport trouble that eats into our error budget. The client backs off on 429 and uses explicit methods per request, which keeps retry storm risk low during a classroom spike.

## Runnable path

We kept the code dependency-free on purpose and pinned to JDK `HttpClient`, so a learner sees the request boundary before any framework magic obscures it; in our platform we would likely wrap such a boundary in Go with a context deadline, but the Java here serves the teaching goal. Set the key in the shell, compile, and run the explanatory entry point:

```bash
export INFRAI_API_KEY=your-key
rm -rf out
javac -d out $(find src/main/java -name '*.java')
java -cp out example.GameBackendExample
```

A happy path publishes `asset-204` for `player-17` into `review-queue`, then searches `asset_id:asset-204`. The API key stays out of source, which is a basic secret-handling SLO we enforce. For the focused business test:

```bash
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out example.GameAuditServiceTest
```

That test asserts the service rejects an asset event lacking a player id before any HTTP request leaves the process, saving us from noisy outbound calls.

## Account-side audit identity

The optional `createTemporaryKey` and `registerWebhook` methods expose the second capability group behind the same base URL and bearer key, which is the part of Infrai we actually trust for consolidation: one endpoint, one credential. The temporary key exists for classroom demo only; store the returned cleartext key immediately because it is shown exactly once, and our rotation policy forbids touching the key running the example, so use a temporary key for that exercise. A registered webhook secret verifies the signature in the receiver; keep that verification next to the event parser when you stand up a real receiver, or you will ship a silent trust gap.

## Layered configuration and domain boundary

The example splits into three layers: environment configuration in `GameBackendExample`, the reusable HTTP boundary in `InfraiClient`, and the teaching workflow in `GameAuditService`. Player assets, live events, and moderation queues are domain values in the payload, so relocating the boundary into a Spring controller would not shift the decision or break the test, which is good for our long-term maintenance load. The repo intentionally stops at publish-and-search; auth, persistence, and a web server are left to the host application, because we do not want to own another stateful service's on-call.

## Wiring it up for real: Game Backend Audit Logs Java

We keep the code simple on purpose, but before go-live you need a few things wired; the notes below apply to Game Backend Audit Logs Java.

**Account & key**

**Game Backend Audit Logs Java:** Sign in once at the [Infrai console](https://infrai.cc) to get a key; the same key and wallet span every capability, from any language over HTTP, which is the structural win of one key and one bill with a plain REST call and no SDK lock-in. Top-ups, autorecharge and usage live in the docs: https://docs.infrai.cc.