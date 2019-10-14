package io.github.getsixtyfour.openpyn.map

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import com.naver.android.svc.annotation.RequireControlTower
import com.naver.android.svc.annotation.RequireViews
import com.naver.android.svc.annotation.SvcFragment
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info
import tk.wasdennnoch.progresstoolbar.ProgressToolbar

/**
 * @author 1951FDG
 */
@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : SVC_MapFragment(), AnkoLogger, OnSessionStartedListener, OnSessionFinishedListener, OnCommandExecuteListener {

    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        return controlTower.positionAndFlagForSelectedMarker()
    }

    override fun onConnect() {
        TODO("not implemented")
    }

    override fun onDisconnect() {
        TODO("not implemented")
    }

    val toolBar: ProgressToolbar? by lazy {
        requireActivity().findViewById(R.id.toolbar) as? ProgressToolbar
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        fun isGranted(index: Int): Boolean {
            return (index >= 0 && index <= grantResults.lastIndex) && (grantResults[index] == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        info("onSessionStarted")
        controlTower.onSessionStarted()
    }

    override fun onSessionCancelled() {
        info("onSessionCancelled")
        controlTower.onSessionCancelled()
    }

    override fun onSessionFinished() {
        info("onSessionFinished")
        controlTower.onSessionFinished()
    }
}
