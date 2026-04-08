package com.hardwaredash.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Centralized localization strings for HardwareDash.
 * Access via: S.nav.logbook, S.logbook.title, etc.
 * All composable reads automatically recompose when language changes.
 */
object S {
    private val lang: Language
        @Composable get() {
            val l by LocalizationManager.currentLanguage
            return l
        }

    // ── Navigation ──────────────────────────────────────────────────────
    val nav: Nav @Composable get() = Nav(lang)

    class Nav(private val l: Language) {
        val logbook get() = m(l, "Logbook", "Logbuch", "Registro", "Journal")
        val torch get() = m(l, "Torch", "Taschenlampe", "Linterna", "Lampe")
        val camera get() = m(l, "Camera", "Kamera", "Cámara", "Caméra")
        val vibration get() = m(l, "Vibration", "Vibration", "Vibración", "Vibration")
        val mic get() = m(l, "Mic", "Mikrofon", "Micrófono", "Micro")
        val radios get() = m(l, "Radios", "Funk", "Radios", "Radios")
        val sensors get() = m(l, "Sensors", "Sensoren", "Sensores", "Capteurs")
        val battery get() = m(l, "Battery", "Akku", "Batería", "Batterie")
        val lock get() = m(l, "Lock", "Sperre", "Bloqueo", "Verrou")
        val settings get() = m(l, "Settings", "Einstellungen", "Ajustes", "Paramètres")
        val fileMeta get() = m(l, "Files", "Dateien", "Archivos", "Fichiers")
    }

    // ── Logbook (formerly Ticked) ───────────────────────────────────────
    val logbook: Logbook @Composable get() = Logbook(lang)

