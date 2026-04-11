package com.gadget.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue

/**
 * Centralized localization strings for Gadget.
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
        val link get() = m(l, "Link", "Link", "Enlace", "Lien")
        val settings get() = m(l, "Settings", "Einstellungen", "Ajustes", "Paramètres")
        val fileMeta get() = m(l, "Files", "Dateien", "Archivos", "Fichiers")
        val bug get() = m(l, "Bug", "Fehler", "Error", "Bug")
        // New top-level tab labels
        val dashboard get() = m(l, "Dashboard", "Dashboard", "Panel", "Tableau de bord")
        val tools get() = m(l, "Tools", "Werkzeuge", "Herramientas", "Outils")
        val monitor get() = m(l, "Monitor", "Monitor", "Monitor", "Moniteur")
        val more get() = m(l, "More", "Mehr", "Más", "Plus")
    }

    // ── Dashboard ───────────────────────────────────────────────────────
    val dashboard: Dashboard @Composable get() = Dashboard(lang)

    class Dashboard(private val l: Language) {
        val title get() = m(l, "Dashboard", "Dashboard", "Panel", "Tableau de bord")
        val subtitle get() = m(l, "Hardware Toolkit", "Hardware-Toolkit", "Kit de herramientas", "Boîte à outils")
        val quickActions get() = m(l, "Quick Actions", "Schnellaktionen", "Acciones rápidas", "Actions rapides")
        val status get() = m(l, "Status", "Status", "Estado", "Statut")
        val recentLogEntry get() = m(l, "Recent Log Entry", "Letzter Eintrag", "Entrada reciente", "Entrée récente")
        val noRecentEntries get() = m(l, "No recent entries", "Keine aktuellen Einträge", "Sin entradas recientes", "Aucune entrée récente")
    }

    // ── Hubs ────────────────────────────────────────────────────────────
    val hubs: Hubs @Composable get() = Hubs(lang)

    class Hubs(private val l: Language) {
        // Tools hub
        val toolsTitle get() = m(l, "Tools", "Werkzeuge", "Herramientas", "Outils")
        val torchSubtitle get() = m(l, "Flashlight & Strobe", "Taschenlampe & Stroboskop", "Linterna y estrobo", "Lampe & Stroboscope")
        val cameraSubtitle get() = m(l, "Multi-lens Capture", "Mehrlinsen-Aufnahme", "Captura multilente", "Capture multi-objectifs")
        val vibrationSubtitle get() = m(l, "Pattern Builder", "Muster-Editor", "Editor de patrones", "Éditeur de motifs")
        val micSubtitle get() = m(l, "dB Meter & Recorder", "dB-Messgerät & Rekorder", "Medidor dB y grabadora", "Sonomètre & enregistreur")
        // Monitor hub
        val monitorTitle get() = m(l, "Monitor", "Monitor", "Monitor", "Moniteur")
        val sensorsSubtitle get() = m(l, "Live hardware sensor charts", "Live-Hardware-Sensordiagramme", "Gráficos de sensores en vivo", "Graphiques de capteurs en direct")
        val batterySubtitle get() = m(l, "Level, health & temperature", "Ladezustand, Zustand & Temperatur", "Nivel, salud y temperatura", "Niveau, santé & température")
        val radiosSubtitle get() = m(l, "WiFi, Bluetooth, NFC, GPS", "WLAN, Bluetooth, NFC, GPS", "WiFi, Bluetooth, NFC, GPS", "WiFi, Bluetooth, NFC, GPS")
        // More hub
        val moreTitle get() = m(l, "More", "Mehr", "Más", "Plus")
        val notifications get() = m(l, "Notifications", "Benachrichtigungen", "Notificaciones", "Notifications")
        val notificationsSubtitle get() = m(l, "Demos, builder & lock screen", "Demos, Builder & Sperrbildschirm", "Demos, constructor y pantalla de bloqueo", "Démos, constructeur & écran de verrouillage")
        val automation get() = m(l, "Automation", "Automatisierung", "Automatización", "Automatisation")
        val automationSubtitle get() = m(l, "IF/THEN metric rules", "WENN/DANN Metrik-Regeln", "Reglas SI/ENTONCES", "Règles SI/ALORS")
        val fileMetaSubtitle get() = m(l, "EXIF & media metadata", "EXIF- & Medien-Metadaten", "Metadatos EXIF y multimedia", "Métadonnées EXIF & média")
        val settingsSubtitle get() = m(l, "Language, config & logging", "Sprache, Konfiguration & Protokollierung", "Idioma, configuración y registro", "Langue, configuration & journalisation")
        val bugReportSubtitle get() = m(l, "Permissions & device info", "Berechtigungen & Geräteinformationen", "Permisos e información del dispositivo", "Permissions & informations appareil")
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
        // Dialog strings
        val clearAllEntries get() = m(l, "Clear all entries?", "Alle Einträge löschen?", "¿Borrar todas las entradas?", "Effacer toutes les entrées ?")
        fun clearAllEntriesMsg(count: Int) = m(l,
            "This will delete all $count entries. This cannot be undone.",
            "Dies löscht alle $count Einträge. Dies kann nicht rückgängig gemacht werden.",
            "Esto eliminará todas las $count entradas. No se puede deshacer.",
            "Cela supprimera toutes les $count entrées. Cette action est irréversible.")
        val clearAllProcesses get() = m(l, "Clear all processes?", "Alle Prozesse löschen?", "¿Borrar todos los procesos?", "Effacer tous les processus ?")
        fun clearAllProcessesMsg(count: Int) = m(l,
            "This will delete all $count processes and their reminders. This cannot be undone.",
            "Dies löscht alle $count Prozesse und deren Erinnerungen. Dies kann nicht rückgängig gemacht werden.",
            "Esto eliminará todos los $count procesos y sus recordatorios. No se puede deshacer.",
            "Cela supprimera tous les $count processus et leurs rappels. Cette action est irréversible.")
        val deleteEntry get() = m(l, "Delete entry?", "Eintrag löschen?", "¿Eliminar entrada?", "Supprimer l'entrée ?")
        fun deleteEntryMsg(text: String) = m(l,
            "\"$text\" will be removed.",
            "\"$text\" wird entfernt.",
            "\"$text\" será eliminado.",
            "\"$text\" sera supprimé.")
        val deleteProcess get() = m(l, "Delete process?", "Prozess löschen?", "¿Eliminar proceso?", "Supprimer le processus ?")
        fun deleteProcessMsg(text: String) = m(l,
            "\"$text\" and all its checkpoints will be removed.",
            "\"$text\" und alle Prüfpunkte werden entfernt.",
            "\"$text\" y todos sus puntos de control serán eliminados.",
            "\"$text\" et tous ses points de contrôle seront supprimés.")
        val deleteCheckpointConfirm get() = m(l, "Delete checkpoint?", "Prüfpunkt löschen?", "¿Eliminar punto de control?", "Supprimer le point de contrôle ?")
        val processNamePlaceholder get() = m(l, "Process name\u2026", "Prozessname\u2026", "Nombre del proceso\u2026", "Nom du processus\u2026")
        val checkpointNamePlaceholder get() = m(l, "Checkpoint name\u2026", "Prüfpunktname\u2026", "Nombre del punto de control\u2026", "Nom du point de contrôle\u2026")
        val commentNote get() = m(l, "Comment / Note", "Kommentar / Notiz", "Comentario / Nota", "Commentaire / Note")
        val addNote get() = m(l, "Add a note\u2026", "Notiz hinzufügen\u2026", "Añadir una nota\u2026", "Ajouter une note\u2026")
        val enterText get() = m(l, "Enter text\u2026", "Text eingeben\u2026", "Introduzca texto\u2026", "Entrez du texte\u2026")
        val tapToSelect get() = m(l, "Tap to select", "Tippen zum Auswählen", "Toque para seleccionar", "Appuyez pour sélectionner")
        val tapToSelectDateTime get() = m(l, "Tap to select date & time", "Tippen zum Auswählen von Datum und Uhrzeit", "Toque para seleccionar fecha y hora", "Appuyez pour sélectionner la date et l'heure")
        fun movedTo(name: String) = m(l, "Moved to \"$name\"", "Verschoben zu \"$name\"", "Movido a \"$name\"", "Déplacé vers \"$name\"")
        val textUpdated get() = m(l, "Text updated", "Text aktualisiert", "Texto actualizado", "Texte mis à jour")
        val timeUpdated get() = m(l, "Time updated", "Zeit aktualisiert", "Hora actualizada", "Heure mise à jour")
        val colorApplied get() = m(l, "Color applied", "Farbe angewendet", "Color aplicado", "Couleur appliquée")
        val searchPlaceholder get() = m(l, "Search\u2026", "Suchen\u2026", "Buscar\u2026", "Rechercher\u2026")
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
        val metricLogging get() = m(l, "Metric Logging", "Metrik-Protokollierung", "Registro de métricas", "Journalisation des métriques")
        val metricLoggingDesc get() = m(l, "Select metrics to capture with each log entry", "Metriken auswählen, die bei jedem Logbuch-Eintrag erfasst werden", "Seleccione métricas para capturar con cada entrada del registro", "Sélectionnez les métriques à capturer avec chaque entrée du journal")
        val bypassDnd get() = m(l, "Bypass Do Not Disturb", "Nicht st\u00F6ren umgehen", "Omitir No molestar", "Contourner Ne pas d\u00E9ranger")
        val bypassDndDesc get() = m(l, "Use alarm audio channel to bypass DND and silent mode for ring, vibration, and notification actions", "Alarm-Audiokanal verwenden, um DND und Lautlos-Modus f\u00FCr Klingeln, Vibration und Benachrichtigungen zu umgehen", "Usar canal de audio de alarma para omitir DND y modo silencioso para acciones de timbre, vibraci\u00F3n y notificaci\u00F3n", "Utiliser le canal audio d'alarme pour contourner NPD et le mode silencieux pour les actions de sonnerie, vibration et notification")
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
        val determinate get() = m(l, "Determinate", "Bestimmt", "Determinado", "Déterminé")
        val indeterminate get() = m(l, "Indeterminate", "Unbestimmt", "Indeterminado", "Indéterminé")
        val normal get() = m(l, "Normal", "Normal", "Normal", "Normal")
    }

    // ── Link ────────────────────────────────────────────────────────────
    val link: Link @Composable get() = Link(lang)

    class Link(private val l: Language) {
        val title get() = m(l, "Link", "Link", "Enlace", "Lien")
        val addLink get() = m(l, "Add Link", "Link hinzufügen", "Añadir enlace", "Ajouter un lien")
        val editLink get() = m(l, "Edit Link", "Link bearbeiten", "Editar enlace", "Modifier le lien")
        val deleteLink get() = m(l, "Delete Link", "Link löschen", "Eliminar enlace", "Supprimer le lien")
        val deleteConfirm get() = m(l, "Delete this link rule?", "Diese Link-Regel löschen?", "¿Eliminar esta regla de enlace?", "Supprimer cette règle de lien ?")
        val ifLabel get() = m(l, "IF", "WENN", "SI", "SI")
        val thenLabel get() = m(l, "THEN", "DANN", "ENTONCES", "ALORS")
        val metric get() = m(l, "Metric", "Metrik", "Métrica", "Métrique")
        val selectMetric get() = m(l, "Select metric\u2026", "Metrik auswählen\u2026", "Seleccionar métrica\u2026", "Sélectionner la métrique\u2026")
        val operator get() = m(l, "Operator", "Operator", "Operador", "Opérateur")
        val threshold get() = m(l, "Threshold", "Schwellenwert", "Umbral", "Seuil")
        val action get() = m(l, "Action", "Aktion", "Acción", "Action")
        val selectAction get() = m(l, "Select action\u2026", "Aktion auswählen\u2026", "Seleccionar acción\u2026", "Sélectionner l'action\u2026")
        val cooldown get() = m(l, "Cooldown", "Abklingzeit", "Enfriamiento", "Temps de recharge")
        val noLinksYet get() = m(l, "No link rules yet", "Noch keine Link-Regeln", "Sin reglas de enlace aún", "Aucune règle de lien")
        val startMonitoring get() = m(l, "Start Monitoring", "Überwachung starten", "Iniciar monitoreo", "Démarrer la surveillance")
        val stopMonitoring get() = m(l, "Stop Monitoring", "Überwachung stoppen", "Detener monitoreo", "Arrêter la surveillance")
        val save get() = m(l, "Save", "Speichern", "Guardar", "Enregistrer")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
        val delete get() = m(l, "Delete", "Löschen", "Eliminar", "Supprimer")
        val ruleName get() = m(l, "Rule name\u2026", "Regelname\u2026", "Nombre de regla\u2026", "Nom de la règle\u2026")
        val notifTitle get() = m(l, "Notification title", "Benachrichtigungstitel", "Título de notificación", "Titre de notification")
        val notifBody get() = m(l, "Notification body", "Benachrichtigungstext", "Cuerpo de notificación", "Corps de notification")
        val monitoring get() = m(l, "Monitoring", "Überwachung", "Monitoreo", "Surveillance")
        val thresholdLow get() = m(l, "Lower bound", "Unterer Grenzwert", "Límite inferior", "Limite inférieure")
        val thresholdHigh get() = m(l, "Upper bound", "Oberer Grenzwert", "Límite superior", "Limite supérieure")
        val recommended get() = m(l, "Recommended", "Empfohlen", "Recomendado", "Recommandé")
        val logEntryText get() = m(l, "Log entry text", "Logbuch-Text", "Texto de registro", "Texte du journal")
        val logEntryPlaceholder get() = m(l, "Leave blank for auto text\u2026", "Leer lassen f\u00FCr Auto-Text\u2026", "Dejar vac\u00EDo para texto autom\u00E1tico\u2026", "Laisser vide pour texte auto\u2026")
        val statsTitle get() = m(l, "Statistics", "Statistiken", "Estad\u00EDsticas", "Statistiques")
        val triggered get() = m(l, "Triggered", "Ausgel\u00F6st", "Activado", "D\u00E9clench\u00E9")
        val cooldownBlocked get() = m(l, "Cooldown blocked", "Abklingzeit blockiert", "Bloqueado por enfriamiento", "Bloqu\u00E9 par recharge")
        val lastTriggered get() = m(l, "Last triggered", "Zuletzt ausgel\u00F6st", "\u00DAltima activaci\u00F3n", "Dernier d\u00E9clenchement")
        val resetStats get() = m(l, "Reset statistics", "Statistiken zur\u00FCcksetzen", "Restablecer estad\u00EDsticas", "R\u00E9initialiser les statistiques")
    }

    // ── Torch ───────────────────────────────────────────────────────────
    val torch: Torch @Composable get() = Torch(lang)

    class Torch(private val l: Language) {
        val title get() = m(l, "Torch / Flashlight", "Taschenlampe / Blitzlicht", "Linterna / Flash", "Lampe torche / Flash")
        val flashDetected get() = m(l, "Flash hardware detected", "Flash-Hardware erkannt", "Hardware de flash detectado", "Matériel flash détecté")
        val noFlash get() = m(l, "No flash hardware on this device", "Kein Flash-Hardware auf diesem Gerät", "Sin hardware de flash en este dispositivo", "Pas de matériel flash sur cet appareil")
        val turnOn get() = m(l, "Turn ON", "Einschalten", "Encender", "Allumer")
        val turnOff get() = m(l, "Turn OFF", "Ausschalten", "Apagar", "Éteindre")
        val strobe get() = m(l, "Strobe", "Stroboskop", "Estrobo", "Stroboscope")
        val stop get() = m(l, "Stop", "Stopp", "Parar", "Arrêter")
        val torchUnavailable get() = m(l, "Torch unavailable -- camera in use?", "Taschenlampe nicht verfügbar -- Kamera in Benutzung?", "Linterna no disponible -- ¿cámara en uso?", "Lampe torche indisponible -- caméra en cours d'utilisation ?")
        val torchNote get() = m(l, "Uses CameraManager.setTorchMode() -- no CAMERA permission needed for torch alone.", "Verwendet CameraManager.setTorchMode() -- keine CAMERA-Berechtigung für die Taschenlampe allein benötigt.", "Usa CameraManager.setTorchMode() -- no se necesita permiso de CÁMARA solo para la linterna.", "Utilise CameraManager.setTorchMode() -- aucune permission CAMÉRA nécessaire pour la lampe seule.")
        val displayBrightness get() = m(l, "Display Brightness", "Bildschirmhelligkeit", "Brillo de pantalla", "Luminosité de l'écran")
        val writeSettingsNeeded get() = m(l, "WRITE_SETTINGS permission needed to control brightness", "WRITE_SETTINGS-Berechtigung zur Steuerung der Helligkeit erforderlich", "Se necesita permiso WRITE_SETTINGS para controlar el brillo", "Permission WRITE_SETTINGS nécessaire pour contrôler la luminosité")
        val grantWriteSettings get() = m(l, "Grant Write Settings", "Schreibeinstellungen gewähren", "Conceder configuración de escritura", "Accorder les paramètres d'écriture")
        val autoBrightness get() = m(l, "Auto Brightness", "Automatische Helligkeit", "Brillo automático", "Luminosité automatique")
        val strobeHz get() = m(l, "Strobe Frequency", "Stroboskop-Frequenz", "Frecuencia de estrobo", "Fréquence stroboscope")
        val alsoAppliesToWidget get() = m(l, "Also applies to home screen widget", "Gilt auch für das Startbildschirm-Widget", "También se aplica al widget de pantalla de inicio", "S'applique également au widget de l'écran d'accueil")
    }

    // ── Camera ──────────────────────────────────────────────────────────
    val camera: Camera @Composable get() = Camera(lang)

    class Camera(private val l: Language) {
        val title get() = m(l, "Camera", "Kamera", "Cámara", "Caméra")
        val lens get() = m(l, "Lens", "Objektiv", "Lente", "Objectif")
        val tapToFocus get() = m(l, "Tap-to-Focus", "Tippen-zum-Fokussieren", "Toque para enfocar", "Toucher pour la mise au point")
        val cameraControls get() = m(l, "Camera controls", "Kamerasteuerung", "Controles de cámara", "Contrôles de la caméra")
        val focusPointSet get() = m(l, "Focus point set", "Fokuspunkt gesetzt", "Punto de enfoque establecido", "Point de mise au point défini")
        val grantCameraPerm get() = m(l, "Grant Camera Permission", "Kameraberechtigung erteilen", "Conceder permiso de cámara", "Accorder la permission caméra")
        val cameraPermRationale get() = m(l, "Camera access is needed to show the live preview and capture photos.", "Kamerazugriff wird benötigt, um die Live-Vorschau anzuzeigen und Fotos aufzunehmen.", "Se necesita acceso a la cámara para mostrar la vista previa en vivo y capturar fotos.", "L'accès à la caméra est nécessaire pour afficher l'aperçu en direct et capturer des photos.")
    }

    // ── Vibration ───────────────────────────────────────────────────────
    val vibration: Vibration @Composable get() = Vibration(lang)

    class Vibration(private val l: Language) {
        val title get() = m(l, "Vibration Motor", "Vibrationsmotor", "Motor de vibración", "Moteur de vibration")
        val noAmplitude get() = m(l, "This device doesn't support amplitude control. Patterns will play at full strength.", "Dieses Gerät unterstützt keine Amplitudensteuerung. Muster werden mit voller Stärke abgespielt.", "Este dispositivo no soporta control de amplitud. Los patrones se reproducirán a máxima intensidad.", "Cet appareil ne prend pas en charge le contrôle d'amplitude. Les motifs seront joués à pleine puissance.")
        val predefinedEffects get() = m(l, "Predefined Haptic Effects", "Vordefinierte haptische Effekte", "Efectos hápticos predefinidos", "Effets haptiques prédéfinis")
        val customWaveform get() = m(l, "Custom Waveform Builder", "Benutzerdefinierter Wellenform-Builder", "Constructor de forma de onda personalizada", "Constructeur de forme d'onde personnalisé")
        val speedPresets get() = m(l, "Speed Presets", "Geschwindigkeitsvoreinstellungen", "Preajustes de velocidad", "Préréglages de vitesse")
        val loopWaveform get() = m(l, "Loop waveform", "Wellenform wiederholen", "Repetir forma de onda", "Boucler la forme d'onde")
        val addStep get() = m(l, "Add Step", "Schritt hinzufügen", "Añadir paso", "Ajouter une étape")
        val playLooping get() = m(l, "Play (Looping)", "Abspielen (Schleife)", "Reproducir (bucle)", "Lire (en boucle)")
        val playCustom get() = m(l, "Play Custom Waveform", "Benutzerdefinierte Wellenform abspielen", "Reproducir forma de onda personalizada", "Lire la forme d'onde personnalisée")
        val stop get() = m(l, "Stop", "Stopp", "Parar", "Arrêter")
        val savePattern get() = m(l, "Save Pattern", "Muster speichern", "Guardar patrón", "Enregistrer le motif")
        val loadPattern get() = m(l, "Load Pattern", "Muster laden", "Cargar patrón", "Charger le motif")
        val drawPattern get() = m(l, "Draw Vibration Pattern", "Vibrationsmuster zeichnen", "Dibujar patrón de vibración", "Dessiner le motif de vibration")
        val drawInstructions get() = m(l, "Draw with your finger: X = time (0\u20132s), Y = intensity (0\u2013100%)", "Mit dem Finger zeichnen: X = Zeit (0\u20132s), Y = Intensität (0\u2013100%)", "Dibuje con su dedo: X = tiempo (0\u20132s), Y = intensidad (0\u2013100%)", "Dessinez avec votre doigt: X = temps (0\u20132s), Y = intensité (0\u2013100%)")
        val loopDrawn get() = m(l, "Loop drawn pattern", "Gezeichnetes Muster wiederholen", "Repetir patrón dibujado", "Boucler le motif dessiné")
        val playDrawn get() = m(l, "Play Drawn", "Gezeichnetes abspielen", "Reproducir dibujado", "Lire le dessin")
        val clearDrawing get() = m(l, "Clear Drawing", "Zeichnung löschen", "Borrar dibujo", "Effacer le dessin")
        val patternName get() = m(l, "Pattern name", "Mustername", "Nombre del patrón", "Nom du motif")
        val save get() = m(l, "Save", "Speichern", "Guardar", "Enregistrer")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
        val close get() = m(l, "Close", "Schließen", "Cerrar", "Fermer")
        val noSavedPatterns get() = m(l, "No saved patterns yet.", "Noch keine gespeicherten Muster.", "Aún no hay patrones guardados.", "Aucun motif enregistré.")
        val slow get() = m(l, "Slow", "Langsam", "Lento", "Lent")
        val medium get() = m(l, "Medium", "Mittel", "Medio", "Moyen")
        val fast get() = m(l, "Fast", "Schnell", "Rápido", "Rapide")
        val rapid get() = m(l, "Rapid", "Sehr schnell", "Muy rápido", "Très rapide")
    }

    // ── Mic ─────────────────────────────────────────────────────────────
    val mic: Mic @Composable get() = Mic(lang)

    class Mic(private val l: Language) {
        val title get() = m(l, "Microphone Meter", "Mikrofon-Messgerät", "Medidor de micrófono", "Mètre de microphone")
        val micPermRationale get() = m(l, "Microphone access is needed to show live audio levels.", "Mikrofonzugriff wird benötigt, um Live-Audiopegel anzuzeigen.", "Se necesita acceso al micrófono para mostrar niveles de audio en vivo.", "L'accès au microphone est nécessaire pour afficher les niveaux audio en direct.")
        val grantPermission get() = m(l, "Grant Permission", "Berechtigung erteilen", "Conceder permiso", "Accorder la permission")
        val spectrumAnalyzer get() = m(l, "Spectrum Analyzer", "Spektrumanalysator", "Analizador de espectro", "Analyseur de spectre")
        val startMonitoring get() = m(l, "Start Monitoring", "Überwachung starten", "Iniciar monitoreo", "Démarrer la surveillance")
        val stopMonitor get() = m(l, "Stop Monitor", "Überwachung stoppen", "Detener monitoreo", "Arrêter la surveillance")
        val audioRecording get() = m(l, "Audio Recording", "Audioaufnahme", "Grabación de audio", "Enregistrement audio")
        val savedRecordings get() = m(l, "Saved Recordings", "Gespeicherte Aufnahmen", "Grabaciones guardadas", "Enregistrements sauvegardés")
        val playbackError get() = m(l, "Playback error", "Wiedergabefehler", "Error de reproducción", "Erreur de lecture")
        fun savedFile(name: String) = m(l, "Saved: $name", "Gespeichert: $name", "Guardado: $name", "Enregistré : $name")
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
        val exampleDomain get() = m(l, "example.com", "beispiel.de", "ejemplo.com", "exemple.com")
        val textPlain get() = m(l, "text/plain", "text/plain", "text/plain", "text/plain")
        val hotspot get() = m(l, "Hotspot", "Hotspot", "Punto de acceso", "Point d'accès")
        val wirelessProjection get() = m(l, "Wireless Projection", "Drahtlose Projektion", "Proyección inalámbrica", "Projection sans fil")
        val saveTag get() = m(l, "Save Tag", "Tag speichern", "Guardar etiqueta", "Enregistrer le tag")
        val savedNfcTags get() = m(l, "Saved NFC Tags", "Gespeicherte NFC-Tags", "Etiquetas NFC guardadas", "Tags NFC enregistrés")
        val emulate get() = m(l, "Emulate", "Emulieren", "Emular", "Émuler")
        val emulating get() = m(l, "Emulating — hold device near reader…", "Emuliert — Gerät an Leser halten…", "Emulando — acerque el dispositivo…", "Émulation — approchez l'appareil…")
        val stopEmulating get() = m(l, "Stop", "Stoppen", "Detener", "Arrêter")
        val tagName get() = m(l, "Tag name", "Tag-Name", "Nombre de etiqueta", "Nom du tag")
        val noSavedTags get() = m(l, "No saved tags", "Keine gespeicherten Tags", "Sin etiquetas guardadas", "Aucun tag enregistré")
        val loadFromSaved get() = m(l, "Load from saved tag…", "Aus gespeichertem Tag laden…", "Cargar desde etiqueta guardada…", "Charger depuis un tag enregistré…")
        val selectTag get() = m(l, "Select a tag…", "Tag auswählen…", "Seleccionar etiqueta…", "Sélectionner un tag…")
        val deleteTag get() = m(l, "Delete", "Löschen", "Eliminar", "Supprimer")
        val cancel get() = m(l, "Cancel", "Abbrechen", "Cancelar", "Annuler")
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
        val sensorReadingsCopied get() = m(l, "Sensor readings copied!", "Sensorwerte kopiert!", "¡Lecturas de sensores copiadas!", "Lectures de capteurs copiées !")
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
        val exifSection get() = m(l, "EXIF", "EXIF", "EXIF", "EXIF")
        val mediaSection get() = m(l, "Media", "Medien", "Medios", "Médias")
        val exifSaved get() = m(l, "EXIF data saved", "EXIF-Daten gespeichert", "Datos EXIF guardados", "Données EXIF enregistrées")
        val exifSaveFailed get() = m(l, "Failed to save EXIF data", "EXIF-Daten konnten nicht gespeichert werden", "Error al guardar datos EXIF", "Échec de l'enregistrement des données EXIF")
        val allFieldsPresent get() = m(l, "All common fields are already present.", "Alle gängigen Felder sind bereits vorhanden.", "Todos los campos comunes ya están presentes.", "Tous les champs courants sont déjà présents.")
        val editMediaMetadata get() = m(l, "Edit Media Metadata", "Medien-Metadaten bearbeiten", "Editar metadatos multimedia", "Modifier les métadonnées média")
        val mediaSaved get() = m(l, "Media metadata saved", "Medien-Metadaten gespeichert", "Metadatos multimedia guardados", "Métadonnées média enregistrées")
        val mediaSaveFailed get() = m(l, "Failed to save media metadata", "Medien-Metadaten konnten nicht gespeichert werden", "Error al guardar metadatos multimedia", "Échec de l'enregistrement des métadonnées média")
    }

    // ── Caller Screen (Batch 8) ─────────────────────────────────────────
    val caller: Caller @Composable get() = Caller(lang)

    class Caller(private val l: Language) {
        val incomingCall get() = m(l, "Incoming Call", "Eingehender Anruf", "Llamada entrante", "Appel entrant")
        val gadget get() = m(l, "Gadget", "Gadget", "Gadget", "Gadget")
        val decline get() = m(l, "Decline", "Ablehnen", "Rechazar", "Refuser")
        val stopRinging get() = m(l, "Stop Ringing", "Klingeln stoppen", "Dejar de sonar", "Arrêter la sonnerie")
    }

    // ── Bug Report ──────────────────────────────────────────────────────
    val bug: Bug @Composable get() = Bug(lang)

    class Bug(private val l: Language) {
        val title get() = m(l, "Bug Report", "Fehlerbericht", "Informe de error", "Rapport de bug")
        val permissionsTitle get() = m(l, "Permission Status", "Berechtigungsstatus", "Estado de permisos", "État des permissions")
        val deviceInfoTitle get() = m(l, "Device Information", "Geräteinformationen", "Información del dispositivo", "Informations sur l'appareil")
        val describeBug get() = m(l, "Describe the Bug", "Fehler beschreiben", "Describir el error", "Décrire le bug")
        val describeBugHint get() = m(l, "Describe the issue you encountered\u2026", "Beschreiben Sie das aufgetretene Problem\u2026", "Describa el problema que encontró\u2026", "Décrivez le problème rencontré\u2026")
        val createBugReport get() = m(l, "Create Bug Report", "Fehlerbericht erstellen", "Crear informe de error", "Créer un rapport de bug")
        val bugReportReady get() = m(l, "Bug Report Ready", "Fehlerbericht bereit", "Informe de error listo", "Rapport de bug prêt")
        val openGithubIssue get() = m(l, "Open GitHub Issue", "GitHub-Issue öffnen", "Abrir issue en GitHub", "Ouvrir une issue GitHub")
        val copyText get() = m(l, "Copy Text", "Text kopieren", "Copiar texto", "Copier le texte")
        val copiedToClipboard get() = m(l, "Copied to clipboard", "In Zwischenablage kopiert", "Copiado al portapapeles", "Copié dans le presse-papiers")
        val permissionLabel get() = m(l, "Permission", "Berechtigung", "Permiso", "Permission")
        val statusLabel get() = m(l, "Status", "Status", "Estado", "Statut")
        val granted get() = m(l, "Granted", "Erteilt", "Concedido", "Accordé")
        val notGranted get() = m(l, "Not Granted", "Nicht erteilt", "No concedido", "Non accordé")
        val disclaimerTitle get() = m(l, "Disclaimer", "Haftungsausschluss", "Aviso legal", "Avertissement")
        val disclaimerBody get() = m(l,
            "Firmware and hardware device-specific limitations apply. Some features may not be available or function correctly depending on your device type, manufacturer, and Android version. Certain features require specific permissions to be granted in order to operate. Audio features such as Phone Ring, vibration, and notifications may be silenced by Do Not Disturb or Silent mode \u2014 enable the Bypass DND option in Settings to override. Please submit feature requests and bug reports via the Bug Report menu.",
            "Es gelten firmware- und hardwarespezifische Einschränkungen. Einige Funktionen sind möglicherweise nicht verfügbar oder funktionieren je nach Gerätetyp, Hersteller und Android-Version nicht korrekt. Bestimmte Funktionen erfordern die Erteilung spezifischer Berechtigungen. Audiofunktionen wie Telefonklingeln, Vibration und Benachrichtigungen können durch den Nicht-stören- oder Lautlos-Modus stummgeschaltet werden \u2014 aktivieren Sie die Option DND umgehen in den Einstellungen. Bitte reichen Sie Funktionswünsche und Fehlerberichte über das Fehlerbericht-Menü ein.",
            "Se aplican limitaciones específicas de firmware y hardware del dispositivo. Algunas funciones pueden no estar disponibles o no funcionar correctamente según el tipo de dispositivo, el fabricante y la versión de Android. Ciertas funciones requieren permisos específicos para operar. Las funciones de audio como el timbre, la vibración y las notificaciones pueden ser silenciadas por el modo No molestar o Silencio \u2014 active la opción Omitir DND en Configuración. Envíe solicitudes de funciones e informes de errores a través del menú de informes de errores.",
            "Des limitations spécifiques au micrologiciel et au matériel de l'appareil s'appliquent. Certaines fonctionnalités peuvent ne pas être disponibles ou ne pas fonctionner correctement selon le type d'appareil, le fabricant et la version Android. Certaines fonctionnalités nécessitent des autorisations spécifiques pour fonctionner. Les fonctions audio telles que la sonnerie, la vibration et les notifications peuvent être réduites au silence par le mode Ne pas déranger ou Silencieux \u2014 activez l'option Contourner NPD dans les Paramètres. Veuillez soumettre vos demandes de fonctionnalités et rapports de bugs via le menu Rapport de bug.",
        )
        val systemModesTitle get() = m(l, "System Modes", "Systemmodi", "Modos del sistema", "Modes syst\u00E8me")
        val modeLabel get() = m(l, "Mode", "Modus", "Modo", "Mode")
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
        val apply get() = m(l, "Apply", "Anwenden", "Aplicar", "Appliquer")
        val add get() = m(l, "Add", "Hinzufügen", "Añadir", "Ajouter")
        val sort get() = m(l, "Sort", "Sortieren", "Ordenar", "Trier")
        val none get() = m(l, "None", "Keine", "Ninguno", "Aucun")
        val next get() = m(l, "Next", "Weiter", "Siguiente", "Suivant")
        val selectTime get() = m(l, "Select Time", "Zeit auswählen", "Seleccionar hora", "Sélectionner l'heure")
        val selectDate get() = m(l, "Select Date", "Datum auswählen", "Seleccionar fecha", "Sélectionner la date")
        val deleteAll get() = m(l, "Delete all", "Alle löschen", "Eliminar todo", "Tout supprimer")
        val clearAll get() = m(l, "Clear all", "Alle löschen", "Borrar todo", "Tout effacer")
    }

    // ── Accessibility ──────────────────────────────────────────────────
    val accessibility: Accessibility @Composable get() = Accessibility(lang)

    class Accessibility(private val l: Language) {
        // Screen announcements
        val dashboardScreen get() = m(l, "Dashboard screen", "Dashboard-Bildschirm", "Pantalla del panel", "Écran du tableau de bord")
        val toolsScreen get() = m(l, "Tools screen", "Werkzeuge-Bildschirm", "Pantalla de herramientas", "Écran des outils")
        val monitorScreen get() = m(l, "Monitor screen", "Monitor-Bildschirm", "Pantalla de monitor", "Écran du moniteur")
        val logbookScreen get() = m(l, "Logbook screen", "Logbuch-Bildschirm", "Pantalla del registro", "Écran du journal")
        val moreScreen get() = m(l, "More screen", "Mehr-Bildschirm", "Pantalla de más opciones", "Écran plus")
        val torchScreen get() = m(l, "Torch screen", "Taschenlampe-Bildschirm", "Pantalla de linterna", "Écran lampe torche")
        val cameraScreen get() = m(l, "Camera screen", "Kamera-Bildschirm", "Pantalla de cámara", "Écran caméra")
        val vibrationScreen get() = m(l, "Vibration screen", "Vibrations-Bildschirm", "Pantalla de vibración", "Écran vibration")
        val micScreen get() = m(l, "Microphone screen", "Mikrofon-Bildschirm", "Pantalla de micrófono", "Écran microphone")
        val sensorsScreen get() = m(l, "Sensors screen", "Sensoren-Bildschirm", "Pantalla de sensores", "Écran capteurs")
        val batteryScreen get() = m(l, "Battery screen", "Akku-Bildschirm", "Pantalla de batería", "Écran batterie")
        val radiosScreen get() = m(l, "Radios screen", "Funk-Bildschirm", "Pantalla de radios", "Écran radios")
        val lockScreen get() = m(l, "Notifications screen", "Benachrichtigungs-Bildschirm", "Pantalla de notificaciones", "Écran notifications")
        val linkScreen get() = m(l, "Link automation screen", "Link-Automatisierungs-Bildschirm", "Pantalla de automatización", "Écran automatisation")
        val fileMetaScreen get() = m(l, "File metadata screen", "Datei-Metadaten-Bildschirm", "Pantalla de metadatos", "Écran métadonnées")
        val settingsScreen get() = m(l, "Settings screen", "Einstellungs-Bildschirm", "Pantalla de ajustes", "Écran paramètres")
        val bugReportScreen get() = m(l, "Bug report screen", "Fehlerbericht-Bildschirm", "Pantalla de informe de errores", "Écran rapport de bug")

        // Settings section
        val accessibilityTitle get() = m(l, "Accessibility", "Barrierefreiheit", "Accesibilidad", "Accessibilité")
        val highContrast get() = m(l, "High contrast mode", "Hochkontrastmodus", "Modo de alto contraste", "Mode contraste élevé")
        val highContrastDesc get() = m(l, "Increases color contrast for better visibility", "Erhöht den Farbkontrast für bessere Sichtbarkeit", "Aumenta el contraste de color para mejor visibilidad", "Augmente le contraste des couleurs pour une meilleure visibilité")
        val largeText get() = m(l, "Large text", "Großer Text", "Texto grande", "Grand texte")
        val largeTextDesc get() = m(l, "Increases text size throughout the app", "Vergrößert die Textgröße in der gesamten App", "Aumenta el tamaño del texto en toda la aplicación", "Augmente la taille du texte dans toute l'application")
        val reducedMotion get() = m(l, "Reduce motion", "Bewegung reduzieren", "Reducir movimiento", "Réduire les animations")
        val reducedMotionDesc get() = m(l, "Minimizes animations and motion effects", "Minimiert Animationen und Bewegungseffekte", "Minimiza las animaciones y efectos de movimiento", "Minimise les animations et effets de mouvement")

        // State descriptions
        val torchOn get() = m(l, "Torch is on", "Taschenlampe ist an", "Linterna encendida", "Lampe allumée")
        val torchOff get() = m(l, "Torch is off", "Taschenlampe ist aus", "Linterna apagada", "Lampe éteinte")
        val recording get() = m(l, "Recording", "Aufnahme", "Grabando", "Enregistrement")
        val notRecording get() = m(l, "Not recording", "Keine Aufnahme", "Sin grabar", "Pas d'enregistrement")
        val enabled get() = m(l, "Enabled", "Aktiviert", "Activado", "Activé")
        val disabled get() = m(l, "Disabled", "Deaktiviert", "Desactivado", "Désactivé")
        val expanded get() = m(l, "Expanded", "Erweitert", "Expandido", "Développé")
        val collapsed get() = m(l, "Collapsed", "Eingeklappt", "Contraído", "Réduit")
        val flashAvailable get() = m(l, "Flash available", "Blitz verfügbar", "Flash disponible", "Flash disponible")
        val flashUnavailable get() = m(l, "Flash unavailable", "Blitz nicht verfügbar", "Flash no disponible", "Flash non disponible")

        // Chart descriptions
        fun sensorChartDesc(name: String, values: String) = m(l,
            "Live chart for $name showing $values",
            "Live-Diagramm für $name zeigt $values",
            "Gráfico en vivo de $name mostrando $values",
            "Graphique en direct de $name affichant $values")
        fun batteryChartDesc(current: String) = m(l,
            "Battery current draw chart, current value $current milliamps",
            "Batteriestrom-Diagramm, aktueller Wert $current Milliampere",
            "Gráfico de corriente de batería, valor actual $current miliamperios",
            "Graphique de courant de batterie, valeur actuelle $current milliampères")
        fun spectrumDesc(peakHz: String) = m(l,
            "Audio spectrum analyzer, peak frequency $peakHz hertz",
            "Audio-Spektrumanalysator, Spitzenfrequenz $peakHz Hertz",
            "Analizador de espectro de audio, frecuencia pico $peakHz hercios",
            "Analyseur de spectre audio, fréquence de pointe $peakHz hertz")
        fun dbMeterDesc(db: String) = m(l,
            "Sound level meter reading $db decibels",
            "Schallpegelmesser zeigt $db Dezibel",
            "Medidor de nivel sonoro leyendo $db decibelios",
            "Sonomètre indiquant $db décibels")
        fun waveformDesc() = m(l,
            "Audio waveform history visualization",
            "Audio-Wellenform-Verlaufsvisualisierung",
            "Visualización del historial de forma de onda de audio",
            "Visualisation de l'historique de la forme d'onde audio")
        fun vibrationCanvasDesc() = m(l,
            "Vibration pattern drawing canvas",
            "Vibrationsmuster-Zeichnungsfläche",
            "Lienzo de dibujo de patrón de vibración",
            "Canevas de dessin de motif de vibration")
        fun batteryLevelDesc(level: Int, status: String) = m(l,
            "Battery at $level percent, $status",
            "Akku bei $level Prozent, $status",
            "Batería al $level por ciento, $status",
            "Batterie à $level pour cent, $status")

        // Navigation
        val skipToContent get() = m(l, "Skip to content", "Zum Inhalt springen", "Saltar al contenido", "Aller au contenu")

        // Actions
        val expandSection get() = m(l, "Expand section", "Abschnitt erweitern", "Expandir sección", "Développer la section")
        val collapseSection get() = m(l, "Collapse section", "Abschnitt einklappen", "Contraer sección", "Réduire la section")
        val playRecording get() = m(l, "Play recording", "Aufnahme abspielen", "Reproducir grabación", "Lire l'enregistrement")
        val stopPlayback get() = m(l, "Stop playback", "Wiedergabe stoppen", "Detener reproducción", "Arrêter la lecture")
        val removeStep get() = m(l, "Remove step", "Schritt entfernen", "Eliminar paso", "Supprimer l'étape")
        val copyReadings get() = m(l, "Copy all sensor readings to clipboard", "Alle Sensorwerte in die Zwischenablage kopieren", "Copiar todas las lecturas del sensor al portapapeles", "Copier toutes les lectures des capteurs dans le presse-papiers")
        val startRecording get() = m(l, "Start recording", "Aufnahme starten", "Iniciar grabación", "Démarrer l'enregistrement")
        val stopRecording get() = m(l, "Stop recording", "Aufnahme stoppen", "Detener grabación", "Arrêter l'enregistrement")
        val loadPattern get() = m(l, "Load pattern", "Muster laden", "Cargar patrón", "Charger le motif")
        val deletePattern get() = m(l, "Delete pattern", "Muster löschen", "Eliminar patrón", "Supprimer le motif")
        val permissionGranted get() = m(l, "Granted", "Erteilt", "Concedido", "Accordée")
        val permissionDenied get() = m(l, "Denied", "Verweigert", "Denegado", "Refusée")
    }

    // ── Widget / Service toasts (non-Composable context) ─────────────────
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

        // Flashlight widget
        fun torchOn(lang: Language) = m(lang, "Torch ON", "Taschenlampe AN", "Linterna encendida", "Lampe allumée")
        fun torchOff(lang: Language) = m(lang, "Torch OFF", "Taschenlampe AUS", "Linterna apagada", "Lampe éteinte")
        fun noFlashAvailable(lang: Language) = m(lang, "No flash available", "Kein Blitz verfügbar", "Sin flash disponible", "Pas de flash disponible")

        // Strobe widget
        fun strobeStarted(lang: Language) = m(lang, "Strobe started", "Stroboskop gestartet", "Estrobo iniciado", "Stroboscope démarré")
        fun strobeStopped(lang: Language) = m(lang, "Strobe stopped", "Stroboskop gestoppt", "Estrobo detenido", "Stroboscope arrêté")

        // Vibration widget
        fun vibrationOn(lang: Language) = m(lang, "Vibration ON", "Vibration AN", "Vibración encendida", "Vibration activée")
        fun vibrationOff(lang: Language) = m(lang, "Vibration OFF", "Vibration AUS", "Vibración apagada", "Vibration désactivée")

        // Widget config
        fun chooseMetric(lang: Language) = m(lang, "Choose a Metric", "Metrik auswählen", "Elegir una métrica", "Choisir une métrique")
        fun unit(lang: Language) = m(lang, "Unit", "Einheit", "Unidad", "Unité")
    }

    // ── Service notifications (non-Composable context) ─────────────────
    object Services {
        // Vibration service
        fun vibrationActive(lang: Language) = m(lang, "Vibration Active", "Vibration aktiv", "Vibración activa", "Vibration active")

        // Strobe service
        fun strobeActive(lang: Language) = m(lang, "Strobe Active", "Stroboskop aktiv", "Estrobo activo", "Stroboscope actif")
        fun tapToStop(lang: Language) = m(lang, "Tap to stop", "Tippen zum Stoppen", "Toque para detener", "Appuyez pour arrêter")

        // Link service
        fun linkMonitoring(lang: Language) = m(lang, "Link Monitoring Active", "Link-Überwachung aktiv", "Monitoreo de enlaces activo", "Surveillance des liens active")
        fun linkTapToStop(lang: Language) = m(lang, "Evaluating link rules", "Link-Regeln werden ausgewertet", "Evaluando reglas de enlace", "Évaluation des règles de lien")
        fun linkTriggered(lang: Language, name: String) = m(lang, "Link triggered: $name", "Link ausgelöst: $name", "Enlace activado: $name", "Lien déclenché : $name")

        // Voice record service
        fun recordingAudio(lang: Language) = m(lang, "Recording Audio", "Audioaufnahme", "Grabando audio", "Enregistrement audio")
        fun tapToStopSave(lang: Language) = m(lang, "Tap to stop and save", "Tippen zum Stoppen und Speichern", "Toque para detener y guardar", "Appuyez pour arrêter et enregistrer")
        fun micInitFailed(lang: Language) = m(lang, "Mic init failed", "Mikrofon-Initialisierung fehlgeschlagen", "Error al inicializar el micrófono", "Échec d'initialisation du micro")
        fun savedFile(lang: Language, name: String) = m(lang, "Saved: $name", "Gespeichert: $name", "Guardado: $name", "Enregistré : $name")
        fun nothingRecorded(lang: Language) = m(lang, "Nothing recorded", "Nichts aufgenommen", "Nada grabado", "Rien enregistré")

        // dB Meter service
        fun dbMeterActive(lang: Language) = m(lang, "dB Meter Active", "dB-Messgerät aktiv", "Medidor dB activo", "Mètre dB actif")
        fun monitoringMicLevel(lang: Language) = m(lang, "Monitoring microphone level", "Mikrofonpegel wird überwacht", "Monitoreando nivel del micrófono", "Surveillance du niveau du microphone")

        // Video record service
        fun recordingVideo(lang: Language) = m(lang, "Recording Video", "Video wird aufgenommen", "Grabando video", "Enregistrement vidéo")

        // Hardware service
        fun hardwareMonitoring(lang: Language) = m(lang, "Hardware monitoring is running in the background.", "Hardware-Überwachung läuft im Hintergrund.", "La monitorización de hardware se ejecuta en segundo plano.", "La surveillance matérielle fonctionne en arrière-plan.")
        fun stop(lang: Language) = m(lang, "Stop", "Stopp", "Parar", "Arrêter")

        // Admin receiver
        fun deviceAdminEnabled(lang: Language) = m(lang, "Gadget Device Admin: enabled", "Gadget Geräteadministrator: aktiviert", "Gadget Administrador del dispositivo: habilitado", "Gadget Administrateur de l'appareil : activé")
        fun deviceAdminDisabled(lang: Language) = m(lang, "Gadget Device Admin: disabled", "Gadget Geräteadministrator: deaktiviert", "Gadget Administrador del dispositivo: deshabilitado", "Gadget Administrateur de l'appareil : désactivé")

        // Sensors
        fun sensorReadingsCopied(lang: Language) = m(lang, "Sensor readings copied!", "Sensorwerte kopiert!", "¡Lecturas de sensores copiadas!", "Lectures de capteurs copiées !")

        // Logbook reminder
        fun checkpointTitle(lang: Language, name: String) = m(lang, "Checkpoint: $name", "Prüfpunkt: $name", "Punto de control: $name", "Point de contrôle : $name")
        fun checkpointDue(lang: Language, procName: String, cpName: String) = m(lang,
            "Process \"$procName\" \u2014 $cpName is due now.",
            "Prozess \"$procName\" \u2014 $cpName ist jetzt fällig.",
            "Proceso \"$procName\" \u2014 $cpName vence ahora.",
            "Processus \"$procName\" \u2014 $cpName est dû maintenant.")
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
