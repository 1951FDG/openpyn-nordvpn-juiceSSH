package io.github.getsixtyfour.openpyn.map.util

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.android.material.tooltip.TooltipDrawable
import io.github.getsixtyfour.openpyn.R
import kotlin.math.sqrt

@SuppressLint("RestrictedApi")
class MaterialInfoWindowAdapter(activity: Activity) : GoogleMap.InfoWindowAdapter {

    @SuppressLint("InflateParams")
    private val view: ViewGroup? = activity.layoutInflater.inflate(R.layout.info_window, null) as? ViewGroup
    private val drawable = TooltipDrawable.createFromAttributes(activity, null, 0, R.style.Widget_App_Tooltip)
    private val arrowSize = activity.resources.getDimensionPixelSize(R.dimen.mtrl_tooltip_arrowSize)

    init {
        view?.updatePadding(top = ((arrowSize * sqrt(2.0) - arrowSize).toInt()))

        drawable.run {
            /*shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS*/
            setRelativeToView(view)
            detachView(view)
        }
    }

    override fun getInfoWindow(marker: Marker): View? = view?.getChildAt(0)?.let {
        drawable.text = marker.title
        it.background = drawable
        it.minimumWidth = drawable.minWidth
        it.rootView
    }

    override fun getInfoContents(marker: Marker): View? {
        return null
    }
}