    class Logbook(private val l: Language) {
        val title get() = m(l, "Logbook", "Logbuch", "Registro", "Journal")
        val logTab get() = m(l, "Log", "Protokoll", "Registro", "Journal")
        val processesTab get() = m(l, "Processes", "Prozesse", "Procesos", "Processus")
        val noEntriesYet get() = m(l, "No entries yet", "Noch keine Einträge", "Sin entradas aún", "Aucune entrée")
        val noMatches get() = m(l, "No matches", "Keine Treffer", "Sin coincidencias", "Aucune correspondance")
        val noProcessesYet get() = m(l, "No processes yet", "Noch keine Prozesse", "Sin procesos aún", "Aucun processus")
        val optionalNote get() = m(l, "Optional note\u2026", "Optionale Notiz\u2026", "Nota opcional\u2026", "Note facultative\u2026")
        val log get() = m(l, "Log", "Loggen", "Registrar", "Enregistrer")
        val add get() = m(l, "Add", "Hinzufügen", "Añadir", "Ajouter")
        val filters get() = m(l, "Filters", "Filter", "Filtros", "Filtres")
        val templates get() = m(l, "Templates", "Vorlagen", "Plantillas", "Modèles")
        val toggleView get() = m(l, "Toggle view", "Ansicht wechseln", "Cambiar vista", "Changer la vue")
        val clearAll get() = m(l, "Clear all", "Alle löschen", "Borrar todo", "Tout effacer")
        val deleteAll get() = m(l, "Delete all", "Alle löschen", "Eliminar todo", "Tout supprimer")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
        val delete get() = m(l, "Delete", "Löschen", "Eliminar", "Supprimer")
        val save get() = m(l, "Save", "Speichern", "Guardar", "Enregistrer")
        val customTimestamp get() = m(l, "Custom timestamp", "Eigener Zeitstempel", "Marca de tiempo personalizada", "Horodatage personnalisé")
        val date get() = m(l, "Date", "Datum", "Fecha", "Date")
        val time get() = m(l, "Time", "Zeit", "Hora", "Heure")
        val name get() = m(l, "Name", "Name", "Nombre", "Nom")
        val filterByDate get() = m(l, "Filter by date", "Nach Datum filtern", "Filtrar por fecha", "Filtrer par date")
        val clearFilters get() = m(l, "Clear filters", "Filter zurücksetzen", "Borrar filtros", "Effacer les filtres")
        val setBackground get() = m(l, "Set Background", "Hintergrund festlegen", "Establecer fondo", "Définir l'arrière-plan")
        val setBorder get() = m(l, "Set Border", "Rand festlegen", "Establecer borde", "Définir la bordure")
        val changeTime get() = m(l, "Change Time", "Zeit ändern", "Cambiar hora", "Changer l'heure")
        val changeText get() = m(l, "Change Text", "Text ändern", "Cambiar texto", "Changer le texte")
        val addCheckpoint get() = m(l, "Add Checkpoint", "Prüfpunkt hinzufügen", "Añadir punto de control", "Ajouter un point de contrôle")
        val deleteCheckpoint get() = m(l, "Delete Checkpoint", "Prüfpunkt löschen", "Eliminar punto de control", "Supprimer le point de contrôle")
        val overdue get() = m(l, "OVERDUE", "ÜBERFÄLLIG", "VENCIDO", "EN RETARD")
        val today get() = m(l, "Today", "Heute", "Hoy", "Aujourd'hui")
        val custom get() = m(l, "Custom", "Benutzerdefiniert", "Personalizado", "Personnalisé")
        val current get() = m(l, "Current", "Aktuell", "Actual", "Actuel")
        val jumpHere get() = m(l, "Jump here", "Hierher springen", "Saltar aquí", "Aller ici")
        val dueDate get() = m(l, "Due Date", "Fälligkeitsdatum", "Fecha límite", "Date d'échéance")
        val reminderTime get() = m(l, "Reminder Time", "Erinnerungszeit", "Hora de recordatorio", "Heure de rappel")
        val enableReminder get() = m(l, "Enable reminder", "Erinnerung aktivieren", "Activar recordatorio", "Activer le rappel")
        val checkpointUpdated get() = m(l, "Checkpoint updated", "Prüfpunkt aktualisiert", "Punto de control actualizado", "Point de contrôle mis à jour")
        val setReminderFirst get() = m(l, "Set a reminder time first", "Erinnerungszeit zuerst festlegen", "Establezca primero una hora de recordatorio", "Définissez d'abord une heure de rappel")
        val exportedSuccessfully get() = m(l, "Exported successfully", "Erfolgreich exportiert", "Exportado con éxito", "Exporté avec succès")
        val exportFailed get() = m(l, "Export failed", "Export fehlgeschlagen", "Error al exportar", "Échec de l'exportation")
        val importFailed get() = m(l, "Import failed", "Import fehlgeschlagen", "Error al importar", "Échec de l'importation")
        fun imported(entries: Int, processes: Int) = m(l,
            "Imported $entries entries, $processes processes",
            "$entries Einträge, $processes Prozesse importiert",
            "Importados $entries entradas, $processes procesos",
            "$entries entrées, $processes processus importés")
        val importLabel get() = m(l, "Import", "Importieren", "Importar", "Importer")
        val exportLabel get() = m(l, "Export", "Exportieren", "Exportar", "Exporter")
        val all get() = m(l, "All", "Alle", "Todos", "Tous")
        val autoLogged get() = m(l, "Auto-logged", "Automatisch", "Automático", "Automatique")
        val edited get() = m(l, "Edited", "Bearbeitet", "Editado", "Modifié")
    }

    // ── Settings ────────────────────────────────────────────────────────
    val settings: Settings @Composable get() = Settings(lang)

