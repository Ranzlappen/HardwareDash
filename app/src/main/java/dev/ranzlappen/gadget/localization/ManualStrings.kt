package dev.ranzlappen.gadget.localization

class Manual(private val l: Language) {

    // ── Labels ──────────────────────────────────────────────────────────
    val title get() = m(l, "User Manual", "Benutzerhandbuch", "Manual de usuario", "Manuel utilisateur")
    val howToUseLabel get() = m(l, "How to Use", "Verwendung", "Cómo usar", "Comment utiliser")
    val prerequisitesLabel get() = m(l, "Prerequisites", "Voraussetzungen", "Requisitos previos", "Prérequis")
    val limitationsLabel get() = m(l, "Limitations", "Einschränkungen", "Limitaciones", "Limitations")
    val widgetsTitle get() = m(l, "Home Screen Widgets", "Startbildschirm-Widgets", "Widgets de pantalla de inicio", "Widgets d'écran d'accueil")

    // ── Getting Started ─────────────────────────────────────────────────
    val gettingStartedTitle get() = m(l,
        "Getting Started",
        "Erste Schritte",
        "Primeros pasos",
        "Prise en main")
    val gettingStartedBody get() = m(l,
        "Gadget is organized into five tabs: Dashboard, Tools, Monitor, Logbook, and More. On first launch the app opens to the Dashboard. Grant permissions as prompted \u2014 each feature explains what it needs. The app works fully offline except for map tiles in GPS.",
        "Gadget ist in f\u00FCnf Tabs unterteilt: Dashboard, Werkzeuge, Monitor, Logbuch und Mehr. Beim ersten Start \u00F6ffnet sich das Dashboard. Erteilen Sie Berechtigungen bei Aufforderung \u2014 jede Funktion erkl\u00E4rt, was sie ben\u00F6tigt. Die App funktioniert vollst\u00E4ndig offline, au\u00DFer f\u00FCr Kartenkacheln im GPS.",
        "Gadget est\u00E1 organizado en cinco pesta\u00F1as: Panel, Herramientas, Monitor, Registro y M\u00E1s. Al iniciar por primera vez se abre el Panel. Conceda permisos cuando se le solicite \u2014 cada funci\u00F3n explica lo que necesita. La app funciona completamente sin conexi\u00F3n, excepto para los mosaicos del mapa en GPS.",
        "Gadget est organis\u00E9 en cinq onglets : Tableau de bord, Outils, Moniteur, Journal et Plus. Au premier lancement, l\u2019application s\u2019ouvre sur le Tableau de bord. Accordez les permissions demand\u00E9es \u2014 chaque fonctionnalit\u00E9 explique ses besoins. L\u2019application fonctionne enti\u00E8rement hors ligne, sauf pour les tuiles de carte GPS.")

    // ── Navigation ──────────────────────────────────────────────────────
    val navigationTitle get() = m(l,
        "Navigation",
        "Navigation",
        "Navegaci\u00F3n",
        "Navigation")
    val navigationBody get() = m(l,
        "Dashboard is your home screen with status cards and quick actions. Tools contains Torch, Camera, Vibration, and Microphone. Monitor shows Sensors, Battery, and Radios. Logbook lets you log timestamped events with processes and checkpoints. More includes User Manual, Notifications, Automation, Files, Settings, and Bug Report.",
        "Das Dashboard ist Ihr Startbildschirm mit Statuskarten und Schnellaktionen. Werkzeuge enth\u00E4lt Taschenlampe, Kamera, Vibration und Mikrofon. Monitor zeigt Sensoren, Akku und Funk. Das Logbuch erm\u00F6glicht zeitgestempelte Eintr\u00E4ge mit Prozessen und Pr\u00FCfpunkten. Mehr umfasst Benutzerhandbuch, Benachrichtigungen, Automatisierung, Dateien, Einstellungen und Fehlerbericht.",
        "El Panel es su pantalla de inicio con tarjetas de estado y acciones r\u00E1pidas. Herramientas contiene Linterna, C\u00E1mara, Vibraci\u00F3n y Micr\u00F3fono. Monitor muestra Sensores, Bater\u00EDa y Radios. El Registro permite registrar eventos con marcas de tiempo, procesos y puntos de control. M\u00E1s incluye Manual de usuario, Notificaciones, Automatizaci\u00F3n, Archivos, Ajustes e Informe de errores.",
        "Le Tableau de bord est votre \u00E9cran d\u2019accueil avec des cartes de statut et des actions rapides. Outils contient Lampe torche, Cam\u00E9ra, Vibration et Microphone. Moniteur affiche Capteurs, Batterie et Radios. Le Journal permet d\u2019enregistrer des \u00E9v\u00E9nements horodat\u00E9s avec des processus et des points de contr\u00F4le. Plus comprend Manuel utilisateur, Notifications, Automatisation, Fichiers, Param\u00E8tres et Rapport de bug.")

    // ── Per-feature sections ────────────────────────────────────────────
    val dashboard get() = DashboardManual(l)
    val torch get() = TorchManual(l)
    val camera get() = CameraManual(l)
    val vibration get() = VibrationManual(l)
    val mic get() = MicManual(l)
    val sensors get() = SensorsManual(l)
    val battery get() = BatteryManual(l)
    val radios get() = RadiosManual(l)
    val logbook get() = LogbookManual(l)
    val notifications get() = NotificationsManual(l)
    val automation get() = AutomationManual(l)
    val files get() = FilesManual(l)
    val widgets get() = WidgetsManual(l)
    val settings get() = SettingsManual(l)
    val bugReport get() = BugReportManual(l)
    val accessibility get() = AccessibilityManual(l)

    // ── Dashboard ───────────────────────────────────────────────────────
    class DashboardManual(private val l: Language) {
        val description get() = m(l,
            "Your home screen showing battery level, WiFi status, quick action shortcuts to all tools, and your most recent logbook entry.",
            "Ihr Startbildschirm mit Akkustand, WLAN-Status, Schnellzugriff auf alle Werkzeuge und Ihrem letzten Logbuch-Eintrag.",
            "Su pantalla de inicio que muestra el nivel de bater\u00EDa, el estado WiFi, accesos directos a todas las herramientas y su entrada m\u00E1s reciente del registro.",
            "Votre \u00E9cran d\u2019accueil affichant le niveau de batterie, l\u2019\u00E9tat WiFi, des raccourcis vers tous les outils et votre derni\u00E8re entr\u00E9e de journal.")
        val howToUse get() = m(l,
            "Tap any quick action chip to jump directly to that feature. Status cards refresh automatically when the screen opens.",
            "Tippen Sie auf eine Schnellaktion, um direkt zur Funktion zu gelangen. Statuskarten aktualisieren sich automatisch beim \u00D6ffnen.",
            "Toque cualquier acci\u00F3n r\u00E1pida para ir directamente a esa funci\u00F3n. Las tarjetas de estado se actualizan autom\u00E1ticamente al abrir la pantalla.",
            "Appuyez sur une action rapide pour acc\u00E9der directement \u00E0 cette fonctionnalit\u00E9. Les cartes de statut se rafra\u00EEchissent automatiquement \u00E0 l\u2019ouverture.")
    }

