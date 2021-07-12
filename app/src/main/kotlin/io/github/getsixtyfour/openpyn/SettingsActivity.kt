package io.github.getsixtyfour.openpyn

import android.os.Bundle
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import com.google.android.material.transition.platform.MaterialFadeThrough
import io.github.getsixtyfour.openpyn.preference.AboutPreferenceFragment
import io.github.getsixtyfour.openpyn.preference.ApiPreferenceFragment
import io.github.getsixtyfour.openpyn.preference.GeneralPreferenceFragmentDirections
import io.github.getsixtyfour.openpyn.preference.ManagementPreferenceFragment
import kotlinx.android.synthetic.main.activity_settings.toolbar

class SettingsActivity : AppCompatActivity(R.layout.activity_settings), OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.enterTransition = MaterialFadeThrough()
        window.returnTransition = MaterialFadeThrough()

        // This app draws behind the system bars, so we want to handle fitting system windows
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = WindowInsetsCompat.Type.systemBars()
            val systemBarsInsets = insets.getInsets(systemBars)
            (v.layoutParams as? MarginLayoutParams)?.updateMargins(top = systemBarsInsets.top)
            WindowInsetsCompat.Builder(insets).apply {
                setInsets(systemBars, Insets.of(systemBarsInsets.left, 0, systemBarsInsets.right, systemBarsInsets.bottom))
            }.build()
        }

        toolbar.hideProgress()
        toolbar.isIndeterminate = true
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(setOf(), null, ::onSupportNavigateUp)
        NavigationUI.setupWithNavController(toolbar, navController, appBarConfiguration)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        when (pref.fragment) {
            AboutPreferenceFragment::class.java.name -> {
                navController.navigate(GeneralPreferenceFragmentDirections.actionGeneralPreferenceFragmentToAboutPreferenceFragment())
            }
            ApiPreferenceFragment::class.java.name -> {
                navController.navigate(GeneralPreferenceFragmentDirections.actionGeneralPreferenceFragmentToApiPreferenceFragment())
            }
            ManagementPreferenceFragment::class.java.name -> {
                navController.navigate(GeneralPreferenceFragmentDirections.actionGeneralPreferenceFragmentToManagementPreferenceFragment())
            }
        }

        return true
    }

    companion object {

        private val sBindPreferenceSummaryToValueListener: Preference.OnPreferenceChangeListener
            get() = Preference.OnPreferenceChangeListener { preference, value ->
                val stringValue = "$value"

                if (preference is ListPreference) {
                    // For list preferences, look up the correct display value in the preference's 'entries' list
                    val index = preference.findIndexOfValue(stringValue)
                    // Set the summary to reflect the new value
                    preference.summary = if (index >= 0) preference.entries[index] else null
                } else {
                    when {
                        stringValue.isEmpty() -> preference.summary = preference.context.getString(R.string.not_set)
                        // For all other preferences, set the summary to the value's simple string representation
                        else -> preference.summary = stringValue
                    }
                }
                true
            }

        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.

         * @see sBindPreferenceSummaryToValueListener
         */
        fun bindPreferenceSummaryToValue(preference: Preference) {
            // Set the listener to watch for value changes
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            // Trigger the listener immediately with the preference's current value
            val newValue = preference.sharedPreferences.getString(preference.key, "")
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, newValue)
        }
    }
}
