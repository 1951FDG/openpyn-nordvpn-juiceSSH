package io.github.getsixtyfour.openpyn.map

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.mayurrokade.minibar.UserMessage
import com.naver.android.svc.core.views.ActionViews
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.dpToPxSize
import kotlinx.android.synthetic.main.fragment_map.view.fab0
import kotlinx.android.synthetic.main.fragment_map.view.fab1
import kotlinx.android.synthetic.main.fragment_map.view.fab2
import kotlinx.android.synthetic.main.fragment_map.view.fab3
import kotlinx.android.synthetic.main.fragment_map.view.fab4
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.android.synthetic.main.fragment_map.view.minibar
import kotlinx.android.synthetic.main.fragment_map.view.settingsfab

class MapViews : ActionViews<MapViewsAction>() {

    override val layoutResId: Int = R.layout.fragment_map

    private val fab0 by lazy { rootView.fab0 }
    private val fab1 by lazy { rootView.fab1 }
    private val fab2 by lazy { rootView.fab2 }
    private val fab3 by lazy { rootView.fab3 }
    private val fab4 by lazy { rootView.fab4 }
    private val settingsFab by lazy { rootView.settingsfab }

    private val map by lazy { rootView.map }
    private val minibarView by lazy { rootView.minibar }
    private val overlay by lazy { LayoutInflater.from(rootView.context).inflate(R.layout.overlay_progress, rootView, false) as ViewGroup }

    private val fabMarginBottom by lazy { rootView.fab0.marginBottom }
    private val fabMarginTop by lazy { rootView.fab4.marginTop }

    private val shortAnimationDuration by lazy { rootView.context.resources.getInteger(android.R.integer.config_shortAnimTime) }

    internal var systemWindowInsetBottom: Int = 0
    internal var systemWindowInsetTop: Int = 0

    // TODO: inner classes
    override fun onCreated() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val systemBars = WindowInsetsCompat.Type.systemBars()
            val systemBarsInsets = insets.getInsets(systemBars)
            (ViewCompat.requireViewById<View>(v, R.id.fab4).layoutParams as ViewGroup.MarginLayoutParams).apply {
                updateMargins(top = fabMarginTop + systemBarsInsets.top)
            }
            (ViewCompat.requireViewById<View>(v, R.id.fab0).layoutParams as ViewGroup.MarginLayoutParams).apply {
                updateMargins(bottom = fabMarginBottom + systemBarsInsets.bottom)
            }

            systemWindowInsetTop = systemBarsInsets.top
            systemWindowInsetBottom = systemBarsInsets.bottom
            // TODO: setup listener for map, live data?
            WindowInsetsCompat.CONSUMED
        }
        fab0.setOnClickListener(viewsAction::toggleCommand)

        fab1.setOnClickListener { viewsAction.updateMasterMarkerWithDelay() }

        fab2.setOnClickListener { viewsAction.showCountryFilterDialog() }

        fab3.setOnClickListener { viewsAction.toggleFavoriteMarker() }

        fab4.setOnClickListener { viewsAction.toggleJuiceSSH() }

        settingsFab.setOnClickListener { viewsAction.toggleSettings() }

        showOverlayLayout()
    }

    private fun addOverlayLayout() = rootView.addView(overlay)

    private fun removeOverlayLayout() = rootView.removeView(overlay)

    fun hideOverlayLayout() {
        overlay.visibility = View.GONE
        removeOverlayLayout()
    }

    fun showOverlayLayout() {
        addOverlayLayout()
        overlay.visibility = View.VISIBLE
    }

    fun crossFadeOverlayLayout() {
        map.run {
            /*alpha = 0f*/
            animate().apply {
                alpha(1f)
                duration = shortAnimationDuration.toLong()
                setListener(null)
            }
        }
        overlay.run {
            animate().apply {
                alpha(0f)
                duration = shortAnimationDuration.toLong()
                setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        (getChildAt(0) as? CircularProgressIndicator)?.isIndeterminate = false
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        hideOverlayLayout()
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        fab0.setOnClickListener(null)

        fab1.setOnClickListener(null)

        fab2.setOnClickListener(null)

        fab3.setOnClickListener(null)

        fab4.setOnClickListener(null)

        settingsFab.setOnClickListener(null)
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
        fab4.layout(i, i, i, i)

        settingsFab.layout(i, i, i, i)
    }

    fun hideFavoriteButton() {
        fab3.hide()
    }

    fun hideListAndLocationButton() {
        fab1.hide()
        fab2.hide()
        fab4.hide()

        settingsFab.hide()
    }

    fun setClickableButtons(clickable: Boolean) {
        fab0.isClickable = clickable
        fab1.isClickable = clickable
        fab2.isClickable = clickable
        fab3.isClickable = clickable
        fab4.isClickable = clickable

        settingsFab.isClickable = clickable
    }

    fun showAllButtons() {
        fab0.show()
        fab1.show()
        fab2.show()
        fab4.show()

        settingsFab.show()
    }

    fun showFavoriteButton() {
        fab3.show()
    }

    fun showListAndLocationButton() {
        fab1.show()
        fab2.show()
        fab4.show()

        settingsFab.show()
    }

    fun showMap() {
        map.findViewWithTag<ImageView>("GoogleWatermark")?.let {
            it.visibility = View.INVISIBLE
            /*
            val params = it.layoutParams as RelativeLayout.LayoutParams
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

    @Suppress("MagicNumber")
    fun showMiniBar(userMessage: UserMessage) {
        minibarView.run {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM
            minHeight = systemWindowInsetTop + dpToPxSize(14F, context)
            translationZ = 0.0f
            show(userMessage)
        }
    }

    fun toggleConnectButton(checked: Boolean) {
        fab0.run {
            isChecked = checked
            show()
        }
    }

    fun toggleFavoriteButton(checked: Boolean) {
        fab3.run {
            isChecked = checked
            show()
        }
    }
}
