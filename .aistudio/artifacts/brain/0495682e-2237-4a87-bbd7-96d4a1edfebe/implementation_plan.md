# Retro Graphics, Sound Synth, Sprites & Custom Launcher Icons Plan

Implementation plan to expand the BASIC IDE and Standalone APK Runner with:
1. **Graphics Modes & Canvas Engine**: 320x200 retro virtual framebuffer with SCREEN, PSET, LINE, RECT, CIRCLE, and CLS.
2. **8-Bit Sound Synthesizer**: Real-time waveform audio engine (Square, Triangle, Sawtooth, Sine, Noise) supporting `SOUND freq, duration, type`, `PLAY "MML"`, and retro sound effects (`SFX preset`).
3. **Hardware Sprite Engine**: 8x8 and 16x16 multi-color sprites with `SPRITEDEF`, `SPRITE id, x, y, pat, color`, `SPRITEMOVE`, and hardware `SPRITEHIT(id1, id2)` collision detection.
4. **Custom Launcher Icons for Exported Apps**: Visual icon picker in the Export dialog featuring 8 curated retro computing & arcade icons, complete with binary APK packaging and dynamic manifest/resource injection.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The architectural direction has been aligned with your preferences:
> - **Graphics Architecture**: 320x200 pixel virtual screen buffer with aspect-ratio scaling and integer-pixel rasterization.
> - **Sound Synthesis**: Real-time 8-bit audio synthesis via Android `AudioTrack` with 5 waveforms and sound effect triggers.
> - **Sprite System**: Hardware-style 8x8 & 16x16 pixel sprites with pattern definitions, positioning, flipping, and bounding box / pixel collision detection.
> - **Launcher Icons**: A selector with 8 default pixel/retro computing icons (BASIC Terminal, 8-Bit Floppy, Microchip, Retro Computer, Space Invader, Arcade Joystick, Diamond Pixel, and 8-Bit Cassette).

---

## 1. Overview & Core Concept

- **What It Does**: Transforms the BASIC studio and standalone runner from a purely text/terminal environment into a full retro 80s microcomputer platform capable of running graphical games, demos, chiptunes, and utilities.
- **Standalone Runner Integration**: Both the interactive IDE (in-app test runner) and the exported APK runner (`RuntimeActivity`) seamlessly run the exact same bytecode with full graphics, sound, and sprites.
- **Export Customization**: When exporting an APK, users can pick from 8 bespoke retro app launcher icons, select the app name and package ID, and produce a signed, zip-aligned standalone APK ready for installation on any Android device.

---

## 2. User Experience & Visual Design

### Key User Flows

```
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Code Editor / Presets                                               │
│    - Select or write BASIC code utilizing SCREEN, PSET, SPRITE, SOUND  │
│    - New sample programs included: "Retro Invader", "Sound Chiptune"   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Tap RUN
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. Dual Canvas & Terminal Screen (or Fullscreen Game Mode)             │
│    - SCREEN 0: Classic 80x25 text terminal                             │
│    - SCREEN 1: 320x200 Retro CRT Canvas with scanline effect & sprites │
│    - Audio plays in background via low-latency AudioTrack synth        │
│    - Live interactive touch / d-pad controls for sprites & input       │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ Tap Export APK
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. Enhanced APK Export Modal                                           │
│    - App Name & Package ID inputs                                      │
│    - 8-Icon Visual Grid Selector (Interactive preview cards)           │
│    - One-click build: injects bytecode + selected icon into signed APK │
│    - Direct "Install APK" and "Share APK" buttons                      │
└────────────────────────────────────────────────────────────────────────┘
```

### Visual Identity & Theme
- **Retro Aesthetic**: Deep midnight background (`#0D1117`), phosphorescent CRT green (`#10B981`), amber warning accents (`#F59E0B`), and cyan system telemetry (`#06B6D4`).
- **Canvas View**: 320x200 pixel display rendered within Compose `Canvas` preserving exact 16:10 or 4:3 aspect ratio, framed by a retro monitor bezel.
- **Icon Selector**: 4x2 grid of preview cards with retro pixel art representations, gold selection borders, and descriptive labels.

---

## 3. Technical Architecture & Command Specifications

### A. BASIC Language Additions

#### 1. Graphics & Screen Commands
- `SCREEN mode`:
  - `SCREEN 0`: Text Terminal Mode (default text console).
  - `SCREEN 1`: 320x200 16-color Retro Graphics Mode with Sprites.
  - `SCREEN 2`: 320x200 High-Color Graphics Mode.
