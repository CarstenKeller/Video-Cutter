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

> Nutzerwunsch (Gerätetest-Feedback): zwischen zwei benachbarten Clips
> unterschiedliche Übergänge einfügbar machen. Bewusst noch nicht
> zusammen mit den anderen Zeitleiste-Fixes umgesetzt – Media3
> Composition/Transition-Effekte sind die technisch unsicherste, noch
> nicht verifizierte Stelle im ganzen Projekt (siehe "Offene technische
> Unsicherheit" oben) und verdienen einen eigenen, sorgfältig getesteten
> Durchlauf statt in einer großen Sammel-Änderung mitgezogen zu werden.

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

> **Korrektur (Nutzerfeedback):** Die ursprüngliche Klärung "ab 10s nur
> noch MP4" war ein Missverständnis – GIF soll bei allen drei Presets
> (2s/5s/10s) möglich sein, nur bei frei gewählter Dauer über 10s hinaus
> ("Länger…") bleibt GIF gesperrt. `ExtractDurationPreset.LONG.allowsGif`
> auf `true` korrigiert.

> **Noch offen (Nutzerwunsch):** Der Dialog soll durch Inline-Bedienung auf
> der Hauptseite ersetzt werden – Längen-/Formatauswahl direkt im Editor,
> der Ausschnitt als verschiebbares Fenster auf der Zeitleiste (zusätzlich
> zum normalen Positionsbalken), Wiedergabe spielt währenddessen nur das
> Fenster in Dauerschleife, nach dem Extrahieren wechselt die Zeitleiste
> zurück in den normalen Positions-Modus. Bewusst nicht zusammen mit den
> übrigen Zeitleiste-Fixes umgesetzt (eigener, nicht-trivialer
> Umbau von Timeline + Player-Loop-Logik, der einen eigenen Durchlauf
> verdient statt in einer großen Sammel-Änderung mitgezogen zu werden).

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
- **Fix (Nutzerfeedback, Gerätetest):** Die Zeitleiste zeigte nirgends an,
  an welcher Stelle im Video sich die Wiedergabe gerade befindet – ein
  Clip-Block war nur ein statischer Thumbnail-Streifen ohne Positions-
  Rückmeldung. Ergänzt: ein sich bewegender Positionsbalken (Playhead) im
  aktuellen Clip-Block sowie eine Zeitanzeige "verstrichene Zeit /
  Gesamtlänge" über der Zeitleiste (ExoPlayer-Position wird dafür alle
  200ms abgefragt, da kein nativer Compose-State dafür existiert).
- **APK-Größe erneut geprüft (Nutzerfeedback, ~36MB):** Erneut per
  `unzip`/`strings` am gebauten APK untersucht statt vermutet. Befund:
  ~94% der APK sind drei `.dex`-Dateien, dominiert von unverkürztem
  Compose/Media3/CameraX/Kotlin-Bytecode – strukturell normal für einen
  Debug-Build ohne R8/Minify, keine einzelne Fehlkonfiguration wie zuvor
  bei `material-icons-extended`. Entscheidung (Nutzer gefragt): R8/
  Minify **nicht** aktivieren, da das echte Regressionsrisiko für Media3
  Transformer/CameraX (Reflection-sensitiver Code) nur per Gerätetest
  prüfbar wäre und nicht gegen eine moderate Größenersparnis aufwiegt.
