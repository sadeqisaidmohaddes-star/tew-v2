# TalkBack / gesture-passthrough spike

The one open technical risk named in [`../README.md`](../README.md): the
card-deck model needs a gesture surface TalkBack does not intercept before the
app sees it. iOS has a direct-interaction trait for this. Android has no clean
equivalent, and the pattern the README proposes to test is *mark the surface
not-important-for-accessibility and handle raw touch dispatch yourself*.

This spike tests that pattern and nothing else. It is throwaway code — no
`:core` dependency, no card-deck logic, delete it once the question is settled.

## Status of the answer

**Not yet answered empirically.** The code is written, builds, and is ready to
run. It has not been run with TalkBack on, because the environment it was built
in has no KVM and no attached device, so no emulator and no real handset. See
[Why this needs a human](#why-this-needs-a-human).

The verdict logic and the latency maths are unit-tested on the JVM
(`./gradlew :feature-carddeck:test`). What those tests cover is the *reading of
the result*, not the result. Do not mistake a green test run for a passing
spike.

## What it measures

| Question | How |
| --- | --- |
| Do raw gestures reach the app with TalkBack on? | `ProbeFrameLayout` records every touch and hover arriving at the content view; `verdictFor()` reduces the stream |
| Does TalkBack still work elsewhere on the same screen? | The header, buttons and log are ordinary accessible content — the control group |
| Gesture → audible feedback latency | Timed from gesture recognition to earcon dispatch and to TTS utterance start |

Two audible routes are timed separately on purpose. A short earcon and a spoken
word are not interchangeable, and the 100ms rule in `IMPLEMENTATION.md` is far
more likely to be satisfiable by the first than the second.

## Getting it onto a phone

### Without a development machine

Every CI run on `dev` leaves an installable debug APK behind, so no toolchain
is needed:

1. Open the repo's **Actions** tab, pick the most recent `CI` run on `dev`.
2. Download the **`tew-debug-apk`** artifact from the bottom of the run page.
3. Unzip it (GitHub wraps artifacts in a `.zip`) and install `app-debug.apk`.
   Android will ask permission to install from an unknown source — this is a
   debug build, not a Play Store one, so that prompt is expected.

Two frictions worth knowing before relying on this: downloading an Actions
artifact requires being **signed in to GitHub**, even though the repo is
public, and the download is a `.zip` rather than the APK directly. Both are
awkward on a phone with a screen reader. If that proves to be a real barrier
rather than a nuisance, attaching the APK to a GitHub prerelease would make it
a one-tap public download — that deviates from `GITHUB_WORKFLOW.md`, which
reserves releases for semver tags on `prod`, so it is Said's call rather than
something to do quietly.

Artifacts are kept for 30 days. Past that, re-run the workflow from the
Actions tab (**Run workflow**) to get a fresh one.

### With a development machine

```
cd android
./gradlew :app:installDebug
```

### Starting the spike

Debug builds put **TEW gesture spike** in the app drawer as its own icon,
next to the TEW app itself. Tap it. That is the whole step — no adb, no
computer.

The icon comes from `src/debug/AndroidManifest.xml`, so it exists only in
debug builds and cannot reach a release. Release builds keep the activity
declared but unreachable from the launcher.

If you would rather start it from a machine that already has adb:

```
adb shell am start -n org.teww.tew/org.teww.tew.feature.carddeck.spike.TalkBackPassthroughSpikeActivity
```

### The protocol

Run all three passes. Pass A is the control — without it, a failure in pass B
cannot be distinguished from a bug in the spike itself.

**Pass A — TalkBack OFF.**
1. Confirm the header reads "Touch exploration: OFF".
2. Swipe left and right on the middle band, ten times, unhurried.
3. Expect: an earcon and a spoken direction on every swipe; verdict line reads
   *Raw gestures REACHED the app*; event log shows `RAW_TOUCH DOWN … UP`.
4. Record both latency lines.

If pass A does not behave this way, the spike is broken — stop and fix it
before reading anything into pass B.

**Pass B — TalkBack ON.** This is the actual test.
1. Enable TalkBack. Return to the spike. Confirm the header now reads
   "Touch exploration: ON".
2. Swipe left and right on the middle band, ten times.
3. Record the verdict line and the event log verbatim.
4. Record both latency lines, or "none" if no swipe was ever recognised.

**Pass C — TalkBack ON, control group.** With TalkBack still on:
1. Swipe through the screen's elements the normal TalkBack way.
2. Confirm the title, the status line, the verdict, the latency lines and both
   buttons are all reachable and announced.
3. Confirm the "Speak results" and "Clear" buttons activate with a double-tap.

Pass C is what separates "we found a selective passthrough" from "we broke
accessibility on this screen". A card deck that gets its gestures by making the
screen unusable to a screen reader has failed, not succeeded.

### Reading the verdict

| Verdict | Meaning |
| --- | --- |
| `RAW_TOUCH_COMPLETED` | Full `DOWN…UP` arrived. The mechanism holds. |
| `RAW_TOUCH_CANCELLED` | App saw the start, then the stream was pulled. Signature of the accessibility input filter taking the gesture over. |
| `ONLY_HOVER` | Only exploration hovers arrived. TalkBack owns the touch stream. |
| `NO_INPUT` | Nothing arrived — or nobody touched the screen. |

`RAW_TOUCH_CANCELLED` and `ONLY_HOVER` are both failures of the mechanism, but
they are different failures and worth recording distinctly.

## What we expect, and why — hypothesis, not result

Stated up front so the run can falsify it rather than confirm it, and flagged
as unverified so nobody downstream reads it as a finding.

When TalkBack turns on touch exploration, interception happens in the system's
accessibility input filter — above the app's window, before the app's view
hierarchy is consulted at all. If that is right, then
`importantForAccessibility` / `clearAndSetSemantics` cannot affect it: those
change the *accessibility node tree* (what gets announced and focused), not the
*input pipeline*. The expected result of pass B is therefore `ONLY_HOVER` or
`RAW_TOUCH_CANCELLED`, not a pass.

Android does have a passthrough mechanism, but it is the wrong shape for this:

- `AccessibilityService#setTouchExplorationPassthroughRegion()` (API 30+) is
  called *by the accessibility service*, not by the app. TEW cannot invoke it
  for its own window.
- TalkBack's double-tap-and-hold passthrough is user-initiated and, critically,
  **is a timed gesture** — which collides head-on with non-negotiable #6, *no
  timed or precise gestures*. Even if it works, it is not available to this
  product.

**If pass B fails, that is a finding, not a defeat** — it is exactly what this
spike was built on day one to discover. The card deck would then need its
gesture vocabulary expressed as semantic actions that TalkBack itself
dispatches (custom accessibility actions surfaced in TalkBack's menu and
mappable to TalkBack's own gestures), rather than as raw touch. That path is
already implied by non-negotiable #5, *every action has three routes*.

## Why this needs a human

The build environment has no hardware virtualisation (`/dev/kvm` absent, no
`vmx`/`svm` in `/proc/cpuinfo`), so no Android emulator. TalkBack also ships
with Google Play services rather than AOSP, so even a working emulator needs a
Google APIs system image.

`HANDLING_PROTOCOLS.md` already covers this case: *never treat "it built" as
"it works"*. The spike is code that compiles and is ready to run. The answer
comes from a person with a phone.

## Limits of the latency numbers

The figures this screen reports are **lower bounds**, not conformance
measurements:

- The earcon number ends when `ToneGenerator` accepts the tone — the app
  handing audio to the framework mixer, not sound reaching the ear. Everything
  after that (mixer, HAL, DAC, and far more over Bluetooth) is invisible from
  inside the process.
- The speech number ends at the TTS engine's `onStart`, which is the start of
  output, not an acoustic event.

So a route that is already over 100ms here is definitively over budget. A route
that looks under 100ms here is *not yet proven* under it. Certifying against
`IMPLEMENTATION.md`'s rule needs external measurement — high-frame-rate capture
or an audio loopback rig — on the 3-year-old budget device the rule names, not
on a development machine.

Numbers from a flagship phone do not answer this question either. That is the
same substitution `IMPLEMENTATION.md` explicitly rules out.
