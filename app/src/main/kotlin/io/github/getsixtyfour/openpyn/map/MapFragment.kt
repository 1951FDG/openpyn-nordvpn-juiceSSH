package io.github.getsixtyfour.openpyn.map

import android.os.Bundle
import android.view.View
import com.google.android.gms.maps.MapView
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

@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : AbstractMapFragment(), AnkoLogger, OnCommandExecuteListener, OnSessionStartedListener, OnSessionFinishedListener {

    val toolBar: ProgressToolbar? by lazy {
        requireActivity().findViewById(R.id.toolbar) as? ProgressToolbar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.findViewById<MapView>(R.id.map)?.run {
            onCreate(savedInstanceState)
            getMapAsync(controlTower)
        }
    }

    override fun positionAndFlagForSelectedMarker(): Pair<Coordinate?, String> {
        info("positionAndFlagForSelectedMarker")
        return controlTower.positionAndFlagForSelectedMarker()
    }

    override fun onConnect() {
        info("onConnect")
        controlTower.onConnect()
    }

    override fun onDisconnect() {
        info("onDisconnect")
        controlTower.onDisconnect()
    }

    override fun onSessionCancelled() {
        info("onSessionCancelled")
        controlTower.onSessionCancelled()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        info("onSessionStarted")
        controlTower.onSessionStarted()
    }

    override fun onSessionFinished() {
        info("onSessionFinished")
        controlTower.onSessionFinished()
    }
}
