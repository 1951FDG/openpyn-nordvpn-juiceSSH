package io.github.getsixtyfour.openpyn.settings

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import com.eggheadgames.aboutbox.AboutBoxUtils
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.material.transition.platform.MaterialFadeThrough
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R
import io.github.getsixtyfour.openpyn.dpToPx
import io.github.getsixtyfour.openpyn.getVersionTitleType
import io.github.getsixtyfour.openpyn.onLicensesItemSelected

/**
 * This fragment shows About settings preferences only.
 */
class AboutPreferenceFragment : PreferenceFragmentCompat() {

    override fun onStop() {
        super.onStop()

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)

        enterTransition = MaterialFadeThrough()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val activity = requireActivity()
        val root = preferenceManager.createPreferenceScreen(activity)

        root.setTitle(R.string.title_about)

        addAboutPreferences(activity, root)

        addSupportPreferences(activity, root)

        addOtherPreferences(activity, root)

        preferenceScreen = root
        setTitle(activity)
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

    private fun addAboutPreferences(activity: Activity, root: PreferenceScreen) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(R.string.pref_category_app)

        root.addPreference(category)

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_github_black_24dp)
            title = activity.getString(R.string.egab_author)
            summary = activity.getString(R.string.github_repo_name)
            onPreferenceClickListener = OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, activity.getString(R.string.github_repo_url))
                true
            }
        })
        // TODO: go to google play or github depending on version
        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_info_outline_black_24dp)
            title = activity.getString(getVersionTitleType(activity))
            summary = activity.getString(R.string.app_version)
            onPreferenceClickListener = OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(
                    activity, "${activity.getString(R.string.github_repo_url)}/tree/${activity.getString(R.string.git_commit_id)}"
                )
                true
            }
        })

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_history_black_24dp)
            title = activity.getString(R.string.egab_changelog)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, "${activity.getString(R.string.github_repo_url)}/releases")
                true
            }
        })
    }

    private fun addSupportPreferences(activity: Activity, root: PreferenceScreen) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(R.string.pref_category_support)

        root.addPreference(category)

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_bug_report_black_24dp)
            title = activity.getString(R.string.egab_submit_issue)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, "${activity.getString(R.string.github_repo_url)}/issues/new")
                true
            }
        })

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_person_black_24dp)
            title = activity.getString(R.string.egab_contact_support)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                EmailUtil.contactUs(activity)
                true
            }
        })
    }

    private fun addOtherPreferences(activity: Activity, root: PreferenceScreen) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(R.string.pref_category_other)

        root.addPreference(category)

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_star_face_black_24dp)
            title = activity.getString(R.string.egab_leave_review)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                AboutBoxUtils.openApp(activity, AboutConfig.BuildType.GOOGLE, activity.getString(R.string.app_id))
                true
            }
        })

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_share_black_24dp)
            title = activity.getString(R.string.egab_share)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                ShareUtil.share(activity)
                true
            }
        })

        category.addPreference(Preference(activity).apply {
            icon = ContextCompat.getDrawable(activity, R.drawable.ic_copyleft_green_24dp)
            title = activity.getString(R.string.oss_license_title)
            summary = null
            onPreferenceClickListener = OnPreferenceClickListener {
                onLicensesItemSelected(activity)
                true
            }
        })
    }

    companion object {
        @Suppress("MagicNumber")
        internal val ActionBar.onScrollListener: RecyclerView.OnScrollListener
            get() = object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    elevation = if (recyclerView.canScrollVertically(-1)) dpToPx(4F, recyclerView.context) else 0F
                }
            }
    }
}
