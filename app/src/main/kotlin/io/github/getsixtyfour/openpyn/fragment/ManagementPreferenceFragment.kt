package io.github.getsixtyfour.openpyn.fragment

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R

/**
 * This fragment shows API settings preferences only.
 */
class ManagementPreferenceFragment : PreferenceFragmentCompat() {

    override fun onDetach() {
        super.onDetach()
        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_activity_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_openvpnmgmt, rootKey)
        setTitle(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this
}
