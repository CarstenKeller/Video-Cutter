# Video Cutter

Android-App zum Schneiden von Videos, entwickelt mit Kotlin, Jetpack Compose
und AndroidX Media3. Der Funktionsumfang und die Reihenfolge der Umsetzung
stehen in [`plan.md`](plan.md).

## Projekt öffnen

1. Android Studio (aktuelle Version) öffnen, „Open" → dieses Verzeichnis wählen.
2. Gradle-Sync abwarten (lädt AGP, Kotlin-Plugin und alle Dependencies aus
   Google/Maven Central – dafür ist Internetzugang nötig).
3. Auf einem Gerät/Emulator mit **Android 10 (API 29) oder neuer** ausführen.

## Wichtiger Hinweis zu dieser Sandbox

Diese Codebasis wurde in einer Cloud-Sandbox ohne Android-SDK und ohne
Netzwerkzugriff auf Googles Maven-Repository erstellt. Das heißt:

- Der Gradle-Wrapper wurde lokal erzeugt (`gradlew`/`gradlew.bat` vorhanden),
  aber **es konnte kein `./gradlew assembleDebug` ausgeführt werden** – der
  Build wurde also nicht kompiliert oder auf einem Gerät getestet.
- Alle Dependency-Versionen (AGP 8.9.2, Kotlin 2.1.0, Compose BOM
  2025.01.01, Media3 1.5.1) sind nach bestem Wissen gewählt, aber nicht
  gegen die tatsächlich verfügbaren Versionen geprüft. Android Studio wird
  beim ersten Sync ggf. ein AGP-/Gradle-Update vorschlagen – das ist normal
  und sollte angenommen werden.
- Am unsichersten ist `app/src/main/java/.../export/VideoExporter.kt`: Die
  exakte Form der Media3-Transformer-API (`Effects`, `DefaultEncoderFactory`,
  `VideoEncoderSettings`) konnte ich nicht kompilieren und daher nicht zu
  100 % verifizieren. Der Code folgt dem Muster aus der Media3-Dokumentation,
  falls der Build dort einen Fehler wirft, bitte gegen die aktuelle
  `androidx.media3:media3-transformer`-Doku prüfen – meist sind es nur
  leicht abweichende Methoden-/Klassennamen.

Bitte beim ersten Build in Android Studio kurz Rückmeldung geben, welche
Fehler ggf. auftreten – dann korrigiere ich gezielt.

## APK herunterladen (automatischer CI-Build)

Da diese Sandbox selbst nicht bauen kann, übernimmt das GitHub-Actions-
Workflow `.github/workflows/build-apk.yml` die echte Kompilierung: Bei jedem
Push auf einen `claude/**`-Branch baut GitHub einen Debug-APK und veröffentlicht
ihn als Release-Asset.

- Datei- und Tag-Name enthalten immer den kurzen Commit-Hash
  (z. B. `video-cutter-a1b2c3d-debug.apk`, Release-Tag `build-a1b2c3d`) –
  dadurch ist jede Version über eine eigene URL erreichbar und der Browser
  kann keine veraltete, gecachte APK-Datei ausliefern.
- Alle Releases: https://github.com/CarstenKeller/Video-Cutter/releases
- Alle Workflow-Läufe (Build-Log bei Fehlern):
  https://github.com/CarstenKeller/Video-Cutter/actions/workflows/build-apk.yml
- Vor der Installation muss auf dem Smartphone „Installation aus unbekannten
  Quellen" für den verwendeten Browser/Dateimanager erlaubt werden, da die
  APK nicht aus dem Play Store kommt.
- Alle Debug-Builds werden mit dem festen, im Repo liegenden Keystore
  `keystore/debug.keystore` signiert (siehe `app/build.gradle.kts`), nicht
  mit dem von Android Studio/AGP pro Maschine automatisch generierten
  `~/.android/debug.keystore`. Ohne das hätte jeder CI-Lauf (frischer
  GitHub-Runner) eine andere Signatur erzeugt, und Android hätte jede neue
  Version mit „App nicht installiert" abgelehnt, sobald schon eine ältere
  Version installiert war. Bei einem Wechsel von einer Version, die noch
  mit einem der alten, automatisch generierten Schlüssel gebaut wurde,
  muss die App einmalig deinstalliert werden – danach installieren sich
  alle folgenden Debug-Builds als normales Update.