    // ── Torch ───────────────────────────────────────────────────────────
    class TorchManual(private val l: Language) {
        val description get() = m(l,
            "Toggle your device\u2019s flashlight, run strobe mode with adjustable frequency, and control display brightness.",
            "Schalten Sie die Taschenlampe ein/aus, nutzen Sie den Stroboskop-Modus mit einstellbarer Frequenz und steuern Sie die Bildschirmhelligkeit.",
            "Encienda y apague la linterna, use el modo estrobo con frecuencia ajustable y controle el brillo de la pantalla.",
            "Allumez/\u00E9teignez la lampe torche, utilisez le mode stroboscope avec fr\u00E9quence r\u00E9glable et contr\u00F4lez la luminosit\u00E9 de l\u2019\u00E9cran.")
        val howToUse get() = m(l,
            "Tap the large toggle button to turn the flashlight on or off. Enable Strobe and adjust the frequency slider (1\u201320 Hz). Use the brightness slider to control display brightness.",
            "Tippen Sie auf den gro\u00DFen Schalter, um die Taschenlampe ein- oder auszuschalten. Aktivieren Sie Stroboskop und stellen Sie den Frequenzregler ein (1\u201320 Hz). Verwenden Sie den Helligkeitsregler f\u00FCr die Bildschirmhelligkeit.",
            "Toque el bot\u00F3n grande para encender o apagar la linterna. Active el Estrobo y ajuste el control de frecuencia (1\u201320 Hz). Use el control de brillo para la pantalla.",
            "Appuyez sur le grand bouton pour allumer ou \u00E9teindre la lampe. Activez le Stroboscope et ajustez le curseur de fr\u00E9quence (1\u201320 Hz). Utilisez le curseur de luminosit\u00E9 pour l\u2019\u00E9cran.")
        val prerequisites get() = m(l,
            "Flash hardware on device. WRITE_SETTINGS permission for brightness control.",
            "Flash-Hardware am Ger\u00E4t. WRITE_SETTINGS-Berechtigung f\u00FCr Helligkeitssteuerung.",
            "Hardware de flash en el dispositivo. Permiso WRITE_SETTINGS para control de brillo.",
            "Mat\u00E9riel flash sur l\u2019appareil. Permission WRITE_SETTINGS pour le contr\u00F4le de luminosit\u00E9.")
        val limitations get() = m(l,
            "Strobe frequency depends on hardware capability. Torch is unavailable while the camera is in use by another app.",
            "Die Stroboskop-Frequenz h\u00E4ngt von der Hardware ab. Die Taschenlampe ist nicht verf\u00FCgbar, wenn die Kamera von einer anderen App verwendet wird.",
            "La frecuencia del estrobo depende del hardware. La linterna no est\u00E1 disponible mientras la c\u00E1mara est\u00E9 en uso por otra app.",
            "La fr\u00E9quence du stroboscope d\u00E9pend du mat\u00E9riel. La lampe torche est indisponible lorsque la cam\u00E9ra est utilis\u00E9e par une autre application.")
    }

    // ── Camera ──────────────────────────────────────────────────────────
    class CameraManual(private val l: Language) {
        val description get() = m(l,
            "Live camera preview with multi-lens selection, tap-to-focus, zoom, exposure compensation, and photo capture.",
            "Live-Kameravorschau mit Mehrlinsenwahl, Tippen-zum-Fokussieren, Zoom, Belichtungskorrektur und Fotoaufnahme.",
            "Vista previa de c\u00E1mara en vivo con selecci\u00F3n de lentes, toque para enfocar, zoom, compensaci\u00F3n de exposici\u00F3n y captura de fotos.",
            "Aper\u00E7u cam\u00E9ra en direct avec s\u00E9lection multi-objectifs, mise au point par toucher, zoom, compensation d\u2019exposition et capture photo.")
        val howToUse get() = m(l,
            "Select a lens from the row at the top (ultrawide, main, telephoto, front). Tap the preview to set focus. Use the zoom slider (1x to max) and exposure slider (\u22128 to +8 EV). Tap the capture button to save a photo to your gallery.",
            "W\u00E4hlen Sie ein Objektiv aus der Reihe oben (Ultraweit, Haupt, Tele, Front). Tippen Sie auf die Vorschau zum Fokussieren. Nutzen Sie den Zoom-Regler (1x bis max) und Belichtungsregler (\u22128 bis +8 EV). Tippen Sie auf den Ausl\u00F6ser, um ein Foto zu speichern.",
            "Seleccione una lente de la fila superior (ultra angular, principal, teleobjetivo, frontal). Toque la vista previa para enfocar. Use el control de zoom (1x a m\u00E1x) y de exposici\u00F3n (\u22128 a +8 EV). Toque el bot\u00F3n de captura para guardar una foto.",
            "S\u00E9lectionnez un objectif en haut (ultra-grand angle, principal, t\u00E9l\u00E9photo, frontal). Touchez l\u2019aper\u00E7u pour la mise au point. Utilisez le curseur de zoom (1x \u00E0 max) et d\u2019exposition (\u22128 \u00E0 +8 EV). Appuyez sur le bouton de capture pour enregistrer une photo.")
        val prerequisites get() = m(l,
            "CAMERA permission.",
            "CAMERA-Berechtigung.",
            "Permiso de C\u00C1MARA.",
            "Permission CAM\u00C9RA.")
        val limitations get() = m(l,
            "Available lenses depend on your device hardware. Video recording is available via the Video Toggle home screen widget.",
            "Verf\u00FCgbare Objektive h\u00E4ngen von Ihrer Ger\u00E4te-Hardware ab. Videoaufnahme ist \u00FCber das Video-Widget auf dem Startbildschirm verf\u00FCgbar.",
            "Las lentes disponibles dependen del hardware del dispositivo. La grabaci\u00F3n de video est\u00E1 disponible mediante el widget de Video en la pantalla de inicio.",
            "Les objectifs disponibles d\u00E9pendent du mat\u00E9riel. L\u2019enregistrement vid\u00E9o est disponible via le widget Vid\u00E9o sur l\u2019\u00E9cran d\u2019accueil.")
    }

