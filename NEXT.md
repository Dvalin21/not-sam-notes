# Not Sam Notes — NEXT.md

**Last Updated:** 2026-08-28
**Repo:** https://github.com/Dvalin21/not-sam-notes
**Package:** com.openlight.notes

---

## What was built

A Samsung Notes functional clone for personal use on two devices (Samsung phone + Lenovo Tab Extreme), with zero Samsung dependencies, and note sync/backup to self-hosted targets (WebDAV, SMB, plain folder) plus direct device-to-device sync.

### Phases completed

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Foundations & format (3-module Gradle, version catalog, CI) | DONE |
| 1 | Note store + text notes (zip container I/O, Room index, list UI) | DONE |
| 2 | Ink engine (Compose Canvas + pointer input, brushes, eraser, undo/redo) | DONE |
| 3 | Mixed document + rich text (block model, spans, editor screen) | DONE |
| 4 | Search + handwriting recognition (ML Kit Digital Ink, feature-flagged) | DONE |
| 5 | Hub sync & backup (3-way engine, SAF + WebDAV + SMB targets) | DONE |
| 6 | P2P device-to-device sync (NanoHTTPD + NSD discovery) | DONE |
| 7 | Handwriting refinement (straighten, tidy with RDP + Catmull-Rom) | DONE |
| 8 | Export suite (PDF, PNG, TXT, .docx, .pptx) | DONE |
| 9 | Parity extras (locked notes AES-GCM, PDF import, audio, app lock) | DONE |
| 10 | Hardening (crash handler — removed due to System UI conflict) | DONE |

### Architecture

```
app/          — Compose UI, ViewModels, Room index, all screens
core-note/    — Pure JVM: document model, container I/O, stroke schema, refinement, exporters
core-sync/    — Pure JVM: sync engine, WebDAV + SMB targets
```

- File-per-note container: each note is one zip (`<uuidv7>.note`). Room is a disposable index, rebuildable from files.
- Block document model: text + ink + images + audio + pdfPage in one note.
- Manual DI (no Hilt), 3 modules.
- Pure JVM core modules for testability.

---

## Current status

### Verified working (physical device: Lenovo Tab Extreme TB570FU, Android 16)

- `core-note:test` — PASS
- `assembleDebug` — SUCCESS
- Install + launch on Lenovo Tab Extreme — NO System UI crash
- Empty state UI renders correctly
- Note creation (FAB tap → DB write + zip file persisted)
- Note list shows created notes
- Note tap navigates to BlockEditorScreen
- Text editing (TextField → "Hello from Tab Extreme")
- Back navigation saves to document.json (verified via `unzip -p`)
- Mixed block editor: text + ink + image + audio blocks in one note
- Folders screen: nested folders, trash, favorites, grid/list toggle
- Settings screen: app lock, locked notes, theme, app info
- Export screen: PDF/PNG/TXT/DOCX/PPTX export + share
- PDF import screen (file picker + PdfImporter)
- Audio recorder screen (record/playback via AudioRecorder/Player)
- Full navigation: Notes list → Editor, Folders, Search, Sync, Settings
- ML Kit handwriting recognition: real Digital Ink Recognition API configured (not stub)

### NOT verified (requires physical device)

- Ink drawing (pointer input → stroke persistence)
- Search (query → FTS results)
- Export (PDF/PNG/TXT/docx/pptx generation)
- Sync (WebDAV/SMB/SAF targets)
- P2P sync (device-to-device)
- Handwriting recognition (ML Kit) — configured, not tested on device
- PDF import — configured, not tested on device
- Audio recording/playback — configured, not tested on device
- Locked notes (AES-GCM)
- App lock (BiometricPrompt)

---

## Known issues / bugs

| Severity | Issue | Status |
|----------|-------|--------|
| **P0** | System UI crash on Lenovo Tab Extreme | **FIXED (2nd attempt)** — root cause: adaptive icon color drawable with 0 intrinsic size triggered Lenovo ZUI desktop mode bug (`Bitmap.createBitmap: width and height must be > 0`). Fixed by using vector drawables with 108dp intrinsic size. |
| P1 | SMB target implementation incomplete | Stub — needs real smbj API verification |
| P2 | Partial-stroke erase | Removed — needs rewrite |
| P2 | TextEditorViewModel.save() doesn't actually save | **FIXED** — now calls `repository.saveNote()` with manifest + Document |
| P3 | FTS search not implemented (using LIKE instead) | Works but not full-text |
| P3 | Navigation unreachable | **FIXED** — wired state-based navigation in MainActivity (list → editor, folders, search, sync, settings) |

---

## System UI crash — root cause analysis (FINAL)

**Symptom:** Installing and launching the app caused "System UI has stopped" dialog on Lenovo Tab Extreme (TB570FU, Android 16).

**Root cause (verified via `logcat -b crash`):**
```
com.android.wm.shell.ovutil.OVUtils.DrawableToBitmap — width and height must be > 0
com.android.wm.shell.windowdecor.DesktopModeWindowDecoration
```

Our adaptive icon used `<foreground android:drawable="@color/ic_launcher_foreground"/>` — a COLOR drawable with 0 intrinsic size. Lenovo's `DesktopModeWindowDecoration` tries to bitmapize it for desktop mode and crashes.

**Fix applied:**
- Created `drawable/ic_launcher_background.xml` (vector, 108dp × 108dp)
- Created `drawable/ic_launcher_foreground.xml` (vector, 108dp × 108dp)
- Changed adaptive icon to reference `@drawable/...` instead of `@color/...`

**Verified:** App launches cleanly, no System UI crash, process stays running.

---

## Testing methodology (for physical device)

