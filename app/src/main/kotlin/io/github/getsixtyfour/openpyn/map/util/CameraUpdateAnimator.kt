package io.github.getsixtyfour.openpyn.map.util

import android.annotation.SuppressLint
import android.os.Handler
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
    private val mHandler = Handler()
    private var mGoogleMap: GoogleMap?
    private var mOnCameraIdleListener: OnCameraIdleListener?
    var isAnimating: Boolean = false
        private set
    var animatorListener: AnimatorListener? = null
    private var mIsRotateGestureEnabled = false
    private var mIsScrollGestureEnabled = false
    private var mIsTiltGestureEnabled = false
    private var mIsZoomControlsEnabled = false
    private var mIsZoomGestureEnabled = false

    constructor(googleMap: GoogleMap, onCameraIdleListener: OnCameraIdleListener) : this(
        googleMap, onCameraIdleListener, ArrayList<Animation>()
    )

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

    fun onDestroy() {
        mHandler.removeCallbacksAndMessages(null)
        mCancelableCallback.setAnimatorListener(null)
        animatorListener = null
        mOnCameraIdleListener = null
        mGoogleMap = null
    }

    private fun executeNext() {
        if (mCameraUpdates.isEmpty()) {
            onAnimationEnd()
        } else {
            val animation = mCameraUpdates.removeAt(0)
            if (animation.isCallback) {
                mCancelableCallback.setAnimation(animation)
                mCancelableCallback.setAnimatorListener(animatorListener)
                if (animation.delay == 0L) {
                    startAnimation(mGoogleMap!!, animation, mCancelableCallback)
                } else {
                    mHandler.postDelayed({
                        startAnimation(
                            mGoogleMap!!, animation, mCancelableCallback
                        )
                    }, animation.delay)
                }
            } else {
                if (animation.delay == 0L) {
                    startAnimation(mGoogleMap!!, animation, null)
                } else {
                    mHandler.postDelayed(
                        { startAnimation(mGoogleMap!!, animation, null) }, animation.delay
                    )
                }
            }
        }
    }

    private fun onAnimationEnd() {
        mOnCameraIdleListener!!.onCameraIdle()
        if (animatorListener != null) {
            animatorListener!!.onAnimationEnd()
        }
        mGoogleMap!!.setOnCameraIdleListener(mOnCameraIdleListener)
        val settings = mGoogleMap!!.uiSettings
        settings.isRotateGesturesEnabled = mIsRotateGestureEnabled
        settings.isScrollGesturesEnabled = mIsScrollGestureEnabled
        settings.isTiltGesturesEnabled = mIsTiltGestureEnabled
        settings.isZoomControlsEnabled = mIsZoomControlsEnabled
        settings.isZoomGesturesEnabled = mIsZoomGestureEnabled
        isAnimating = false
    }

    private fun onAnimationStart() {
        if (animatorListener != null) {
            animatorListener!!.onAnimationStart()
        }
        val settings = mGoogleMap!!.uiSettings
        settings.isRotateGesturesEnabled = false
        settings.isScrollGesturesEnabled = false
        settings.isTiltGesturesEnabled = false
        settings.isZoomControlsEnabled = false
        settings.isZoomGesturesEnabled = false
        mGoogleMap!!.setOnCameraIdleListener(this)
        isAnimating = true
    }

    private fun setUiSettings() {
        val settings = mGoogleMap!!.uiSettings
        mIsRotateGestureEnabled = settings.isRotateGesturesEnabled
        mIsScrollGestureEnabled = settings.isScrollGesturesEnabled
        mIsTiltGestureEnabled = settings.isTiltGesturesEnabled
        mIsZoomControlsEnabled = settings.isZoomControlsEnabled
        mIsZoomGestureEnabled = settings.isZoomGesturesEnabled
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

    class Animation(val cameraUpdate: CameraUpdate) {
        var isAnimate: Boolean = false
        var isCallback: Boolean = false
        var isClosest: Boolean = false
        var delay: Long = 0
        var tag: Any? = null
        var target: LatLng? = null
    }

    private class CancelableCallback internal constructor() : GoogleMap.CancelableCallback {
        private var mAnimation: Animation? = null
        private var mAnimatorListener: AnimatorListener? = null
        override fun onCancel() {
            if (mAnimatorListener != null) {
                mAnimatorListener!!.onAnimationCancel(mAnimation!!)
            }
        }

        override fun onFinish() {
            if (mAnimatorListener != null) {
                mAnimatorListener!!.onAnimationFinish(mAnimation!!)
            }
        }

        fun setAnimation(animation: Animation?) {
            mAnimation = animation
        }

        fun setAnimatorListener(animatorListener: AnimatorListener?) {
            mAnimatorListener = animatorListener
        }
    }

    companion object {
        private fun startAnimation(
            googleMap: GoogleMap, animation: Animation, cancelableCallback: GoogleMap.CancelableCallback?
        ) {
            if (animation.isAnimate) {
                if (cancelableCallback != null) {
                    googleMap.animateCamera(animation.cameraUpdate, cancelableCallback)
                } else {
                    googleMap.animateCamera(animation.cameraUpdate)
                }
            } else {
                googleMap.moveCamera(animation.cameraUpdate)
            }
        }
    }

    init {
        mGoogleMap = googleMap
        mOnCameraIdleListener = onCameraIdleListener
        mCameraUpdates = ArrayList(animations)
    }
}
