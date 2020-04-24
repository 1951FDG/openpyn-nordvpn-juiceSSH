package io.github.getsixtyfour.openpyn.map

import android.view.View
import android.widget.ImageView
import com.mayurrokade.minibar.UserMessage
import com.naver.android.svc.core.views.ActionViews
import io.github.getsixtyfour.openpyn.MainActivity
import io.github.getsixtyfour.openpyn.R
import kotlinx.android.synthetic.main.fragment_map.view.fab0
import kotlinx.android.synthetic.main.fragment_map.view.fab1
import kotlinx.android.synthetic.main.fragment_map.view.fab2
import kotlinx.android.synthetic.main.fragment_map.view.fab3
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.android.synthetic.main.fragment_map.view.minibar

class MapViews : ActionViews<MapViewsAction>() {

    override val layoutResId: Int = R.layout.fragment_map
    private val fab0 by lazy { rootView.fab0 }
    private val fab1 by lazy { rootView.fab1 }
    private val fab2 by lazy { rootView.fab2 }
    private val fab3 by lazy { rootView.fab3 }
    private val map by lazy { rootView.map }
    private val minibarView by lazy { rootView.minibar }

    // TODO: inner classes
    override fun onCreated() {
        // TODO: decouple
        (screen.hostActivity as? MainActivity)?.mSnackProgressBarManager?.setViewsToMove(arrayOf(fab0, fab1))
        fab0.setOnClickListener { viewsAction.toggleCommand(fab0) }

        fab1.setOnClickListener { viewsAction.updateMasterMarkerWithDelay() }

        fab2.setOnClickListener { viewsAction.showCountryFilterDialog() }

        fab3.setOnClickListener { viewsAction.toggleFavoriteMarker() }
    }

    override fun onDestroy() {
        super.onDestroy()

        fab0.setOnClickListener(null)

        fab1.setOnClickListener(null)

        fab2.setOnClickListener(null)

        fab3.setOnClickListener(null)
    }

    fun callConnectFabOnClick() {
        fab0.callOnClick()
    }

    fun fakeLayoutButtons() {
        val i = 0
        fab0.layout(i, i, i, i)
        fab1.layout(i, i, i, i)
        fab2.layout(i, i, i, i)
        fab3.layout(i, i, i, i)
    }

    fun hideFavoriteButton() {
        fab3.hide()
    }

    fun hideListAndLocationButton() {
        fab1.hide()
        fab2.hide()
    }

    fun setClickableButtons(clickable: Boolean) {
        fab0.isClickable = clickable
        fab1.isClickable = clickable
        fab2.isClickable = clickable
        fab3.isClickable = clickable
    }

    fun showAllButtons() {
        fab0.show()
        fab1.show()
        fab2.show()
    }

    fun showFavoriteButton() {
        fab3.show()
    }

    fun showListAndLocationButton() {
        fab1.show()
        fab2.show()
    }

    fun showMap() {
        map.findViewWithTag<ImageView>("GoogleWatermark")?.run {
            visibility = View.INVISIBLE
            /*
            val params = layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
            */
        }
        map.visibility = View.VISIBLE
    }

    fun showMiniBar(userMessage: UserMessage) {
        minibarView.translationZ = 0.0f
        minibarView.show(userMessage)
    }

    fun toggleConnectButton(checked: Boolean) {
        fab0.isChecked = checked
        fab0.show()
    }

    fun toggleFavoriteButton(checked: Boolean) {
        fab3.isChecked = checked
        fab3.show()
    }
}
