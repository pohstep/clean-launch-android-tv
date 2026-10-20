package app.cleanlaunch.tv

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cleanlaunch.tv.data.AppCatalog
import app.cleanlaunch.tv.data.ClausePref
import app.cleanlaunch.tv.data.EveningStore
import app.cleanlaunch.tv.data.ExampleSources
import app.cleanlaunch.tv.data.ForecastClient
import app.cleanlaunch.tv.data.FrontPref
import app.cleanlaunch.tv.data.Headline
import app.cleanlaunch.tv.data.HeadlineClient
import app.cleanlaunch.tv.data.HomeApp
import app.cleanlaunch.tv.data.IndexFile
import app.cleanlaunch.tv.data.InstalledApp
import app.cleanlaunch.tv.data.LiveForecast
import app.cleanlaunch.tv.data.MarketHours
import app.cleanlaunch.tv.data.PhotoEntry
import app.cleanlaunch.tv.data.PhotoFolder
import app.cleanlaunch.tv.data.PhotoLibrary
import app.cleanlaunch.tv.data.ProviderLine
import app.cleanlaunch.tv.data.Quote
import app.cleanlaunch.tv.data.QuoteClient
import app.cleanlaunch.tv.data.SlotPhotos
import app.cleanlaunch.tv.data.SkyMark
import app.cleanlaunch.tv.data.TickerList
import app.cleanlaunch.tv.input.InputSwitcher
import app.cleanlaunch.tv.input.SourceSlot
import app.cleanlaunch.tv.location.IpFix
import app.cleanlaunch.tv.location.IpLocator
import app.cleanlaunch.tv.location.LocationCheck
import app.cleanlaunch.tv.location.LocationResolve
import app.cleanlaunch.tv.location.Places
import app.cleanlaunch.tv.location.ResolvedPlace
import app.cleanlaunch.tv.nav.BackState
import app.cleanlaunch.tv.nav.reduceBack
import app.cleanlaunch.tv.sentence.CustomCopy
import app.cleanlaunch.tv.sentence.FrontPage
import app.cleanlaunch.tv.sentence.LineRole
import app.cleanlaunch.tv.sentence.PageLine
import app.cleanlaunch.tv.data.Shelf
import app.cleanlaunch.tv.sentence.SentenceComposer
import app.cleanlaunch.tv.theme.ReviewLayout
import app.cleanlaunch.tv.sentence.SentenceData
import app.cleanlaunch.tv.theme.Design
import app.cleanlaunch.tv.theme.DesignKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

enum class Overlay { None, Settings, Editor, Picker }

data class UiState(
    val schemeId: String,
    val previewSchemeId: String?,
    val dateLabel: String,
    val dateChinese: String = "",
    val timeLabel: String,
    val sentence: SentenceData,
    val clauses: List<ClausePref>,
    val templateMode: Boolean,
    val template: String,
    val customText: String,
    val apps: List<HomeApp>,
    val hidden: List<HomeApp>,
    val installed: List<InstalledApp>,
    val homeAppId: String?,
    val sources: List<SourceSlot>,
    val idleMinutes: Int,
    val plate: Boolean,
    val dimmed: Boolean,
    val overlay: Overlay,
    val menuAppId: String?,
    val movingClauseId: String?,
    val status: String?,
    val focusToken: Int,
    val focusZone: String,
    val focusedAppId: String?,
    val providerLines: List<ProviderLine>,
    val showAv: Boolean,
    val showTuner: Boolean,
    val front: List<FrontPref>,
    val customLines: List<CustomCopy>,
    val link: String,
    val pageLines: List<app.cleanlaunch.tv.sentence.PageLine>,
    val weatherShort: String,
    val place: ResolvedPlace,
    val locationMode: String,
    val manualCity: String,
    val locationNote: String,
    val zoneCity: String,
    val debugFocus: String? = null,
    val forecast: LiveForecast? = null,
    val quotes: List<Quote> = emptyList(),
    val headlines: List<Headline> = emptyList(),
    val tickers: String = EveningStore.DEFAULT_TICKERS,
    val wave: Boolean = true,
    val foreground: Boolean = true,
    val sky: SkyMark? = null,
    val hour: Int = 12,
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean = false,
    val volume: Int = 0,
    val volumeMax: Int = 1,
    val volumeToken: Int = 0,
    val activeSourceId: String? = null,
    val photoMode: String = "off",
    val photoMinutes: Int = 15,
    val photoSeconds: Int = 20,
    val photos: List<PhotoEntry> = emptyList(),
    val photoFixedId: String = "",
    val liftingPhotoId: String? = null,
    val photoReadable: Boolean = false,
    val settingsSection: String = "inputs",
    val settingsInPanel: Boolean = false,
    val settingsFocusAppId: String? = null,
    val movingAppId: String? = null,
    val renameKind: String? = null,
    val renameId: String? = null,
    val showAllApps: Boolean = false,
    val tickerChinese: Boolean = false,
    val frontPhoto: String = "",
    val slotName: String = "",
    val slotCount: Int = 0,
    val slotToken: Int = 0,
    val slotSeconds: Int = SlotPhotos.DEFAULT_SECONDS,
    /** App row to restore after a pin menu action, so focus does not fall onto the section list. */
    val focusHoldId: String? = null,
)