    // ── Vibration ───────────────────────────────────────────────────────
    class VibrationManual(private val l: Language) {
        val description get() = m(l,
            "Test predefined haptic effects, build custom waveform patterns with a visual editor, draw patterns with your finger, and save or load patterns.",
            "Testen Sie vordefinierte haptische Effekte, erstellen Sie eigene Wellenmuster mit dem visuellen Editor, zeichnen Sie Muster mit dem Finger und speichern oder laden Sie Muster.",
            "Pruebe efectos h\u00E1pticos predefinidos, cree patrones de onda personalizados con el editor visual, dibuje patrones con el dedo y guarde o cargue patrones.",
            "Testez des effets haptiques pr\u00E9d\u00E9finis, cr\u00E9ez des motifs de vibration avec l\u2019\u00E9diteur visuel, dessinez des motifs avec votre doigt et enregistrez ou chargez des motifs.")
        val howToUse get() = m(l,
            "Tap a predefined effect to feel it instantly. In the Waveform Builder, add steps with amplitude and duration, then tap Play. Use the Draw canvas to trace a pattern with your finger. Save patterns by name and load them later.",
            "Tippen Sie auf einen vordefinierten Effekt, um ihn sofort zu sp\u00FCren. Im Wellenform-Builder f\u00FCgen Sie Schritte mit Amplitude und Dauer hinzu und tippen auf Abspielen. Nutzen Sie die Zeichenfl\u00E4che, um ein Muster mit dem Finger zu zeichnen. Speichern Sie Muster mit Namen und laden Sie sie sp\u00E4ter.",
            "Toque un efecto predefinido para sentirlo al instante. En el Constructor de ondas, a\u00F1ada pasos con amplitud y duraci\u00F3n, luego toque Reproducir. Use el lienzo para trazar un patr\u00F3n con el dedo. Guarde patrones por nombre y c\u00E1rguelos despu\u00E9s.",
            "Appuyez sur un effet pr\u00E9d\u00E9fini pour le ressentir instantan\u00E9ment. Dans le Constructeur, ajoutez des \u00E9tapes avec amplitude et dur\u00E9e, puis appuyez sur Lire. Utilisez le canevas pour tracer un motif avec votre doigt. Enregistrez les motifs par nom et chargez-les plus tard.")
        val prerequisites get() = m(l,
            "VIBRATE permission.",
            "VIBRATE-Berechtigung.",
            "Permiso de VIBRACI\u00D3N.",
            "Permission VIBRATION.")
        val limitations get() = m(l,
            "Amplitude control is not available on all devices; patterns play at full strength on unsupported hardware. Maximum 20 saved patterns.",
            "Amplitudensteuerung ist nicht auf allen Ger\u00E4ten verf\u00FCgbar; Muster werden auf nicht unterst\u00FCtzter Hardware mit voller St\u00E4rke abgespielt. Maximal 20 gespeicherte Muster.",
            "El control de amplitud no est\u00E1 disponible en todos los dispositivos; los patrones se reproducen a m\u00E1xima intensidad en hardware no compatible. M\u00E1ximo 20 patrones guardados.",
            "Le contr\u00F4le d\u2019amplitude n\u2019est pas disponible sur tous les appareils ; les motifs sont jou\u00E9s \u00E0 pleine puissance sur le mat\u00E9riel non compatible. Maximum 20 motifs enregistr\u00E9s.")
    }

    // ── Microphone ──────────────────────────────────────────────────────
    class MicManual(private val l: Language) {
        val description get() = m(l,
            "Real-time dB meter with FFT spectrum analyzer, waveform history, audio recording to WAV, and playback.",
            "Echtzeit-dB-Messger\u00E4t mit FFT-Spektrumanalysator, Wellenformverlauf, Audioaufnahme als WAV und Wiedergabe.",
            "Medidor de dB en tiempo real con analizador de espectro FFT, historial de forma de onda, grabaci\u00F3n de audio en WAV y reproducci\u00F3n.",
            "Sonomètre en temps r\u00E9el avec analyseur de spectre FFT, historique de forme d\u2019onde, enregistrement audio en WAV et lecture.")
        val howToUse get() = m(l,
            "Tap Start Monitoring to see live dB levels and the frequency spectrum. Tap the record button to capture audio as a WAV file. Saved recordings appear in the list below for playback or deletion.",
            "Tippen Sie auf \u00DCberwachung starten, um Live-dB-Pegel und das Frequenzspektrum zu sehen. Tippen Sie auf Aufnahme, um Audio als WAV aufzunehmen. Gespeicherte Aufnahmen erscheinen in der Liste zur Wiedergabe oder zum L\u00F6schen.",
            "Toque Iniciar monitoreo para ver los niveles de dB en vivo y el espectro de frecuencias. Toque el bot\u00F3n de grabaci\u00F3n para capturar audio como archivo WAV. Las grabaciones guardadas aparecen en la lista para reproducci\u00F3n o eliminaci\u00F3n.",
            "Appuyez sur D\u00E9marrer la surveillance pour voir les niveaux de dB en direct et le spectre fr\u00E9quentiel. Appuyez sur le bouton d\u2019enregistrement pour capturer l\u2019audio en WAV. Les enregistrements sauvegard\u00E9s apparaissent dans la liste pour lecture ou suppression.")
        val prerequisites get() = m(l,
            "RECORD_AUDIO permission.",
            "RECORD_AUDIO-Berechtigung.",
            "Permiso de GRABACI\u00D3N DE AUDIO.",
            "Permission ENREGISTREMENT AUDIO.")
        val limitations get() = m(l,
            "Measurement accuracy depends on device microphone hardware. FFT analysis covers frequencies up to 22.05 kHz.",
            "Die Messgenauigkeit h\u00E4ngt von der Mikrofon-Hardware des Ger\u00E4ts ab. Die FFT-Analyse deckt Frequenzen bis 22,05 kHz ab.",
            "La precisi\u00F3n de la medici\u00F3n depende del hardware del micr\u00F3fono. El an\u00E1lisis FFT cubre frecuencias hasta 22,05 kHz.",
            "La pr\u00E9cision de la mesure d\u00E9pend du mat\u00E9riel du microphone. L\u2019analyse FFT couvre les fr\u00E9quences jusqu\u2019\u00E0 22,05 kHz.")
    }

