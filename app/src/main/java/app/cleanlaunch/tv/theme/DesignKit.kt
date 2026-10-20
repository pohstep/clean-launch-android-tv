package app.cleanlaunch.tv.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class FocusTokens(
    val style: String,
    val swipe: Color,
    val swipeOpacity: Float,
    val pencil: Color,
    val pencilPx: Int,
    val edge: Color,
    val edgePx: Int,
    val glow: Color,
    val glowOpacity: Float,
    val nameScale: Float,
    val text: Color,
)

data class SchemeTokens(
    val id: String,
    val name: String,
    val masthead: String,
    val idle: String,
    val background: Color,
    val glow: Color,
    val vignette: Color,
    val moon: Color?,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val time: Color,
    val rule: Color,
    val plate: Color,
    val dimInk: Color,
    val numeral: Color,
    val footerInk: Color,
    val chipBorder: Color,
    val tagFill: Color,
    val tagText: Color,
    val flatPaper: Boolean,
    val paperGlow: String,
    val idleDusk: Color,
    val idleRead: Color,
    val idleScrim: Color,
    val idleScrimAlpha: Float,
    val idleContentAlpha: Float,
    val texture: ImageBitmap?,
    val focus: FocusTokens,
)

data class MarkDef(val id: String, val label: String, val packages: List<String>)

data class LayoutTokens(
    val safeHorizontalPx: Int,
    val safeVerticalPx: Int,
    val sentenceWeight: Float,
    val indexWeight: Float,
    val columnGapPx: Int,
    val minTextPx: Int,
    val iconPx: Int,
    val platePadPx: Int,
    val plateRadiusPx: Int,
    val maxSentenceLines: Int,
    val rulePx: Int,
    val mastheadGapPx: Int,
    val sectionGapPx: Int,
)

data class TypeTokens(
    val mastheadPx: Int,
    val sentencePx: Int,
    val sentenceLinePx: Int,
    val indexPx: Int,
    val kickerPx: Int,
    val kickerTracking: Float,
    val uiPx: Int,
    val sourcePx: Int,
    val footerPx: Int,
)

data class CopyTokens(
    val examplePill: String,
    val indexKicker: String,
    val sourcesKicker: String,
    val editPage: String,
    val ok: String,
    val idlePrefix: String,
)

data class EveningType(
    val headline: FontFamily,
    val headlineItalic: FontFamily,
    val ui: FontFamily,
    val uiMedium: FontFamily,
    val headlinesAreBoska: Boolean,
    val chinese: FontFamily = FontFamily.Serif,
    val focus: FontFamily = FontFamily.Serif,
    val focusItalic: FontFamily = FontFamily.Serif,
    val display: FontFamily = FontFamily.Serif,
)

data class Design(
    val schemes: List<SchemeTokens>,
    val marks: List<MarkDef>,
    val layout: LayoutTokens,
    val type: TypeTokens,
    val copy: CopyTokens,
    val faces: EveningType,
    val scrim: Color,
) {
    fun scheme(id: String): SchemeTokens {
        return schemes.firstOrNull { it.id == id } ?: schemes.first()
    }
}

object DesignKit {
    private const val TAG = "CleanLaunch"