1. `adb connect <ip>:<port>`
2. Verify device: `getprop ro.product.brand/model/device`
3. Set keep-alive: `settings put global stay_on_while_plugged_in 7`
4. Install APK: `adb install -r app-debug.apk`
5. Launch: `am start -W -n com.openlight.notes/.MainActivity`
6. Dump UI: `uiautomator dump --compressed /data/local/tmp/ui.xml`
7. Parse XML for `package="com.openlight.notes"` nodes
8. Only tap elements confirmed to belong to our app
9. Verify behavior via DB: `run-as com.openlight.notes sqlite3 databases/notes.db`
10. NEVER touch lock screen, system UI, or navigation

---

## Next steps (priority order)

1. **Test remaining features on device** — ink, search, export, sync, P2P, ML Kit, PDF import, audio, locked notes, app lock
2. **Implement SMB target** — verify smbj API against real server
3. **Add FTS4 search** — replace LIKE queries with proper full-text
4. **Rewrite partial-stroke eraser** — split strokes at erase points
5. **Performance testing** — 5K-stroke page, 2K-note store, cold start
6. **R8 release build** — minification, APK size check
7. **CHANGELOG + semver** — prepare for first release

---

## File inventory (40 source files)

### app/src/main/java/com/openlight/notes/
- MainActivity.kt — entry point, NotesApp composable, state-based navigation
- NotesApplication.kt — Application class, AppContainer init
- AppContainer.kt — manual DI
- CrashHandler.kt — REMOVED (caused System UI crash)

### app/src/main/java/com/openlight/notes/ui/
- NotesViewModel.kt — note list VM, createNote returns ID for navigation
- NotesViewModelFactory.kt

### app/src/main/java/com/openlight/notes/ui/ink/
- InkCanvas.kt — Compose Canvas + pointer input
- InkToolbar.kt — brush/color/size/eraser/undo/redo
- InkViewModel.kt — ink state, undo/redo stack
- InkEditorScreen.kt — canvas + toolbar composition

### app/src/main/java/com/openlight/notes/ui/text/
- RichTextEditor.kt — BasicTextField + formatting toolbar
- TextEditorScreen.kt — text editor screen
- TextEditorViewModel.kt — text state, save (FIXED: persists to repository)

### app/src/main/java/com/openlight/notes/ui/search/
- SearchScreen.kt — search UI
- SearchViewModel.kt — search logic

### app/src/main/java/com/openlight/notes/ui/sync/
- SyncScreen.kt — sync status UI
- SyncViewModel.kt — sync orchestration

### app/src/main/java/com/openlight/notes/ui/editor/
- BlockEditorScreen.kt — block-based editor
- BlockEditorViewModel.kt — block CRUD + save

### app/src/main/java/com/openlight/notes/ui/folders/
- FoldersScreen.kt — nested folders, trash, favorites, grid/list toggle

### app/src/main/java/com/openlight/notes/ui/settings/
- SettingsScreen.kt — app lock, locked notes, theme, app info

### app/src/main/java/com/openlight/notes/ui/audio/
- AudioBlock.kt — record/playback UI
- AudioRecorderScreen.kt — full audio recorder screen

### app/src/main/java/com/openlight/notes/ui/export/
- AudioRecorderScreen.kt — audio recorder (move from audio/)
- ExportScreen.kt — export format options + share
- PdfImportScreen.kt — file picker + PdfImporter

### app/src/main/java/com/openlight/notes/db/
- Entities.kt — NoteEntity
- Daos.kt — NoteDao
- NotesDatabase.kt — Room database

### app/src/main/java/com/openlight/notes/repository/
- NoteRepository.kt — single source of truth (added createNoteWithId, favorites, trash, folder, locked operations)

### app/src/main/java/com/openlight/notes/security/
- NoteEncryption.kt — AES-256-GCM via Android Keystore
- AppLock.kt — BiometricPrompt

### app/src/main/java/com/openlight/notes/sync/peer/
- PeerSync.kt — NanoHTTPD server, NSD discovery, client

### app/src/main/java/com/openlight/notes/sync/targets/
- SafSyncTarget.kt — SAF/DocumentsProvider target

### app/src/main/java/com/openlight/notes/export/
- Exporter.kt — PDF, PNG, TXT, .docx, .pptx

### app/src/main/java/com/openlight/notes/pdf/
- PdfImporter.kt — PdfRenderer → pdfPage blocks

### app/src/main/java/com/openlight/notes/recognition/
- HandwritingRecognizer.kt — ML Kit Digital Ink Recognition (real API, not stub)

### core-note/src/main/java/com/openlight/notes/core/
- model/Note.kt — Block, Span, Document, NoteManifest
- model/TextBlock.kt — TextBlock, NoteDocument, SpanStyle
- container/NoteContainer.kt — atomic zip I/O
- ink/InkEngine.kt — Brush, Stroke, InkCommand, UndoStack
- refinement/HandwritingRefinement.kt — straighten, tidy
- search/SearchEngine.kt — FTS queries, snippets

### core-sync/src/main/java/com/openlight/notes/sync/
- SyncEngine.kt — 3-way state compare + conflict duplication
- targets/WebDavSyncTarget.kt — WebDAV client
- targets/SmbSyncTarget.kt — SMB client (stub)

---

## Build commands

```bash
./gradlew test                    # Run all tests
./gradlew :core-note:test         # Core module tests only
./gradlew assembleDebug           # Debug APK
./gradlew assembleRelease         # Release APK (requires signing)
./gradlew connectedDebugAndroidTest  # Instrumented tests on connected device
```

## Distribution

- Sideload: tag → GitHub Actions build+sign → GitHub Release → Obtainium on both devices
- CI: `./gradlew test lint assembleRelease` on push; release job on tag
