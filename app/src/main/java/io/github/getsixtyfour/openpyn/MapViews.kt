package io.github.getsixtyfour.openpyn

import com.naver.android.svc.core.views.ActionViews
import kotlinx.android.synthetic.main.fragment_map.fab0
import kotlinx.android.synthetic.main.fragment_map.fab1
import kotlinx.android.synthetic.main.fragment_map.fab2
import kotlinx.android.synthetic.main.fragment_map.fab3
import kotlinx.android.synthetic.main.fragment_map.map
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_map.*
import kotlinx.android.synthetic.main.fragment_map.view.fab1

/**
 * @author 1951FDG
 */
class MapViews : ActionViews<MapViewsAction>() {

    override val layoutResId = R.layout.fragment_map

    override fun onCreated() {
        rootView.fab1
    }
}