    class Settings(private val l: Language) {
        val title get() = m(l, "Settings", "Einstellungen", "Ajustes", "Paramètres")
        val language get() = m(l, "Language", "Sprache", "Idioma", "Langue")
        val languageDesc get() = m(l, "Select your preferred language", "Wählen Sie Ihre bevorzugte Sprache", "Seleccione su idioma preferido", "Sélectionnez votre langue préférée")
        val widgetCustomizer get() = m(l, "Widget Customizer", "Widget-Anpassung", "Personalización de widgets", "Personnalisation des widgets")
        val phoneRingDuration get() = m(l, "Phone Ring Duration", "Klingeldauer", "Duración del timbre", "Durée de la sonnerie")
        val notifyDelay get() = m(l, "Notification Delay", "Benachrichtigungsverzögerung", "Retraso de notificación", "Délai de notification")
        val seconds get() = m(l, "seconds", "Sekunden", "segundos", "secondes")
        val phoneRingDesc get() = m(l, "How long the phone rings when triggered", "Wie lange das Telefon klingelt", "Cuánto tiempo suena el teléfono", "Combien de temps le téléphone sonne")
        val notifyDesc get() = m(l, "Delay before notification appears", "Verzögerung vor der Benachrichtigung", "Retraso antes de que aparezca la notificación", "Délai avant l'apparition de la notification")
    }

    // ── Lock Screen & Notifications ─────────────────────────────────────
    val lock: Lock @Composable get() = Lock(lang)

