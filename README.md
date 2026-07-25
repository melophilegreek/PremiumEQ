# Premium EQ

A system-wide Android equalizer built with Kotlin, Jetpack Compose, Material 3,
Hilt, and the official `android.media.audiofx` APIs. Targets Android 8.0
(API 26) through Android 16.

## ⚠️ One manual step for LOCAL builds only (not needed for GitHub Actions)

I generated every source file, resource, and Gradle config in this project, but
I could **not** download the Gradle wrapper JAR (`gradle/wrapper/gradle-wrapper.jar`)
because this environment has no network access.

- **Building via GitHub Actions:** nothing to do — `.github/workflows/build.yml`
  installs Gradle directly and generates the wrapper on the runner if it's
  missing, before building.
- **Building locally in Android Studio:** open the project folder; Android
  Studio detects the missing wrapper jar and generates it automatically on sync.
- **Building locally from a terminal:** run `gradle wrapper` once in the
  project root (requires any local Gradle install) to generate it.

## What's fully implemented

- **Runtime capability detection** – every effect (`Equalizer`, `BassBoost`,
  `Virtualizer`, `LoudnessEnhancer`) is instantiated defensively; the UI only
  shows controls for what actually succeeded on the running device, with a
  visible "not supported" message otherwise.
- **Multi-band EQ** – reads the device's real band count/frequencies/range
  (5/10/15/31-band devices are all handled the same way, since the code adapts
  to whatever `Equalizer.getNumberOfBands()` reports rather than hard-coding a
  layout) with 0.1 dB precision via millibel-level control.
- **Bass Boost, Virtualizer, Loudness Enhancer** – strength/gain control with
  graceful on/off-only fallback on devices that don't support variable strength.
- **Preamp with a gain limiter** – implemented as a per-band offset clamped to
  the hardware's own reported range, so it can't push a band past what the
  device supports.
- **Undo/redo history** – every mutating action (band edit, preamp, bass boost,
  virtualizer, loudness, preset apply) pushes a real snapshot onto an undo
  stack (50 deep); redo is a true forward-replay, not just "undo the undo."
- **A/B compare** – save the current state to slot A or B, then flip between
  them instantly to actually hear a change rather than just read numbers.
- **Per-band and global reset** – a reset button under every band, plus a
  one-tap "reset all" that's itself undoable.
- **Preset diffing** – `computePresetDiff` compares two presets band-by-band
  (matched by frequency, not index) plus every effect value, with a Compose
  view (`PresetDiffView`) to show it.
- **Preset thumbnails** – a small Canvas sparkline of each preset's curve
  (`PresetThumbnail`), so presets are recognizable without reading the name.
- **Diagnostics panel** – shows the real session id, exactly which effects
  attached successfully, the live band count/range, and the currently detected
  output device - for troubleshooting on unusual OEM builds.
- **Persisted, working customization** – dynamic color, AMOLED mode, a custom
  accent color, and corner radius are stored in DataStore and actually flow
  into `MaterialTheme`'s color scheme and `Shapes` app-wide (not just a toggle
  that does nothing). Haptics on/off is respected by `BandSlider`. Visualizer
  animation speed/sensitivity are persisted and ready for the visualizer UI to
  consume once the additional render styles are built.
- **Presets** – unlimited, foldered, favoritable, renameable, duplicable,
  exportable/importable as JSON (`PresetRepository`). Presets store gain by
  frequency (not raw band index) so they remain valid across devices with a
  different band count.
- **Output device detection** – `DeviceOutputMonitor` uses
  `AudioDeviceCallback` to report speaker / wired / Bluetooth / USB-DAC changes
  in real time.
- **Visualizer engine** – real FFT + waveform capture via
  `android.media.audiofx.Visualizer`, exposed as a `Flow<VisualizerFrame>`.
- **Home screen widget** (Glance) and **Quick Settings tile** – both real and
  functional; the tile shares live state with the app via the same
  `AudioEffectManager` singleton.
- Hilt/MVVM/Repository architecture throughout, Kotlin Coroutines + Flow for
  all async and reactive state.

## Explicitly not implemented (and why)

