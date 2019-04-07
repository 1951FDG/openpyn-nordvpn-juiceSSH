package io.github.getsixtyfour.openpyn

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import com.google.android.gms.location.FusedLocationProviderClient
import com.naver.android.svc.annotation.RequireControlTower
import com.naver.android.svc.annotation.RequireViews
import com.naver.android.svc.annotation.SvcFragment
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import kotlinx.android.synthetic.main.fragment_map.map
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.error
import tk.wasdennnoch.progresstoolbar.ProgressToolbar

/**
 * @author 1951FDG
 */
@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : SVC_MapFragment(),
    AnkoLogger,
    OnSessionStartedListener,
    OnSessionFinishedListener {

    var lastLocation: Location? = null
    val toolBar: ProgressToolbar?
        get() = requireActivity().findViewById(R.id.toolbar) as? ProgressToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissions = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        requestPermissions(permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        map?.getMapAsync(controlTower)
        map?.onCreate(savedInstanceState)
        val watermark = map?.findViewWithTag<ImageView>("GoogleWatermark")

        if (watermark != null) {
            watermark.visibility = View.INVISIBLE
            /*
            val params = watermark.layoutParams as RelativeLayout.LayoutParams
            params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
            params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_START, 0)
            params.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
            */
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        fun isGranted(index: Int): Boolean {
            return (index >= 0 && index <= grantResults.lastIndex) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
        }

        fun getLastLocation() {
            FusedLocationProviderClient(requireActivity()).lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        lastLocation = location
                    }
                }
                .addOnFailureListener { e: Exception ->
                    error(e)
                }
        }

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (isGranted(0)) {
                getLastLocation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        map?.onStart()
    }

    override fun onResume() {
        super.onResume()
        map?.onResume()
    }

    override fun onPause() {
        super.onPause()
        map?.onPause()
    }

    override fun onStop() {
        super.onStop()
        map?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        map?.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        map?.onLowMemory()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        controlTower.onSessionStarted()
    }

    override fun onSessionCancelled() {
        controlTower.onSessionCancelled()
    }

    override fun onSessionFinished() {
        controlTower.onSessionFinished()
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 23
    }
}