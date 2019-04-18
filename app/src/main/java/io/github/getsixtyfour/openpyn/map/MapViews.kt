package io.github.getsixtyfour.openpyn.map

import android.view.View
import com.mayurrokade.minibar.UserMessage
import com.naver.android.svc.core.views.ActionViews
import io.github.getsixtyfour.openpyn.MainActivity
import io.github.getsixtyfour.openpyn.R
import kotlinx.android.synthetic.main.fragment_map.view.fab0
import kotlinx.android.synthetic.main.fragment_map.view.fab1
import kotlinx.android.synthetic.main.fragment_map.view.fab2
import kotlinx.android.synthetic.main.fragment_map.view.fab3
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.android.synthetic.main.fragment_map.view.minibarView

/**
 * @author 1951FDG
 */
class MapViews : ActionViews<MapViewsAction>() {

    override val layoutResId: Int = R.layout.fragment_map
    private val fab0 by lazy { rootView.fab0 }
    private val fab1 by lazy { rootView.fab1 }
    private val fab2 by lazy { rootView.fab2 }
    private val fab3 by lazy { rootView.fab3 }
    private val map by lazy { rootView.map }
    private val minibarView by lazy { rootView.minibarView }
    override fun onCreated() {
        // todo decouple
        (screen.hostActivity as? MainActivity)?.getSnackProgressBarManager()?.setViewsToMove(arrayOf(fab0, fab1))

        fab0.setOnClickListener { viewsAction.toggleCommand(fab0) }

        fab1.setOnClickListener { viewsAction.updateMasterMarker() }

        fab2.setOnClickListener { viewsAction.showCountryFilterDialog() }

        fab3.setOnClickListener { viewsAction.toggleFavoriteMarker() }
    }

    fun hideFavoriteFab() {
        fab3.hide()
    }

    fun hideListAndLocationFab() {
        fab1.hide()
        fab2.hide()
    }

    fun setAppearanceConnectFab(connected: Boolean) {
        fab0.setImageResource(
            if (connected) R.drawable.ic_flash_off_white_24dp
            else R.drawable.ic_flash_on_white_24dp
        )
    }

    fun setClickableConnectFab(clickable: Boolean) {
        fab0.isClickable = clickable
    }

    fun setClickableFabs(clickable: Boolean) {
        fab0.isClickable = clickable
        fab1.isClickable = clickable
        fab2.isClickable = clickable
    }

    fun setClickableLocationFab(clickable: Boolean) {
        fab1.isClickable = clickable
    }

    fun showAllFabs() {
        fab0.show()
        fab1.show()
        fab2.show()
    }

    fun showFavoriteFab() {
        fab3.show()
    }

    fun showListAndLocationFab() {
        fab1.show()
        fab2.show()
    }

    fun showMap() {
        map.visibility = View.VISIBLE
    }

    fun showMiniBar(userMessage: UserMessage) {
        minibarView.translationZ = 0.0f
        minibarView.show(userMessage)
    }

    fun toggleFavoriteFab(checked: Boolean) {
        fab3.isChecked = checked
        fab3.refreshDrawableState()
    }
}