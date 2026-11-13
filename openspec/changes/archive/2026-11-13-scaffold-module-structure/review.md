# Artifact review (light): scaffold-module-structure

**Verdict:** Ready
**Reviewed:** 2026-11-13, schema `careful-planning`, repo kind `code`

| Pass | Blocker | Major | Minor |
|---|---|---|---|
| Testability | 0 | 1 | 3 |
| Security | 0 | 0 | 0 |
| Hidden assumptions | 0 | 0 | 2 |
| Design contract | 0 | 1 | 1 |

## Findings

### T1: iOS network error mapping is not specified
**Severity:** major · **Status:** applied
**Location:** design.md > 7. Core data: HttpClient and safe calls; 10. Tests
**Current text:** "iOS adds `DarwinHttpRequestException` → `NSError` code mapping." and "`iosTest`: `NSError.toNetworkError()` codes and a foreign domain → `UNKNOWN`."
**Failure scenario:** Android lists exact exceptions, iOS lists none, so the iOS tests assert whatever the implementer picked. Because no `HttpTimeout` plugin is installed (D14), `HttpRequestTimeoutException` never fires in production. A real iOS timeout arrives only as `NSURLErrorTimedOut`, and if the mapping misses it, it becomes `UNKNOWN` on iOS but `REQUEST_TIMEOUT` on Android. The common `HttpRequestTimeoutException` test still passes. "Backend not running" has the same problem: `ConnectException` gives `NO_INTERNET` on Android, but `NSURLErrorCannotConnectToHost` could give `UNKNOWN` on iOS.
**Fix:** In design.md §7, replace the iOS bullet with:
"iOS adds `DarwinHttpRequestException` → `NSError.toNetworkError()`. For domain `NSURLErrorDomain`: `NSURLErrorNotConnectedToInternet` (-1009), `NSURLErrorNetworkConnectionLost` (-1005), `NSURLErrorCannotFindHost` (-1003), `NSURLErrorCannotConnectToHost` (-1004), `NSURLErrorDNSLookupFailed` (-1006) → `NO_INTERNET`; `NSURLErrorTimedOut` (-1001) → `REQUEST_TIMEOUT`; every other code → `UNKNOWN`. Any other domain → `UNKNOWN`. This matches Android, where `UnknownHostException`/`ConnectException` → `NO_INTERNET` and `SocketTimeoutException` → `REQUEST_TIMEOUT`. Since no `HttpTimeout` plugin is installed, these platform exceptions are the only production timeout path."
In §10, replace the `iosTest` sentence with: "`iosTest`: one test per code listed in §7, one `NSURLErrorDomain` code not listed (e.g. `NSURLErrorBadURL`, -1000) → `UNKNOWN`, and a foreign domain with code -1009 → `UNKNOWN`."
Catches: an implementation that maps only -1009 and lets iOS timeouts become `UNKNOWN`.

### D1: Task 5.2 requires editing `local.properties`, which task 5.1 forbids
**Severity:** major · **Status:** applied
**Location:** tasks.md > 5.1 and 5.2
**Current text:** 5.1: "Never edit `local.properties` yourself." 5.2: "Then temporarily rename one key and confirm the build fails with the documented message."
**Failure scenario:** The implementer either breaks 5.1 by editing the developer's `local.properties`, risking a lost or corrupted local setup, or skips the failure-path check and ticks 5.2 without observing it.
**Fix:** Replace the last two sentences of 5.2 with: "Then ask the user to comment out `BASE_URL_IOS` in `local.properties`, run `./gradlew :core:data:help`, and confirm the output contains `Missing BASE_URL_IOS in local.properties`. Ask the user to restore the key. Verify: both checks are observed and the user confirms the key is restored."

### T2: Task 2.4's feature-isolation check can't fail
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 2.4
**Current text:** "`grep -rn \"projects.feature\" feature/*/*/build.gradle.kts` shows no dependency on another feature."
**Failure scenario:** Every `data` and `presentation` module depends on its own `projects.feature.<f>.domain`, so the grep always has matches. A cross-feature dependency hidden among them goes unnoticed.
**Fix:** "Verify: `for f in auth boards cards labels transfer; do grep -n 'projects\.feature\.' feature/$f/*/build.gradle.kts | grep -v \"projects\.feature\.$f\.\"; done` prints nothing."
Catches: `feature/cards/presentation` depending on `projects.feature.labels.domain`.

### T3: The Koin test passes even if the engine override doesn't apply
**Severity:** minor · **Status:** applied
**Location:** tasks.md > 6.3
**Current text:** "starts Koin with `coreDataModule` plus a test module overriding the engine with `MockEngine`, resolves `HttpClient` and stops Koin."
**Failure scenario:** If the override is ignored, for example because the test module loads first, `HttpClient` still resolves with the real OkHttp/Darwin engine and the test is green. If Koin isn't stopped after a failure, later tests in the module fail with `KoinApplicationAlreadyStartedException`.
**Fix:** "...resolves `HttpClient`, sends one `get<Unit>("/ping")` through it, and asserts the `MockEngine` recorded exactly one request to `${BuildKonfig.BASE_URL}/ping`. `stopKoin()` runs in `@AfterTest`."
Catches: a module order where the platform engine wins.

