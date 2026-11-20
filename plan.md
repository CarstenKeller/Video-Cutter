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

**Wichtige Einschränkung für Phase 8 (Hintergrund-Ersatz):** Echte Transparenz
in einem exportierten Video ist mit Standard-MP4/H.264 auf Android nicht
möglich (Hardware-Encoder unterstützen keinen Alphakanal). "Hintergrund
entfernen" wird daher technisch als Ersatz durch eine Fläche (Standard:
Schwarz) umgesetzt, nicht als echte Transparenz.

---

## Phase 1 – MVP (Start)

> Status: Code für alle unten stehenden Punkte ist geschrieben und gepusht,
> aber **nicht kompiliert/getestet** (Sandbox ohne Android-SDK/Netzwerk).
> Häkchen bedeuten "implementiert", nicht "verifiziert". Erster echter Test
> erfolgt beim Öffnen in Android Studio.

- [x] Projekt-Grundgerüst (Gradle-Wrapper, Compose, Media3-Dependencies, Manifest)
- [x] Video-Import über System-Picker (`ActivityResultContracts.PickVisualMedia`, videoOnly)
- [x] Vorschau-Player (ExoPlayer/PlayerView) mit Play/Pause, Zeitanzeige, Scrubbing (Standard-Controller)
- [x] Einfache Single-Track-Timeline:
  - Clip(s) als Blöcke, proportional zur Dauer
  - Trimmen über Schieberegler (Material3 RangeSlider)
  - Schneiden/Split an der aktuellen Player-Position
  - Clip löschen
  - Reihenfolge der Clips ändern (aktuell über Pfeiltasten am ausgewählten
    Clip statt Drag-Geste – siehe Hinweis unten)
- [x] Export-Dialog:
  - Auflösung wählen (Original, 4K, 1080p, 720p, 480p)
  - Kompression/Qualität wählen (Original, Hoch, Mittel, Niedrig → Bitrate-Presets)
- [x] Speichern des Exports in MediaStore-Album „Video Cutter" (Album-Erstellung automatisch durch MediaStore beim ersten Insert)
- [x] Export-Fortschrittsanzeige (Progress-Overlay, Erfolg/Fehler als Toast)

**Bewusste Vereinfachung:** Echtes Drag-and-Drop zum Verschieben von Clips
wurde durch Pfeiltasten (◄/►) ersetzt. Gesten-Code lässt sich ohne reales
Gerät kaum zuverlässig blind schreiben; sobald der Build läuft, kann Drag
als Verbesserung nachgezogen werden, falls gewünscht.

## Phase 2 – Mehrere Videospuren

> **Hinweis für diese Phase:** Der Toolbar-Button "Ausschnitt extrahieren"
> (Schnellausschnitt, Phase 9) ergibt nur auf der Filmspur Sinn – ein
> GIF/MP4-Ausschnitt lässt sich nicht sinnvoll aus einer Audio- oder
> Bildspur extrahieren. Sobald hier mehrere/andere Spurtypen existieren,
> muss der Button je nach ausgewählter Spur deaktiviert/ausgeblendet
> werden (nur aktiv, wenn die Filmspur aktiv/ausgewählt ist).

- [ ] Zweite (und weitere) Video-Track(s) hinzufügen
- [ ] Kompositions-Vorschau (Overlay/Picture-in-Picture) im Player
- [ ] Track-Steuerung: Sichtbarkeit ein/aus, Reihenfolge/Ebene, stumm schalten
- [ ] Positionierung/Skalierung von Overlay-Spuren (einfacher Ansatz: vordefinierte Layouts – PiP-Ecke, Split-Screen)

## Phase 3 – Audiospur

> **Hinweis:** Siehe Phase 2 – "Ausschnitt extrahieren" bleibt auf die
> Filmspur beschränkt, nicht auf der Audiospur verfügbar.

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

## Phase 7 – Kamera-Aufnahme (Camtasia-artig, Teil 1)

> Nutzerwunsch: Videos direkt in der App aufnehmen können, wie bei Camtasia.
> Geklärt: Kamera-Aufnahme (Webcam-Stil, keine Bildschirmaufnahme).
>
> Status: Code geschrieben und gepusht, noch nicht auf echtem Gerät
> getestet (CI kann keine Kamera-Hardware simulieren, nur Kompilierung
> prüfen).

- [x] Neuer Aufnahme-Screen mit Live-Kameravorschau (CameraX `PreviewView`)
- [x] Start/Stopp-Aufnahme, Kamera-Wechsel (vorne/hinten), Aufnahmedauer-Anzeige
- [x] Laufzeit-Berechtigungen für Kamera + Mikrofon
- [x] Aufgenommenes Video landet als neuer Clip in der Timeline (gleicher
      Weg wie importierte Videos)
