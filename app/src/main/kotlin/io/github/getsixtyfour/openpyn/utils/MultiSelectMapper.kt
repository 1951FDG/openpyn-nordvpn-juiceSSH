package io.github.getsixtyfour.openpyn.utils

import android.content.Context
import android.text.SpannableString
import androidx.annotation.RawRes
import com.abdeveloper.library.MultiSelectModelExtra
import com.abdeveloper.library.MultiSelectable
import com.jayrave.moshi.pristineModels.Mapper
import com.jayrave.moshi.pristineModels.PristineModelsJsonAdapterFactory
import com.jayrave.moshi.pristineModels.PropertyExtractor
import com.jayrave.moshi.pristineModels.Value
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.Types
import java.io.BufferedReader
import java.lang.reflect.Type

class MultiSelectMapper : Mapper<MultiSelectModelExtra>() {

    private val id = field("id", false, object : PropertyExtractor<MultiSelectModelExtra, Int> {
        override val type: Type = Int::class.javaPrimitiveType!!
        override fun extractFrom(t: MultiSelectModelExtra): Int = t.id
    })
    private val name = field("country", false, object : PropertyExtractor<MultiSelectModelExtra, CharSequence> {
        override val type: Type = CharSequence::class.javaObjectType
        override fun extractFrom(t: MultiSelectModelExtra): CharSequence = t.name
    })
    private val tag = field("flag", false, object : PropertyExtractor<MultiSelectModelExtra, String> {
        override val type: Type = String::class.javaObjectType
        override fun extractFrom(t: MultiSelectModelExtra): String = t.tag
    })
    private val unicode = field("emoji", false, object : PropertyExtractor<MultiSelectModelExtra, String> {
        override val type: Type = String::class.javaObjectType
        override fun extractFrom(t: MultiSelectModelExtra): String = t.unicode
    })

    override fun create(value: Value<MultiSelectModelExtra>): MultiSelectModelExtra {
        return MultiSelectModelExtra(value of id, value of name, 0, value of tag, value of unicode)
    }
}

internal fun countryListFromJson(context: Context, @RawRes id: Int): List<MultiSelectable> {
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
