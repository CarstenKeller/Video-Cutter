# Video Cutter – Feature- und Umsetzungsplan

> Diese Datei ist die Arbeitsgrundlage. Wir arbeiten die Phasen der Reihe nach ab,
> beginnend mit Phase 1 (MVP). Erledigte Punkte werden mit [x] markiert.

## Technische Grundentscheidungen

- **Sprache/UI:** Kotlin + Jetpack Compose
- **Medien-Engine:** AndroidX **Media3** (ExoPlayer für die Vorschau, `Transformer`/`Composition`-API für den Export). Bewusst **kein** FFmpegKit/Mobile-FFmpeg, da beide Projekte inzwischen eingestellt/archiviert sind (FFmpegKit 2025, Mobile-FFmpeg bereits 2021).
- **Speicherort Export:** MediaStore-Video-Collection, Album `Video Cutter` (`Movies/Video Cutter`). Album wird beim ersten Export automatisch angelegt, falls es noch nicht existiert.
- **minSdk:** 29 (Android 10) – *meine Interpretation*, da nur ein Testgerät mit Android 16 genannt wurde, die App aber nicht unnötig auf dieses eine Gerät beschränkt werden soll. Vereinheitlicht zudem das Scoped-Storage-Verhalten (kein Legacy-Storage-Pfad nötig).
- **compileSdk/targetSdk:** 36 (Android 16), passend zum Testgerät.
- **Architektur:** Single-Activity, Compose Navigation, MVVM (ViewModel + StateFlow), Timeline-Datenmodell unabhängig von der UI.

## Offene technische Unsicherheit (Flag)

Ob Media3 `Transformer`/`Composition` echte Überblend-Übergänge (Crossfade etc.) zwischen aufeinanderfolgenden Clips bereits vollständig unterstützt, ist mir nicht mit Sicherheit bekannt und wird in Phase 5 im Code verifiziert, bevor Übergänge als Feature zugesagt werden. Falls die API das nicht hergibt, wird als Fallback ein Overlap-Rendering (Cross-Dissolve via `Composition` mit zeitversetzten Sequenzen + Alpha-Effekt) geprüft.

---

## Phase 1 – MVP (Start)

- [ ] Projekt-Grundgerüst (Gradle, Compose, Media3-Dependencies, Manifest, Berechtigungen)
- [ ] Video-Import über System-Picker (`ActivityResultContracts.OpenDocument`/`GetContent`, mp4)
- [ ] Vorschau-Player (ExoPlayer) mit Play/Pause, Zeitanzeige, Scrubbing
- [ ] Einfache Single-Track-Timeline:
  - Clip(s) als Blöcke mit Thumbnail-Streifen
  - Trimmen (Start-/End-Handle ziehen)
  - Schneiden/Split am Playhead
  - Clip löschen
  - Reihenfolge der Clips per Drag ändern (Abschnitte verschieben)
- [ ] Export-Dialog:
  - Auflösung wählen (Original, 1080p, 720p, 480p)
  - Kompression/Qualität wählen (Original, Hoch, Mittel, Niedrig → Bitrate-Presets)
- [ ] Speichern des Exports in MediaStore-Album „Video Cutter" (Album-Erstellung falls nicht vorhanden)
- [ ] Export-Fortschrittsanzeige (Progress, Erfolg/Fehler)

## Phase 2 – Mehrere Videospuren

- [ ] Zweite (und weitere) Video-Track(s) hinzufügen
- [ ] Kompositions-Vorschau (Overlay/Picture-in-Picture) im Player
- [ ] Track-Steuerung: Sichtbarkeit ein/aus, Reihenfolge/Ebene, stumm schalten
- [ ] Positionierung/Skalierung von Overlay-Spuren (einfacher Ansatz: vordefinierte Layouts – PiP-Ecke, Split-Screen)

## Phase 3 – Audiospur

- [ ] Audiodatei vom Gerät hinzufügen (separate Musikspur)
- [ ] Audioclip trimmen, verschieben, Lautstärke regeln
- [ ] Fade-in/Fade-out für Audioclips
- [ ] Original-Videoton stumm schalten (optional, pro Videospur)

## Phase 4 – Bilder in der Timeline

- [ ] Bild aus Galerie einfügen als Clip
- [ ] Anzeigedauer pro Bild festlegen
- [ ] Optional: einfacher Ken-Burns-Effekt (langsames Zoomen/Schwenken) vs. statisch

## Phase 5 – Übergänge

- [ ] Technische Verifikation der Media3-Fähigkeiten (siehe oben)
- [ ] Crossfade zwischen Clips
- [ ] Fade to Black/White
- [ ] Slide/Wipe
- [ ] Übergangsdauer einstellbar pro Übergang

## Phase 6 – Politur & erweiterte Funktionen

- [ ] Undo/Redo
- [ ] Mehrere Projekte speichern/laden (Projekt-Verwaltung)
- [ ] Performantere Thumbnail-Generierung/Caching für die Timeline
- [ ] Geschwindigkeit pro Clip (Zeitlupe/Zeitraffer)
- [ ] Text-Overlays
- [ ] Rotieren/Zuschneiden/Seitenverhältnis ändern
- [ ] Export als Hintergrundprozess (WorkManager/Foreground Service) statt blockierend in der Activity
- [ ] Fehlerbehandlung/Diagnose (z. B. nicht unterstützte Codecs)

---

## Fortschritt

_Wird laufend aktualisiert, sobald Punkte umgesetzt sind._