- [x] Zugriff sowohl von der Startseite ("Video aufnehmen") als auch aus dem
      Editor heraus ("+ Aufnehmen" neben "+ Clip")

## Phase 8 – Hintergrund entfernen/ersetzen (Camtasia-artig, Teil 2)

> Nutzerwunsch: Hintergrund einer aufgenommenen/importierten Person
> entfernen oder ersetzen. Geklärt: nachträglich beim Bearbeiten (nicht
> live während der Aufnahme), Optionen: Entfernen (siehe Einschränkung
> oben), Weichzeichnen, einfarbig, eigenes Bild/Video.

> **Risikohinweis:** Das ist die technisch aufwändigste Funktion im
> gesamten Projekt (Personen-Segmentierung pro Bildframe + Video-Neuaufbau
> mit Ton-Erhalt). Anders als bisherige Phasen lässt sich das Ergebnis
> nicht über CI prüfen (keine Kamera/GPU/ML-Ausführung auf dem CI-Runner) –
> nur ein echter Gerätetest zeigt, ob es funktioniert. Wird deshalb bewusst
> nach Phase 7 und mit besonderer Sorgfalt umgesetzt, nicht "blind"
> mitgezogen.

- [ ] Bearbeitungsoption "Hintergrund" für den ausgewählten Clip
- [ ] Personen-Segmentierung pro Frame (Google ML Kit Selfie-Segmentierung, on-device)
- [ ] Hintergrund-Modi: Ersetzen durch Farbe (inkl. "Entfernen" = Standardfarbe), Weichzeichnen, eigenes Bild, eigenes Video
- [ ] Neuaufbau des Videos aus bearbeiteten Frames unter Beibehaltung der Original-Tonspur
- [ ] Fortschrittsanzeige (kann je nach Cliplänge mehrere Sekunden bis Minuten dauern)
- [ ] Bekannte Einschränkung: feste Ausgabe-Framerate (siehe Implementierung), nicht zwingend identisch zur Quell-Framerate

## Phase 9 – Schnellausschnitt: GIF/Video-Clip extrahieren

> Nutzerwunsch: An der aktuellen Abspielposition einen Ausschnitt fester
> Länge auswählen, auf der Zeitachse verschieben, dann als GIF oder MP4
> exportieren. Geklärt: Dauer-Presets 2s/5s/10s; GIF nur bis 5s verfügbar,
> ab 10s nur noch MP4 (Dateigröße/Performance).

> **Technische Entscheidung:** Android hat keine eingebaute GIF-Kodierung.
> Statt eines komplexen adaptiven LZW-Encoders (hohes Risiko für subtile
> Fehler, die sich hier nicht testen lassen) wird ein bewusst vereinfachter,
> aber korrekter GIF89a-Encoder eingesetzt: feste 256-Farben-Palette
> (3-3-2-Bit-Reduktion, keine adaptive Farboptimierung) und "LZW ohne echte
> Kompression" (jeder Pixel ein eigener fester 9-Bit-Code, kein wachsendes
> Wörterbuch). Ergebnis: garantiert spezifikationskonforme, korrekt
> abspielbare GIFs, nur ohne den Dateigrößen-Vorteil von echtem LZW – für
> kurze/kleine Clips ein akzeptabler Kompromiss gegenüber dem Risiko eines
> fehlerhaften, nicht testbaren adaptiven Encoders.

> **Einschränkung (Nutzerfeedback):** Macht nur auf der Filmspur Sinn.
> Aktuell gibt es nur eine Spur (die Filmspur), daher noch kein Problem –
> sobald Phase 2/3 (mehrere Video-/Audiospuren) umgesetzt sind, muss der
> Button auf die Filmspur beschränkt werden (siehe Hinweis dort).

> **Fix (Nutzerfeedback, Gerätetest):** Zwei Probleme behoben.
> 1. Der Ausschnitt war auf den aktuell in der Timeline eingestellten
>    Trimm-Bereich des Clips begrenzt, statt auf die komplette Videospur
>    (Quelldatei) zuzugreifen – jetzt steht dafür immer die volle
>    Videolänge zur Verfügung, unabhängig vom aktuellen Timeline-Trimm.
> 2. Für MP4 war die Länge hart auf die Presets 2s/5s/10s begrenzt, ein
>    längerer Ausschnitt war nicht möglich. Neue Option "Länger…" neben
>    den Presets: frei wählbare Dauer per Schieberegler bis zur vollen
>    Videolänge, nur für MP4 (GIF bleibt bei den kurzen Presets wegen
>    Dateigröße/Performance).

