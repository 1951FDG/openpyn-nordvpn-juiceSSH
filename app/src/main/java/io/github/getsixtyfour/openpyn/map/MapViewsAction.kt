package io.github.getsixtyfour.openpyn.map

import android.view.View
import com.naver.android.svc.core.views.ViewsAction

/**
 * @author 1951FDG
 */
interface MapViewsAction : ViewsAction {

    fun showCountryFilterDialog()
    fun toggleCommand(v: View?)
    fun toggleFavoriteMarker()
    fun updateMasterMarker(show: Boolean = false)
}