    // ── Sensors ─────────────────────────────────────────────────────────
    class SensorsManual(private val l: Language) {
        val description get() = m(l,
            "Real-time readings from all available hardware sensors with live charts and clipboard export.",
            "Echtzeit-Messwerte aller verf\u00FCgbaren Hardware-Sensoren mit Live-Diagrammen und Export in die Zwischenablage.",
            "Lecturas en tiempo real de todos los sensores de hardware disponibles con gr\u00E1ficos en vivo y exportaci\u00F3n al portapapeles.",
            "Lectures en temps r\u00E9el de tous les capteurs mat\u00E9riels disponibles avec graphiques en direct et export vers le presse-papiers.")
        val howToUse get() = m(l,
            "Scroll through available sensors. Tap any sensor card to expand it and see a live multi-axis chart. Tap the copy button at the top to copy all current readings to the clipboard.",
            "Scrollen Sie durch die verf\u00FCgbaren Sensoren. Tippen Sie auf eine Sensorkarte, um sie zu erweitern und ein Live-Mehrachsen-Diagramm zu sehen. Tippen Sie oben auf Kopieren, um alle aktuellen Messwerte in die Zwischenablage zu kopieren.",
            "Despl\u00E1cese por los sensores disponibles. Toque cualquier tarjeta de sensor para expandirla y ver un gr\u00E1fico multi-eje en vivo. Toque el bot\u00F3n de copiar arriba para copiar todas las lecturas al portapapeles.",
            "Parcourez les capteurs disponibles. Appuyez sur une carte de capteur pour la d\u00E9velopper et voir un graphique multi-axes en direct. Appuyez sur le bouton copier en haut pour copier toutes les lectures dans le presse-papiers.")
    }

    // ── Battery ─────────────────────────────────────────────────────────
    class BatteryManual(private val l: Language) {
        val description get() = m(l,
            "Detailed battery information including level, status, health, temperature, voltage, technology, current draw, and estimated charge time.",
            "Detaillierte Akkuinformationen einschlie\u00DFlich Ladezustand, Status, Zustand, Temperatur, Spannung, Technologie, Stromverbrauch und gesch\u00E4tzte Ladezeit.",
            "Informaci\u00F3n detallada de la bater\u00EDa incluyendo nivel, estado, salud, temperatura, voltaje, tecnolog\u00EDa, consumo de corriente y tiempo de carga estimado.",
            "Informations d\u00E9taill\u00E9es sur la batterie : niveau, statut, sant\u00E9, temp\u00E9rature, tension, technologie, consommation de courant et temps de charge estim\u00E9.")
        val howToUse get() = m(l,
            "Open the Battery screen to see all metrics update in real time. The current draw chart shows a rolling history. Scroll down for detailed health and technology information.",
            "\u00D6ffnen Sie den Akku-Bildschirm, um alle Metriken in Echtzeit zu sehen. Das Stromverbrauchsdiagramm zeigt einen rollierenden Verlauf. Scrollen Sie nach unten f\u00FCr detaillierte Zustands- und Technologieinformationen.",
            "Abra la pantalla de Bater\u00EDa para ver todas las m\u00E9tricas actualizarse en tiempo real. El gr\u00E1fico de consumo muestra un historial continuo. Despl\u00E1cese hacia abajo para informaci\u00F3n detallada de salud y tecnolog\u00EDa.",
            "Ouvrez l\u2019\u00E9cran Batterie pour voir toutes les m\u00E9triques en temps r\u00E9el. Le graphique de consommation affiche un historique glissant. Faites d\u00E9filer pour les informations de sant\u00E9 et de technologie.")
    }

    // ── Radios ──────────────────────────────────────────────────────────
    class RadiosManual(private val l: Language) {
        val description get() = m(l,
            "Monitor WiFi, Bluetooth, NFC, GPS, cellular signal, and network speed from a single screen.",
            "\u00DCberwachen Sie WLAN, Bluetooth, NFC, GPS, Mobilfunksignal und Netzwerkgeschwindigkeit auf einem einzigen Bildschirm.",
            "Monitoree WiFi, Bluetooth, NFC, GPS, se\u00F1al celular y velocidad de red desde una sola pantalla.",
            "Surveillez WiFi, Bluetooth, NFC, GPS, signal cellulaire et vitesse r\u00E9seau depuis un seul \u00E9cran.")
        val howToUse get() = m(l,
            "Each radio section expands to show details. For NFC, hold a tag near your device to read it; use the Write tab to write data to writable tags. Save scanned tags and emulate them via HCE. GPS shows a live OpenStreetMap view with your coordinates.",
            "Jeder Funkbereich l\u00E4sst sich f\u00FCr Details erweitern. F\u00FCr NFC halten Sie einen Tag an Ihr Ger\u00E4t zum Lesen; verwenden Sie den Schreiben-Tab zum Beschreiben. Speichern Sie gescannte Tags und emulieren Sie sie per HCE. GPS zeigt eine Live-OpenStreetMap-Ansicht mit Ihren Koordinaten.",
            "Cada secci\u00F3n de radio se expande para mostrar detalles. Para NFC, acerque una etiqueta al dispositivo para leerla; use la pesta\u00F1a Escribir para escribir datos. Guarde etiquetas escaneadas y em\u00FAlelas via HCE. GPS muestra un mapa OpenStreetMap en vivo con sus coordenadas.",
            "Chaque section radio se d\u00E9veloppe pour afficher les d\u00E9tails. Pour NFC, approchez un tag pour le lire ; utilisez l\u2019onglet \u00C9crire pour \u00E9crire des donn\u00E9es. Sauvegardez les tags scann\u00E9s et \u00E9mulez-les via HCE. GPS affiche une carte OpenStreetMap en direct avec vos coordonn\u00E9es.")
        val prerequisites get() = m(l,
            "ACCESS_WIFI_STATE, BLUETOOTH_CONNECT, NFC, ACCESS_FINE_LOCATION, and INTERNET (for map tiles).",
            "ACCESS_WIFI_STATE, BLUETOOTH_CONNECT, NFC, ACCESS_FINE_LOCATION und INTERNET (f\u00FCr Kartenkacheln).",
            "ACCESS_WIFI_STATE, BLUETOOTH_CONNECT, NFC, ACCESS_FINE_LOCATION e INTERNET (para mosaicos del mapa).",
            "ACCESS_WIFI_STATE, BLUETOOTH_CONNECT, NFC, ACCESS_FINE_LOCATION et INTERNET (pour les tuiles de carte).")
        val limitations get() = m(l,
            "WiFi SSID may show as unknown on some devices. NFC requires compatible hardware. GPS accuracy depends on environment and device.",
            "WLAN-SSID kann auf manchen Ger\u00E4ten als unbekannt angezeigt werden. NFC erfordert kompatible Hardware. GPS-Genauigkeit h\u00E4ngt von Umgebung und Ger\u00E4t ab.",
            "El SSID WiFi puede mostrarse como desconocido en algunos dispositivos. NFC requiere hardware compatible. La precisi\u00F3n del GPS depende del entorno y el dispositivo.",
            "Le SSID WiFi peut s\u2019afficher comme inconnu sur certains appareils. NFC n\u00E9cessite un mat\u00E9riel compatible. La pr\u00E9cision GPS d\u00E9pend de l\u2019environnement et de l\u2019appareil.")
    }