    class Lock(private val l: Language) {
        val title get() = m(l, "Lock Screen & Notifications", "Sperrbildschirm & Benachrichtigungen", "Pantalla de bloqueo y notificaciones", "Écran de verrouillage et notifications")
        val notificationDemos get() = m(l, "Notification Demos", "Benachrichtigungs-Demos", "Demos de notificaciones", "Démonstrations de notifications")
        val simpleNotification get() = m(l, "Simple Notification", "Einfache Benachrichtigung", "Notificación simple", "Notification simple")
        val simpleNotifDesc get() = m(l, "Basic icon + text, default priority", "Einfaches Symbol + Text, Standardpriorität", "Icono básico + texto, prioridad predeterminada", "Icône basique + texte, priorité par défaut")
        val headsUp get() = m(l, "Heads-Up (High Priority)", "Heads-Up (Hohe Priorität)", "Heads-Up (Alta prioridad)", "Heads-Up (Haute priorité)")
        val headsUpDesc get() = m(l, "Pops up on screen even when app is in background", "Erscheint auf dem Bildschirm, auch wenn die App im Hintergrund ist", "Aparece en pantalla incluso con la app en segundo plano", "Apparaît à l'écran même quand l'app est en arrière-plan")
        val withActionButtons get() = m(l, "With Action Buttons", "Mit Aktionsschaltflächen", "Con botones de acción", "Avec boutons d'action")
        val actionButtonsDesc get() = m(l, "Expandable notification with tappable action buttons", "Erweiterbare Benachrichtigung mit Aktionsschaltflächen", "Notificación expandible con botones de acción", "Notification extensible avec boutons d'action")
        val progressBar get() = m(l, "Progress Bar", "Fortschrittsbalken", "Barra de progreso", "Barre de progression")
        val progressBarDesc get() = m(l, "Indeterminate progress spinner notification", "Unbestimmter Fortschrittsbalken", "Notificación con progreso indeterminado", "Notification avec progression indéterminée")
        val bigPicture get() = m(l, "Big Picture Style", "Großbildstil", "Estilo de imagen grande", "Style grande image")
        val bigPictureDesc get() = m(l, "Expandable notification with an image", "Erweiterbare Benachrichtigung mit Bild", "Notificación expandible con imagen", "Notification extensible avec image")
        val send get() = m(l, "Send", "Senden", "Enviar", "Envoyer")
        val customNotifBuilder get() = m(l, "Custom Notification Builder", "Benutzerdefinierter Benachrichtigungs-Builder", "Constructor de notificaciones personalizado", "Constructeur de notifications personnalisé")
        val titleLabel get() = m(l, "Title", "Titel", "Título", "Titre")
        val bodyLabel get() = m(l, "Body", "Inhalt", "Cuerpo", "Corps")
        val priority get() = m(l, "Priority", "Priorität", "Prioridad", "Priorité")
        val lockScreenVisibility get() = m(l, "Lock Screen Visibility", "Sperrbildschirm-Sichtbarkeit", "Visibilidad en pantalla de bloqueo", "Visibilité sur écran de verrouillage")
        val accentColor get() = m(l, "Accent Color", "Akzentfarbe", "Color de acento", "Couleur d'accentuation")
        val sendCustomNotif get() = m(l, "Send Custom Notification", "Benutzerdefinierte Benachrichtigung senden", "Enviar notificación personalizada", "Envoyer notification personnalisée")
        val emergencyAlerts get() = m(l, "Emergency Alerts", "Notfallwarnungen", "Alertas de emergencia", "Alertes d'urgence")
        val cancelAllNotif get() = m(l, "Cancel All Notifications", "Alle Benachrichtigungen löschen", "Cancelar todas las notificaciones", "Annuler toutes les notifications")
        val capabilities get() = m(l, "Lock Screen Capabilities", "Sperrbildschirm-Funktionen", "Capacidades de pantalla de bloqueo", "Capacités de l'écran de verrouillage")
        val deviceAdmin get() = m(l, "Device Admin", "Geräteadministrator", "Administrador del dispositivo", "Administrateur de l'appareil")
        val overlayPermission get() = m(l, "Overlay Permission", "Overlay-Berechtigung", "Permiso de superposición", "Permission de superposition")
        val actions get() = m(l, "Actions", "Aktionen", "Acciones", "Actions")
        val lockScreenNow get() = m(l, "Lock Screen Now", "Bildschirm jetzt sperren", "Bloquear pantalla ahora", "Verrouiller l'écran maintenant")
        val lockScreenDesigner get() = m(l, "Lock Screen Notification Designer", "Sperrbildschirm-Benachrichtigungs-Designer", "Diseñador de notificaciones de pantalla de bloqueo", "Concepteur de notifications d'écran de verrouillage")
        val scheduleAction get() = m(l, "Schedule Action", "Aktion planen", "Programar acción", "Planifier une action")
        val sendNow get() = m(l, "Send Now", "Jetzt senden", "Enviar ahora", "Envoyer maintenant")
        val schedule get() = m(l, "Schedule", "Planen", "Programar", "Planifier")
        val notification get() = m(l, "Notification", "Benachrichtigung", "Notificación", "Notification")
        val lockScreen get() = m(l, "Lock Screen", "Sperrbildschirm", "Pantalla de bloqueo", "Écran de verrouillage")
        val phoneRing get() = m(l, "Phone Ring", "Telefonklingeln", "Timbre del teléfono", "Sonnerie du téléphone")
        val alertText get() = m(l, "Alert text", "Alarmtext", "Texto de alerta", "Texte d'alerte")
        val ok get() = m(l, "OK", "OK", "OK", "OK")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
        val selectTime get() = m(l, "Select Time", "Zeit auswählen", "Seleccionar hora", "Sélectionner l'heure")
        val grantPermission get() = m(l, "Grant Permission", "Berechtigung erteilen", "Conceder permiso", "Accorder la permission")
        val activateDeviceAdmin get() = m(l, "Activate Device Admin", "Geräteadministrator aktivieren", "Activar administrador del dispositivo", "Activer l'administrateur de l'appareil")
        val deactivateDeviceAdmin get() = m(l, "Deactivate Device Admin", "Geräteadministrator deaktivieren", "Desactivar administrador del dispositivo", "Désactiver l'administrateur de l'appareil")
        val openOverlaySettings get() = m(l, "Open Overlay Permission Settings", "Overlay-Berechtigungseinstellungen öffnen", "Abrir configuración de permisos de superposición", "Ouvrir les paramètres de permission de superposition")
        val openEmergencySettings get() = m(l, "Open Emergency Alert Settings", "Notfallwarnungseinstellungen öffnen", "Abrir configuración de alertas de emergencia", "Ouvrir les paramètres d'alertes d'urgence")
        val scheduledActions get() = m(l, "Scheduled Actions", "Geplante Aktionen", "Acciones programadas", "Actions planifiées")
        val publicVisibility get() = m(l, "Public -- Full content visible on lock screen", "Öffentlich -- Vollständiger Inhalt auf dem Sperrbildschirm sichtbar", "Público -- Contenido completo visible en pantalla de bloqueo", "Public -- Contenu complet visible sur l'écran de verrouillage")
        val privateVisibility get() = m(l, "Private -- Icon only, content hidden", "Privat -- Nur Symbol, Inhalt verborgen", "Privado -- Solo icono, contenido oculto", "Privé -- Icône seulement, contenu masqué")
        val secretVisibility get() = m(l, "Secret -- Completely hidden from lock screen", "Geheim -- Vollständig vom Sperrbildschirm verborgen", "Secreto -- Completamente oculto de la pantalla de bloqueo", "Secret -- Complètement masqué de l'écran de verrouillage")
        val min get() = m(l, "Min", "Min", "Mín", "Min")
        val low get() = m(l, "Low", "Niedrig", "Bajo", "Bas")
        val default_ get() = m(l, "Default", "Standard", "Predeterminado", "Par défaut")
        val high get() = m(l, "High", "Hoch", "Alto", "Élevé")
        val max get() = m(l, "Max", "Max", "Máx", "Max")
        val message get() = m(l, "Message", "Nachricht", "Mensaje", "Message")
        val alarm get() = m(l, "Alarm", "Alarm", "Alarma", "Alarme")
        val reminder get() = m(l, "Reminder", "Erinnerung", "Recordatorio", "Rappel")
        val event get() = m(l, "Event", "Ereignis", "Evento", "Événement")
        val granted get() = m(l, "Granted", "Erteilt", "Concedido", "Accordé")
        val notGranted get() = m(l, "Not granted", "Nicht erteilt", "No concedido", "Non accordé")
        // Notification builder additions (Batch 7)
        val actionButtons get() = m(l, "Action Buttons", "Aktionsschaltflächen", "Botones de acción", "Boutons d'action")
        val enableProgress get() = m(l, "Show Progress Bar", "Fortschrittsbalken anzeigen", "Mostrar barra de progreso", "Afficher la barre de progression")
        val ongoing get() = m(l, "Ongoing (persistent)", "Fortlaufend (persistent)", "Continua (persistente)", "En cours (persistant)")
        val autoCancel get() = m(l, "Auto-cancel on tap", "Automatisch schließen bei Tippen", "Cancelar automáticamente al tocar", "Annuler automatiquement au toucher")
        val delay get() = m(l, "Delay", "Verzögerung", "Retraso", "Délai")
        val preview get() = m(l, "Preview", "Vorschau", "Vista previa", "Aperçu")
        val style get() = m(l, "Style", "Stil", "Estilo", "Style")
        val bigText get() = m(l, "Big Text", "Großer Text", "Texto grande", "Grand texte")
        val inbox get() = m(l, "Inbox", "Posteingang", "Bandeja", "Boîte de réception")
    }