- [ ] Neuer Toolbar-Button "Ausschnitt extrahieren" an der aktuellen Abspielposition
- [ ] Dialog: Längen-Presets (2s/5s/10s), Formatwahl (GIF/MP4, abhängig von Preset), Positions-Schieberegler
- [ ] MP4-Export: Wiederverwendung der bestehenden Media3-Transformer-Pipeline (Einzel-Clip mit Trim-Grenzen = Fenster)
- [ ] GIF-Export: eigener, vereinfachter GIF89a-Encoder (siehe oben), Frames per `MediaMetadataRetriever`, feste 10 fps, max. Breite 480px
- [ ] Speichern: MP4 im bestehenden Album "Video Cutter" (Filme), GIF analog in "Video Cutter" (Bilder/`image/gif`)
- [ ] Fortschrittsanzeige (Wiederverwendung von `ExportProgressOverlay`)

---

## Fortschritt

_Wird laufend aktualisiert, sobald Punkte umgesetzt sind._

- CI-Pipeline (`.github/workflows/build-apk.yml`) baut bei jedem Push einen
  Debug-APK und veröffentlicht ihn als GitHub-Release (Tag/Dateiname mit
  Commit-Hash gegen Browser-Caching). Fester Debug-Keystore
  (`keystore/debug.keystore`), damit aufeinanderfolgende Builds sich als
  Update installieren lassen.
- Auf echtem Gerät bestätigt: Import, Trimmen, Schneiden, Speichern
  funktionieren. Nav-Bar-Überlappung behoben, Timeline zeigt echte
  Video-Thumbnails statt Platzhalter.
- Kamera-Aufnahme + Kamerawechsel auf echtem Gerät bestätigt funktionsfähig.
- Performance-Fix: Der Vorschau-Player wurde bisher bei jeder einzelnen
  Fingerbewegung auf dem Trimm-Regler komplett neu aufgebaut
  (`setMediaItems`+`prepare()`), was die App spürbar träge machte. Jetzt
  entprellt (debounced), Rebuild erst nach kurzer Pause.
- UI: Funktionsbuttons (Schneiden/Löschen/Clip/Aufnehmen/Exportieren,
  Kamera-Steuerung, Clip verschieben) sind jetzt Icons mit Tooltip statt
  ausgeschriebenem Text.
- CI-Fix: Die Action `android-actions/setup-android@v3` versuchte ein von
  Google inzwischen entferntes Legacy-Paket (`tools`) zu installieren und
  brach die Pipeline. Ersatzlos entfernt, da der GitHub-Runner bereits ein
  vollständiges Android SDK mitbringt.
- **APK-Größe (>60MB) untersucht und behoben:** Ursache empirisch ermittelt
  (gebautes APK heruntergeladen, mit `unzip -l` und `strings`/`grep` auf den
  `.dex`-Dateien inspiziert statt vermutet) – die Abhängigkeit
  `androidx.compose.material:material-icons-extended` allein war für ~66MB
  der ~68MB verantwortlich, da sie alle ~12.000 Material-Icons in 5
  Stilvarianten ungeschrumpft enthält (Debug-Build ohne Minify), obwohl nur
  ca. 10 Icons genutzt wurden. Fix: Abhängigkeit entfernt, stattdessen ~10
  eigene, schlanke Vektor-Icons (`res/drawable/ic_*.xml`, Strichzeichnungen)
  angelegt und `IconActionButton` von `ImageVector` auf `@DrawableRes Int`
  umgestellt. **Bestätigt:** Build `f9ff47a` liefert eine APK von 34,3MB
  statt zuvor >60MB (gut halbiert).
- **Design-Prinzipien geprüft:** Durchgesehen, welche hartkodierten Farben
  im UI-Code legitim sind (z. B. `Color.Black`/`Color.White` als Overlay auf
  Video-/Kamera-Vorschau – dort wird garantierter Kontrast unabhängig vom
  Theme benötigt) und welche eigentlich Theme-Tokens sein sollten. Fund:
  Der nicht ausgewählte Clip-Rahmen in der Timeline nutzte hartkodiertes
  `Color.Gray` statt `MaterialTheme.colorScheme.outline` – korrigiert.
- **Neues App-Logo:** Platzhalter-Icon (einfaches weißes Play-Dreieck) durch
  ein eigenständiges Motiv ersetzt: ein horizontal in zwei Keile
  "geschnittener" Play-Button (`ic_launcher_foreground.xml`), der Schneiden
  und Abspielen symbolisiert. Hintergrundfarbe von `#1E3A5F` auf ein
  kräftigeres `#1565C0` (Blau) umgestellt. Noch nicht auf dem Gerät
  gegengeprüft, wie das Icon auf dem Homescreen tatsächlich wirkt.