    // ── Logbook ─────────────────────────────────────────────────────────
    class LogbookManual(private val l: Language) {
        val description get() = m(l,
            "Log timestamped events with notes, organize processes with checkpoints and due dates, set reminders, and import or export data as JSON.",
            "Protokollieren Sie zeitgestempelte Ereignisse mit Notizen, organisieren Sie Prozesse mit Pr\u00FCfpunkten und F\u00E4lligkeitsdaten, richten Sie Erinnerungen ein und importieren oder exportieren Sie Daten als JSON.",
            "Registre eventos con marca de tiempo y notas, organice procesos con puntos de control y fechas l\u00EDmite, configure recordatorios e importe o exporte datos como JSON.",
            "Enregistrez des \u00E9v\u00E9nements horodat\u00E9s avec des notes, organisez des processus avec des points de contr\u00F4le et des \u00E9ch\u00E9ances, d\u00E9finissez des rappels et importez ou exportez les donn\u00E9es en JSON.")
        val howToUse get() = m(l,
            "Type a note and tap Log to create an entry. Switch to the Processes tab to create multi-step workflows with checkpoints. Set due dates and enable reminders for upcoming deadlines. Use the menu to import or export your data as JSON.",
            "Geben Sie eine Notiz ein und tippen Sie auf Loggen. Wechseln Sie zum Prozesse-Tab, um mehrstufige Abl\u00E4ufe mit Pr\u00FCfpunkten zu erstellen. Legen Sie F\u00E4lligkeitsdaten fest und aktivieren Sie Erinnerungen. Verwenden Sie das Men\u00FC zum Importieren oder Exportieren als JSON.",
            "Escriba una nota y toque Registrar para crear una entrada. Cambie a la pesta\u00F1a Procesos para crear flujos de trabajo con puntos de control. Establezca fechas l\u00EDmite y active recordatorios. Use el men\u00FA para importar o exportar datos como JSON.",
            "Saisissez une note et appuyez sur Enregistrer. Passez \u00E0 l\u2019onglet Processus pour cr\u00E9er des flux de travail avec des points de contr\u00F4le. D\u00E9finissez des \u00E9ch\u00E9ances et activez les rappels. Utilisez le menu pour importer ou exporter vos donn\u00E9es en JSON.")
        val prerequisites get() = m(l,
            "POST_NOTIFICATIONS permission for reminders.",
            "POST_NOTIFICATIONS-Berechtigung f\u00FCr Erinnerungen.",
            "Permiso POST_NOTIFICATIONS para recordatorios.",
            "Permission POST_NOTIFICATIONS pour les rappels.")
        val limitations get() = m(l,
            "No cloud sync \u2014 use JSON export/import for cross-device transfer. Reminders may be delayed by system battery optimization.",
            "Keine Cloud-Synchronisierung \u2014 verwenden Sie JSON-Export/Import f\u00FCr die ger\u00E4te\u00FCbergreifende \u00DCbertragung. Erinnerungen k\u00F6nnen durch Akku-Optimierung verz\u00F6gert werden.",
            "Sin sincronizaci\u00F3n en la nube \u2014 use la exportaci\u00F3n/importaci\u00F3n JSON para transferir entre dispositivos. Los recordatorios pueden retrasarse por la optimizaci\u00F3n de bater\u00EDa del sistema.",
            "Pas de synchronisation cloud \u2014 utilisez l\u2019export/import JSON pour le transfert entre appareils. Les rappels peuvent \u00EAtre retard\u00E9s par l\u2019optimisation de la batterie du syst\u00E8me.")
    }