    // ── Torch ───────────────────────────────────────────────────────────
    val torch: Torch @Composable get() = Torch(lang)

    class Torch(private val l: Language) {
        val title get() = m(l, "Torch & Flashlight", "Taschenlampe & Blitzlicht", "Linterna y flash", "Lampe torche et flash")
        val flashlightToggle get() = m(l, "Flashlight", "Taschenlampe", "Linterna", "Lampe torche")
        val strobeMode get() = m(l, "Strobe Mode", "Stroboskop-Modus", "Modo estroboscópico", "Mode stroboscopique")
        val frequency get() = m(l, "Frequency", "Frequenz", "Frecuencia", "Fréquence")
        val brightness get() = m(l, "Brightness", "Helligkeit", "Brillo", "Luminosité")
    }

    // ── Camera ──────────────────────────────────────────────────────────
    val camera: Camera @Composable get() = Camera(lang)

    class Camera(private val l: Language) {
        val title get() = m(l, "Camera", "Kamera", "Cámara", "Caméra")
        val takeSnapshot get() = m(l, "Take Snapshot", "Foto aufnehmen", "Tomar foto", "Prendre une photo")
        val startRecording get() = m(l, "Start Recording", "Aufnahme starten", "Iniciar grabación", "Démarrer l'enregistrement")
        val stopRecording get() = m(l, "Stop Recording", "Aufnahme stoppen", "Detener grabación", "Arrêter l'enregistrement")
        val switchCamera get() = m(l, "Switch Camera", "Kamera wechseln", "Cambiar cámara", "Changer de caméra")
        val cameraPermRequired get() = m(l, "Camera permission required", "Kameraberechtigung erforderlich", "Se requiere permiso de cámara", "Permission caméra requise")
    }

