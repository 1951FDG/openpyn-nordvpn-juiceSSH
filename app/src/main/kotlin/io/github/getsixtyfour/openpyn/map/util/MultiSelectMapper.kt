package io.github.getsixtyfour.openpyn.map.util

import com.abdeveloper.library.MultiSelectModelExtra
import com.jayrave.moshi.pristineModels.Mapper
import com.jayrave.moshi.pristineModels.PropertyExtractor
import com.jayrave.moshi.pristineModels.Value
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
