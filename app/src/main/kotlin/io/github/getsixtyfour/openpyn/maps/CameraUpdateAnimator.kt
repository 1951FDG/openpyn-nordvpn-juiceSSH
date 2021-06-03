package io.github.getsixtyfour.openpyn.maps

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener
import com.google.android.gms.maps.model.LatLng
import java.util.ArrayList

class CameraUpdateAnimator @SuppressLint("LambdaLast") constructor(
    googleMap: GoogleMap, onCameraIdleListener: OnCameraIdleListener, animations: Collection<Animation>
) : OnCameraIdleListener {

    private val mCameraUpdates: ArrayList<Animation>
    private val mCancelableCallback = CancelableCallback()
    private val mHandler = Handler(Looper.getMainLooper())
    private var mGoogleMap: GoogleMap?
    private var mOnCameraIdleListener: OnCameraIdleListener?
    private var mIsRotateGesturesEnabled = false
    private var mIsScrollGesturesEnabled = false
    private var mIsTiltGesturesEnabled = false
    private var mIsZoomControlsEnabled = false
    private var mIsZoomGesturesEnabled = false
    var isAnimating: Boolean = false
        private set
    var animatorListener: AnimatorListener? = null

    constructor(googleMap: GoogleMap, onCameraIdleListener: OnCameraIdleListener) : this(
        googleMap, onCameraIdleListener, ArrayList<Animation>()
    )

    init {
        mGoogleMap = googleMap
        mOnCameraIdleListener = onCameraIdleListener
        mCameraUpdates = ArrayList(animations)
    }

    fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        mCancelableCallback.setAnimatorListener(null)
        animatorListener = null
        mOnCameraIdleListener = null
        mGoogleMap = null
    }

    override fun onCameraIdle() {
        executeNext()
    }

    fun add(animation: Animation): Boolean {
        return mCameraUpdates.add(animation)
    }

    fun addAll(animations: Collection<Animation>): Boolean {
        return mCameraUpdates.addAll(animations)
    }

    fun clear() {
        mCameraUpdates.clear()
    }

    fun execute() {
        if (mCameraUpdates.isNotEmpty()) {
            setUiSettings()
            onAnimationStart()
            executeNext()
        }
    }

    private fun executeNext() {
        if (mCameraUpdates.isEmpty()) {
            onAnimationEnd()
            return
        }
        val animation = mCameraUpdates.removeAt(0)
        if (animation.isCallback) {
            mCancelableCallback.setAnimation(animation)
            mCancelableCallback.setAnimatorListener(animatorListener)

            when (animation.delay) {
                0L -> {
                    startAnimation(mGoogleMap, animation, mCancelableCallback)
                }
                else -> {
                    mHandler.postDelayed(
                        { startAnimation(mGoogleMap, animation, mCancelableCallback) }, animation.delay
                    )
                }
            }
        } else {
            when (animation.delay) {
                0L -> {
                    startAnimation(mGoogleMap, animation, null)
                }
                else -> {
                    mHandler.postDelayed(
                        { startAnimation(mGoogleMap, animation, null) }, animation.delay
                    )
                }
            }
        }
    }

    private fun onAnimationEnd() {
        mOnCameraIdleListener?.onCameraIdle()

        isAnimating = false

        mGoogleMap?.uiSettings?.apply {
            isRotateGesturesEnabled = mIsRotateGesturesEnabled
            isScrollGesturesEnabled = mIsScrollGesturesEnabled
            isTiltGesturesEnabled = mIsTiltGesturesEnabled
            isZoomControlsEnabled = mIsZoomControlsEnabled
            isZoomGesturesEnabled = mIsZoomGesturesEnabled
        }

        mGoogleMap?.setOnCameraIdleListener(mOnCameraIdleListener)

        animatorListener?.onAnimationEnd()
    }

    private fun onAnimationStart() {
        isAnimating = true

        mGoogleMap?.uiSettings?.apply {
            isRotateGesturesEnabled = false
            isScrollGesturesEnabled = false
            isTiltGesturesEnabled = false
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
        }

        mGoogleMap?.setOnCameraIdleListener(this)

        animatorListener?.onAnimationStart()
    }

    private fun setUiSettings() {
        mGoogleMap?.uiSettings?.let {
            mIsRotateGesturesEnabled = it.isRotateGesturesEnabled
            mIsScrollGesturesEnabled = it.isScrollGesturesEnabled
            mIsTiltGesturesEnabled = it.isTiltGesturesEnabled
            mIsZoomControlsEnabled = it.isZoomControlsEnabled
            mIsZoomGesturesEnabled = it.isZoomGesturesEnabled
        }
    }

    class Animation(val cameraUpdate: CameraUpdate) {

        var isAnimate: Boolean = false
        var isCallback: Boolean = false
        var isClosest: Boolean = false
        var delay: Long = 0
        var tag: Any? = null
        var target: LatLng? = null
    }

    private class CancelableCallback : GoogleMap.CancelableCallback {

        private lateinit var mAnimation: Animation
        private var mAnimatorListener: AnimatorListener? = null

        override fun onCancel() {
            mAnimatorListener?.onAnimationCancel(mAnimation)
        }

        override fun onFinish() {
            mAnimatorListener?.onAnimationFinish(mAnimation)
        }

        fun setAnimation(animation: Animation) {
            mAnimation = animation
        }

        fun setAnimatorListener(animatorListener: AnimatorListener?) {
            mAnimatorListener = animatorListener
        }
    }

    interface AnimatorListener {

        /**
         *
         * Notifies the start of the animation.
         */
        fun onAnimationStart()

        /**
         *
         * Notifies the end of the animation.
         */
        fun onAnimationEnd()

        /**
         *
         * Notifies the finishing of the animation.
         *
         * @param animation The animation which was finished.
         */
        fun onAnimationFinish(animation: Animation)

        /**
         *
         * Notifies the cancellation of the animation.
         *
         * @param animation The animation which was canceled.
         */
        fun onAnimationCancel(animation: Animation)
    }

    companion object {

        internal fun startAnimation(
            googleMap: GoogleMap?, animation: Animation, cancelableCallback: GoogleMap.CancelableCallback?
        ) {
            if (animation.isAnimate) {
                googleMap?.animateCamera(animation.cameraUpdate, cancelableCallback)
            } else {
                googleMap?.moveCamera(animation.cameraUpdate)
            }
        }
    }
}