- **Crossfeed** – there is no official Android `AudioEffect` type for
  crossfeed, and the global-session (0) technique this app uses gives no hook
  into raw PCM to implement one manually. Doing this for real would require
  either root/Magisk-level system audio interception, or turning this app into
  a full media player with its own `AudioProcessing` chain (Media3). Not
  something to fake with a cosmetic slider.
- **True per-app EQ profiles** – third-party apps cannot attach an
  `AudioEffect` to *another app's* private audio session; only to the global
  mix (session 0) or a session the app itself created. What other "per-app EQ"
  apps on the Play Store actually do is either (a) also act as the media
  player itself, or (b) use Accessibility/root-level tricks. Neither fits this
  app's architecture, so it isn't included.

## What's intentionally scaffolded or not yet started

Given the scope of the original request, these have real interfaces and/or
partial implementations wired into the architecture, but are not complete
end-to-end features yet. Each is called out with a comment at its definition:

- **Smart Profiles auto-switching** – `DeviceOutputMonitor` detects device
  changes correctly today; the "remember a preset per device type and
  auto-apply it" persistence/logic is not yet built on top of it.
- **Widget live state** – the widget opens the app on tap but doesn't yet show
  live enabled/preset state or offer an in-widget toggle (needs a small shared
  DataStore between the widget process and the app).
- **Additional visualizer render styles** (Circular, Spectrum, Particle, Neon)
  – the capture engine emits the same `VisualizerFrame` data all styles would
  need, and the settings for animation speed/sensitivity are already persisted;
  only `BARS`/`WAVEFORM` have Canvas renderers today, and there's no
  visualizer screen wired up yet to show any of them.
- **Dashboard customization** (reorder/hide/resize cards, pin quick controls)
  and **balance/mono/channel-swap audio routing** – modeled in the preset data
  class, not yet wired to a UI or `AudioTrack`-level implementation.
- **Cloud sync** – the repository is file/JSON based by design so a future
  sync layer can diff/merge the same format; no network sync code exists.
- **Not started yet** (suggested in a later round, not yet built): sample
  rate/bit depth display, auto-EQ from a headphone target-curve database,
  scheduled/time-based profile switching. None of these are faked or stubbed -
  they simply aren't in the codebase yet.

## Known platform limitation (please read)

Effects are attached to audio session `0`, which the platform treats as the
global output mix — the same technique other system-wide EQ apps use, since
Android has no public "apply to everything" API. It is **not** a documented
guarantee for every playback path: some OEM spatial-audio or hardware-offload
pipelines may not route through it. The app never claims a feature works where
it hasn't verified success against the live platform API.

## Architecture

```
app/
 ├─ audio/            AudioEffectManager, EqualizerCapabilities, DeviceOutputMonitor
 ├─ visualizer/        VisualizerEngine (FFT/waveform capture)
 ├─ util/              PresetDiff (band-by-band preset comparison)
 ├─ data/
 │   ├─ model/         EqualizerPreset, PresetFolder, OutputProfile, EffectSnapshot
 │   └─ repository/    PresetRepository (JSON), AppSettingsRepository (DataStore)
 ├─ di/                Hilt module
 ├─ ui/
 │   ├─ viewmodel/      EqualizerViewModel, SettingsViewModel
 │   ├─ theme/          Material3 theme, dynamic color, AMOLED mode, accent + corner radius
 │   ├─ components/     BandSlider, PresetThumbnail, PresetDiffView, DiagnosticsPanel, CustomizationPanel
 │   └─ screens/        EqualizerScreen
 └─ widget/            Glance home screen widget, Quick Settings tile
```

## Build

```bash
git clone <your-fork-url>
cd PremiumEQ
./gradlew assembleDebug
```

Minimum SDK 26, target/compile SDK 36, Kotlin 2.0, AGP 8.7.

## CI

`.github/workflows/build.yml` runs unit tests and builds both debug and
unsigned release APKs on every push/PR, uploading them as workflow artifacts.
To ship a signed release build, add your keystore as GitHub Secrets and extend
the `release` build type in `app/build.gradle.kts` with a `signingConfig`.

## License

MIT — see [LICENSE](LICENSE).
