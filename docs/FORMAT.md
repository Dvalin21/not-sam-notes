# Not Sam Notes — On-Disk Format Specification v1

> Written before the app (Phase 0 → FORMAT.md). Versioned; readers reject unknown major versions loudly, tolerate unknown fields.

## 1. Container

Each note is one zip named `<uuidv7>.note`:

```
<uuidv7>.note              # zip
├── manifest.json          # identity + metadata (tiny, read first)
├── document.json          # ordered block list
├── strokes/<blockId>.json # stroke data per ink block (§3)
├── media/<uuidv7>.<ext>   # jpg/png/m4a/pdf
└── recognition/<blockId>.json # cached recognition (rebuildable)
```

Flat storage by UUID; folder path lives in the manifest (renames never touch remote objects; SMB/WebDAV rename semantics vary).

## 2. manifest.json / document.json

```json
// manifest.json
{ "format": 1, "id": "0192f3...", "title": "Meeting 8/25",
  "folder": "/Work/2026", "created": 1756130000000, "modified": 1756131111000,
  "favorite": false, "trashed": false, "locked": false,
  "template": "grid", "device": "tab-extreme" }

// document.json
{ "blocks": [
  { "type": "text", "id": "b1", "text": "Agenda",
    "spans": [ { "start": 0, "end": 6, "style": "h1" } ] },
  { "type": "ink",  "id": "b2", "height": 900 },
  { "type": "image","id": "b3", "media": "media/0192f4.jpg", "w": 1600, "h": 900 },
  { "type": "audio","id": "b4", "media": "media/0192f5.m4a", "durMs": 32000 }
] }
```

Spans are offset ranges — 1:1 with Spannable, Compose AnnotatedString, and Word run properties.

## 3. Stroke schema (per ink block)

```json
{ "strokes": [
  { "brush": "pen", "color": "#1A1A1A", "size": 3.5,
    "points": [ [x, y, tMs, pressure, tiltX, tiltY], ... ] }
] }
```

Block-local dp. Sovereign format (AD-5); consumed directly by rendering reconstruction, ML Kit, and refinement math.

## 4. Integrity rules (never cut)

- Every save: `.tmp` → fsync → atomic rename; remote: PUT temp then MOVE. Peer server enforces the same on its side.
- Zip CRC verified on read; corrupt notes quarantined (`.corrupt`) and reported loudly.
- Locked notes: `document.json`, `strokes/`, `media/`, `recognition/` are AES-256-GCM ciphertext; `manifest.json` stays plaintext so lists and sync work.

## 5. UUIDv7

Notes use UUIDv7 (time-ordered, monotonic) for natural sort order and collision-free distributed creation. Implementation: `java.util.UUID` with custom v7 generator (included in `core-note`).

## 6. Media types

| Type | Extensions | Notes |
|------|-----------|-------|
| Image | `.jpg`, `.png`, `.webp` | Photo Picker output |
| Audio | `.m4a` | MediaRecorder AAC |
| PDF | `.pdf` | Imported documents (Phase 9) |

## 7. Version migration

- Major version bump = breaking change (reader rejects).
- Minor version bump = additive (reader tolerates unknown fields).
- Migration code lives in `core-note`, one function per major version.
