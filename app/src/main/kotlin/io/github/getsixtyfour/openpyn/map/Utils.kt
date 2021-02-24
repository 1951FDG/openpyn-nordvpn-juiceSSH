package io.github.getsixtyfour.openpyn.map

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources.NotFoundException
import android.text.SpannableString
import androidx.annotation.ArrayRes
import androidx.annotation.RawRes
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.map.util.CameraUpdateAnimator.Animation
import io.github.getsixtyfour.openpyn.maps.MapBoxOfflineTileProvider
import io.github.getsixtyfour.openpyn.utils.PrintArray
import mu.KotlinLogging
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.HashSet
import kotlin.math.pow

private val logger = KotlinLogging.logger {}

internal fun setUpPrintArray(context: Context, countries: List<MultiSelectable>, hashSet: HashSet<CharSequence>): HashSet<CharSequence> {
    val length = hashSet.size
    val defaultSelectedIdsList = ArrayList<Int>(length)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            if (hashSet.contains(selectable.tag)) {
                defaultSelectedIdsList.add(selectable.id)
            }
        }
    }
    val defValue = defaultSelectedIdsList.joinToString(separator = PrintArray.delimiter)
    val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    val prefSelectedIdsList = PrintArray.getListInt("pref_country_values", defValue, preferences)
    val currentFlags = HashSet<CharSequence>(length)
    val currentIds = ArrayList<Int>(length)
    val currentCountries = ArrayList<MultiSelectable>(countries.size)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            val id = selectable.id
            val tag = selectable.tag

            if (hashSet.contains(tag)) {
                currentCountries.add(it)

                if (prefSelectedIdsList.contains(id)) {
                    currentFlags.add(tag)
                    currentIds.add(id)
                }
            }
        }
    }

    PrintArray.apply {
        setHint(R.string.search_hint)
        setItems(currentCountries)
        setCheckedItems(currentIds)
    }

    return currentFlags
}

@Suppress("unused", "SpellCheckingInspection")
internal fun fileBackedTileProvider(path: String): MapBoxOfflineTileProvider {
    // Use a file backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider(path)
    logger.debug { "$tileProvider" }
    return tileProvider
}

@Suppress("unused", "SpellCheckingInspection")
internal fun memoryBackedTileProvider(path: String): MapBoxOfflineTileProvider {
    // Use a memory backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider(path, null)
    logger.debug { "$tileProvider" }
    return tileProvider
}

internal fun getCameraUpdates(zoom: Int = 3): ArrayList<Animation> {
    // # of tiles at zoom level 3 = 64 = 8 * 8
    val rows = 2F.pow(zoom).toInt()
    val cameraUpdates = ArrayList<Animation>(rows * rows)
    for (y in 0 until rows) {
        for (x in 0 until rows) {
            val bounds = MapBoxOfflineTileProvider.calculateTileBounds(x, y, zoom)
            val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
            val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
            val animation = Animation(cameraUpdate)
            cameraUpdates.add(animation)
        }
    }
    return cameraUpdates
}

internal fun getCurrentFlags(countries: List<MultiSelectable>, selectedIds: ArrayList<Int>): HashSet<CharSequence> {
    val currentFlags = HashSet<CharSequence>(selectedIds.size)
    countries.forEach {
        (it as? MultiSelectModelExtra)?.let { selectable ->
            if (selectedIds.contains(selectable.id)) {
                currentFlags.add(selectable.tag)
            }
        }
    }
    return currentFlags
}

internal fun copyToExternalFilesDir(context: Context, list: List<Pair<Int, String>>) {
    list.forEach { (id, ext) ->
        try {
            val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
            if (!file.exists()) {
                copyRawResourceToFile(context, id, file)
            }
        } catch (e: NotFoundException) {
            logger.error(e) { "" }
        } catch (e: FileNotFoundException) {
            logger.error(e) { "" }
        } catch (e: IOException) {
            logger.error(e) { "" }
        }
    }
}

internal fun copyRawResourceToFile(context: Context, @RawRes id: Int, file: File) {
    context.resources.openRawResource(id).use { input ->
        file.outputStream().buffered().use { input.copyTo(it) }
    }
}

internal fun createJsonArray(context: Context, @RawRes id: Int, ext: String): JSONArray {
    try {
        val file = File(context.getExternalFilesDir(null), context.resources.getResourceEntryName(id) + ext)
        if (!file.exists()) {
            copyRawResourceToFile(context, id, file)
        }
        val json = file.bufferedReader().use(BufferedReader::readText)
        return JSONArray(json)
    } catch (e: NotFoundException) {
        logger.error(e) { "" }
    } catch (e: FileNotFoundException) {
        logger.error(e) { "" }
    } catch (e: IOException) {
        logger.error(e) { "" }
    } catch (e: JSONException) {
        logger.error(e) { "" }
    }
    return JSONArray()
}

@Suppress("MagicNumber")
@SuppressLint("ResourceType")
internal fun countryList(context: Context, @ArrayRes id: Int): List<MultiSelectable> {
    val typedArray = context.resources.obtainTypedArray(id)
    val size = typedArray.length()
    val list = ArrayList<MultiSelectable>(size)
    for (i in 0 until size) {
        val array = context.resources.obtainTypedArray(typedArray.getResourceId(i, 0))
        // <array name="us">
        // 0 <item>United States</item>
        // 1 <item>🇺🇸</item>
        // 2 <item>us</item>
        // 3 <item>60</item>
        // 4 <item>@drawable/ic_united_states_40dp</item>
        // </array>
        val id = array.getInt(3, 0)
        val name = array.getString(0)!!
        val resId = array.getResourceId(4, 0)
        val tag = array.getString(2)!!
        val unicode = array.getString(1)!!
        val multiSelectModelExtra = MultiSelectModelExtra(id, SpannableString(name), resId, tag, unicode)

        list.add(multiSelectModelExtra)
        array.recycle()
    }
    typedArray.recycle()
    return list
}
