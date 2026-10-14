# PresetPilot — switch VST presets from your phone over USB MIDI

Tap a preset on your phone → your VST on the PC changes preset. Banks,
setlists, panic button. No camera, no ML, no hardware — just MIDI.

Full spec: [`../SPEC.md`](../SPEC.md).

## How it works

Each preset sends, in order: **Bank Select MSB (CC0) → Bank Select LSB (CC32)
→ Program Change** on the bank's MIDI channel. The phone exposes a virtual
`MidiDeviceService` ("PresetPilot"); with the phone in USB MIDI peripheral
mode ("Use USB for → MIDI"), the PC sees it as a class-compliant controller.
No drivers, no companion app.

In your DAW: enable the "PresetPilot" MIDI input on the track holding the VST.
Most VSTs (Kontakt, Serum, Neural DSP, Helix Native…) respond to Program
Change; a few need it enabled in their own MIDI settings.

## Features (v1.0)

- **Preset grid** — big tap targets, flash confirms, long-press to edit/delete/add-to-setlist
- **Banks** — per-bank MIDI channel + bank MSB/LSB
- **Setlist mode** — ordered songs, giant Prev/Next, reorder, panic (All Notes Off)
- **MIDI monitor** — timestamped log of everything leaving the phone
- **Target routing** — virtual USB device or any physical MIDI device
- **Onboarding** — USB MIDI setup, DAW hookup, how preset switching works

## Build

**GitHub Actions (recommended):** push to `main` — the workflow builds the
debug APK and uploads it as an artifact.

**Locally:** Android Studio (Ladybug+), JDK 17, Android SDK 34, any Android
8.0+ phone.

## Verification note

The virtual-device → PC path is the documented purpose of Android's USB MIDI
peripheral mode, but OEMs customize USB behavior — verify once on your
hardware using Settings → MIDI monitor (proof of what's leaving the phone).
If a combo doesn't forward, switch the MIDI target to a physical device.

## License

TBD before any Play Store listing (Apache-2.0 recommended).