    // ── Vibration ───────────────────────────────────────────────────────
    val vibration: Vibration @Composable get() = Vibration(lang)

    class Vibration(private val l: Language) {
        val title get() = m(l, "Vibration", "Vibration", "Vibración", "Vibration")
        val pattern get() = m(l, "Pattern", "Muster", "Patrón", "Modèle")
        val intensity get() = m(l, "Intensity", "Intensität", "Intensidad", "Intensité")
        val duration get() = m(l, "Duration", "Dauer", "Duración", "Durée")
        val vibrate get() = m(l, "Vibrate", "Vibrieren", "Vibrar", "Vibrer")
        val stop get() = m(l, "Stop", "Stoppen", "Parar", "Arrêter")
    }

    // ── Mic ─────────────────────────────────────────────────────────────
    val mic: Mic @Composable get() = Mic(lang)

    class Mic(private val l: Language) {
        val title get() = m(l, "Microphone", "Mikrofon", "Micrófono", "Microphone")
        val startListening get() = m(l, "Start Listening", "Zuhören starten", "Empezar a escuchar", "Commencer à écouter")
        val stopListening get() = m(l, "Stop Listening", "Zuhören stoppen", "Dejar de escuchar", "Arrêter d'écouter")
        val dbLevel get() = m(l, "dB Level", "dB-Pegel", "Nivel dB", "Niveau dB")
        val audioPermRequired get() = m(l, "Audio permission required", "Audioberechtigung erforderlich", "Se requiere permiso de audio", "Permission audio requise")
        val recording get() = m(l, "Recording", "Aufnahme", "Grabando", "Enregistrement")
    }

    // ── Radios ──────────────────────────────────────────────────────────
    val radios: Radios @Composable get() = Radios(lang)

    class Radios(private val l: Language) {
        val title get() = m(l, "Radios & Connectivity", "Funk & Konnektivität", "Radios y conectividad", "Radios et connectivité")
        val wifi get() = m(l, "Wi-Fi", "WLAN", "Wi-Fi", "Wi-Fi")
        val bluetooth get() = m(l, "Bluetooth", "Bluetooth", "Bluetooth", "Bluetooth")
        val nfc get() = m(l, "NFC", "NFC", "NFC", "NFC")
        val gpsLocation get() = m(l, "GPS / Location", "GPS / Standort", "GPS / Ubicación", "GPS / Localisation")
        val gpsTracking get() = m(l, "GPS Tracking", "GPS-Tracking", "Seguimiento GPS", "Suivi GPS")
        val latitude get() = m(l, "Latitude", "Breitengrad", "Latitud", "Latitude")
        val longitude get() = m(l, "Longitude", "Längengrad", "Longitud", "Longitude")
        val altitude get() = m(l, "Altitude", "Höhe", "Altitud", "Altitude")
        val speed get() = m(l, "Speed", "Geschwindigkeit", "Velocidad", "Vitesse")
        val accuracy get() = m(l, "Accuracy", "Genauigkeit", "Precisión", "Précision")
        val bearing get() = m(l, "Bearing", "Richtung", "Rumbo", "Direction")
        val provider get() = m(l, "Provider", "Anbieter", "Proveedor", "Fournisseur")
        val gpsLog get() = m(l, "GPS Log", "GPS-Protokoll", "Registro GPS", "Journal GPS")
        val locationPermRequired get() = m(l, "Location permission required for GPS", "Standortberechtigung für GPS erforderlich", "Se requiere permiso de ubicación para GPS", "Permission de localisation requise pour le GPS")
        val grantLocationPerm get() = m(l, "Grant Location Permission", "Standortberechtigung erteilen", "Conceder permiso de ubicación", "Accorder la permission de localisation")
        val currentLocation get() = m(l, "Current Location", "Aktueller Standort", "Ubicación actual", "Position actuelle")
        val clearLog get() = m(l, "Clear log", "Protokoll löschen", "Borrar registro", "Effacer le journal")
    }

