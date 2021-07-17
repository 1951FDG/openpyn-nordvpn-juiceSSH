package io.github.getsixtyfour.openpyn.preference

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.core.view.ViewCompat
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.platform.MaterialFadeThrough
import io.github.getsixtyfour.functions.dpToPx
import io.github.getsixtyfour.openpyn.R

class ApiPreferenceFragment : PreferenceFragmentCompat() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)

        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFadeThrough()

        findPreference<ListPreference>("pref_geo_client")?.apply {
            summaryProvider = Preference.SummaryProvider<ListPreference> {
                when (val entry = it.entry) {
                    "ipdata.co" -> "\uD83D\uDEE1 $entry (key required)"
                    "ipinfo.io" -> "\uD83D\uDEE1 $entry (key optional)"
                    "ipregistry.co" -> "\uD83D\uDEE1 $entry (key required)"
                    "ipstack.com" -> "$entry (key required)"
                    null, "" -> it.context.getString(R.string.not_set)
                    else -> entry
                }
            }
        }

        findPreference<EditTextPreference>("pref_api_ipdata")?.apply {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>("pref_api_ipinfo")?.apply {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>("pref_api_ipregistry")?.apply {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>("pref_api_ipstack")?.apply {
            setSummaryProvider(::provideSummary)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_api, rootKey)
    }

    override fun onCreateRecyclerView(inflater: LayoutInflater, parent: ViewGroup, savedInstanceState: Bundle?): RecyclerView {
        val view = super.onCreateRecyclerView(inflater, parent, savedInstanceState).also { it.clipToPadding = true }

        ViewCompat.setScrollIndicators(
            view,
            ViewCompat.SCROLL_INDICATOR_TOP or ViewCompat.SCROLL_INDICATOR_BOTTOM,
            ViewCompat.SCROLL_INDICATOR_TOP or ViewCompat.SCROLL_INDICATOR_BOTTOM
        )
        /*(activity as? AppCompatActivity)?.supportActionBar?.onScrollListener?.let(view::addOnScrollListener)*/
        return view
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this

    companion object {

        @Suppress("MagicNumber", "unused")
        internal val ActionBar.onScrollListener: RecyclerView.OnScrollListener
            get() = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    elevation = if (recyclerView.canScrollVertically(-1)) dpToPx(4F, recyclerView.context) else 0F
                }
            }

        fun provideSummary(preference: Preference): CharSequence {
            return when (preference.sharedPreferences.getString(preference.key, null)) {
                null, "" -> preference.context.getString(R.string.not_set)
                else -> preference.context.getString(R.string.key_set)
            }
        }
    }
}
