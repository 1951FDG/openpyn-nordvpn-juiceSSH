package io.github.getsixtyfour.openpyn.map

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.text.SpannableString
import androidx.annotation.RawRes
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.antoniocarlon.map.CameraUpdateAnimator.Animation
import com.cocoahero.android.gmaps.addons.mapbox.MapBoxOfflineTileProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.jayrave.moshi.pristineModels.PristineModelsJsonAdapterFactory
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.utils.MultiSelectMapper
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
internal fun fileBackedTileProvider(): MapBoxOfflineTileProvider {
    // Use a file backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro")
    logger.debug { "$tileProvider" }
    return tileProvider
}

@Suppress("unused", "SpellCheckingInspection")
internal fun memoryBackedTileProvider(): MapBoxOfflineTileProvider {
    // Use a memory backed SQLite database
    val tileProvider = MapBoxOfflineTileProvider("file:world.mbtiles?vfs=ndk-asset&immutable=1&mode=ro", null)
    logger.debug { "$tileProvider" }
    return tileProvider
}

internal fun getCameraUpdates(): ArrayList<Animation> {
    // Load all map tiles
    @Suppress("MagicNumber") val z = 3
    //val z = tileProvider!!.minimumZoom.toInt()
    val rows = 2.0.pow(z.toDouble()).toInt() - 1
    val cameraUpdates = ArrayList<Animation>(rows)
    // Traverse through all rows
    for (y in 0..rows) {
        for (x in 0..rows) {
            val bounds = MapBoxOfflineTileProvider.calculateTileBounds(x, y, z)
            val cameraPosition = CameraPosition.Builder().target(bounds.northeast).build()
            val animation = Animation(CameraUpdateFactory.newCameraPosition(cameraPosition))
            // Add animations
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
    for ((id, ext) in list) {
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
        file.outputStream().buffered().use { output ->
            input.copyTo(output)
        }
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
internal fun countryList(context: Context, @RawRes id: Int): List<MultiSelectable> {
    val json = context.resources.openRawResource(id).bufferedReader().use(BufferedReader::readText)
    val factory = PristineModelsJsonAdapterFactory.Builder().apply { add(MultiSelectModelExtra::class.java, MultiSelectMapper()) }
    val moshi = Moshi.Builder().add(factory.build()).add(object {
        @ToJson
        @Suppress("unused")
        fun toJson(value: CharSequence): String = "$value"

        @FromJson
        @Suppress("unused")
        fun fromJson(value: String): CharSequence = SpannableString(value)
    }).build()
    val listType = Types.newParameterizedType(List::class.java, MultiSelectModelExtra::class.java)
    val adapter: JsonAdapter<List<MultiSelectModelExtra>> = moshi.adapter(listType)
    return adapter.nonNull().fromJson(json).orEmpty()
}
