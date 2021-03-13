package io.github.getsixtyfour.openpyn.core

import android.os.Bundle
import android.view.View
import com.abdeveloper.library.MultiSelectDialog.SubmitCallbackListener
import com.naver.android.svc.annotation.RequireControlTower
import com.naver.android.svc.annotation.RequireViews
import com.naver.android.svc.annotation.SvcFragment
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionFinishedListener
import com.sonelli.juicessh.pluginlibrary.listeners.OnSessionStartedListener
import io.github.getsixtyfour.functions.showSystemUI
import io.github.getsixtyfour.ktextension.isPlayServiceAvailable
import io.github.getsixtyfour.openpyn.R
import io.github.sdsstudios.nvidiagpumonitor.listeners.OnCommandExecuteListener
import io.github.sdsstudios.nvidiagpumonitor.model.Coordinate
import mu.KotlinLogging
import tk.wasdennnoch.progresstoolbar.ProgressToolbar
import java.util.ArrayList

@SvcFragment
@RequireViews(MapViews::class)
@RequireControlTower(MapControlTower::class)
class MapFragment : AbstractMapFragment(), OnCommandExecuteListener, OnSessionStartedListener, OnSessionFinishedListener,
    SubmitCallbackListener {

    private val logger = KotlinLogging.logger {}

    val toolBar: ProgressToolbar? by lazy {
        requireActivity().findViewById(R.id.toolbar) as? ProgressToolbar
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireActivity().isPlayServiceAvailable()) {
            views.map.run {
                onCreate(savedInstanceState)
                getMapAsync(controlTower)
            }

            views.showOverlayLayout()
        } else {
            requireActivity().run {
                showSystemUI(window, views.rootView)
            }

            views.showAllButtons()
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

    override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
        controlTower.onSelected(selectedIds, selectedNames, dataString)
    }

    override fun onCancel() {
    }
}
