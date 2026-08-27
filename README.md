# Not Sam Notes

A Samsung Notes functional clone for personal use. Zero Samsung dependencies. Self-hosted sync/backup.

## Features

- **Typed notes** with rich text: bold/italic/underline/strikethrough, text color, highlight, headings, bulleted/numbered lists, checklists
- **Handwriting/drawing** with pressure + tilt, multiple brushes (pen, marker, highlighter), eraser, lasso select/move, undo/redo, zoom/pan, page templates
- **Handwriting refinement**: straighten (auto-level slanted lines) and tidy (smooth, simplify, size-normalize)
- **Mixed notes**: text + ink + images + audio clips in one note
- **Organization**: nested folders, favorites, trash with retention, sort, grid/list views
- **Search**: full-text over typed text and handwriting (on-device recognition)
- **PDF import** + ink annotation
- **Export**: PDF, PNG, TXT, .docx, .pptx
- **Locked notes** (biometric + recovery passphrase), app lock
- **Sync/backup**: SAF folder, WebDAV, SMB, and direct device-to-device P2P sync

## Architecture

```
app/          — Compose UI, ViewModels, Room index, SAF + peer-server glue
core-note/    — Pure JVM: document model, container I/O, stroke schema, refinement, exporters
core-sync/    — Pure JVM: sync engine, WebDAV + SMB + peer targets, fault suite
```

- **File-per-note container**: each note is one zip (`<uuidv7>.note`). Room is a disposable index, rebuildable from files.
- **Block document model**: text + ink + images + audio in one note
- **Ink engine**: Jetpack androidx.ink (pressure, tilt, motion prediction)
- **Handwriting recognition**: ML Kit Digital Ink Recognition (feature-flagged)
- **Sync**: 3-way state compare + conflict duplication (no merging, no clock trust)

## Stack

- Kotlin 2.0.21, AGP 8.7.3, Compose BOM 2024.12.01
- Room 2.6.1 + FTS4, DataStore, WorkManager
- androidx.ink 1.0.0, ML Kit 19.0.0
- SMBJ 0.13.0, NanoHTTPD 2.3.1, OkHttp 4.12.0

## Build

```bash
./gradlew assembleDebug   # Debug APK
./gradlew assembleRelease # Release APK (requires signing config)
```

## Phases

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Foundations & format | ✅ |
| 1 | Note store + text notes | ✅ |
| 2 | Ink engine | ✅ |
| 3 | Mixed document + rich text | ✅ |
| 4 | Search + handwriting recognition | ✅ |
| 5 | Hub sync & backup | ✅ |
| 6 | P2P device-to-device sync | ✅ |
| 7 | Handwriting refinement | ✅ |
| 8 | Export suite | ✅ |
| 9 | Parity extras | ✅ |
| 10 | Hardening loop | ✅ |

## License

GPLv3
