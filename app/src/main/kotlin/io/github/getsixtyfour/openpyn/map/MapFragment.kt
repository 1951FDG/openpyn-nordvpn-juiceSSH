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
import mu.KLogging
import tk.wasdennnoch.progresstoolbar.ProgressToolbar

@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : AbstractMapFragment(), OnCommandExecuteListener, OnSessionStartedListener, OnSessionFinishedListener {

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
        logger.debug("positionAndFlagForSelectedMarker")
        return controlTower.positionAndFlagForSelectedMarker()
    }

    override fun onConnect() {
        logger.debug("onConnect")
        controlTower.onConnect()
    }

    override fun onDisconnect() {
        logger.debug("onDisconnect")
        controlTower.onDisconnect()
    }

    override fun onSessionCancelled() {
        logger.debug("onSessionCancelled")
        controlTower.onSessionCancelled()
    }

    override fun onSessionStarted(sessionId: Int, sessionKey: String) {
        logger.debug("onSessionStarted")
        controlTower.onSessionStarted()
    }

    override fun onSessionFinished() {
        logger.debug("onSessionFinished")
        controlTower.onSessionFinished()
    }

    companion object : KLogging()
}