    // ── Notifications ───────────────────────────────────────────────────
    class NotificationsManual(private val l: Language) {
        val description get() = m(l,
            "Send demo notifications, build custom notifications with actions and styles, schedule alerts, control the lock screen, and trigger a phone ring.",
            "Senden Sie Demo-Benachrichtigungen, erstellen Sie benutzerdefinierte Benachrichtigungen mit Aktionen und Stilen, planen Sie Alarme, steuern Sie den Sperrbildschirm und l\u00F6sen Sie ein Telefonklingeln aus.",
            "Env\u00EDe notificaciones de demostraci\u00F3n, cree notificaciones personalizadas con acciones y estilos, programe alertas, controle la pantalla de bloqueo y active el timbre del tel\u00E9fono.",
            "Envoyez des notifications de d\u00E9monstration, cr\u00E9ez des notifications personnalis\u00E9es avec actions et styles, planifiez des alertes, contr\u00F4lez l\u2019\u00E9cran de verrouillage et d\u00E9clenchez une sonnerie.")
        val howToUse get() = m(l,
            "Try the demo buttons to see different notification styles. Use the Custom Builder to set title, body, priority, visibility, action buttons, and progress bars. Schedule notifications or phone rings for a specific time. Activate Device Admin to enable screen locking.",
            "Probieren Sie die Demo-Schaltfl\u00E4chen aus, um verschiedene Benachrichtigungsstile zu sehen. Verwenden Sie den Builder f\u00FCr Titel, Inhalt, Priorit\u00E4t, Sichtbarkeit, Aktionsschaltfl\u00E4chen und Fortschrittsbalken. Planen Sie Benachrichtigungen oder Telefonklingeln. Aktivieren Sie den Ger\u00E4teadministrator f\u00FCr die Bildschirmsperre.",
            "Pruebe los botones de demostraci\u00F3n para ver diferentes estilos. Use el Constructor personalizado para t\u00EDtulo, cuerpo, prioridad, visibilidad, botones de acci\u00F3n y barras de progreso. Programe notificaciones o timbres. Active el Administrador del dispositivo para bloqueo de pantalla.",
            "Essayez les boutons de d\u00E9mo pour voir les diff\u00E9rents styles. Utilisez le Constructeur pour titre, corps, priorit\u00E9, visibilit\u00E9, boutons d\u2019action et barres de progression. Planifiez des notifications ou sonneries. Activez l\u2019Administrateur de l\u2019appareil pour le verrouillage d\u2019\u00E9cran.")
        val prerequisites get() = m(l,
            "POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW (for overlay), and Device Admin activation for screen lock.",
            "POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW (f\u00FCr Overlay) und Ger\u00E4teadministrator-Aktivierung f\u00FCr Bildschirmsperre.",
            "POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW (para superposici\u00F3n) y activaci\u00F3n de Administrador del dispositivo para bloqueo de pantalla.",
            "POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW (pour la superposition) et activation Administrateur de l\u2019appareil pour le verrouillage d\u2019\u00E9cran.")
        val limitations get() = m(l,
            "Lock screen overlay is restricted on Android 12+. Phone ring and vibration may be silenced by Do Not Disturb \u2014 enable Bypass DND in Settings.",
            "Sperrbildschirm-Overlay ist auf Android 12+ eingeschr\u00E4nkt. Telefonklingeln und Vibration k\u00F6nnen durch Nicht st\u00F6ren stummgeschaltet werden \u2014 aktivieren Sie DND umgehen in den Einstellungen.",
            "La superposici\u00F3n de pantalla de bloqueo est\u00E1 restringida en Android 12+. El timbre y la vibraci\u00F3n pueden ser silenciados por No molestar \u2014 active Omitir DND en Ajustes.",
            "La superposition d\u2019\u00E9cran de verrouillage est restreinte sur Android 12+. La sonnerie et la vibration peuvent \u00EAtre r\u00E9duites au silence par Ne pas d\u00E9ranger \u2014 activez Contourner NPD dans les Param\u00E8tres.")
    }

    // ── Automation ──────────────────────────────────────────────────────
    class AutomationManual(private val l: Language) {
        val description get() = m(l,
            "Create IF/THEN rules that trigger actions automatically when hardware metrics meet specified conditions.",
            "Erstellen Sie WENN/DANN-Regeln, die automatisch Aktionen ausl\u00F6sen, wenn Hardware-Metriken bestimmte Bedingungen erf\u00FCllen.",
            "Cree reglas SI/ENTONCES que activan acciones autom\u00E1ticamente cuando las m\u00E9tricas de hardware cumplen condiciones especificadas.",
            "Cr\u00E9ez des r\u00E8gles SI/ALORS qui d\u00E9clenchent des actions automatiquement lorsque les m\u00E9triques mat\u00E9rielles remplissent des conditions sp\u00E9cifi\u00E9es.")
        val howToUse get() = m(l,
            "Tap Add Link to create a rule. Select a metric, an operator, a threshold value, and an action. Set a cooldown to prevent rapid re-triggering. Tap Start Monitoring to activate all enabled rules.",
            "Tippen Sie auf Link hinzuf\u00FCgen, um eine Regel zu erstellen. W\u00E4hlen Sie eine Metrik, einen Operator, einen Schwellenwert und eine Aktion. Legen Sie eine Abklingzeit fest. Tippen Sie auf \u00DCberwachung starten, um alle aktivierten Regeln zu starten.",
            "Toque A\u00F1adir enlace para crear una regla. Seleccione una m\u00E9trica, un operador, un valor umbral y una acci\u00F3n. Establezca un tiempo de enfriamiento. Toque Iniciar monitoreo para activar todas las reglas habilitadas.",
            "Appuyez sur Ajouter un lien pour cr\u00E9er une r\u00E8gle. S\u00E9lectionnez une m\u00E9trique, un op\u00E9rateur, un seuil et une action. D\u00E9finissez un temps de recharge. Appuyez sur D\u00E9marrer la surveillance pour activer toutes les r\u00E8gles activ\u00E9es.")
        val prerequisites get() = m(l,
            "Varies by metric. For example, GPS metrics require location permission.",
            "Variiert je nach Metrik. GPS-Metriken erfordern beispielsweise die Standortberechtigung.",
            "Var\u00EDa seg\u00FAn la m\u00E9trica. Por ejemplo, las m\u00E9tricas GPS requieren permiso de ubicaci\u00F3n.",
            "Varie selon la m\u00E9trique. Par exemple, les m\u00E9triques GPS n\u00E9cessitent la permission de localisation.")
        val limitations get() = m(l,
            "Rules use simple conditions only (no AND/OR logic). Monitoring polls every 500 ms. Limited to built-in actions.",
            "Regeln verwenden nur einfache Bedingungen (keine UND/ODER-Logik). \u00DCberwachung alle 500 ms. Begrenzt auf eingebaute Aktionen.",
            "Las reglas usan solo condiciones simples (sin l\u00F3gica Y/O). El monitoreo consulta cada 500 ms. Limitado a acciones integradas.",
            "Les r\u00E8gles utilisent uniquement des conditions simples (pas de logique ET/OU). Surveillance toutes les 500 ms. Limit\u00E9 aux actions int\u00E9gr\u00E9es.")
    }