    fun load(context: Context): Design {
        val root = try {
            JSONObject(readTokens(context))
        } catch (error: Throwable) {
            Log.e(TAG, "Design tokens failed to load", error)
            return fallback()
        }
        val faces = try {
            loadFaces(context, root.getJSONObject("fonts"))
        } catch (error: Throwable) {
            Log.e(TAG, "Fonts failed to load; headlines fall back to a system serif", error)
            EveningType(FontFamily.Serif, FontFamily.Serif, FontFamily.SansSerif, FontFamily.SansSerif, false)
        }
        return try {
            Design(
                schemes = parseSchemes(context, root.getJSONArray("schemes")),
                marks = parseMarks(root.optJSONArray("marks")),
                layout = parseLayout(root.getJSONObject("layout")),
                type = parseType(root.getJSONObject("type")),
                copy = parseCopy(root.getJSONObject("copy")),
                faces = faces,
                scrim = color(root.optString("scrim", "#80140E0A")),
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Design tokens failed to parse", error)
            fallback()
        }
    }

    private fun readTokens(context: Context): String {
        val external = File(context.getExternalFilesDir(null), "design/tokens.json")
        if (external.exists()) return external.readText()
        return context.assets.open("design/tokens.json").bufferedReader().use { it.readText() }
    }

    private fun resolve(context: Context, relative: String): File? {
        val external = File(context.getExternalFilesDir(null), "design/$relative")
        if (external.exists() && external.length() > 0L) return external
        return try {
            val cache = File(context.filesDir, "design-cache/$relative")
            if (!cache.exists() || cache.length() == 0L) {
                cache.parentFile?.mkdirs()
                context.assets.open("design/$relative").use { input ->
                    cache.outputStream().use { input.copyTo(it) }
                }
            }
            cache.takeIf { it.exists() && it.length() > 0L }
        } catch (_: Throwable) {
            null
        }
    }

    private fun loadFaces(context: Context, fonts: JSONObject): EveningType {
        val chinese = loadChinese(context)
        val focus = loadFocus(context)
        val display = loadDisplay(context)
        val boska = resolve(context, fonts.optString("boska", "fonts/boska/Boska-Regular.otf"))
        val boskaItalic = resolve(context, fonts.optString("boskaItalic", "fonts/boska/Boska-Italic.otf"))
        if (boska != null) {
            Log.i(TAG, "Headlines: Boska from ${boska.absolutePath}")
            val regular = staticFamily(boska)
            val italic = boskaItalic?.let { staticFamily(it, FontStyle.Italic) } ?: regular
            val uiFile = resolve(context, fonts.getString("ui"))
            val ui = uiFile?.let { variableFamily(it, null, italicFile = false) } ?: FontFamily.SansSerif
            return EveningType(regular, italic, ui, ui, headlinesAreBoska = true, chinese = chinese, focus = focus.first, focusItalic = focus.second, display = display)
        }
        val headlineFile = resolve(context, fonts.getString("headline"))
        val italicFile = resolve(context, fonts.getString("headlineItalic"))
        val uiFile = resolve(context, fonts.getString("ui"))
        if (headlineFile == null) {
            Log.e(TAG, "Newsreader file missing. Headlines would fall back to a system serif.")
        } else {
            Log.i(TAG, "Headlines: Newsreader light (wght 300, opsz 48) from ${headlineFile.name}")
        }
        val headline = headlineFile?.let { variableFamily(it, 48f, italicFile = false) } ?: FontFamily.Serif
        val italic = italicFile?.let { variableFamily(it, 48f, italicFile = true) } ?: headline
        val ui = uiFile?.let { variableFamily(it, null, italicFile = false) } ?: FontFamily.SansSerif
        return EveningType(
            headline,
            italic,
            ui,
            ui,
            headlinesAreBoska = false,
            chinese = chinese,
            focus = focus.first,
            focusItalic = focus.second,
            display = display,
        )
    }

    private fun loadDisplay(context: Context): FontFamily {
        val file = resolve(context, "fonts/boska/Boska-Light.otf") ?: return FontFamily.Serif
        Log.i(TAG, "Display: Boska Light from ${file.name}")
        return staticFamily(file)
    }

    private fun loadChinese(context: Context): FontFamily {
        val file = assetFont(context, "fonts/SmileySans-Oblique.otf") ?: return FontFamily.Serif
        Log.i(TAG, "Chinese: Smiley Sans from ${file.name}")
        return FontFamily(Font(file, FontWeight.Light), Font(file, FontWeight.Normal))
    }

    private fun loadFocus(context: Context): Pair<FontFamily, FontFamily> {
        val medium = resolve(context, "fonts/boska/Boska-Medium.otf")
        val italic = resolve(context, "fonts/boska/Boska-MediumItalic.otf")
        if (medium == null) return FontFamily.Serif to FontFamily.Serif
        Log.i(TAG, "Focus: Boska Medium from ${medium.name}")
        val face = staticFamily(medium)
        val slant = italic?.let { staticFamily(it, FontStyle.Italic) } ?: face
        return face to slant
    }

    private fun assetFont(context: Context, path: String): File? {
        return try {
            val bytes = context.assets.open(path).use { it.readBytes() }
            val cache = File(context.filesDir, "font-cache/$path")
            if (!cache.exists() || cache.length() != bytes.size.toLong()) {
                cache.parentFile?.mkdirs()
                cache.writeBytes(bytes)
            }
            cache
        } catch (error: Throwable) {
            Log.e(TAG, "Chinese font missing", error)
            null
        }
    }

    private fun staticFamily(file: File, style: FontStyle = FontStyle.Normal): FontFamily {
        return FontFamily(
            Font(file, FontWeight.Normal, style),
            Font(file, FontWeight.Medium, style),
            Font(file, FontWeight.SemiBold, style),
        )
    }

    private fun variableFamily(file: File, opsz: Float?, italicFile: Boolean): FontFamily {
        val style = if (italicFile) FontStyle.Italic else FontStyle.Normal
        fun face(weight: Int): Font {
            bake(file, weight, opsz)
            val settings = if (opsz != null) {
                FontVariation.Settings(FontVariation.weight(weight), FontVariation.Setting("opsz", opsz))
            } else {
                FontVariation.Settings(FontVariation.weight(weight))
            }
            return Font(file, FontWeight(weight), style, settings)
        }
        return FontFamily(face(300), face(400), face(500), face(700))
    }

    /** Bakes wght and opsz into a Typeface so the device cannot fall back to a heavy default instance. */
    private fun bake(file: File, weight: Int, opsz: Float?): Typeface? {
        return try {
            val variation = if (opsz != null) "'wght' $weight, 'opsz' $opsz" else "'wght' $weight"
            val typeface = Typeface.Builder(file).setFontVariationSettings(variation).build()
            if (typeface == null) {
                Log.e(TAG, "Typeface.Builder returned null for ${file.name} $variation")
            } else {
                Log.i(TAG, "Font ready ${file.name} $variation")
            }
            typeface
        } catch (error: Throwable) {
            Log.e(TAG, "Could not bake ${file.name} at weight $weight", error)
            null
        }
    }

    private fun parseSchemes(context: Context, array: JSONArray): List<SchemeTokens> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val focus = item.getJSONObject("focus")
                val texturePath = item.optString("texture")
                val texture = if (texturePath.isBlank()) null else resolve(context, texturePath)?.let { file ->
                    BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                }
                add(
                    SchemeTokens(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        masthead = item.getString("masthead"),
                        idle = item.getString("idle"),
                        background = color(item.getString("background")),
                        glow = color(item.getString("glow")),
                        vignette = color(item.getString("vignette")),
                        moon = if (item.has("moon")) color(item.getString("moon")) else null,
                        ink = color(item.getString("ink")),
                        muted = color(item.getString("muted")),
                        accent = color(item.getString("accent")),
                        time = color(item.getString("time")),
                        rule = color(item.getString("rule")),
                        plate = color(item.getString("plate")),
                        dimInk = color(item.optString("dimInk", item.optString("muted", "#524f4a"))),
                        numeral = color(item.optString("numeral", item.optString("muted", "#66625c"))),
                        footerInk = color(item.optString("footerInk", item.getString("ink"))),
                        chipBorder = color(item.optString("chipBorder", item.getString("ink"))),
                        tagFill = color(item.optString("tagFill", "#ffd84a")),
                        tagText = color(item.optString("tagText", "#111111")),
                        flatPaper = item.optBoolean("flatPaper", item.optString("id") == "cream"),
                        paperGlow = item.optString("paperGlow", "none"),
                        idleDusk = color(item.optString("idleDusk", "#2A2622")),
                        idleRead = color(item.optString("idleRead", "#55534F")),
                        idleScrim = color(item.optString("idleScrim", item.optString("idleDusk", "#2A2622"))),
                        idleScrimAlpha = item.optDouble("idleScrimAlpha", 1.0).toFloat(),
                        idleContentAlpha = item.optDouble("idleContentAlpha", 1.0).toFloat(),
                        texture = texture,
                        focus = FocusTokens(
                            style = focus.getString("style"),
                            swipe = color(focus.getString("swipe")),
                            swipeOpacity = focus.getDouble("swipeOpacity").toFloat(),
                            pencil = color(focus.getString("pencil")),
                            pencilPx = focus.getInt("pencilPx"),
                            edge = color(focus.getString("edge")),
                            edgePx = focus.getInt("edgePx"),
                            glow = color(focus.getString("glow")),
                            glowOpacity = focus.getDouble("glowOpacity").toFloat(),
                            nameScale = focus.getDouble("nameScale").toFloat(),
                            text = color(focus.getString("text")),
                        ),
                    ),
                )
            }
        }
    }

    private fun parseMarks(array: JSONArray?): List<MarkDef> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val packages = item.optJSONArray("packages")
                val names = buildList {
                    if (packages != null) {
                        for (slot in 0 until packages.length()) add(packages.getString(slot))
                    }
                }
                add(MarkDef(item.getString("id"), item.getString("label"), names))
            }
        }
    }

    private fun parseLayout(item: JSONObject) = LayoutTokens(
        safeHorizontalPx = item.getInt("safeHorizontalPx"),
        safeVerticalPx = item.getInt("safeVerticalPx"),
        sentenceWeight = item.getDouble("sentenceWeight").toFloat(),
        indexWeight = item.getDouble("indexWeight").toFloat(),
        columnGapPx = item.getInt("columnGapPx"),
        minTextPx = item.getInt("minTextPx"),
        iconPx = item.getInt("iconPx"),
        platePadPx = item.getInt("platePadPx"),
        plateRadiusPx = item.getInt("plateRadiusPx"),
        maxSentenceLines = item.getInt("maxSentenceLines"),
        rulePx = item.getInt("rulePx"),
        mastheadGapPx = item.getInt("mastheadGapPx"),
        sectionGapPx = item.getInt("sectionGapPx"),
    )

    private fun parseType(item: JSONObject) = TypeTokens(
        mastheadPx = item.getInt("mastheadPx"),
        sentencePx = item.getInt("sentencePx"),
        sentenceLinePx = item.getInt("sentenceLinePx"),
        indexPx = item.getInt("indexPx"),
        kickerPx = item.getInt("kickerPx"),
        kickerTracking = item.getDouble("kickerTracking").toFloat(),
        uiPx = item.getInt("uiPx"),
        sourcePx = item.getInt("sourcePx"),
        footerPx = item.getInt("footerPx"),
    )

    private fun parseCopy(item: JSONObject) = CopyTokens(
        examplePill = item.getString("examplePill"),
        indexKicker = item.getString("indexKicker"),
        sourcesKicker = item.getString("sourcesKicker"),
        editPage = item.getString("editPage"),
        ok = item.getString("ok"),
        idlePrefix = item.getString("idlePrefix"),
    )

    private fun color(hex: String): Color {
        val clean = hex.trim().removePrefix("#")
        val packed = clean.toLong(16)
        val argb = if (clean.length <= 6) 0xFF000000L or packed else packed
        return Color(argb.toInt())
    }

    private fun fallback(): Design {
        val focus = FocusTokens(
            style = "highlighter",
            swipe = Color(0xFFF4CF86),
            swipeOpacity = 1f,
            pencil = Color(0xFF5F574B),
            pencilPx = 8,
            edge = Color(0xFF5F574B),
            edgePx = 8,
            glow = Color(0xFFF4CF86),
            glowOpacity = 0f,
            nameScale = 1.19f,
            text = Color(0xFF1C1A17),
        )
        val scheme = SchemeTokens(
            id = "cream",
            name = "Paper",
            masthead = "The Paper",
            idle = "page dims",
            background = Color(0xFFF3EEE3),
            glow = Color(0xFFF3EEE3),
            vignette = Color(0xFFF3EEE3),
            moon = null,
            ink = Color(0xFF1C1A17),
            muted = Color(0xFF524F4A),
            accent = Color(0xFF1C1A17),
            time = Color(0xFF1C1A17),
            rule = Color(0xFF1C1A17),
            plate = Color(0xFFE6D9C8),
            dimInk = Color(0xFF524F4A),
            numeral = Color(0xFF66625C),
            footerInk = Color(0xFF1C1A17),
            chipBorder = Color(0xFF1C1A17),
            tagFill = Color(0xFFFFD84A),
            tagText = Color(0xFF111111),
            flatPaper = true,
            paperGlow = "none",
            idleDusk = Color(0xFF2A2622),
            idleRead = Color(0xFF55534F),
            idleScrim = Color(0xFF2A2622),
            idleScrimAlpha = 1f,
            idleContentAlpha = 1f,
            texture = null,
            focus = focus,
        )
        return Design(
            schemes = listOf(scheme),
            marks = emptyList(),
            layout = LayoutTokens(96, 54, 1.15f, 0.88f, 48, 28, 70, 8, 14, 7, 2, 28, 36),
            type = TypeTokens(34, 46, 64, 40, 28, 0.18f, 30, 30, 28),
            copy = CopyTokens(
                "",
                "INDEX",
                "SOURCES",
                "Edit page",
                "OK",
                "Idle: ",
            ),
            faces = EveningType(FontFamily.Serif, FontFamily.Serif, FontFamily.SansSerif, FontFamily.SansSerif, false),
            scrim = Color(0x80140E0A),
        )
    }
}
