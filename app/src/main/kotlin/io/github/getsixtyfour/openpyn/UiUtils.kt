package io.github.getsixtyfour.openpyn

import android.content.Context
import android.util.DisplayMetrics
import android.util.TypedValue
import androidx.annotation.Dimension
import androidx.annotation.Px

/**
 * Convert a dimension value in density independent pixels to pixels.
 *
 * @param dp the dimension value in density independent pixels
 * @param context the context to get the [DisplayMetrics]
 * @return the pixels
 *
 * @see TypedValue.complexToDimension
 */
@Dimension
fun dpToPx(@Dimension(unit = Dimension.DP) dp: Float, context: Context): Float {
    val metrics = context.resources.displayMetrics
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, metrics
    )
}

/**
 * Convert a dimension value in density independent pixels to an integer pixel offset.
 *
 * @param dp the dimension value in density independent pixels
 * @param context the context to get the [DisplayMetrics]
 * @return the integer pixel offset
 *
 * @see TypedValue.complexToDimensionPixelOffset
 */
@Px
fun dpToPxOffset(@Dimension(unit = Dimension.DP) dp: Float, context: Context): Int {
    return dpToPx(dp, context).toInt()
}

/**
 * Convert a dimension value in density independent pixels to an integer pixel size.
 *
 * @param dp the dimension value in density independent pixels
 * @param context the context to get the [DisplayMetrics]
 * @return the integer pixel size
 *
 * @see TypedValue.complexToDimensionPixelSize
 */
@Px
fun dpToPxSize(@Dimension(unit = Dimension.DP) dp: Float, context: Context): Int {
    val value = dpToPx(dp, context)
    val size = (if (value >= 0) value + 0.5f else value - 0.5f).toInt()
    return when {
        size != 0 -> size
        value == 0f -> 0
        value > 0 -> 1
        else -> -1
    }
}