    // ── Sensors ─────────────────────────────────────────────────────────
    val sensors: Sensors @Composable get() = Sensors(lang)

    class Sensors(private val l: Language) {
        val title get() = m(l, "Sensors", "Sensoren", "Sensores", "Capteurs")
        val accelerometer get() = m(l, "Accelerometer", "Beschleunigungsmesser", "Acelerómetro", "Accéléromètre")
        val gyroscope get() = m(l, "Gyroscope", "Gyroskop", "Giroscopio", "Gyroscope")
        val magnetometer get() = m(l, "Magnetometer", "Magnetometer", "Magnetómetro", "Magnétomètre")
        val proximity get() = m(l, "Proximity", "Näherung", "Proximidad", "Proximité")
        val light get() = m(l, "Light", "Licht", "Luz", "Lumière")
        val barometer get() = m(l, "Barometer", "Barometer", "Barómetro", "Baromètre")
        val stepCounter get() = m(l, "Step Counter", "Schrittzähler", "Podómetro", "Compteur de pas")
    }

    // ── Battery ─────────────────────────────────────────────────────────
    val battery: Battery @Composable get() = Battery(lang)

    class Battery(private val l: Language) {
        val title get() = m(l, "Battery", "Akku", "Batería", "Batterie")
        val level get() = m(l, "Level", "Ladezustand", "Nivel", "Niveau")
        val status get() = m(l, "Status", "Status", "Estado", "Statut")
        val health get() = m(l, "Health", "Zustand", "Salud", "Santé")
        val temperature get() = m(l, "Temperature", "Temperatur", "Temperatura", "Température")
        val voltage get() = m(l, "Voltage", "Spannung", "Voltaje", "Tension")
        val technology get() = m(l, "Technology", "Technologie", "Tecnología", "Technologie")
        val charging get() = m(l, "Charging", "Laden", "Cargando", "En charge")
        val discharging get() = m(l, "Discharging", "Entladen", "Descargando", "Décharge")
        val full get() = m(l, "Full", "Voll", "Completa", "Pleine")
        val notCharging get() = m(l, "Not charging", "Nicht laden", "No cargando", "Pas en charge")
    }

    // ── File Metadata (Batch 10) ────────────────────────────────────────
    val fileMeta: FileMeta @Composable get() = FileMeta(lang)

