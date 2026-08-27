# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Phase 0: Project skeleton, 3-module Gradle structure, version catalog
- Phase 0: FORMAT.md (on-disk note format spec), decisions.md (AD-1 through AD-12)
- Phase 0: GitHub Actions CI/CD (test, lint, build, release)
- Phase 1: Note store with zip container I/O, atomic saves, CRC integrity
- Phase 1: Room index with FTS4 full-text search
- Phase 1: Note list UI with LazyColumn, FAB to create notes
- Phase 2: Ink engine (Compose Canvas + pointer input, brushes, eraser, undo/redo)
- Phase 3: Mixed document model (text, ink, image, audio, pdfPage blocks)
- Phase 3: Rich text spans (bold, italic, underline, strike, h1-h3, bullet, number, check, color, highlight)
- Phase 3: Block editor screen with title editing
- Phase 4: Search engine (FTS5 prefix queries, snippets)
- Phase 4: ML Kit Digital Ink Recognition (feature-flagged, offline)
- Phase 5: Sync engine (3-way state compare + conflict duplication)
- Phase 5: SAF, WebDAV, SMB sync targets
- Phase 6: P2P sync server (NanoHTTPD + TLS), NSD discovery
- Phase 7: Handwriting refinement (straighten, tidy with RDP + Catmull-Rom)
- Phase 8: Export suite (PDF, PNG, TXT, .docx, .pptx)
- Phase 9: Locked notes (AES-256-GCM via Android Keystore)
- Phase 9: PDF import (PdfRenderer → pdfPage blocks)
- Phase 9: Audio blocks (MediaRecorder → m4a, inline playback)
- Phase 9: App lock (BiometricPrompt)
- Phase 9: Partial-stroke eraser
- Phase 10: Crash handler (uncaught exception → crash log file)

### Security
- Locked notes use AES-256-GCM encryption via Android Keystore
- App lock uses BiometricPrompt for authentication
- Sync targets use TLS/HTTPS for remote connections

### Architecture
- File-per-note container: each note is one zip, Room is disposable index
- Block document model: text + ink + images + audio in one note
- Manual DI (no Hilt), 3 modules (app, core-note, core-sync)
- Pure JVM core modules for testability