- **Zeitleiste grundlegend überarbeitet (Nutzerfeedback, Gerätetest):**
  - Tippen auf einen Clip-Block springt im Video an diese Stelle
    (Scrubben), ersetzt die reine "Clip auswählen"-Funktion.
  - Trimmen geschieht jetzt direkt auf der Zeitleiste über zwei
    Zieh-Griffe am Rand des aktiven Clip-Blocks statt über einen
    separaten Regler unterhalb des Videos (`TrimControls`/`RangeSlider`
    entfernt). Die Griffe sitzen an den Blockrändern (Start/Ende sind per
    Definition 0%/100% der getrimmten Länge).
  - "Löschen" löscht jetzt den Clip, in dem sich die Wiedergabeposition
    gerade befindet (nicht mehr eine separate Auswahl) – das bisherige
    `selectedClipId`-Feld in `TimelineState`/`TimelineViewModel` wurde
    komplett entfernt, da "aktiver Clip" jetzt ausschließlich aus der
    Player-Position abgeleitet wird.
  - Dabei gefundener, vorher unsichtbarer Fehler mitbehoben: Der
    Playlist-Neuaufbau nach jeder Änderung (`setMediaItems`) hat die
    Wiedergabe bisher immer auf den Projektanfang zurückgesetzt – ohne
    Positionsanzeige nicht auffällig, mit Positionsbalken wäre das jetzt
    als Sprung nach jedem Trimmen/Schneiden sichtbar geworden. Behoben,
    indem die Position als "verstrichene Zeit" vor dem Neuaufbau
    gesichert und danach an der äquivalenten Stelle im neuen Clip-Satz
    fortgesetzt wird.
  - **Nicht verifiziert:** Die Geste-Ebenen (Tippen zum Springen, Ziehen
    an den Trimm-Griffen, Verschieben-Pfeile) wurden sorgfältig nach
    gängigen Compose-Mustern geschichtet, konnten aber nicht auf einem
    echten Gerät getestet werden – insbesondere ob sich Tippen und
    Trimm-Griffe nicht gegenseitig stören, muss der nächste Gerätetest
    zeigen.
  - **Bekannte Einschränkung:** Da ein Row-Layout ein Element nur von
    rechts sichtbar schrumpft/wächst, bleibt der linke Trimm-Griff beim
    Ziehen optisch an Ort und Stelle, während sich der Block dahinter
    verkleinert/vergrößert. Die Geste (Fingerbewegung → Trimm-Wert)
    funktioniert trotzdem korrekt, nur die visuelle Rückmeldung ist nicht
    perfekt 1:1. Eine pixelgenaue Lösung bräuchte ein eigenes,
    nicht-flow-basiertes Layout – als mögliche Folgeverbesserung
    vorgemerkt, falls sich das im Gerätetest als störend erweist.