## Aktueller Funktionsstand (Phase 1 / MVP)

- Video über den System-Picker importieren
- Vorschau mit ExoPlayer
- Mehrere Clips in einer Spur, Reihenfolge per Pfeiltasten ändern
- Trimmen über Schieberegler, Schneiden am aktuellen Wiedergabepunkt,
  Clip löschen
- Export mit wählbarer Auflösung (Original/4K/1080p/720p/480p) und
  Qualität/Kompression (Original/Hoch/Mittel/Niedrig)
- Speichern im Album „Video Cutter" (`Filme/Video Cutter`), Album wird bei
  Bedarf automatisch angelegt

## Phase 7: Kamera-Aufnahme

- Video direkt in der App mit der Gerätekamera aufnehmen (CameraX),
  Kamera-Wechsel vorne/hinten, Aufnahmedauer-Anzeige
- Aufgenommenes Video landet wie ein importiertes Video als neuer Clip in
  der Timeline
- Erreichbar über „Video aufnehmen" auf der Startseite oder „+ Aufnehmen"
  im Editor
- Benötigt Kamera- und Mikrofon-Berechtigung (Laufzeit-Abfrage)
- **Nicht verifiziert:** CameraX-Aufnahme lässt sich nicht per CI testen
  (kein Kamerahardware auf dem Build-Runner) – nur ein echter Gerätetest
  zeigt, ob Aufnahme/Vorschau/Speichern zuverlässig funktionieren.

## Phase 9: Schnellausschnitt (GIF/Video-Clip extrahieren)

- Neuer Toolbar-Button an der aktuellen Abspielposition: Längen-Preset
  wählen (2s/5s/10s) oder per "Länger…" eine frei wählbare Dauer bis zur
  vollen Videolänge, Position per Schieberegler verschieben, als GIF
  oder MP4 extrahieren
- GIF nur bis 5s verfügbar (ab 10s bzw. bei freier Dauer nur MP4), wegen
  Dateigröße/Performance
- Der Ausschnitt greift immer auf die komplette Videospur (Quelldatei) zu,
  nicht nur auf den aktuell in der Timeline eingestellten Trimm-Bereich
- MP4-Export nutzt die bestehende Media3-Transformer-Pipeline; GIF-Export
  nutzt einen selbst geschriebenen, bewusst vereinfachten GIF89a-Encoder
  (feste 256-Farben-Palette, keine echte LZW-Kompression – siehe
  `extract/GifEncoder.kt` für die Begründung)
- Speichern: MP4 im Album „Video Cutter" (Filme), GIF im Album
  „Video Cutter" (Bilder)
- **Nicht verifiziert:** Der GIF-Encoder wurde Byte für Byte gegen die
  GIF89a-Spezifikation durchgerechnet, aber nicht auf einem Gerät
  getestet, ob die erzeugten Dateien von Galerie-Apps korrekt angezeigt
  werden.

## APK-Größe & App-Icon

- Die Debug-APK war zuvor über 60MB groß. Ursache (empirisch per
  `unzip`/`strings` am gebauten APK ermittelt): die Abhängigkeit
  `androidx.compose.material:material-icons-extended` lieferte alle
  ~12.000 Material-Icons ungeschrumpft mit aus, obwohl nur rund 10 Icons
  verwendet wurden. Entfernt und durch eigene, schlanke
  Strich-Vektor-Icons (`app/src/main/res/drawable/ic_*.xml`) ersetzt –
  dadurch sollte die APK deutlich kleiner werden. **Bestätigt:** Build
  `f9ff47a` liefert 34,3MB statt zuvor >60MB.
- App-Icon überarbeitet: statt eines einfachen weißen Play-Dreiecks zeigt
  es jetzt einen horizontal "geschnittenen" Play-Button (zwei Keile mit
  Lücke dazwischen) auf kräftigem Blau – als Symbol für "Schneiden" +
  "Abspielen". **Nicht verifiziert:** Wirkung auf echtem Homescreen/App-
  Drawer noch nicht gegengeprüft.

Alles Weitere (mehrere Videospuren, Audiospur, Bilder, Übergänge,
Hintergrund-Ersatz, Undo/Redo etc.) folgt gemäß `plan.md`.
