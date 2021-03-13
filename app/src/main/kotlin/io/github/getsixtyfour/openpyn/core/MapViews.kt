package io.github.getsixtyfour.openpyn.core

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.text.TextUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.naver.android.svc.core.views.ActionViews
import io.github.getsixtyfour.openpyn.R
import kotlinx.android.synthetic.main.fragment_map.view.fab0
import kotlinx.android.synthetic.main.fragment_map.view.fab1
import kotlinx.android.synthetic.main.fragment_map.view.fab2
import kotlinx.android.synthetic.main.fragment_map.view.fab3
import kotlinx.android.synthetic.main.fragment_map.view.fab4
import kotlinx.android.synthetic.main.fragment_map.view.map
import kotlinx.android.synthetic.main.fragment_map.view.minibar
import kotlinx.android.synthetic.main.fragment_map.view.settingsfab

class MapViews : ActionViews<MapViewsAction>(), View.OnClickListener, OnApplyWindowInsetsListener {

    override val layoutResId: Int = R.layout.fragment_map

    private val fab0 by lazy { rootView.fab0 }
    private val fab1 by lazy { rootView.fab1 }
    private val fab2 by lazy { rootView.fab2 }
    private val fab3 by lazy { rootView.fab3 }
    private val fab4 by lazy { rootView.fab4 }
    private val fab5 by lazy { rootView.settingsfab }

    internal val map by lazy { rootView.map }
    private val minibarView by lazy { rootView.minibar }
    private val overlay by lazy { LayoutInflater.from(rootView.context).inflate(R.layout.overlay_progress, rootView, false) as ViewGroup }

    private val fabMarginBottom by lazy { rootView.fab0.marginBottom }
    private val fabMarginTop by lazy { rootView.fab4.marginTop }

    private val shortAnimationDuration by lazy { rootView.context.resources.getInteger(android.R.integer.config_shortAnimTime) }

    internal var systemWindowInsetBottom: Int = 0
    internal var systemWindowInsetTop: Int = 0

    override fun onCreated() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, this)

        fab0.setOnClickListener(this)
        fab1.setOnClickListener(this)
        fab2.setOnClickListener(this)
        fab3.setOnClickListener(OnFavoriteClickListener())
        fab4.setOnClickListener(this)
        fab5.setOnClickListener(this)

        initMiniBar()
    }

    override fun onDestroy() {
        super.onDestroy()

        fab0.setOnClickListener(null)
        fab1.setOnClickListener(null)
        fab2.setOnClickListener(null)
        fab3.setOnClickListener(null)
        fab4.setOnClickListener(null)
        fab5.setOnClickListener(null)
    }

    override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        val systemBars = WindowInsetsCompat.Type.systemBars()
        val systemBarsInsets = insets.getInsets(systemBars)
        (ViewCompat.requireViewById<View>(v, R.id.fab4).layoutParams as ViewGroup.MarginLayoutParams).apply {
            updateMargins(top = fabMarginTop + systemBarsInsets.top)
        }
        (ViewCompat.requireViewById<View>(v, R.id.fab0).layoutParams as ViewGroup.MarginLayoutParams).apply {
            updateMargins(bottom = fabMarginBottom + systemBarsInsets.bottom)
        }
        (ViewCompat.requireViewById<View>(v, R.id.minibar)).apply {
            updatePadding(top = systemBarsInsets.top)
        }

        systemWindowInsetTop = systemBarsInsets.top
        systemWindowInsetBottom = systemBarsInsets.bottom
        // TODO: setup listener for map, live data?
        return WindowInsetsCompat.CONSUMED
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.fab0 -> viewsAction.toggleCommand(v)
            R.id.fab1 -> viewsAction.updateMasterMarkerWithDelay()
            R.id.fab2 -> viewsAction.showCountryFilterDialog()
            R.id.fab3 -> viewsAction.toggleFavoriteMarker()
            R.id.fab4 -> viewsAction.toggleJuiceSSH()
            R.id.settingsfab -> viewsAction.toggleSettings()
        }
    }

    fun callConnectFabOnClick() {
        fab0.callOnClick()
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

    fun fakeLayoutButtons() {
        val i = 0
        fab0.layout(i, i, i, i)
        fab1.layout(i, i, i, i)
        fab2.layout(i, i, i, i)
        fab3.layout(i, i, i, i)
        fab4.layout(i, i, i, i)
        fab5.layout(i, i, i, i)
    }

    fun setClickableButtons(clickable: Boolean) {
        fab0.isClickable = clickable
        fab1.isClickable = clickable
        fab2.isClickable = clickable
        fab3.isClickable = clickable
        fab4.isClickable = clickable
        fab5.isClickable = clickable
    }

    fun showAllButtons() {
        fab0.show()
        fab1.show()
        fab2.show()
        fab4.show()
        fab5.show()
    }

    fun hideAllExceptConnectAndFavoriteButton() {
        fab1.hide()
        fab2.hide()
        fab4.hide()
        fab5.hide()
    }

    fun showAllExceptConnectAndFavoriteButton() {
        fab1.show()
        fab2.show()
        fab4.show()
        fab5.show()
    }

    fun hideFavoriteButton() {
        fab3.hide()
    }

    fun showFavoriteButton() {
        fab3.show()
    }

    fun showMap() {
        map.findViewWithTag<ImageView>("GoogleWatermark")?.let {
            it.visibility = View.GONE
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
        map.onResume()
    }

    @Suppress("MagicNumber")
    fun showMiniBar(message: CharSequence, duration: Long = 1000L) {
        minibarView.apply {
            /*setBackgroundColor(ContextCompat.getColor(context, R.color.accent_material_indigo_200))
            setTextColor(ContextCompat.getColor(context, android.R.color.white))*/
            text = message
        }.show(duration)
    }

    fun hideOverlayLayout() {
        overlay.visibility = View.GONE
        rootView.removeView(overlay)
    }

    fun showOverlayLayout() {
        rootView.addView(overlay)
        overlay.visibility = View.VISIBLE
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

    private fun initMiniBar() {
        minibarView.apply {
            dismissInterpolator = FastOutSlowInInterpolator()
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = 1
            movementMethod = ScrollingMovementMethod()
            showInterpolator = FastOutSlowInInterpolator()
        }
    }

    inner class OnFavoriteClickListener : View.OnClickListener {

        override fun onClick(v: View?) {
            viewsAction.toggleFavoriteMarker()
        }
    }
}
