package io.github.getsixtyfour.openpyn

import com.naver.android.svc.annotation.ControlTower
import com.naver.android.svc.annotation.RequireScreen
import com.naver.android.svc.annotation.RequireViews

/**
 * @author 1951FDG
 */
@ControlTower
@RequireViews(MapViews::class)
@RequireScreen(MapFragment::class)
class MapControlTower : SVC_MapControlTower(), MapViewsAction {

    override fun onCreated() {
    }
}