- `CLS`: Clears the current screen (or resets graphics canvas to background color).
- `COLOR fg [, bg]`: Sets active drawing and text foreground/background color.
- `PSET (x, y) [, color]`: Sets a single pixel at `(x, y)`.
- `LINE (x1, y1)-(x2, y2) [, color]`: Bresenham line between two points.
- `RECT (x1, y1)-(x2, y2) [, color] [, fill]`: Draws rectangle or filled box.
- `CIRCLE (x, y), radius [, color] [, fill]`: Midpoint circle algorithm.

#### 2. Sound & Music Commands
- `SOUND freq, duration [, waveType]`:
  - `freq`: Frequency in Hz (e.g. `440` for A4, `20` to `5000`).
  - `duration`: Duration in milliseconds (e.g. `200`).
  - `waveType`: `0` = Square (pulse), `1` = Triangle, `2` = Sawtooth, `3` = Sine, `4` = Noise.
- `PLAY "MML_STRING"`: Music Macro Language parser (e.g., `PLAY "T120 O4 C D E F G A B > C"`).
- `SFX preset`: Plays built-in classic sound effect presets (`0` = Laser, `1` = Jump, `2` = Explosion, `3` = Coin, `4` = Powerup).

#### 3. Sprite Engine Commands
- `SPRITEDEF id, size, "hex_or_binary_data"`:
  - `id`: Sprite slot `0..15`.
  - `size`: `8` (for 8x8) or `16` (for 16x16).
  - Hex or binary string defining the pixel mask/colors.
- `SPRITE id, x, y [, pattern] [, color]`: Sets position and appearance of sprite `id`.
- `SPRITEMOVE id, dx, dy`: Translates sprite position relative to current coordinates.
- `SPRITESHOW id, visible`: Toggle visibility (0 or 1).
- `SPRITEHIT(id1, id2)`: Built-in function returning `1` if bounding boxes/pixels overlap, `0` otherwise.

#### 4. Custom Launcher Icons (8 Default Options)
1. **Retro Microcomputer**: Classic beige desktop monitor and keyboard.
2. **BASIC 8-Bit Logo**: Glowing emerald CRT terminal prompt (`READY > _`).
3. **3.5" Floppy Disk**: Iconic blue/gray floppy diskette with label.
4. **Silicon Microchip**: DIP integrated circuit package with gold pins.
5. **Space Invader**: Classic pixel art arcade alien.
6. **Arcade Joystick**: Retro red-ball joystick with action buttons.
7. **Pixel Gem / Diamond**: 8-bit shiny game diamond.
8. **Retro Cassette Tape**: 8-bit program data storage cassette tape.

---

## 4. System & Execution Plan

```
┌────────────────────────────────────────────────────────────────────────┐
│ BASIC Source Code                                                      │
│    │                                                                   │
│    ▼                                                                   │
│ BasicCompiler / Lexer                                                  │
│    ├─► Emits OpCodes: OP_SCREEN, OP_PSET, OP_LINE, OP_RECT,            │
│    │                  OP_CIRCLE, OP_SOUND, OP_SFX, OP_SPRITEDEF,       │
│    │                  OP_SPRITE, OP_SPRITEMOVE, OP_SPRITEHIT           │
│    ▼                                                                   │
│ BasicVirtualMachine                                                    │
│    ├─► AudioSynthEngine (AudioTrack PCM streaming)                     │
│    ├─► RetroGraphicsState (320x200 pixel buffer + Sprite slots)        │
│    └─► TerminalLine text stream                                        │
│    │                                                                   │
│    ▼                                                                   │
│ UI (BasicStudioScreens & RuntimeActivity)                              │
│    ├─► Compose Canvas: renders RetroGraphicsState + Sprites            │
│    └─► Compose TerminalScreen: text and input                          │
│                                                                        │
│ StandaloneApkBuilder                                                   │
│    ├─► Icon Generator: Writes mipmap PNGs for selected retro icon      │
│    └─► APK Packager: Repacks runner-base.apk with icon + bytecode      │
└────────────────────────────────────────────────────────────────────────┘
```

### Verification & Testing
1. **Compiler & VM Unit Tests**: Add test cases in `app/src/test/` verifying opcode generation and execution for `SCREEN`, `PSET`, `LINE`, `SOUND`, `SPRITE`, and `SPRITEHIT`.
2. **Audio Track Synthesis Test**: Validate PCM buffer calculations and waveform generation.
3. **Sprite Collision Test**: Verify `SPRITEHIT` returns 1 on overlap and 0 on separation.
4. **Standalone APK Builder Test**: Verify packaging with each of the 8 icon options produces valid APKs without Zip or AXML corruption.
5. **Sample Programs**: Include a complete new sample program showcasing graphics, sound effects, and movable sprites.