    class FileMeta(private val l: Language) {
        val title get() = m(l, "File Metadata", "Datei-Metadaten", "Metadatos de archivo", "Métadonnées de fichier")
        val selectFile get() = m(l, "Select a File", "Datei auswählen", "Seleccionar un archivo", "Sélectionner un fichier")
        val fileName get() = m(l, "File Name", "Dateiname", "Nombre de archivo", "Nom du fichier")
        val fileSize get() = m(l, "File Size", "Dateigröße", "Tamaño de archivo", "Taille du fichier")
        val mimeType get() = m(l, "MIME Type", "MIME-Typ", "Tipo MIME", "Type MIME")
        val dateModified get() = m(l, "Date Modified", "Änderungsdatum", "Fecha de modificación", "Date de modification")
        val addMetadata get() = m(l, "Add Metadata", "Metadaten hinzufügen", "Añadir metadatos", "Ajouter des métadonnées")
        val editMetadata get() = m(l, "Edit Metadata", "Metadaten bearbeiten", "Editar metadatos", "Modifier les métadonnées")
        val commonFields get() = m(l, "Common Metadata Fields", "Häufige Metadatenfelder", "Campos de metadatos comunes", "Champs de métadonnées courants")
        val noFileSelected get() = m(l, "No file selected", "Keine Datei ausgewählt", "Ningún archivo seleccionado", "Aucun fichier sélectionné")
        val saveChanges get() = m(l, "Save Changes", "Änderungen speichern", "Guardar cambios", "Enregistrer les modifications")
        val author get() = m(l, "Author", "Autor", "Autor", "Auteur")
        val version get() = m(l, "Version", "Version", "Versión", "Version")
        val copyright get() = m(l, "Copyright", "Urheberrecht", "Derechos de autor", "Droits d'auteur")
        val description get() = m(l, "Description", "Beschreibung", "Descripción", "Description")
    }

    // ── Caller Screen (Batch 8) ─────────────────────────────────────────
    val caller: Caller @Composable get() = Caller(lang)

    class Caller(private val l: Language) {
        val incomingCall get() = m(l, "Incoming Call", "Eingehender Anruf", "Llamada entrante", "Appel entrant")
        val hardwareDash get() = m(l, "HardwareDash", "HardwareDash", "HardwareDash", "HardwareDash")
        val decline get() = m(l, "Decline", "Ablehnen", "Rechazar", "Refuser")
        val stopRinging get() = m(l, "Stop Ringing", "Klingeln stoppen", "Dejar de sonar", "Arrêter la sonnerie")
    }

    // ── Common / Shared ─────────────────────────────────────────────────
    val common: Common @Composable get() = Common(lang)

    class Common(private val l: Language) {
        val ok get() = m(l, "OK", "OK", "OK", "OK")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
        val save get() = m(l, "Save", "Speichern", "Guardar", "Enregistrer")
        val delete get() = m(l, "Delete", "Löschen", "Eliminar", "Supprimer")
        val close get() = m(l, "Close", "Schließen", "Cerrar", "Fermer")
        val confirm get() = m(l, "Confirm", "Bestätigen", "Confirmar", "Confirmer")
        val error get() = m(l, "Error", "Fehler", "Error", "Erreur")
        val success get() = m(l, "Success", "Erfolg", "Éxito", "Succès")
        val loading get() = m(l, "Loading...", "Laden...", "Cargando...", "Chargement...")
        val search get() = m(l, "Search", "Suchen", "Buscar", "Rechercher")
        val back get() = m(l, "Back", "Zurück", "Atrás", "Retour")
    }

    // ── Widget toasts (non-Composable context) ──────────────────────────
    object Widget {
        fun phoneRingToast(lang: Language, seconds: Int) = m(lang,
            "Phone will ring in $seconds seconds",
            "Telefon klingelt in $seconds Sekunden",
            "El teléfono sonará en $seconds segundos",
            "Le téléphone sonnera dans $seconds secondes")

        fun notifyToast(lang: Language, seconds: Int) = m(lang,
            "Notification in $seconds seconds",
            "Benachrichtigung in $seconds Sekunden",
            "Notificación en $seconds segundos",
            "Notification dans $seconds secondes")
    }
}

/** Helper: pick the string for the given language. Order: EN, DE, ES, FR. */
private fun m(lang: Language, en: String, de: String, es: String, fr: String): String =
    when (lang) {
        Language.EN -> en
        Language.DE -> de
        Language.ES -> es
        Language.FR -> fr
    }