    // ── Files ───────────────────────────────────────────────────────────
    class FilesManual(private val l: Language) {
        val description get() = m(l,
            "Browse files on your device, view metadata, read and edit EXIF data for images, and view media metadata for audio and video.",
            "Durchsuchen Sie Dateien auf Ihrem Ger\u00E4t, sehen Sie Metadaten ein, lesen und bearbeiten Sie EXIF-Daten f\u00FCr Bilder und sehen Sie Medien-Metadaten f\u00FCr Audio und Video.",
            "Explore archivos en su dispositivo, vea metadatos, lea y edite datos EXIF de im\u00E1genes y vea metadatos multimedia de audio y video.",
            "Parcourez les fichiers sur votre appareil, consultez les m\u00E9tadonn\u00E9es, lisez et modifiez les donn\u00E9es EXIF des images et consultez les m\u00E9tadonn\u00E9es m\u00E9dia pour l\u2019audio et la vid\u00E9o.")
        val howToUse get() = m(l,
            "Tap Select a File to open the system file picker. Once selected, metadata is displayed. For images, expand the EXIF section to view or edit tags. Tap Save Changes to write EXIF edits.",
            "Tippen Sie auf Datei ausw\u00E4hlen, um den Datei-Picker zu \u00F6ffnen. Nach der Auswahl werden Metadaten angezeigt. F\u00FCr Bilder erweitern Sie den EXIF-Bereich zum Anzeigen oder Bearbeiten. Tippen Sie auf \u00C4nderungen speichern.",
            "Toque Seleccionar archivo para abrir el selector de archivos. Una vez seleccionado, se muestran los metadatos. Para im\u00E1genes, expanda la secci\u00F3n EXIF para ver o editar etiquetas. Toque Guardar cambios.",
            "Appuyez sur S\u00E9lectionner un fichier pour ouvrir le s\u00E9lecteur. Une fois s\u00E9lectionn\u00E9, les m\u00E9tadonn\u00E9es s\u2019affichent. Pour les images, d\u00E9veloppez la section EXIF pour consulter ou modifier les tags. Appuyez sur Enregistrer les modifications.")
        val prerequisites get() = m(l,
            "File access via the system file picker.",
            "Dateizugriff \u00FCber den System-Datei-Picker.",
            "Acceso a archivos mediante el selector de archivos del sistema.",
            "Acc\u00E8s aux fichiers via le s\u00E9lecteur de fichiers du syst\u00E8me.")
        val limitations get() = m(l,
            "EXIF editing requires write access to the file. Media metadata (audio/video) is read-only.",
            "EXIF-Bearbeitung erfordert Schreibzugriff auf die Datei. Medien-Metadaten (Audio/Video) sind schreibgesch\u00FCtzt.",
            "La edici\u00F3n EXIF requiere acceso de escritura al archivo. Los metadatos multimedia (audio/video) son de solo lectura.",
            "La modification EXIF n\u00E9cessite un acc\u00E8s en \u00E9criture au fichier. Les m\u00E9tadonn\u00E9es m\u00E9dia (audio/vid\u00E9o) sont en lecture seule.")
    }

    // ── Widgets ─────────────────────────────────────────────────────────
    class WidgetsManual(private val l: Language) {
        val description get() = m(l,
            "Ten home screen widgets for quick access to Gadget features without opening the app.",
            "Zehn Startbildschirm-Widgets f\u00FCr schnellen Zugriff auf Gadget-Funktionen ohne die App zu \u00F6ffnen.",
            "Diez widgets de pantalla de inicio para acceso r\u00E1pido a las funciones de Gadget sin abrir la app.",
            "Dix widgets d\u2019\u00E9cran d\u2019accueil pour un acc\u00E8s rapide aux fonctionnalit\u00E9s de Gadget sans ouvrir l\u2019application.")
        val howToUse get() = m(l,
            "Long-press your home screen, select Widgets, find Gadget, and place a widget. Available: Gadget Metric, Quick Log, Flashlight Toggle, Strobe Toggle, Camera Snapshot, Video Toggle, Voice Record, Phone Ring, Quick Notify, and dB Meter. Configure delays in Settings.",
            "Dr\u00FCcken Sie lange auf den Startbildschirm, w\u00E4hlen Sie Widgets, finden Sie Gadget und platzieren Sie ein Widget. Verf\u00FCgbar: Gadget-Metrik, Schnell-Log, Taschenlampe, Stroboskop, Kamera-Schnappschuss, Video, Sprachaufnahme, Telefonklingeln, Schnell-Benachrichtigung und dB-Messger\u00E4t. Verz\u00F6gerungen in den Einstellungen konfigurieren.",
            "Mantenga presionada la pantalla de inicio, seleccione Widgets, busque Gadget y coloque un widget. Disponibles: M\u00E9trica Gadget, Registro r\u00E1pido, Linterna, Estrobo, Foto r\u00E1pida, Video, Grabaci\u00F3n de voz, Timbre, Notificaci\u00F3n r\u00E1pida y Medidor dB. Configure los retrasos en Ajustes.",
            "Appuyez longuement sur l\u2019\u00E9cran d\u2019accueil, s\u00E9lectionnez Widgets, trouvez Gadget et placez un widget. Disponibles : M\u00E9trique Gadget, Journal rapide, Lampe torche, Stroboscope, Photo rapide, Vid\u00E9o, Enregistrement vocal, Sonnerie, Notification rapide et M\u00E8tre dB. Configurez les d\u00E9lais dans les Param\u00E8tres.")
        val prerequisites get() = m(l,
            "Relevant permissions per widget function (e.g. CAMERA for Camera Snapshot, RECORD_AUDIO for Voice Record).",
            "Entsprechende Berechtigungen pro Widget-Funktion (z.B. CAMERA f\u00FCr Kamera-Schnappschuss, RECORD_AUDIO f\u00FCr Sprachaufnahme).",
            "Permisos relevantes por funci\u00F3n del widget (por ej. C\u00C1MARA para Foto r\u00E1pida, GRABACI\u00D3N DE AUDIO para Grabaci\u00F3n de voz).",
            "Permissions correspondantes par fonction de widget (ex. CAM\u00C9RA pour Photo rapide, ENREGISTREMENT AUDIO pour Enregistrement vocal).")
        val limitations get() = m(l,
            "Metric widgets refresh every 30 minutes. Instant actions (toggle, record) respond immediately on tap.",
            "Metrik-Widgets aktualisieren sich alle 30 Minuten. Sofortaktionen (Umschalten, Aufnahme) reagieren sofort auf Tippen.",
            "Los widgets de m\u00E9tricas se actualizan cada 30 minutos. Las acciones instant\u00E1neas (alternar, grabar) responden inmediatamente al tocar.",
            "Les widgets de m\u00E9triques se rafra\u00EEchissent toutes les 30 minutes. Les actions instantan\u00E9es (basculer, enregistrer) r\u00E9pondent imm\u00E9diatement au toucher.")
    }