class EveningViewModel(app: Application) : AndroidViewModel(app) {
    val design: Design = DesignKit.load(app)
    private val store = EveningStore(app)
    private val switcher = InputSwitcher(app)
    private val dateFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.UK)
    private val chineseDate = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.TRADITIONAL_CHINESE)
    private val timeFormat = DateTimeFormatter.ofPattern("H:mm", Locale.UK)
    private var lastInputMs = SystemClock.elapsedRealtime()
    private var statusUntil = 0L
    private var minute = ""
    private var demo = false
    private var slotDeck = SlotPhotos.Deck(emptyList(), 0)
    private val slotRandom = kotlin.random.Random(System.currentTimeMillis())
    private var slotShownAt = SystemClock.elapsedRealtime()
    private var slotPeriodOverrideMs: Long? = null
    private var longPage = false
    private var skyForce: SkyMark? = null
    private var volumeSeen = false
    private var lookupRunning = false
    private var photoScan: kotlinx.coroutines.Job? = null
    var launchPhotoPicker: (() -> Unit)? = null
    var pickingFrontPhoto = false
    var askPhotoRead: (() -> Unit)? = null
    private val photoObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refreshPhotoFolder()
        }
    }

    @Volatile private var quoteDue = 0L
    @Volatile private var newsDue = 0L
    @Volatile private var weatherDue = 0L
    private val quoteGen = AtomicInteger()
    private val newsGen = AtomicInteger()
    private val weatherGen = AtomicInteger()
    private var lastIndexJson: String? = null
    private val indexGeneration = AtomicInteger()
    private val indexLock = Any()

    var state = initial()
        private set

    private val listeners = mutableListOf<(UiState) -> Unit>()

    fun observe(listener: (UiState) -> Unit): () -> Unit {
        listeners += listener
        listener(state)
        return {
            listeners -= listener
        }
    }

    init {
        if (store.layoutVersion < 2) store.layoutVersion = 2
        refreshSlotPhotos()
        refreshApps()
        refreshInputs()
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                tick()
            }
        }
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val visible = withContext(Dispatchers.Main) { state.foreground }
                val now = System.currentTimeMillis()
                if (visible) {
                    if (now >= weatherDue) pullWeather()
                    if (now >= quoteDue) pullQuotes()
                    if (now >= newsDue) pullNews()
                }
                delay(5_000)
            }
        }
        runCatching {
            getApplication<Application>().contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                photoObserver,
            )
        }
        refreshPhotoFolder()
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(photoObserver) }
        super.onCleared()
    }

    fun setForeground(on: Boolean) {
        if (state.foreground == on) return
        state = state.copy(foreground = on)
        publish()
    }

    fun onKeyWhileDimmed(): Boolean {
        if (!state.dimmed) return false
        state = state.copy(
            dimmed = false,
            overlay = Overlay.None,
            menuAppId = null,
            movingClauseId = null,
            previewSchemeId = null,
            focusToken = state.focusToken + 1,
        )
        publish()
        noteInput()
        advanceSlot("wake")
        return true
    }

    fun noteInput() {
        lastInputMs = SystemClock.elapsedRealtime()
    }

    fun onHomeIntent() {
        state = state.copy(
            overlay = Overlay.None,
            menuAppId = null,
            movingClauseId = null,
            previewSchemeId = null,
            dimmed = false,
            focusToken = state.focusToken + 1,
        )
        publish()
    }

    fun applyDebugScheme(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        if (intent.getStringExtra("evening_long") == "1") longPage = true
        skyForce = when (intent.getStringExtra("evening_sky")) {
            "sun" -> SkyMark.SUN
            "rain" -> SkyMark.DOWNPOUR
            "cloud" -> SkyMark.CLOUD
            "drizzle" -> SkyMark.DRIZZLE
            "storm" -> SkyMark.STORM
            "snow" -> SkyMark.SNOW
            "fog" -> SkyMark.FOG
            else -> skyForce
        }
        val forcedPhoto = intent.getStringExtra("evening_photo")?.takeIf { it in PhotoLibrary.modes }
        val forcedSource = intent.getStringExtra("evening_source")?.takeIf { it.isNotBlank() }
        val raw = intent.getStringExtra("evening_scheme")
        if (raw != null) {
            val id = raw
            if (design.schemes.any { it.id == id }) commitScheme(id)
        }
        var next = state
        if (intent.getStringExtra("evening_demo") == "1") {
            demo = true
            val fixed = LocalDateTime.of(2026, 10, 20, 21, 40)
            minute = "21:40"
            next = rebuildSentence(
                next.copy(
                    timeLabel = "21:40",
                    dateLabel = fixed.format(dateFormat),
                    dateChinese = fixed.format(chineseDate),
                ),
            )
        }
        val focus = intent.getStringExtra("evening_focus")
        if (!focus.isNullOrBlank()) {
            next = next.copy(
                debugFocus = focus,
                focusToken = next.focusToken + 1,
                homeAppId = if (focus == "netflix") "netflix" else next.homeAppId,
                overlay = Overlay.None,
                dimmed = false,
                menuAppId = null,
            )
        }
        if (intent.hasExtra("evening_scheme") || intent.hasExtra("evening_focus") || intent.hasExtra("evening_dim")) {
            next = next.copy(
                dimmed = intent.getStringExtra("evening_dim") == "1",
                overlay = Overlay.None,
                menuAppId = null,
            )
        }
        if (forcedPhoto != null) {
            store.photoMode = forcedPhoto
            next = next.copy(photoMode = forcedPhoto)
            refreshPhotoFolder()
        }
        if (forcedSource != null) {
            store.lastSourceId = forcedSource
            next = next.copy(activeSourceId = forcedSource)
        }
        if (intent.getStringExtra("evening_page") == "settings") {
            next = next.copy(overlay = Overlay.Settings, dimmed = false, menuAppId = null, renameKind = null)
        }
        intent.getStringExtra("evening_slot_ms")?.toLongOrNull()?.let { slotPeriodOverrideMs = it }
        if (next != state) {
            state = next
            publish()
        }
    }

    fun onBack() {
        val result = reduceBack(
            BackState(
                dimmed = state.dimmed,
                menu = state.menuAppId != null,
                overlay = state.overlay != Overlay.None,
                onLastUsed = state.focusZone == "app" &&
                    state.focusedAppId != null &&
                    state.focusedAppId == state.homeAppId,
                rename = state.renameKind != null,
                moving = state.movingAppId != null,
                allApps = state.showAllApps,
                settingsPanel = state.overlay == Overlay.Settings && state.settingsInPanel,
            ),
        )
        state = state.copy(
            dimmed = result.dimmed,
            menuAppId = if (result.menu) state.menuAppId else null,
            movingAppId = if (result.moving) state.movingAppId else null,
            renameKind = if (result.rename) state.renameKind else null,
            renameId = if (result.rename) state.renameId else null,
            showAllApps = result.allApps,
            settingsInPanel = result.settingsPanel,
            overlay = if (result.overlay) state.overlay else Overlay.None,
            previewSchemeId = if (result.overlay) state.previewSchemeId else null,
            movingClauseId = if (result.overlay) state.movingClauseId else null,
            liftingPhotoId = if (result.overlay) state.liftingPhotoId else null,
            focusToken = if (result.focusHome) state.focusToken + 1 else state.focusToken,
            settingsFocusAppId = if (!result.menu && state.menuAppId != null) state.menuAppId else state.settingsFocusAppId,
        )
        publish()
    }

    fun reportFocus(zone: String, appId: String?) {
        val focused = if (zone == "app") appId else state.focusedAppId
        if (state.focusZone == zone && state.focusedAppId == focused) return
        state = state.copy(focusZone = zone, focusedAppId = focused)
    }

    fun openSettings(section: String? = null, appId: String? = null) {
        refreshApps()
        refreshInputs()
        val next = section ?: store.settingsSection
        store.settingsSection = next
        state = state.copy(
            overlay = Overlay.Settings,
            menuAppId = null,
            previewSchemeId = state.schemeId,
            settingsSection = next,
            settingsInPanel = appId != null,
            settingsFocusAppId = appId,
            showAllApps = false,
            movingAppId = null,
            renameKind = null,
        )
        publish()
    }

    fun selectSettings(section: String) {
        store.settingsSection = section
        state = state.copy(settingsSection = section, settingsInPanel = false, menuAppId = null, movingAppId = null)
        publish()
    }

    fun enterSettingsPanel() {
        if (state.settingsInPanel) return
        state = state.copy(settingsInPanel = true)
        publish()
    }

    fun noteSettingsMenu() {
        if (!state.settingsInPanel && state.menuAppId == null) return
        state = state.copy(settingsInPanel = false, menuAppId = null, movingAppId = null)
        publish()
    }

    fun consumeSettingsFocus() {
        if (state.settingsFocusAppId == null) return
        state = state.copy(settingsFocusAppId = null)
    }

    fun openAllApps() {
        state = state.copy(showAllApps = true, menuAppId = null)
        publish()
    }

    fun beginRename(kind: String, id: String?) {
        state = state.copy(renameKind = kind, renameId = id, menuAppId = null)
        publish()
    }

    fun cancelRename() {
        state = state.copy(renameKind = null, renameId = null)
        publish()
    }

    fun commitRename(value: String) {
        when (state.renameKind) {
            "app" -> state.renameId?.let { setAppName(it, value) }
            "input" -> state.renameId?.let { setInputName(it, value) }
            "tickers" -> setTickers(value)
            "city" -> setManualCity(value)
            "custom-en" -> state.renameId?.toIntOrNull()?.let { setCustomEnglish(it, value) }
            "custom-zh" -> state.renameId?.toIntOrNull()?.let { setCustomChinese(it, value) }
        }
        state = state.copy(renameKind = null, renameId = null)
        publish()
    }

    fun openEditor() {
        state = state.copy(overlay = Overlay.Editor, menuAppId = null, movingClauseId = null)
        publish()
    }

    fun openPicker() {
        refreshApps()
        state = state.copy(overlay = Overlay.Picker)
        publish()
    }

    fun closePicker() {
        state = state.copy(overlay = Overlay.Settings)
        publish()
    }

    fun previewScheme(id: String) {
        if (state.previewSchemeId == id) return
        state = state.copy(previewSchemeId = id)
        publish()
    }

    fun commitScheme(id: String) {
        store.schemeId = id
        state = state.copy(schemeId = id, previewSchemeId = id)
        publish()
    }

    fun setIdle(minutes: Int) {
        store.idleMinutes = minutes
        state = state.copy(idleMinutes = minutes)
        publish()
    }

    fun cycleIdle() {
        val options = ReviewLayout.idleChoices
        val index = options.indexOf(state.idleMinutes)
        val next = options[(if (index < 0) 0 else index + 1) % options.size]
        setIdle(next)
    }

    fun setPlate(on: Boolean) {
        store.plate = on
        state = state.copy(plate = on)
        publish()
    }

    fun setShowAv(on: Boolean) {
        store.showAv = on
        state = state.copy(showAv = on)
        publish()
        refreshInputs()
    }

    fun setShowTuner(on: Boolean) {
        store.showTuner = on
        state = state.copy(showTuner = on)
        publish()
        refreshInputs()
    }

    fun setLocationMode(mode: String) {
        store.locationMode = mode
        state = applyPlace(state.copy(locationMode = mode))
        publish()
        if (mode == "auto") refreshIp()
    }

    fun setManualCity(city: String) {
        val text = city.trim()
        if (text.isEmpty()) {
            store.manualCity = ""
            store.locationMode = "auto"
            state = applyPlace(state.copy(manualCity = "", locationMode = "auto"))
            publish()
            refreshIp()
            return
        }
        store.manualCity = text
        store.locationMode = "manual"
        state = applyPlace(state.copy(manualCity = text, locationMode = "manual"))
        publish()
    }

    fun toggleFront(id: String) {
        val next = state.front.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        store.setFront(next)
        state = rebuildSentence(state.copy(front = next))
        publish()
    }

    fun moveFront(id: String, direction: Int) {
        val list = state.front.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        store.setFront(list)
        state = rebuildSentence(state.copy(front = list))
        publish()
    }

    fun setCustomEnglish(index: Int, text: String) {
        if (index !in state.customLines.indices) return
        store.setCustomEnglish(index, text)
        val lines = state.customLines.toMutableList()
        lines[index] = lines[index].copy(english = text)
        state = rebuildSentence(state.copy(customLines = lines))
        publish()
    }

    fun setCustomChinese(index: Int, text: String) {
        if (index !in state.customLines.indices) return
        store.setCustomChinese(index, text)
        val lines = state.customLines.toMutableList()
        lines[index] = lines[index].copy(chinese = text)
        state = rebuildSentence(state.copy(customLines = lines))
        publish()
    }

    fun setTickers(raw: String) {
        store.tickers = raw
        val symbols = TickerList.parse(raw)
        val allowed = symbols.toSet()
        val front = state.front.map { pref ->
            if (pref.id == "stocks") pref.copy(enabled = symbols.isNotEmpty()) else pref
        }
        if (front != state.front) store.setFront(front)
        quoteGen.incrementAndGet()
        quoteDue = 0L
        state = rebuildSentence(
            state.copy(
                tickers = raw,
                quotes = state.quotes.filter { it.symbol in allowed },
                front = front,
            ),
        )
        publish()
        viewModelScope.launch(Dispatchers.IO) { pullQuotes() }
    }

    fun setWave(on: Boolean) {
        store.wave = on
        state = state.copy(wave = on)
        publish()
    }

    fun cyclePhotoMode() {
        val next = PhotoLibrary.nextMode(state.photoMode)
        store.photoMode = next
        state = state.copy(photoMode = next, liftingPhotoId = null)
        publish()
    }

    fun cyclePhotoMinutes() {
        val next = PhotoLibrary.nextIn(PhotoLibrary.rotationMinutes, state.photoMinutes)
        store.photoMinutes = next
        state = state.copy(photoMinutes = next)
        publish()
    }

    fun cyclePhotoSeconds() {
        val next = PhotoLibrary.nextIn(PhotoLibrary.carouselSeconds, state.photoSeconds)
        store.photoSeconds = next
        state = state.copy(photoSeconds = next)
        publish()
    }

    fun choosePhoto(id: String) {
        if (state.photos.none { it.id == id }) return
        store.photoFixedId = id
        state = state.copy(photoFixedId = id)
        publish()
    }

    fun liftPhoto(id: String) {
        state = state.copy(liftingPhotoId = id)
        publish()
    }

    fun dropPhoto() {
        state = state.copy(liftingPhotoId = null)
        publish()
    }

    fun movePhoto(id: String, direction: Int) {
        val next = PhotoLibrary.reorder(state.photos, id, direction)
        store.savePhotos(next)
        state = state.copy(photos = next)
        publish()
    }

    fun removePhoto(id: String) {
        val entry = state.photos.find { it.id == id } ?: return
        val app = getApplication<Application>()
        if (entry.source == "folder") store.hidePhoto(id) else PhotoFolder.release(app, entry.uri)
        val next = state.photos.filter { it.id != id }
        store.savePhotos(next)
        val fixed = if (state.photoFixedId == id) next.firstOrNull()?.id.orEmpty() else state.photoFixedId
        if (fixed != state.photoFixedId) store.photoFixedId = fixed
        state = state.copy(photos = next, photoFixedId = fixed, liftingPhotoId = null)
        publish()
    }

    fun addPickedPhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        val incoming = uris.map { uri ->
            PhotoEntry(
                id = PhotoLibrary.pickId(uri.toString()),
                uri = uri.toString(),
                name = PhotoFolder.displayName(app, uri),
                source = "picker",
            )
        }
        val next = PhotoLibrary.addPicked(state.photos, incoming)
        store.savePhotos(next)
        val fixed = state.photoFixedId.ifBlank { next.firstOrNull()?.id.orEmpty() }
        if (state.photoFixedId.isBlank() && fixed.isNotBlank()) store.photoFixedId = fixed
        state = state.copy(photos = next, photoFixedId = fixed)
        publish()
        Log.i(TAG, "Photos picked ${incoming.size}")
    }

    fun openPhotoPicker() {
        pickingFrontPhoto = false
        launchPhotoPicker?.invoke()
    }

    fun openFrontPhotoPicker() {
        pickingFrontPhoto = true
        launchPhotoPicker?.invoke()
    }

    fun setFrontPhoto(uri: String) {
        store.frontPhoto = uri
        state = state.copy(frontPhoto = uri)
        publish()
    }

    fun clearFrontPhoto() {
        setFrontPhoto("")
    }

    fun takePickedPhotos(uris: List<Uri>): Boolean {
        if (!pickingFrontPhoto) return false
        pickingFrontPhoto = false
        uris.firstOrNull()?.let { setFrontPhoto(it.toString()) }
        return true
    }

    fun photoReadAsked(): Boolean = store.photoAsked

    fun requestPhotoRead() {
        if (PhotoFolder.canRead(getApplication())) {
            state = state.copy(photoReadable = true)
            publish()
            refreshPhotoFolder()
        } else {
            store.photoAsked = true
            askPhotoRead?.invoke()
        }
    }

    fun onPhotoAccess(granted: Boolean) {
        store.photoAsked = true
        state = state.copy(photoReadable = granted)
        publish()
        if (granted) refreshPhotoFolder()
    }

    fun refreshPhotoFolder() {
        val app = getApplication<Application>()
        val readable = PhotoFolder.canRead(app)
        if (readable != state.photoReadable) {
            state = state.copy(photoReadable = readable)
            publish()
        }
        if (!readable) return
        photoScan?.cancel()
        photoScan = viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            val found = PhotoFolder.scan(app)
            val merged = PhotoLibrary.merge(store.photos(), found, store.photoHidden())
            withContext(Dispatchers.Main) {
                store.savePhotos(merged)
                val fixed = state.photoFixedId.ifBlank { merged.firstOrNull()?.id.orEmpty() }
                if (fixed != state.photoFixedId && fixed.isNotBlank()) store.photoFixedId = fixed
                state = state.copy(photos = merged, photoFixedId = fixed, photoReadable = true)
                publish()
                Log.i(TAG, "Photos ${merged.size}")
            }
        }
    }

    fun openSystemSettings() {
        if (!launchSystemSettings(getApplication())) {
            showStatus("System settings is not available on this device.")
        }
    }

    fun openWifiSettings() {
        if (!launchWifiSettings(getApplication())) {
            showStatus("Wi-Fi settings is not available on this device.")
        }
    }

    fun setTemplateMode(on: Boolean) {
        store.templateMode = on
        state = state.copy(templateMode = on)
        publish()
    }

    fun setTemplate(text: String) {
        store.template = text
        state = state.copy(template = text)
        publish()
    }

    fun setCustom(text: String) {
        store.customText = text
        state = rebuildSentence(state.copy(customText = text))
        publish()
    }

    fun toggleClause(id: String) {
        val next = state.clauses.map { if (it.id == id) it.copy(enabled = !it.enabled) else it }
        store.setClauses(next)
        state = rebuildSentence(state.copy(clauses = next, movingClauseId = null))
        publish()
    }

    fun liftClause(id: String) {
        state = state.copy(movingClauseId = id)
        publish()
    }

    fun dropClause() {
        state = state.copy(movingClauseId = null)
        publish()
    }

    fun moveClause(direction: Int) {
        val id = state.movingClauseId ?: return
        val list = state.clauses.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
        store.setClauses(list)
        state = rebuildSentence(state.copy(clauses = list))
        publish()
    }

    fun longPressApp(id: String) {
        openSettings(section = "apps", appId = id)
    }

    fun closeMenu() {
        state = state.copy(menuAppId = null)
        publish()
    }

    fun togglePin(id: String) {
        val pins = store.pins().toMutableList()
        val wasPinned = id in pins
        if (wasPinned) {
            pins.remove(id)
        } else if (pins.size >= ReviewLayout.pinCap) {
            showStatus("Six apps can be starred. Unpin one first.")
            return
        } else {
            pins.add(0, id)
            store.setHidden(store.hidden() - id)
        }
        store.setPins(pins)
        Log.i(TAG, if (wasPinned) "Unpin $id" else "Pin $id")
        state = state.copy(menuAppId = null, focusHoldId = id)
        refreshApps()
    }

    fun releaseFocusHold() {
        if (state.focusHoldId == null) return
        state = state.copy(focusHoldId = null)
        publish()
    }

    fun openAppActions(id: String) {
        state = state.copy(menuAppId = id, movingAppId = null, settingsInPanel = true)
        publish()
    }

    fun beginMove(id: String) {
        Log.i(TAG, "Move $id")
        state = state.copy(movingAppId = id, menuAppId = null, settingsInPanel = true, focusHoldId = id)
        publish()
    }

    fun finishMove() {
        state = state.copy(movingAppId = null)
        publish()
    }

    fun moveApp(id: String, direction: Int) {
        val pins = store.pins().toMutableList()
        if (id in pins) {
            shift(pins, id, direction)
            store.setPins(pins)
            Log.i(TAG, "Reorder pin $id $direction")
        } else {
            val order = state.apps.map { it.id }.filter { it !in pins }.toMutableList()
            if (id !in order) order.add(id)
            shift(order, id, direction)
            store.setShelfOrder(order)
            Log.i(TAG, "Reorder shelf $id $direction")
        }
        refreshApps()
    }

    private fun shift(list: MutableList<String>, id: String, direction: Int) {
        val index = list.indexOf(id)
        val target = index + direction
        if (index < 0 || target !in list.indices) return
        val item = list.removeAt(index)
        list.add(target, item)
    }

    fun hideApp(id: String) {
        val order = store.appOrder().filterNot { it == id }
        store.setAppOrder(order)
        store.setPins(store.pins().filterNot { it == id })
        store.setHidden(store.hidden() + id)
        if (store.lastAppId == id) store.lastAppId = null
        Log.i(TAG, "Hide $id")
        state = state.copy(menuAppId = null, focusHoldId = id)
        refreshApps()
    }

    fun restore(id: String) {
        Log.i(TAG, "Show $id")
        store.setHidden(store.hidden() - id)
        val order = store.appOrder()
        if (id !in order) store.setAppOrder(order + id)
        state = state.copy(menuAppId = null, focusHoldId = id)
        refreshApps()
    }

    fun addInstalled(packageName: String) {
        val id = AppCatalog.idFor(packageName, design.marks)
        store.setHidden(store.hidden() - id)
        val order = store.appOrder()
        if (id !in order) store.setAppOrder(order + id)
        refreshApps()
        showStatus("Added.")
    }

    fun launch(app: HomeApp) {
        val id = AppCatalog.canonical(app.id, design.marks)
        store.markOpened(id)
        val message = AppCatalog.launch(getApplication(), app)
        if (message == null) {
            store.noteRecent(id)
            store.lastAppId = id
            state = state.copy(homeAppId = id)
            publish()
            refreshApps()
        } else {
            showStatus(message)
            refreshApps()
        }
    }

    fun switchSource(slot: SourceSlot) {
        store.lastSourceId = slot.id
        state = state.copy(activeSourceId = slot.id)
        publish()
        viewModelScope.launch(Dispatchers.IO) {
            val message = switcher.switch(slot)
            withContext(Dispatchers.Main) { showStatus(message) }
        }
    }

    fun setAppName(id: String, value: String) {
        store.setAppName(id, value.trim())
        refreshApps()
    }

    fun onVolumeHint() {
        val context = getApplication<Application>()
        val audio = context.getSystemService(android.media.AudioManager::class.java) ?: return
        val max = audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val value = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).coerceIn(0, max)
        val changed = volumeSeen && value != state.volume
        volumeSeen = true
        if (changed) Log.i(TAG, "Volume $value/$max")
        if (!changed && value == state.volume && max == state.volumeMax) return
        state = state.copy(
            volume = value,
            volumeMax = max,
            volumeToken = if (changed) state.volumeToken + 1 else state.volumeToken,
        )
        publish()
    }

    fun setInputName(id: String, value: String) {
        store.setInputName(id, value.trim())
        refreshInputs()
    }

    fun toggleInputShown(slot: SourceSlot) {
        when (slot.id) {
            "av" -> setShowAv(!state.showAv)
            "tuner" -> setShowTuner(!state.showTuner)
            else -> {
                val hidden = store.hiddenInputs().toMutableSet()
                if (slot.id in hidden) hidden.remove(slot.id) else hidden.add(slot.id)
                store.setHiddenInputs(hidden)
                refreshInputs()
            }
        }
    }

    fun openInputActions(id: String) {
        state = state.copy(menuAppId = "input:$id", movingAppId = null, settingsInPanel = true)
        publish()
    }

    fun refreshInputs() {
        val next = switcher.slots(state.showAv, state.showTuner, store.inputNames(), store.hiddenInputs())
        if (next == state.sources) return
        state = state.copy(sources = next)
        publish()
    }

    fun refreshApps() {
        val context = getApplication<Application>()
        val installed = AppCatalog.installedApps(context)
        store.touchSeen(installed.map { it.packageName }.toSet())
        if (!store.shelfSeeded) {
            if (store.pins().isEmpty()) {
                val packages = installed.map { it.packageName }.toSet()
                val seeded = Shelf.defaultPinIds(packages) { pkg -> AppCatalog.idFor(pkg, design.marks) }
                if (seeded.isNotEmpty()) store.setPins(seeded)
            }
            store.shelfSeeded = true
        }
        val hidden = store.hidden()
        val names = store.names()
        val apps = AppCatalog.assemble(
            context,
            design.marks,
            hidden,
            store.pins(),
            store.recent(),
            names,
            store.shelfOrder(),
        )
        val hiddenApps = AppCatalog.hiddenApps(context, design.marks, hidden, names)
        val home = when {
            state.debugFocus == "netflix" && apps.any { it.id == "netflix" } -> "netflix"
            else -> store.lastAppId?.takeIf { id -> apps.any { it.id == id } } ?: apps.firstOrNull()?.id
        }
        state = state.copy(apps = apps, hidden = hiddenApps, installed = installed, homeAppId = home)
        publish()
        exportIndex()
    }

    /** Rewrites index.json when the page order, names, pins, or hidden set changed. */
    private fun exportIndex() {
        val rows = IndexFile.rows(state.apps, state.hidden)
        val json = IndexFile.encode(rows)
        val generation = indexGeneration.incrementAndGet()
        viewModelScope.launch(Dispatchers.IO) {
            synchronized(indexLock) {
                if (generation != indexGeneration.get() || json == lastIndexJson) return@synchronized
                val file = IndexFile.write(getApplication(), json) ?: return@synchronized
                lastIndexJson = json
                Log.i(TAG, "Index file ${file.absolutePath} (${rows.size})")
            }
        }
    }

    private fun tick() {
        val before = state
        if (!demo) {
            val now = LocalDateTime.now()
            val time = now.format(timeFormat)
            if (time != minute) {
                minute = time
                state = rebuildSentence(
                    state.copy(
                        timeLabel = time,
                        dateLabel = now.format(dateFormat),
                        dateChinese = now.format(chineseDate),
                    ),
                )
            }
        }
        val idle = state.idleMinutes
        if (idle > 0 && !state.dimmed && state.debugFocus == null) {
            val elapsed = SystemClock.elapsedRealtime() - lastInputMs
            if (elapsed >= idle * 60_000L) {
                state = state.copy(dimmed = true, overlay = Overlay.None, menuAppId = null, previewSchemeId = null)
            }
        }
        if (state.status != null && SystemClock.elapsedRealtime() > statusUntil) {
            state = state.copy(status = null)
        }
        val link = readLink()
        if (link != state.link) state = state.copy(link = link)
        val hour = if (demo) 21 else LocalDateTime.now().hour
        if (hour != state.hour) state = state.copy(hour = hour)
        val battery = readBattery()
        if (battery?.first != state.batteryPercent || (battery?.second ?: false) != state.batteryCharging) {
            state = state.copy(batteryPercent = battery?.first, batteryCharging = battery?.second ?: false)
        }
        val tuned = switcher.tunedSlotId(state.sources) ?: store.lastSourceId
        if (tuned != state.activeSourceId) state = state.copy(activeSourceId = tuned)
        if (state != before) publish()
        maybeRotateSlot()
    }

    fun refreshSlotPhotos() {
        val names = SlotPhotos.names(SlotPhotos.directory(getApplication()))
        slotDeck = if (slotDeck.order.isEmpty()) {
            SlotPhotos.fresh(names, slotRandom)
        } else {
            SlotPhotos.reconcile(slotDeck, names, slotRandom)
        }
        val name = slotDeck.current.orEmpty()
        val changed = name != state.slotName
        if (changed) slotShownAt = SystemClock.elapsedRealtime()
        state = state.copy(
            slotName = name,
            slotCount = names.size,
            slotToken = if (changed) state.slotToken + 1 else state.slotToken,
            slotSeconds = store.slotSeconds,
        )
        publish()
        Log.i(TAG, "Slot folder ${SlotPhotos.PUSH_PATH} count ${names.size} showing ${name.ifBlank { "sample" }}")
    }

    fun cycleSlotInterval() {
        val options = SlotPhotos.INTERVALS
        val index = options.indexOf(state.slotSeconds).let { if (it < 0) options.indexOf(SlotPhotos.DEFAULT_SECONDS) else it }
        val next = options[(index + 1) % options.size]
        store.slotSeconds = next
        slotShownAt = SystemClock.elapsedRealtime()
        state = state.copy(slotSeconds = next)
        publish()
    }

    private fun maybeRotateSlot() {
        if (state.dimmed) return
        val period = slotPeriodOverrideMs ?: state.slotSeconds * 1_000L
        if (period <= 0L) return
        if (SystemClock.elapsedRealtime() - slotShownAt < period) return
        advanceSlot("interval")
    }

    private fun advanceSlot(reason: String) {
        val names = SlotPhotos.names(SlotPhotos.directory(getApplication()))
        slotDeck = SlotPhotos.advance(slotDeck, names, slotRandom)
        slotShownAt = SystemClock.elapsedRealtime()
        val name = slotDeck.current.orEmpty()
        val changed = name != state.slotName
        state = state.copy(
            slotName = name,
            slotCount = names.size,
            slotToken = if (changed) state.slotToken + 1 else state.slotToken,
        )
        publish()
        Log.i(TAG, "Slot $reason ${name.ifBlank { "sample" }} of ${names.size}")
    }

    private fun showStatus(message: String) {
        statusUntil = SystemClock.elapsedRealtime() + 6_000L
        state = state.copy(status = message)
        publish()
    }

    private fun initial(): UiState {
        val now = LocalDateTime.now()
        minute = now.format(timeFormat)
        return applyPlace(
            UiState(
                schemeId = store.schemeId,
                previewSchemeId = null,
                dateLabel = now.format(dateFormat),
                dateChinese = now.format(chineseDate),
                timeLabel = minute,
                sentence = SentenceData("", "", null, null, null, null, ""),
                clauses = store.clauses(),
                templateMode = store.templateMode,
                template = store.template,
                customText = store.customText,
                apps = emptyList(),
                hidden = emptyList(),
                installed = emptyList(),
                homeAppId = null,
                sources = emptyList(),
                idleMinutes = store.idleMinutes,
                plate = store.plate,
                dimmed = false,
                overlay = Overlay.None,
                menuAppId = null,
                movingClauseId = null,
                status = null,
                focusToken = 0,
                focusZone = "app",
                focusedAppId = null,
                providerLines = ExampleSources.lines,
                showAv = store.showAv,
                showTuner = store.showTuner,
                front = store.front(),
                customLines = listOf(
                    CustomCopy(store.customEnglish(0), store.customChinese(0)),
                    CustomCopy(store.customEnglish(1), store.customChinese(1)),
                ),
                link = readLink(),
                pageLines = emptyList(),
                weatherShort = "",
                place = Places.none(),
                locationMode = store.locationMode,
                manualCity = store.manualCity,
                locationNote = "",
                zoneCity = "",
                tickers = store.tickers,
                wave = store.wave,
                photoMode = store.photoMode,
                photoMinutes = store.photoMinutes,
                photoSeconds = store.photoSeconds,
                photos = store.photos(),
                photoFixedId = store.photoFixedId,
                photoReadable = PhotoFolder.canRead(getApplication()),
                settingsSection = store.settingsSection,
                tickerChinese = store.tickerChinese,
                frontPhoto = store.frontPhoto,
                slotSeconds = store.slotSeconds,
            ),
        )
    }

    private fun rebuildSentence(base: UiState): UiState {
        val place = base.place
        val weather = ExampleSources.weather.current(place.latitude, place.longitude)
        val data = SentenceData(
            time = base.timeLabel,
            date = base.dateLabel,
            weather = weather,
            event = ExampleSources.calendar.next(),
            watchlist = ExampleSources.watchlist.current(),
            nowPlaying = ExampleSources.nowPlaying.current(),
            custom = base.customText,
        )
        val sky = skyForce ?: base.forecast?.sky
        val forecast = if (skyForce != null) sampleForecast(skyForce!!) else base.forecast
        val lines = if (longPage) {
            longSample(sky ?: SkyMark.DOWNPOUR)
        } else {
            FrontPage.page(
                order = base.front.map { it.id },
                enabled = base.front.filter { it.enabled }.map { it.id }.toSet(),
                forecast = forecast,
                quotes = base.quotes,
                headlines = base.headlines,
                customs = base.customLines,
            )
        }
        val shownSky = when {
            longPage -> sky ?: SkyMark.DOWNPOUR
            skyForce != null -> skyForce
            else -> forecast?.sky
        }
        val short = when {
            longPage && shownSky == SkyMark.SUN -> "21° clear"
            longPage -> "21° rain"
            skyForce != null -> forecast?.short.orEmpty()
            else -> forecast?.short.orEmpty()
        }
        val hour = if (demo) 21 else LocalDateTime.now().hour
        return base.copy(
            sentence = data,
            pageLines = lines,
            weatherShort = short,
            forecast = forecast,
            sky = shownSky,
            hour = hour,
        )
    }

    private fun sampleForecast(sky: SkyMark): LiveForecast = when (sky) {
        SkyMark.SUN -> LiveForecast(21, "21° clear", "It's 21° and clear.", "現在21°，晴朗。", SkyMark.SUN)
        SkyMark.CLOUD -> LiveForecast(21, "21° cloudy", "It's 21° and partly cloudy.", "現在21°，多雲。", SkyMark.CLOUD)
        SkyMark.DRIZZLE -> LiveForecast(21, "21° drizzle", "It's 21° and drizzling.", "現在21°，毛毛雨。", SkyMark.DRIZZLE)
        SkyMark.STORM -> LiveForecast(21, "21° storms", "It's 21° and stormy.", "現在21°，有雷雨。", SkyMark.STORM)
        SkyMark.SNOW -> LiveForecast(21, "21° snow", "It's 21° and snowing.", "現在21°，下雪。", SkyMark.SNOW)
        SkyMark.FOG -> LiveForecast(21, "21° fog", "It's 21° and foggy.", "現在21°，有霧。", SkyMark.FOG)
        SkyMark.DOWNPOUR -> LiveForecast(21, "21° rain", "It's 21° and raining.", "現在21°，下雨。", SkyMark.DOWNPOUR)
    }

    /** Layout check only: a wrapping forecast and four bilingual quotes. */
    private fun longSample(sky: SkyMark): List<PageLine> {
        val weather = if (sky == SkyMark.SUN) {
            PageLine(
                "It's 21° and clear, with a warm evening and a light wind.",
                "現在21°，晴朗，傍晚仍暖，有輕風。",
                sky = SkyMark.SUN,
            )
        } else {
            PageLine(
                "It's 21° and raining, with showers through the evening and a cooler wind after midnight.",
                "現在21°，下雨，傍晚仍有陣雨，午夜之後風轉涼。",
                sky = sky,
            )
        }
        return listOf(
            weather.copy(role = LineRole.WEATHER),
            PageLine("AAPL  100.00  +1.00%", "", change = 1.00, role = LineRole.QUOTE),
            PageLine("MSFT  200.00  -0.50%", "", change = -0.50, role = LineRole.QUOTE),
            PageLine("IBM  150.00  +0.25%", "", change = 0.25, role = LineRole.QUOTE),
            PageLine("ORCL  80.00  +0.10%", "", change = 0.10, role = LineRole.QUOTE),
            PageLine("A reef survey finds a new ridge in the Pacific.", "太平洋礁調查發現一道新海脊。", role = LineRole.NEWS),
            PageLine("Talks on a border river stall overnight.", "界河談判一夜停滯。", role = LineRole.NEWS),
            PageLine("A lab ships a quieter chip for telescopes.", "實驗室交出更安靜的望遠鏡晶片。", role = LineRole.NEWS),
        )
    }

    private suspend fun pullWeather() {
        val gen = weatherGen.incrementAndGet()
        val forecast = runCatching { ForecastClient.fetch(state.place.latitude, state.place.longitude, state.place.city, state.place.chinese) }.getOrElse {
            Log.i(TAG, "Weather failed: ${it.message}")
            null
        }
        withContext(Dispatchers.Main) {
            if (gen != weatherGen.get()) return@withContext
            if (forecast != null) {
                state = rebuildSentence(state.copy(forecast = forecast))
                publish()
                weatherDue = System.currentTimeMillis() + FEED_SLOW
                Log.i(TAG, "Weather ${forecast.short}")
            } else {
                weatherDue = System.currentTimeMillis() + FEED_RETRY
                Log.i(TAG, "Weather empty; the block stays hidden until a real reading")
            }
        }
    }

    private suspend fun pullQuotes() {
        val gen = quoteGen.incrementAndGet()
        val symbols = TickerList.parse(store.tickers)
        val open = MarketHours.isOpen(ZonedDateTime.now())
        val quotes = runCatching { QuoteClient.fetch(symbols) }.getOrElse {
            Log.i(TAG, "Quotes failed: ${it.message}")
            null
        }
        withContext(Dispatchers.Main) {
            if (gen != quoteGen.get()) return@withContext
            if (!quotes.isNullOrEmpty()) {
                state = rebuildSentence(state.copy(quotes = quotes))
                publish()
                quoteDue = System.currentTimeMillis() + if (open) QUOTE_OPEN else FEED_SLOW
                Log.i(TAG, "Quotes ${quotes.size} open=$open")
            } else {
                quoteDue = System.currentTimeMillis() + FEED_RETRY
                Log.i(TAG, "Quotes empty; retry")
            }
        }
    }

    private suspend fun pullNews() {
        val gen = newsGen.incrementAndGet()
        val headlines = runCatching {
            HeadlineClient.fetch(store::translation, store::saveTranslation)
        }.getOrElse {
            Log.i(TAG, "Headlines failed: ${it.message}")
            null
        }
        withContext(Dispatchers.Main) {
            if (gen != newsGen.get()) return@withContext
            if (!headlines.isNullOrEmpty()) {
                state = rebuildSentence(state.copy(headlines = headlines))
                publish()
                newsDue = System.currentTimeMillis() + FEED_SLOW
                Log.i(TAG, "Headlines ${headlines.size}")
            } else {
                newsDue = System.currentTimeMillis() + FEED_RETRY
                Log.i(TAG, "Headlines empty; retry")
            }
        }
    }

    private fun applyPlace(base: UiState): UiState {
        val zone = deviceZone()
        val country = deviceCountry(zone)
        val known = Places.forZone(zone, country)
        val place = LocationResolve.resolve(
            mode = base.locationMode,
            manualCity = base.manualCity,
            deviceZone = zone,
            deviceCountry = country,
            ip = cachedFix(),
            ipZoneCountry = cachedFix()?.timezone?.let { deviceCountry(it) },
        )
        val cityChanged = place.city != base.place.city
        if (cityChanged) weatherDue = 0L
        val forecast = if (place.city.isBlank() || cityChanged) null else base.forecast
        Log.i(TAG, "Place ${place.city.ifBlank { "(none)" }} via ${place.source}")
        return rebuildSentence(
            base.copy(
                place = place,
                zoneCity = known?.city.orEmpty(),
                locationNote = locationNote(base.locationMode, place, zone, ipAgrees()),
                forecast = forecast,
            ),
        )
    }

    private fun refreshIp() {
        if (store.locationMode != "auto") return
        val now = System.currentTimeMillis()
        val cached = store.cachedIp()
        if (cached != null && now - cached.at < DAY_MS) return
        if (now - store.ipFailedAt < FAIL_MS) return
        if (lookupRunning) return
        lookupRunning = true
        viewModelScope.launch(Dispatchers.IO) {
            val fix = IpLocator().locate()
            withContext(Dispatchers.Main) {
                lookupRunning = false
                if (fix == null) {
                    store.ipFailedAt = System.currentTimeMillis()
                    Log.i(TAG, "IP lookup failed; weather stays off until a city is known")
                } else {
                    store.saveIp(
                        city = fix.city,
                        latitude = fix.latitude,
                        longitude = fix.longitude,
                        country = fix.country.orEmpty(),
                        timezone = fix.timezone.orEmpty(),
                        at = System.currentTimeMillis(),
                    )
                    val zone = deviceZone()
                    val agrees = LocationCheck.agrees(
                        zone,
                        deviceCountry(zone),
                        fix.timezone,
                        fix.country,
                        fix.timezone?.let { deviceCountry(it) },
                    )
                    Log.i(TAG, "IP lookup ${fix.city} ${fix.country} ${fix.timezone} agrees=$agrees")
                }
                state = applyPlace(state)
                publish()
            }
        }
    }

    private fun cachedFix(): IpFix? {
        val cached = store.cachedIp() ?: return null
        return IpFix(
            city = cached.city,
            latitude = cached.latitude,
            longitude = cached.longitude,
            country = cached.country.ifBlank { null },
            timezone = cached.timezone.ifBlank { null },
        )
    }

    private fun ipAgrees(): Boolean? {
        val fix = cachedFix() ?: return null
        val zone = deviceZone()
        return LocationCheck.agrees(
            zone,
            deviceCountry(zone),
            fix.timezone,
            fix.country,
            fix.timezone?.let { deviceCountry(it) },
        )
    }

    private fun locationNote(mode: String, place: ResolvedPlace, zone: String, agrees: Boolean?): String {
        if (mode == "manual" && place.city.isNotBlank()) return "Using ${place.city}."
        if (place.source == "ip" && place.city.isNotBlank()) return "Automatic from the network: ${place.city}."
        return when (agrees) {
            false -> "The network location does not match this television. Weather stays off until you set a city."
            else -> "Weather stays off until the network reports a city that matches $zone, or you set one."
        }
    }

    private fun deviceZone(): String = java.util.TimeZone.getDefault().id

    private fun deviceCountry(zone: String): String? {
        return try {
            val region = android.icu.util.TimeZone.getRegion(zone)
            LocationCheck.country(region)
        } catch (_: Throwable) {
            null
        }
    }

    /** Null when the device does not report a battery, which is a television. */
    private fun readBattery(): Pair<Int, Boolean>? {
        val context = getApplication<Application>()
        val sticky = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        if (!sticky.getBooleanExtra(android.os.BatteryManager.EXTRA_PRESENT, false)) return null
        val level = sticky.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = sticky.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
        val charging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
            status == android.os.BatteryManager.BATTERY_STATUS_FULL
        return (level * 100) / scale to charging
    }

    private fun readLink(): String {
        val context = getApplication<Application>()
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return "offline"
        val network = manager.activeNetwork ?: return "offline"
        val caps = manager.getNetworkCapabilities(network) ?: return "offline"
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return "offline"
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val rssi = try {
                context.getSystemService(WifiManager::class.java)?.connectionInfo?.rssi
            } catch (_: Throwable) {
                null
            }
            if (rssi != null && rssi < -70 && rssi > -127) return "weak"
        }
        return "connected"
    }

    private fun launchWifiSettings(context: Context): Boolean {
        val actions = listOf(
            Settings.ACTION_WIFI_SETTINGS,
            Settings.ACTION_WIRELESS_SETTINGS,
            Settings.ACTION_SETTINGS,
        )
        return launchFirst(context, actions.map { Intent(it) }, "Wi-Fi settings")
    }

    private fun launchSystemSettings(context: Context): Boolean {
        val intents = ArrayList<Intent>()
        intents += Intent(Settings.ACTION_SETTINGS)
        val manager = context.packageManager
        listOf("com.sony.dtv.settings", "com.android.tv.settings", "com.android.settings").forEach { pkg ->
            val launch = manager.getLeanbackLaunchIntentForPackage(pkg) ?: manager.getLaunchIntentForPackage(pkg)
            if (launch != null) intents += launch
        }
        return launchFirst(context, intents, "System settings")
    }

    private fun launchFirst(context: Context, intents: List<Intent>, label: String): Boolean {
        val manager = context.packageManager
        for (intent in intents) {
            val target = Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (target.resolveActivity(manager) == null) continue
            try {
                context.startActivity(target)
                Log.i(TAG, "$label opened ${target.action ?: target.component}")
                return true
            } catch (error: Throwable) {
                Log.i(TAG, "$label refused", error)
            }
        }
        return false
    }

    private fun publish() {
        val snapshot = state
        listeners.forEach { it(snapshot) }
    }

    fun sentenceTokens(): Map<String, String> = SentenceComposer.tokens(state.sentence)

    companion object {
        private const val TAG = "CleanLaunch"
        private const val DAY_MS = 24L * 60L * 60L * 1000L
        private const val FAIL_MS = 30L * 60L * 1000L
        private const val QUOTE_OPEN = 3L * 60L * 1000L
        private const val FEED_SLOW = 30L * 60L * 1000L
        private const val FEED_RETRY = 90L * 1000L
    }
}
