package io.github.getsixtyfour.openpyn.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceChangeListener
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreference
import com.eggheadgames.aboutbox.AboutBoxUtils
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.michaelflisar.gdprdialog.GDPR
import com.michaelflisar.gdprdialog.GDPRConsent.NON_PERSONAL_CONSENT_ONLY
import com.michaelflisar.gdprdialog.GDPRConsent.PERSONAL_CONSENT
import com.michaelflisar.gdprdialog.GDPRConsentState
import com.michaelflisar.gdprdialog.GDPRLocation.UNDEFINED
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.ktextension.verifyInstallerId
import io.github.getsixtyfour.openpyn.R

/**
 * This fragment shows About settings preferences only.
 */
class AboutPreferenceFragment : PreferenceFragmentCompat() {

    override fun onDetach() {
        super.onDetach()

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val activity = requireActivity()
        val root = preferenceManager.createPreferenceScreen(activity)
        val config = AboutConfig.getInstance()

        root.setTitle(R.string.title_about)

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

    override fun getCallbackFragment(): PreferenceFragmentCompat = this

    private fun addAboutPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)

        root.addPreference(category)

        category.addPreference(getSwitchPreference(
            activity,
            GDPR.getInstance().consentState.consent.isPersonalConsent,
            R.string.egab_telemetry,
            "Automatically sends usage statistics and crash reports to Google",
            R.drawable.ic_firebase_black_24dp,
            OnPreferenceChangeListener { preference, value ->
                if (value as Boolean) {
                    // User consent given: user accepts personal data usage
                    val consentState = GDPRConsentState(activity, PERSONAL_CONSENT, UNDEFINED)
                    GDPR.getInstance().setConsent(consentState)
                } else {
                    // User consent given: user accepts non personal data only
                    val consentState = GDPRConsentState(activity, NON_PERSONAL_CONSENT_ONLY, UNDEFINED)
                    GDPR.getInstance().setConsent(consentState)
                }
                // You can't stop Crashlytics reporting once you've initialized it in an app session
                // TODO: show dialog to mention that restart is required
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(value)

                return@OnPreferenceChangeListener true
            }
        ))

        category.addPreference(getPreference(
            activity,
            R.string.egab_author,
            config.author,
            R.drawable.ic_github_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.companyHtmlPath)
                true
            }
        ))

        // TODO: go to google play or github depending on version
        category.addPreference(getPreference(
            activity,
            if (activity.verifyInstallerId(GooglePlayServicesUtil.GOOGLE_PLAY_STORE_PACKAGE)) R.string.egab_play_store_version else R.string.egab_version,
            config.version,
            R.drawable.ic_info_outline_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            R.string.egab_changelog,
            null,
            R.drawable.ic_history_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage + "/releases")
                true
            }
        ))
    }

    private fun addSupportPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(R.string.pref_category_support)

        root.addPreference(category)

        category.addPreference(getPreference(
            activity,
            R.string.egab_submit_issue,
            null,
            R.drawable.ic_bug_report_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openHTMLPage(activity, config.webHomePage + "/issues/new")
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            R.string.egab_contact_support,
            null,
            R.drawable.ic_email_black_24dp,
            OnPreferenceClickListener {
                EmailUtil.contactUs(activity)
                true
            }
        ))
    }

    private fun addOtherPreferences(activity: Activity, root: PreferenceScreen, config: AboutConfig) {
        val category = PreferenceCategory(activity)
        category.title = activity.getString(R.string.pref_category_other)

        root.addPreference(category)

        category.addPreference(getPreference(
            activity,
            R.string.egab_leave_review,
            null,
            R.drawable.ic_google_play_black_24dp,
            OnPreferenceClickListener {
                AboutBoxUtils.openApp(activity, config.buildType, config.packageName)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            R.string.egab_share,
            null,
            R.drawable.ic_share_black_24dp,
            OnPreferenceClickListener {
                ShareUtil.share(activity)
                true
            }
        ))

        category.addPreference(getPreference(
            activity,
            R.string.oss_license_title,
            null,
            R.drawable.ic_copyleft_green_24dp,
            OnPreferenceClickListener {
                OssLicensesMenuActivity.setActivityTitle(getString(R.string.title_licenses))
                val intent = Intent(activity, OssLicensesMenuActivity::class.java)
                ContextCompat.startActivity(activity, intent, null)
                true
            }
        ))
    }

    private fun getPreference(
        context: Context, @StringRes titleResId: Int?, summary: String?, @DrawableRes iconResId: Int?, listener: OnPreferenceClickListener?
    ): Preference {
        val preference = Preference(context)
        iconResId?.let { preference.icon = ContextCompat.getDrawable(context, it) }
        titleResId?.let { preference.title = context.getString(it) }
        summary?.let { preference.summary = it }
        listener?.let { preference.onPreferenceClickListener = it }
        return preference
    }

    private fun getSwitchPreference(
        context: Context,
        defaultValue: Boolean,
        @StringRes titleResId: Int?,
        summary: String?,
        @DrawableRes iconResId: Int?,
        listener: OnPreferenceChangeListener?
    ): Preference {
        val preference = SwitchPreference(context)
        preference.setDefaultValue(defaultValue)
        iconResId?.let { preference.icon = ContextCompat.getDrawable(context, it) }
        titleResId?.let { preference.title = context.getString(it) }
        summary?.let { preference.summary = it }
        listener?.let { preference.onPreferenceChangeListener = it }
        return preference
    }
}