    // ── Settings ────────────────────────────────────────────────────────
    class SettingsManual(private val l: Language) {
        val description get() = m(l,
            "Configure language, widget behavior, Do Not Disturb bypass, metric logging, and accessibility options.",
            "Konfigurieren Sie Sprache, Widget-Verhalten, Nicht-st\u00F6ren-Umgehung, Metrik-Protokollierung und Barrierefreiheitsoptionen.",
            "Configure idioma, comportamiento de widgets, omisi\u00F3n de No molestar, registro de m\u00E9tricas y opciones de accesibilidad.",
            "Configurez la langue, le comportement des widgets, le contournement Ne pas d\u00E9ranger, la journalisation des m\u00E9triques et les options d\u2019accessibilit\u00E9.")
        val howToUse get() = m(l,
            "Select your preferred language for instant UI reload. Adjust phone ring duration and notification delay for widgets. Enable Bypass DND to override silent mode. Choose which metrics to capture with logbook entries. Configure accessibility options like high contrast, large text, and reduced motion.",
            "W\u00E4hlen Sie Ihre bevorzugte Sprache f\u00FCr sofortiges UI-Neuladen. Passen Sie Klingeldauer und Benachrichtigungsverz\u00F6gerung f\u00FCr Widgets an. Aktivieren Sie DND umgehen, um den Lautlos-Modus zu \u00FCberschreiben. W\u00E4hlen Sie, welche Metriken bei Logbuch-Eintr\u00E4gen erfasst werden. Konfigurieren Sie Barrierefreiheitsoptionen.",
            "Seleccione su idioma preferido para recarga instant\u00E1nea de la interfaz. Ajuste la duraci\u00F3n del timbre y el retraso de notificaciones para widgets. Active Omitir DND para anular el modo silencioso. Elija qu\u00E9 m\u00E9tricas capturar con las entradas del registro. Configure opciones de accesibilidad.",
            "S\u00E9lectionnez votre langue pr\u00E9f\u00E9r\u00E9e pour un rechargement instantan\u00E9 de l\u2019interface. Ajustez la dur\u00E9e de sonnerie et le d\u00E9lai de notification pour les widgets. Activez Contourner NPD pour outrepasser le mode silencieux. Choisissez quelles m\u00E9triques capturer avec les entr\u00E9es du journal. Configurez les options d\u2019accessibilit\u00E9.")
    }

    // ── Bug Report ──────────────────────────────────────────────────────
    class BugReportManual(private val l: Language) {
        val description get() = m(l,
            "View permission statuses, system modes, and device information. Generate a structured bug report to share with developers.",
            "Sehen Sie Berechtigungsstatus, Systemmodi und Ger\u00E4teinformationen ein. Erstellen Sie einen strukturierten Fehlerbericht zum Teilen mit Entwicklern.",
            "Vea el estado de los permisos, modos del sistema e informaci\u00F3n del dispositivo. Genere un informe de errores estructurado para compartir con los desarrolladores.",
            "Consultez les statuts des permissions, les modes syst\u00E8me et les informations de l\u2019appareil. G\u00E9n\u00E9rez un rapport de bug structur\u00E9 \u00E0 partager avec les d\u00E9veloppeurs.")
        val howToUse get() = m(l,
            "Scroll through permissions, system modes, and device info to review your current state. Describe the issue in the text field. Tap Create Bug Report to generate a markdown-formatted report, then copy it to clipboard or open GitHub Issues directly.",
            "Scrollen Sie durch Berechtigungen, Systemmodi und Ger\u00E4teinformationen. Beschreiben Sie das Problem im Textfeld. Tippen Sie auf Fehlerbericht erstellen, um einen Markdown-Bericht zu generieren, dann kopieren Sie ihn oder \u00F6ffnen Sie direkt GitHub Issues.",
            "Despl\u00E1cese por los permisos, modos del sistema e informaci\u00F3n del dispositivo. Describa el problema en el campo de texto. Toque Crear informe de errores para generar un informe en formato markdown, luego c\u00F3pielo o abra GitHub Issues directamente.",
            "Parcourez les permissions, les modes syst\u00E8me et les informations de l\u2019appareil. D\u00E9crivez le probl\u00E8me dans le champ de texte. Appuyez sur Cr\u00E9er un rapport de bug pour g\u00E9n\u00E9rer un rapport en markdown, puis copiez-le ou ouvrez directement GitHub Issues.")
    }

    // ── Accessibility ───────────────────────────────────────────────────
    class AccessibilityManual(private val l: Language) {
        val description get() = m(l,
            "Built-in accessibility features to make Gadget usable for everyone.",
            "Integrierte Barrierefreiheitsfunktionen, die Gadget f\u00FCr alle nutzbar machen.",
            "Funciones de accesibilidad integradas para hacer Gadget utilizable para todos.",
            "Fonctionnalit\u00E9s d\u2019accessibilit\u00E9 int\u00E9gr\u00E9es pour rendre Gadget utilisable par tous.")
        val howToUse get() = m(l,
            "Open Settings and scroll to the Accessibility section. Enable High Contrast for increased color contrast, Large Text for bigger fonts, or Reduced Motion to disable animations. Gadget fully supports TalkBack with screen announcements and semantic labels on all interactive elements.",
            "\u00D6ffnen Sie die Einstellungen und scrollen Sie zum Abschnitt Barrierefreiheit. Aktivieren Sie Hochkontrast f\u00FCr erh\u00F6hten Farbkontrast, Gro\u00DFer Text f\u00FCr gr\u00F6\u00DFere Schrift oder Bewegung reduzieren zum Deaktivieren von Animationen. Gadget unterst\u00FCtzt TalkBack vollst\u00E4ndig mit Bildschirmansagen und semantischen Beschriftungen.",
            "Abra Ajustes y despl\u00E1cese hasta la secci\u00F3n Accesibilidad. Active Alto contraste para mayor contraste de color, Texto grande para fuentes m\u00E1s grandes o Reducir movimiento para desactivar animaciones. Gadget es totalmente compatible con TalkBack con anuncios de pantalla y etiquetas sem\u00E1nticas.",
            "Ouvrez les Param\u00E8tres et faites d\u00E9filer jusqu\u2019\u00E0 la section Accessibilit\u00E9. Activez Contraste \u00E9lev\u00E9 pour un meilleur contraste, Grand texte pour des polices plus grandes ou R\u00E9duire les animations pour d\u00E9sactiver les animations. Gadget prend enti\u00E8rement en charge TalkBack avec des annonces d\u2019\u00E9cran et des \u00E9tiquettes s\u00E9mantiques.")
    }
}