- **Regression gefunden und behoben (Nutzerfeedback: "kann ich immer noch
  nicht erkennen, wo ich bin"):** Ein Zwischenstand dieser Überarbeitung
  (Commit `c875cd6`) hatte Blockbreite auf die volle Quelllänge
  umgestellt (um das linke-Trimm-Griff-Problem oben "richtig" zu lösen),
  mit abgedunkeltem Bereich außerhalb des Trimm-Fensters. Das hat bei
  normal langen Videos absurd breite Blöcke erzeugt (z. B. 60s Quelle →
  ca. 3600dp breiter Block), wodurch Positionsbalken und Schnitte
  praktisch unsichtbar bzw. weggescrollt waren – das war die eigentliche
  Ursache für "ich erkenne meine Position nicht" und "den Schnitt kann
  ich kaum erkennen". Zurückgesetzt auf Blockbreite = getrimmte Länge
  (siehe oben); Thumbnails werden wieder aus dem getrimmten Bereich des
  jeweiligen Clips extrahiert (nicht aus der vollen Quelldatei), damit
  zwei durch einen Schnitt entstandene Clips wieder sichtbar
  unterschiedliche Vorschaubilder zeigen. Positionsbalken zusätzlich
  deutlich auffälliger gemacht (4dp, weiß mit schwarzer Außenlinie statt
  2dp Primärfarbe) und die Zeitanzeige über der Zeitleiste vergrößert
  ("Position: mm:ss / mm:ss", `titleMedium` statt `labelLarge`).
- **Weitere Fixes nach erneutem Gerätetest:**
  - **Thumbnail-Bug nach Schnitt behoben:** Nur `secondHalf` bekam beim
    Schneiden eine neue Clip-Id, `firstHalf` behielt die alte - die
    Timeline cacht Thumbnails pro Clip-Id (bewusst, damit Trimm-Ziehen
    nicht laufend neu extrahiert), wodurch `firstHalf` zufällig noch die
    (jetzt teils falschen) alten Thumbnails zeigte, während die frische
    Extraktion für `secondHalf` bei vereinzelten fehlschlagenden Frames
    (`MediaMetadataRetriever.getFrameAtTime` kann `null` liefern) nur 1
    Bild zurückgab, das dann über die volle Blockbreite gestreckt wurde
    ("absurde Vergrößerung"). Behoben: `splitClip` gibt jetzt *beiden*
    Hälften eine neue Id; Thumbnail-Liste hat immer feste Länge mit
    Platzhalter-Kacheln für fehlgeschlagene Frames (kein Strecken mehr
    möglich) plus ein Fallback auf `OPTION_CLOSEST`, falls
    `OPTION_CLOSEST_SYNC` `null` liefert.
  - **Trimm-Griffe auf `Modifier.draggable()` umgestellt:** Die
    handgeschriebene `detectDragGestures`-Logik wurde durch die
    offizielle, dafür vorgesehene Compose-API ersetzt (robuster bei
    Gesten-Konflikten mit dem Tipp-zum-Springen-Handler auf demselben
    Block) und der Treffer-Bereich von 14dp auf 20dp vergrößert.
  - Tipp-zum-Springen nutzt jetzt den benannten `onTap`-Parameter statt
    eines positionellen Trailing-Lambdas (Eindeutigkeit).
  - **Nicht verifiziert:** Ob das die berichtete Unzuverlässigkeit beim
    Springen und das komplett nicht funktionierende Trimmen tatsächlich
    behebt, muss der nächste Gerätetest zeigen - ohne Laufzeitzugriff auf
    ein echtes Gerät lässt sich Gesten-Verhalten nicht abschließend
    verifizieren.
- **Kurskorrektur nach wiederholt fehlgeschlagenen Gesten-Fixes
  (Nutzerfeedback: "absoluter Murks, kein bisschen besser"):** Vier
  Iterationen in Folge (0cf4cd9 → c875cd6 → 8eada7b → a0daa0f) haben das
  Problem mit komplexen, nur anhand von Compose-Quellcode-Kenntnis
  entworfenen Zeitleiste-Gesten (Tippen-an-beliebiger-Stelle, Zieh-Griffe
  direkt auf dem Clip-Block) nicht zuverlässig gelöst - ohne echtes Gerät
  lässt sich Gesten-/Layout-Verhalten in Compose nicht abschließend
  verifizieren, und das wiederholte Raten hat Zeit des Nutzers gekostet,
  ohne den eigentlichen Wunsch ("erst einmal Standardfunktionalität wie
  bei jeder Android-Videobearbeitung") zu erfüllen.
  - **Entscheidung:** Zurück auf die bewährte, vor dieser Session
    funktionierende Bedienung: Tippen auf einen Clip-Block springt an
    dessen Anfang (statt an eine beliebige, aus der Tipp-Position
    berechnete Stelle), Trimmen erfolgt wieder über einen separaten
    `RangeSlider` unterhalb der Zeitleiste (statt Zieh-Griffe direkt am
    Blockrand) - für den Clip, in dem sich die Wiedergabeposition gerade
    befindet. Alle Zieh-Gesten-Experimente (`Modifier.draggable`,
    Tipp-Fraktion-Berechnung) entfernt.
  - **Beibehalten**, da laut Nutzer tatsächlich bestätigt funktionierend:
    Schneiden und Löschen am Playhead statt separater Auswahl, die
    Thumbnail-Korrektheit-Fixes (beide Hälften neue Clip-Id,
    Platzhalter-Kacheln statt Strecken) und der Fix gegen den
    Rücksprung-zum-Anfang bei jeder Änderung.
  - Komfort-Gesten direkt auf der Zeitleiste (präzises Scrubben per Tipp,
    Trimmen per Zieh-Griff) bleiben als mögliche spätere Verbesserung
    vorgemerkt, aber erst, wenn die Standardfunktionalität auf einem
    echten Gerät als stabil bestätigt ist.
- **Zeitleiste nach Vorlage der Standard-Galerie-Oberfläche neu gebaut
  (Nutzer hat ein Screenrecording der Android-Galerie-Videobearbeitung
  geschickt: "so will ich das haben"):** Per `ffmpeg` Einzelbilder aus dem
  Screenrecording extrahiert und ausgewertet (keine Vermutung, sondern
  tatsächlich abgelesen). Erkanntes Muster: ein durchgehender Filmstreifen
  über die komplette Videolänge, zwei weiße Trimm-Griffe an den Rändern
  des gewählten Bereichs (Rest abgedunkelt), ein davon unabhängiger,
  ziehbarer Positionsstrich dazwischen - kein "an beliebiger Stelle
  tippen" zum Springen.
  - Umgesetzt als `ActiveClipTrimStrip`: Streifen ist **bildschirmbreit,
    konstant** (nicht proportional zur Videolänge - das war die
    eigentliche Ursache des `c875cd6`-Fehlschlags, nicht das Full-Source-
    Prinzip an sich). Dadurch ändert sich die Streifenbreite nie während
    einer Trimm-Geste, was Griff-Positionierung und Pixel-pro-Mikrosekunde-
    Berechnung stabil und unabhängig vom bisher riskanten
    "Row schrumpft nur von rechts"-Problem macht.
  - Drei Griffe (`StripHandle`, über `Modifier.draggable()`/
    `rememberDraggableState`): zwei für Trimm-Start/-Ende, einer für die
    Wiedergabeposition (scrubben). Jeder Griff hat eine eigene, klar
    getrennte Trefferfläche - bewusst **kein** zusätzliches
    Tippen-auf-beliebige-Stelle mehr, das war in den vorherigen vier
    Versuchen die Hauptursache für Gesten-Konflikte.
  - Mehrere Clips (nach einem Schnitt) werden weiterhin über eine
    einfache Auswahl-Reihe (`ClipChip`, Tippen + Verschieben-Pfeile, kein
    Ziehen) oberhalb des Trimm-Streifens gewechselt - diese Interaktion
    war bereits bestätigt funktionsfähig und wurde unverändert gelassen.
  - **Nicht verifiziert:** Erster Durchlauf nach Vorlage, noch nicht auf
    einem echten Gerät getestet.
- **Zoom-Funktion für den Trimm-Streifen ergänzt (Nutzerfeedback):** Die
  feste Bildschirmbreite (bewusste Entscheidung gegen absurd breite
  Streifen, siehe oben) machte es bei längeren Videos schwer, eine
  genaue Stelle zu treffen - zu wenig Pixel pro Sekunde. Zwei
  Zoom-Buttons (−/+, Faktor 1,6 pro Stufe, bis 6x) strecken den
  Streifen über die Bildschirmbreite hinaus; der Bereich wird dann per
  `horizontalScroll` durchfahren. Beim Zoomen wird automatisch auf die
  aktuelle Wiedergabeposition zentriert, damit sie nicht aus dem
  Blickfeld scrollt. Die Trimm-Griffe und der Positionsgriff
  funktionieren unverändert (sie rechnen bereits generisch mit der
  jeweils aktuellen Streifenbreite in Pixeln, die jetzt zoomabhängig
  ist statt fix). Die Anzahl der Vorschaubilder wird mit dem Zoom-Level
  mitskaliert (10 bis 30), für mehr Detail bei höherer Vergrößerung.
  **Nicht verifiziert:** insbesondere, ob Ziehen an einem Griff und das
  Scrollen des Streifens sich nicht gegenseitig stören (beides sind
  offizielle, zum Verschachteln gedachte Compose-Gesten-APIs, aber ohne
  Gerät nicht abschließend geprüft).
- **Positionsbezogene Aktionen direkt an die Zeitleiste verschoben
  (Nutzerwunsch, nach Vorbild der Galerie-Vorlage):** Schneiden, Löschen
  und Ausschnitt extrahieren funktionierten bereits positionsbezogen
  (abhängig von der aktuellen Wiedergabeposition/dem aktiven Clip),
  saßen aber in einer vom Bearbeitungskontext getrennten Toolbar oben.
  Zunächst unterhalb des Trimm-Streifens platziert.
  **Korrektur (Folgefeedback):** Das verteilte Buttons auf zwei Stellen
  (oberhalb + unterhalb der Zeitleiste). Zurückgesetzt auf eine
  gemeinsame Toolbar oberhalb der Zeitleiste mit allen sechs Buttons
  (Schneiden, Löschen, Clip hinzufügen, Aufnehmen, Ausschnitt
  extrahieren, Exportieren) - entspricht dem Nutzerwunsch "alle
  oberhalb".
  **Übergänge zwischen Clips bleiben bewusst unangetastet** - diese
  Funktion existiert technisch noch gar nicht (siehe Phase 5) und lässt
  sich nicht einfach "platzieren", sondern müsste erst von Grund auf neu
  gebaut werden; das verdient einen eigenen, sorgfältig getesteten
  Durchlauf statt in eine Button-Umsortierung hineingezogen zu werden.
- **Schnellausschnitt vollständig in die Hauptseite integriert
  (Nutzerwunsch, wie ursprünglich beschrieben):** Der separate Dialog
  (`QuickExtractDialog`) ist entfernt. Stattdessen:
  - "Ausschnitt extrahieren" öffnet ein `ExtractControls`-Panel direkt
    auf der Hauptseite (Längen-Presets 2s/5s/10s/"Länger…", Format
    GIF/MP4 mit denselben Verfügbarkeitsregeln wie zuvor).
  - Auf dem Trimm-Streifen erscheint statt der zwei Trimm-Griffe ein
    einzelnes, durchsichtig hervorgehobenes Ausschnitt-Fenster fester
    Länge (`ExtractWindow`), das als Ganzes gezogen wird (nicht an
    Rändern skaliert, da die Länge über die Längenauswahl feststeht).
  - Solange das Fenster aktiv ist, läuft die Wiedergabe nur innerhalb
    des Fensters in Dauerschleife (die bestehende 200ms-Positions-
    Abfrage prüft zusätzlich, ob die Position das Fenster verlassen hat,
    und springt dann an den Fensteranfang zurück).
  - Nach dem Extrahieren (Tippen auf "Extrahieren") schließt sich das
    Panel, das Fenster verschwindet und die Zeitleiste zeigt wieder den
    normalen Positions-/Trimm-Modus - wie beschrieben ("Markierung
    wechselt wieder in den Positionsmodus").
  - Wiederverwendet dieselbe, bereits bewährte Zieh-Griff-Technik
    (`Modifier.draggable()`, fester Pixel-pro-Mikrosekunde-Faktor dank
    konstanter Streifenbreite) wie die Trimm-Griffe - kein neues
    Gesten-Muster, nur ein weiterer Anwendungsfall davon.
  - **Nicht verifiziert:** Erster Durchlauf, insbesondere das
    Zusammenspiel aus Ziehen des Ausschnitt-Fensters und der
    Dauerschleifen-Wiedergabe wurde nicht auf einem echten Gerät
    getestet.
- **Schwerwiegender GIF-Encoder-Bug gefunden und behoben (Nutzerfeedback:
  "das extrahierte GIF lässt sich nicht abspielen"):** Durch genaues
  Nachrechnen der GIF89a-/LZW-Dekodierregeln (nicht durch Raten)
  gefunden: Ein konformer GIF-Decoder baut sein LZW-Wörterbuch immer mit,
  unabhängig davon, welche Codes der Encoder tatsächlich benutzt - und
  erhöht automatisch die Code-Breite von 9 auf 10 Bit, sobald das
  Wörterbuch 512 Einträge erreicht (passiert nach spätestens 254
  verarbeiteten Pixel-Codes). Der bisherige Encoder schrieb aber
  dauerhaft feste 9-Bit-Codes, ohne das zu berücksichtigen - Encoder und
  Decoder liefen bei jedem Bild mit mehr als ca. 250 Pixeln (praktisch
  immer) bezüglich der Bit-Ausrichtung auseinander, das Bild war ab dort
  unlesbar. Behoben: Alle 100 Pixel wird vorsorglich ein Clear-Code
  gesendet, der das Decoder-Wörterbuch zurücksetzt, bevor die kritische
  Schwelle erreicht wird - der Encoder bleibt dadurch dauerhaft bei 9 Bit
  pro Code, keine strukturelle Änderung am bewusst vereinfachten Ansatz
  nötig.
- **Zoom-Maximum auf 12x erhöht** (Nutzerfeedback: "muss mindestens bis
  10 gehen"), vorher 6x.
- **Zoom-Maximum weiter auf 60x erhöht** (Nutzerfeedback: "muss mindestens
  doppelt so hoch möglich sein. Bei einem Video von 11 Minuten Länge ist
  das bei 10s nicht praktikabel"): 12x reichte bei langen Videos nicht,
  um ein 10-Sekunden-Schnellausschnitt-Fenster präzise zu platzieren.
  60x statt nur der verlangten Verdopplung (24x), weil die konkrete
  Beispielrechnung (11 Minuten, 10s-Fenster) bei 24x immer noch recht
  schmal ausfällt. Die Anzahl der Vorschaubilder im Streifen ist bei
  hohem Zoom weiterhin auf 40 begrenzt (vorher 30) - mehr Einzelbilder
  pro Darstellung würden die Ladezeit (MediaMetadataRetriever pro Bild)
  deutlich verschlechtern; **nicht verifiziert**, ob 40 bei Zoom 60x
  visuell noch dicht genug wirkt, das muss der Nutzer auf dem Gerät
  beurteilen.
- **Layout-Fehler behoben: Video-Vorschau schrumpfte, wenn das
  Schnellausschnitt-Panel eingeblendet wurde** (Nutzerfeedback: "nicht
  bedienbar, weil das Menü in der Mitte so viele Platz einnimmt und den
  Videobereich nach oben wegdrückt"). Ursache: die Vorschau hatte
  `Modifier.weight(1f)` in einer `Column` mit sonst nur
  wrap-content-hohen Geschwistern - jedes zusätzliche Element (z. B. das
  Ausschnitt-Panel) nahm der Vorschau direkt Höhe weg. Behoben, indem die
  Vorschau stattdessen eine feste Größe über ein Seitenverhältnis
  (16:9, `Modifier.aspectRatio`) bekommt und nicht mehr schrumpfen kann;
  der gesamte Bereich darunter (Toolbar, ggf. Ausschnitt-Panel,
  Zeitleiste) ist jetzt selbst `weight(1f)` und vertikal scrollbar
  (`verticalScroll`), sodass er bei Bedarf scrollt statt die Vorschau zu
  verdrängen.
- **`ExtractControls`-Panel radikal verschlankt** (Nutzerfeedback: "nimmt
  die Hälfte des Bildschirms ein... keinen Erklärtext und keine zwei
  Zeilen für Überschriften"). Vorher: Titel + Erklärsatz, eigene
  Überschriftenzeile "Länge" über den Längen-Chips, eigene
  Überschriftenzeile "Format" über den Format-Radiobuttons (mit
  Textbeschriftung daneben), ein Erklärsatz zur GIF-Einschränkung,
  separate Abbrechen-/Extrahieren-Textbuttons in eigener Zeile - macht
  typisch 5-6 Zeilen. Jetzt: eine einzige horizontal scrollbare Zeile
  mit Längen-Chips, Format-Chips (GIF/MP4, deaktiviert statt mit
  Erklärtext wenn nicht verfügbar) und zwei kompakten Icon-Buttons
  (Haken/Kreuz, neues `ic_check.xml` passend zum bestehenden
  `ic_close.xml`) für Extrahieren/Abbrechen - keine Überschriften, kein
  Fließtext. Nur bei benutzerdefinierter Länge ("…"-Chip) kommt eine
  zweite, ebenso knappe Zeile mit Zahl + Schieberegler hinzu.
- **`ExtractControls` erneut überarbeitet** (Nutzerfeedback: Schieberegler
  "sehr fummelig und nicht exakt zu bedienen"; außerdem sollten alle
  Elemente inkl. Abbrechen ohne horizontales Scrollen in zwei Zeilen
  passen):
  - Schieberegler für die freie Länge ersetzt durch einen Stepper
    (Zahl + Rauf-/Runter-Buttons, Schrittweite 5s) - exakt statt
    fummelig zu bedienen.
  - Horizontales Scrollen komplett entfernt: Zeile 1 = Längen-Presets +
    Stepper, Zeile 2 = Format-Chips + Abbrechen-/Extrahieren-Icons
    (rechtsbündig über einen `Spacer(Modifier.weight(1f))`) - durch den
    kompakteren Stepper passt jetzt alles ohne Scrollen auf den
    Bildschirm, Abbrechen ist immer sichtbar.
  - **Bug behoben: Dauerschleife blieb dauerhaft auf der zuerst
    gewählten Länge**, auch nach Wahl einer anderen Länge (Nutzerfeedback:
    "läuft die loop immer nur in dieser Schleife, auch wenn ich eine
    andere Dauer wähle"). Ursache: Die dauerhaft laufende
    `LaunchedEffect(player)`-Coroutine in `EditorScreen` wird nur einmal
    gestartet (ihr Key `player` ändert sich nie) und hatte sich beim
    Start über die damals aktuelle `extractDurationUs` geschlossen - ein
    einfacher lokaler `val`, keine Compose-`State`, also ohne Wirkung bei
    späteren Recompositions. Behoben mit `rememberUpdatedState`.
  - **Bug behoben: Längenänderung verschob das Fenster an seinen alten
    Anfang statt um die Mitte zu zentrieren** (Nutzerfeedback: "setzt du
    die Auswahl als Startzeitpunkt für die Sequenz, es sollte genau die
    Mitte sein"). Neue Hilfsfunktion `recenteredExtractStartUs` hält bei
    einer Längenänderung die bisherige Fenster-MITTE fest, statt nur den
    alten Start-Zeitpunkt in den neuen Längenbereich zu clampen.
  - **Bug behoben: Vorschaubilder im Filmstreifen wurden beim Reinzoomen
    größer** (Nutzerfeedback: "Sie sollten gleich groß bleiben"). Ursache:
    die Anzahl der *angezeigten* Kacheln war auf 40 gedeckelt, während die
    Streifenbreite mit dem Zoom bis auf das 60-Fache wuchs - ab Zoom 4x
    wurden die Kacheln dadurch zunehmend breiter gestreckt. Jetzt
    getrennt: eine feste, von Zoom unabhängige Anzahl tatsächlich
    extrahierter Einzelbilder (`THUMBNAIL_EXTRACT_COUNT = 40`, teuer -
    ein `MediaMetadataRetriever`-Seek pro Bild) gegenüber einer mit dem
    Zoom mitwachsenden Anzahl *angezeigter* Kacheln (`FULL_STRIP_THUMBNAILS
    * zoomLevel`, günstig - mehrere Kacheln zeigen bei hohem Zoom
    dasselbe Einzelbild); über `weight(1f)` bleibt jede Kachel dadurch
    rechnerisch exakt bildschirmbreite/10 breit, unabhängig vom Zoom.
  - **Nachbesserung zum vorherigen Punkt: Kachel-Breite war weiterhin
    falsch, weil `FULL_STRIP_THUMBNAILS = 10` eine willkürliche, vom
    Videoformat unabhängige Zahl war** (Nutzerfeedback: "du presst das
    Video in der Ganzen Länge in eine Bildschirmbreite. Die Thumbnails
    sind dadurch 4x so hoch wie breit. Die Thumbnails müssen im
    Bildformat des Videos bleiben"). Der vorherige Fix hat zwar
    erfolgreich verhindert, dass Kacheln beim Zoomen größer *werden*,
    aber die Basis-Kachelbreite (bildschirmbreite/10) hatte nie etwas mit
    dem tatsächlichen Seitenverhältnis des Videos zu tun - bei normalen
    Formaten (16:9, 9:16) waren die Kacheln dadurch immer schon verzerrt,
    nicht erst beim Zoomen. Jetzt wird die Kachel-Breite aus dem
    tatsächlichen Bildformat abgeleitet (Breite/Höhe des ersten geladenen
    Vorschaubilds, Fallback 16:9 bis das erste Bild geladen ist) mal
    fester Streifenhöhe (`STRIP_HEIGHT_DP = 80`) - jede Kachel behält so
    ihr korrektes Seitenverhältnis. Die Streifenbreite bleibt bewusst
    weiterhin bildschirmbreite × Zoom (nicht an die Videolänge gekoppelt
    - das war die ursprüngliche, mehrfach bestätigte Fehlerquelle);
    lediglich die *Anzahl* der (jetzt seitenverhältnis-treuen) Kacheln
    darin ergibt sich daraus, wächst also mit dem Zoom mit - genau wie
    vom Nutzer gewünscht ("Beim zoom nimmt die Anzahl der Thumbnails
    einfach zu und die Scrollstrecke... nimmt zu"). Da die Kachelbreite
    jetzt eine bewusst feste Pixelgröße ist (nicht mehr `weight(1f)`),
    kann am rechten Rand des Streifens eine letzte, nur teilweise
    gefüllte (durch den Streifen-Rahmen abgeschnittene) Kachel entstehen
    - das ist in Kauf genommen, da die Alternative (Kacheln wieder
    verzerren, um exakt aufzugehen) genau das wäre, was hier behoben
    wurde. **Nicht verifiziert:** Ob `MediaMetadataRetriever.getFrameAtTime`
    das Seitenverhältnis in allen Rotationsfällen (insbesondere bei
    Hochformat-Aufnahmen mit Rotations-Metadaten) korrekt wiedergibt -
    das lässt sich nur auf dem echten Gerät mit echten Testvideos
    verifizieren.
  - **Positionsgriff läuft bei der Wiedergabe aus dem sichtbaren Bereich,
    statt dass die Zeitleiste folgt** (Nutzerfeedback: "Nicht die
    Markierung in der Zeitleiste soll sich aus dem Bild bewegen, sondern
    die Zeitleiste bewegt sich fort"). Bisher wurde der Scroll-Zustand
    nur beim Ändern des Zoom-Levels neu zentriert, nicht während der
    laufenden Wiedergabe - bei Zoom > 1x lief der Positionsgriff dadurch
    irgendwann aus dem sichtbaren Ausschnitt heraus. Neue, separate
    `LaunchedEffect(playheadFraction)`: sobald der Griff den sichtbaren
    Bereich tatsächlich verlässt, scrollt der Streifen (sanft animiert
    über `animateScrollTo`) so, dass der Griff wieder mittig sichtbar
    ist. Bewusst *kein* Zentrieren bei jedem Positions-Tick (alle 200ms),
    damit sich der Streifen im pausierten Zustand weiterhin frei
    verschieben lässt, um sich andere Stellen anzusehen, ohne dass das
    sofort wieder zurückspringt.
