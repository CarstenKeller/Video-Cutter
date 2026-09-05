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

Alles Weitere (mehrere Videospuren, Audiospur, Bilder, Übergänge, Undo/Redo
etc.) folgt gemäß `plan.md`.
