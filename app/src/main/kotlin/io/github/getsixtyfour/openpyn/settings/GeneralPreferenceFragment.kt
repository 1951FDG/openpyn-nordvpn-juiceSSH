package io.github.getsixtyfour.openpyn.settings

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceFragmentCompat.OnPreferenceStartFragmentCallback
import com.google.android.material.textfield.TextInputLayout
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.onRefreshItemSelected
import io.github.getsixtyfour.openpyn.utils.NetworkInfo

/**
 * This fragment shows General settings preferences only.
 */
class GeneralPreferenceFragment : PreferenceFragmentCompat(), OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)

        findPreference<EditTextPreference>("pref_server")?.run {
            setOnBindEditTextListener {
                it.addTextChangedListener(it.textInputLayout()?.serverErrorTextWatcher)
            }
            summaryProvider = Preference.SummaryProvider<EditTextPreference> {
                when (val text = it.text) {
                    null, "" -> it.context.getString(R.string.not_set)
                    else -> "$text.nordvpn.com"
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_settings, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_refresh -> {
                if (!NetworkInfo.getInstance().isOnline()) return true
                lifecycleScope.onRefreshItemSelected(requireActivity(), item)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val activity = requireActivity()
        val fragmentManager = activity.supportFragmentManager
        val args = pref.extras
        val fragment = fragmentManager.fragmentFactory.instantiate(activity.classLoader, pref.fragment)
        fragment.arguments = args
        fragment.setTargetFragment(caller, 0)
        fragmentManager.beginTransaction().replace((requireView().parent as View).id, fragment).addToBackStack(null).commit()

        return true
    }

    companion object {
        internal val TextInputLayout.serverErrorTextWatcher: AbstractTextWatcher
            get() = object : AbstractTextWatcher(this) {
                val array = context.resources.getTextArray(R.array.pref_country_values)
                val regex = Regex("""^[a-z]{2}\d{1,4}$""")
                val message = ctx.getString(R.string.pref_server_error)
                override val submitButtonId: Int
                    get() = android.R.id.button1

                init {
                    textInputLayout.helperText = ctx.getString(R.string.pref_server_helper_text)
                }

                override fun validate(s: String): String? = if (s.isEmpty()) null else try {
                    require(regex.matches(s)) { message }
                    require(isServerName(s.take(2))) { message }
                    null
                } catch (e: Exception) {
                    e.message
                }

                fun isServerName(name: String): Boolean = when (name) {
                    "uk" -> true
                    else -> array.contains(name)
                }
            }

        fun EditText.textInputLayout(): TextInputLayout? = (parent.parent as? TextInputLayout)
    }
}
