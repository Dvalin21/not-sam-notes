# Architecture Decisions

## AD-1: File-per-note container

**Rung:** Platform (zip + fs)

Each note is one zip named `<uuidv7>.note`. Room/SQLite is a disposable index, rebuildable from files.

Why load-bearing:
- Sync = file replication over any dumb store (folder, WebDAV, SMB, peer device)
- Backup = the sync target
- Atomic saves via tmp → fsync → rename
- Corruption blast radius = one note
- Index loss = "Rebuild index"

## AD-2: Block document model

A note = ordered blocks: text (spans), ink (canvas region), image, audio, pdfPage.

Ink-first note = one auto-growing ink block + page template.

Samsung's write-anywhere-over-reflowing-text anchoring is the hardest problem in the app for marginal personal value. The block model is the honest 90%-fidelity / 20%-complexity trade.

Bonus: blocks map cleanly onto Word paragraphs and PowerPoint slides (AD-12).

## AD-3: Ink engine = Jetpack androidx.ink

**Rung:** Platform

Verified Aug 2026: 1.0.0 stable with Compose modules:
- `ink-authoring-compose`
- `ink-brush-compose`
- `ink-geometry-compose`
- `ink-rendering`
- `ink-strokes`
- `ink-storage`
- `ink-nativeloader`

Low-latency wet ink, motion prediction, pressure-aware brushes, geometry utilities for erase/lasso.

API 21+; core modules run on plain JVM — powers PDF and OOXML raster export identically to screen.

Pin 1.0.0.

## AD-4: Handwriting recognition = ML Kit Digital Ink Recognition

**Rung:** Existing dependency

Verified Aug 2026: `com.google.mlkit:digital-ink-recognition:19.0.0`

Fully offline after per-language model download, 300+ languages, consumes strokes as (x, y, t).

Requires GMS for model download (both devices have it); feature-flagged so absence never blocks note-taking.

## AD-5: Own the stroke persistence format

Notes are decade-scale data. Container stores strokes in our schema (§3.3 of FORMAT.md): brush params + (x, y, t, pressure, tiltX, tiltY) point arrays.

~50 lines, format sovereignty. Strokes rebuilt at load. Doubles as ML Kit input and direct substrate for straighten/tidy math.

## AD-6: Sync = 3-way state compare + conflict duplication

Per note × target, sync index stores content hash at last successful sync (base):

- local == base, remote == base → nothing
- Exactly one side changed → copy it over
- Both changed → keep both: import remote as `<title> (conflict from <device>, <date>)`

Zero data loss, zero merge logic. Timestamps display-only.

Trash is a manifest flag. Permanent delete writes `<uuid>.tomb` (90-day retention) so deletion propagates.

## AD-7: Sync targets (build order)

Interface: `SyncTarget { list(): [id, size, stamp]; get(id); putAtomic(id, bytes); delete(id) }`

1. SAF folder (platform, zero protocol code)
2. WebDAV (minimal client over OkHttp: PROPFIND, GET, PUT, MKCOL, MOVE, DELETE; ~300 lines)
3. SMB (`com.hierynomus:smbj`, SMB2/3, pure JVM)
4. Peer device (AD-10)

## AD-8: Rich text — decide by 3-day spike at Phase 3 start

Candidates:
- `com.mohamedrejeb.richeditor:richeditor-compose` (1.0.0 stable, rich-text-aware undo/redo)
- EditText + Spannable in AndroidView (platform, battle-tested)

Criteria: full §1.1 styling set, stable span serialization, undo interop, stylus IME behavior. Less code wins; loser deleted.

## AD-9: App structure — 3 modules, manual DI, MVVM

- `app` — Compose UI, ViewModels, Room index, SAF + peer-server glue
- `core-note` — pure JVM: document model, container I/O, stroke schema, refinement geometry, exporters
- `core-sync` — pure JVM: engine, WebDAV + SMB + peer-client targets, fault suite

No Hilt (hand-written AppContainer). Three modules is the number.

Stack: Kotlin 2.x, Compose (M3), Room + FTS4, DataStore, WorkManager, kotlinx.serialization.

## AD-10: P2P device-to-device sync

The whole feature is transport + pairing; zero new sync logic.

**Serving side:** NanoHTTPD (tiny, single-file, embeddable, BSD-licensed, HTTPS-capable). Four fixed HTTP routes over TLS on random high port.

**Identity/auth:** On first pairing, each device generates keypair + self-signed X.509 (BouncyCastle, already present via SMBJ). Pairing = QR containing {cert fingerprint, IP:port hint, one-time token}.

**Discovery:** NsdManager (mDNS/DNS-SD) advertising `_opennotes._tcp`; manual IP:port entry as first-class fallback.

**Session model:** Manual, user-initiated, both apps foreground. No background peer sync.

## AD-11: Handwriting refinement = deterministic geometry

Three tiers:
1. **Straighten** (committed): cluster strokes into text lines, fit baseline by least-squares, rotate by −slope
2. **Tidy** (committed): Ramer–Douglas–Peucker simplification + Catmull-Rom smoothing + x-height normalization
3. **Rewrite-in-neat-hand** (timeboxed 3 days): ML Kit recognition → re-render in synthetic hand. Ships only if actually useful.

## AD-12: .docx/.pptx = export-only minimal OOXML writers

OOXML files are zips of XML — `java.util.zip` + `XmlSerializer` are the whole toolchain. Apache POI and docx4j rejected (multi-MB dependency trains built for reading arbitrary documents).

**.docx mapping:** text block → paragraph; spans → run properties; headings → pStyle; lists → numbering.xml; checklists → ☐/☑ glyph runs; images → embedded media; ink → PNG rendered by JVM ink renderer.

**.pptx mapping:** each H1 block starts a new slide; no headings → paginate by content height.

Compatibility gate: exported files must open with no repair prompt in desktop Word, desktop PowerPoint, LibreOffice, and Google Docs/Slides.
