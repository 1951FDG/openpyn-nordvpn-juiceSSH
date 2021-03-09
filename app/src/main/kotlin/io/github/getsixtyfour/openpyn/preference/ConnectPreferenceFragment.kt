package io.github.getsixtyfour.openpyn.preference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.getsixtyfour.openpyn.R

/**
 * This fragment shows Connect settings preferences only.
 */
@Suppress("unused")
class ConnectPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_connect)
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
        // Set up scroll indicators (Marshmallow or newer)
        ViewCompat.setScrollIndicators(
            view,
            ViewCompat.SCROLL_INDICATOR_TOP or ViewCompat.SCROLL_INDICATOR_BOTTOM,
            ViewCompat.SCROLL_INDICATOR_TOP or ViewCompat.SCROLL_INDICATOR_BOTTOM
        )
        return view
    }
}
