package io.github.getsixtyfour.openpyn.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.eggheadgames.aboutbox.AboutBoxUtils
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R.drawable
import io.github.getsixtyfour.openpyn.R.string

/**
 * This fragment shows About settings preferences only.
 */
class AboutPreferenceFragment : PreferenceFragmentCompat() {

    override fun onDetach() {
        super.onDetach()
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val activity = requireActivity()
        val root = preferenceManager.createPreferenceScreen(activity)
        val config = AboutConfig.getInstance()

        root.setTitle(string.title_activity_about)

        addAboutPreferences(activity, root, config)

        addSupportPreferences(activity, root, config)

        addOtherPreferences(activity, root, config)

        preferenceScreen = root
        setTitle(activity)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat {
        return this
    }

    private fun addAboutPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(string.pref_category_about)

        root.addPreference(category)

        category.addPreference(getPreference(
            activity,
            string.egab_author,
            config.author,
            drawable.ic_person_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.companyHtmlPath)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            string.egab_version,
            config.version,
            drawable.ic_info_outline_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            string.egab_changelog,
            null,
            drawable.ic_history_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage + "/releases")
                true
            }
        ))
    }

    private fun addSupportPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(string.pref_category_support)

        root.addPreference(category)

        category.addPreference(getPreference(
            activity,
            string.egab_submit_issue,
            null,
            drawable.ic_bug_report_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage + "/issues/new")
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            string.egab_contact_support,
            null,
            drawable.ic_email_black_24dp,
            OnPreferenceClickListener {
                EmailUtil.contactUs(activity)
                true
            }
        ))
    }

    private fun addOtherPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(string.pref_category_other)

        root.addPreference(category)

        category.addPreference(getPreference(
            activity,
            string.egab_leave_review,
            null,
            drawable.ic_star_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openApp(activity, config.buildType, config.packageName)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            string.egab_share,
            null,
            drawable.ic_share_black_24dp,
            OnPreferenceClickListener {
                ShareUtil.share(activity)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            string.egab_licenses,
            null,
            drawable.ic_copyleft_green_24dp,
            OnPreferenceClickListener {
                OssLicensesMenuActivity.setActivityTitle(getString(string.title_activity_licenses))
                val intent = Intent(activity, OssLicensesMenuActivity::class.java)
                ContextCompat.startActivity(activity, intent, null)
                true
            }
        ))
    }

    private fun getPreference(
        context: Context,
        titleResId: Int?,
        summary: String?,
        iconResId: Int?,
        listener: OnPreferenceClickListener?
    ): Preference {
        val preference = Preference(context)
        iconResId?.let { preference.icon = ContextCompat.getDrawable(context, it) }
        titleResId?.let { preference.title = context.getString(it) }
        summary?.let { preference.summary = it }
        listener?.let { preference.onPreferenceClickListener = listener }
        return preference
    }
}
