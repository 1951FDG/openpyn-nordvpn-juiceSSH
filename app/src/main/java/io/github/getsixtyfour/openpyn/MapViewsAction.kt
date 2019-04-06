package io.github.getsixtyfour.openpyn

import android.view.View
import com.naver.android.svc.core.views.ViewsAction

/**
 * @author 1951FDG
 */
interface MapViewsAction : ViewsAction {

    fun toggleFavoriteMarker()

    fun showCountryFilterDialog()

    fun toggleCommand(v: View?)

    fun updateMasterMarker(show: Boolean = false)
}