### T4: The catch-all status branches have no fixed test values
**Severity:** minor · **Status:** applied
**Location:** design.md > 10. Tests
**Current text:** "It covers each status in D11"
**Failure scenario:** D11's "other 5xx" and "other statuses" have no example status. An implementation that maps only `500 → SERVER_ERROR` and sends 502 to `UNKNOWN` passes if only the named statuses are tested. Shared `api-errors` requires 5xx to be treated as temporary.
**Fix:** "It covers each status named in D11, plus 502 and 504 → `SERVER_ERROR`, and 418 and 451 → `UNKNOWN`."

### A1: Release builds embed the dev HTTP base URL
**Severity:** minor · **Status:** applied
**Location:** design.md > Risks / Trade-offs
**Current text:** (missing)
**Failure scenario:** KMP library modules have no build types, so BuildKonfig gives `assembleRelease` the same `http://10.0.2.2:8080/api/v1`. On Android, release blocks cleartext, so every call fails with `UnknownServiceException` → `UNKNOWN`. A release iOS build talks HTTP to localhost. Someone testing a release build sees "unknown error" everywhere with no hint why.
**Fix:** Add a Risks entry: "[One `BASE_URL` per platform for all build types, so release builds embed the dev HTTP URL and fail every request on Android] → Release builds aren't supported by this change. Per-build-type URLs are settled together with the production URL open item."

### A2: A `BASE_URL_*` value without `/api/v1` fails silently
**Severity:** minor · **Status:** applied
**Location:** design.md > 4. Base URL through BuildKonfig
**Current text:** "It requires `BASE_URL_ANDROID` and `BASE_URL_IOS`. If either is missing, it throws ..."
**Failure scenario:** CLAUDE.md still describes the base URL as `http://10.0.2.2:8080` without the prefix, so a developer enters that. Every call then returns 404 → `NOT_FOUND`, which looks like a missing resource, not a config error.
**Fix:** Append: "It also fails, with `GradleException(\"BASE_URL_ANDROID in local.properties must be an http(s) URL ending in /api/v1, got <value>\")`, when a value doesn't start with `http://` or `https://` or doesn't end in `/api/v1` after trimming a trailing `/`." (This adds validation the decisions don't cover. Accept or reject it as a new choice.)

### D2: The docs commit differs between design and tasks and skips the gates
**Severity:** minor · **Status:** applied
**Location:** design.md > 11. Commits (item 6); tasks.md > 7.1, 7.2
**Current text:** design: "`docs: update agents.md for base url keys` and any skill note fixes the user approves." tasks 7.1: "commit `docs: update agents.md for module skeleton`". 7.2: "without editing anything".
**Failure scenario:** The message differs, design allows skill edits that 7.2 forbids, and 7.1 commits without running the gates, which CLAUDE.md requires before every commit.
**Fix:** design §11 item 6: "`docs: update agents.md for module skeleton`. Skill conflicts are reported only, not edited (tasks 7.2)." tasks 7.1: "...Verify: the user approves and all gates pass, then commit `docs: update agents.md for module skeleton`."

## Trust boundaries
- Network transport, debug: cleartext allowed to any host in Android debug builds. Accepted by D17, and design §9 explains why there's no domain list. Stopped for release by the absent override.
- Network transport, release: iOS keeps `NSAllowsLocalNetworking` in release. This is accepted in Risks. The release base URL is still the dev HTTP URL (A1), which leads to failures, not exposure.
- Server responses: bodies are decoded with `ignoreUnknownKeys`, error bodies aren't parsed, and nothing is logged. Nothing could leak, and a malformed body maps to `SERIALIZATION`/`UNKNOWN`. Stopped.
- Request routing: `constructRoute` always prefixes the base URL, so an absolute foreign URL can't redirect a request (relevant once bearer tokens exist). Stopped.
- `local.properties` → generated source: values come from the developer. Not an attack surface.
- Auth, token storage, imported files, deep links: not in this change.

## Coverage
Read: decisions.md (D1–D26), proposal.md, design.md, tasks.md (31 tasks), `openspec/config.yaml`, AGENTS.md (CLAUDE.md is a symlink to it), the current Gradle files, version catalog, `:shared` build file, iOS Swift files, `Info.plist`, `project.pbxproj` build phase, Android manifest, and shared specs `api-errors` and `api-conventions`. Specs are skipped by decision (D24), so there were no requirements or scenarios to check. The testability pass was applied to the test plan in design §10 and the task verifications instead. The light review didn't open the `kmp-*` skills or check library version compatibility, which the design defers to apply time.

## Applied changes
All eight findings were applied on 2026-11-13 at the user's request.

- `design.md`: §4 adds base URL format validation (A2). §7 lists the iOS `NSError` codes and notes the production timeout path (T1). §10 adds the catch-all status cases (T4) and the per-code `iosTest` list (T1). §11 aligns the docs commit (D2). Risks adds the release-build entry (A1).
- `tasks.md`: 1.5 names both failure messages (A2). 2.4 uses the per-feature grep (T2). 5.2 has the user toggle the key (D1). 6.3 sends a request through `MockEngine` and stops Koin in `@AfterTest` (T3). 7.1 runs the gates before committing (D2).
- `proposal.md`: the breaking-change note says the values must be http(s) URLs ending in `/api/v1` (A2).
- A2 adds validation that `decisions.md` doesn't cover. It doesn't contradict a decision, but it isn't recorded there.
- `openspec validate scaffold-module-structure --strict` passes. No accepted risks, and no non-standard artifacts to update.
