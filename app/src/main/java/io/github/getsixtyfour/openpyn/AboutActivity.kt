package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.danielstone.materialaboutlibrary.MaterialAboutActivity
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem
import com.danielstone.materialaboutlibrary.items.MaterialAboutItemOnClickAction
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard.Builder
import com.danielstone.materialaboutlibrary.model.MaterialAboutList
import com.eggheadgames.aboutbox.AboutBoxUtils.openApp
import com.eggheadgames.aboutbox.AboutBoxUtils.openHTMLPage
import com.eggheadgames.aboutbox.AboutConfig
import com.eggheadgames.aboutbox.share.EmailUtil
import com.eggheadgames.aboutbox.share.ShareUtil
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity

class AboutActivity : MaterialAboutActivity() {
    override fun getMaterialAboutList(context: Context): MaterialAboutList {
        val config = AboutConfig.getInstance()

        return MaterialAboutList.Builder()
                .addCard(buildGeneralInfoCard(config))
                .addCard(buildReleaseCard(config))
                .addCard(buildReportCard(config))
                .addCard(buildSupportCard(config))
                .addCard(buildShareCard(config))
                .addCard(buildLicenseCard(config))
                .build()
    }

    private fun buildGeneralInfoCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(MaterialAboutTitleItem.Builder()
                .text(config.appName)
                .icon(config.appIcon)
                .setOnClickAction { openHTMLPage(this, config.webHomePage) }
                .build())

        card.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.egab_author)
                .subText(config.author)
                .icon(R.drawable.ic_person_black_24dp)
                .setOnClickAction { openHTMLPage(this, config.companyHtmlPath) }
                .build())

        card.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.egab_version)
                .subText(config.version)
                .icon(R.drawable.ic_info_outline_black_24dp)
                .build())

        return card.build()
    }

    private fun buildReleaseCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(itemHelper(
                R.string.egab_changelog,
                R.drawable.ic_history_black_24dp,
                MaterialAboutItemOnClickAction { openHTMLPage(this, config.webHomePage + "/releases") }
        ))

        return card.build()
    }

    private fun buildReportCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(itemHelper(
                R.string.egab_submit_issue,
                R.drawable.ic_bug_report_black_24dp,
                MaterialAboutItemOnClickAction { openHTMLPage(this, config.webHomePage + "/issues/new") }
        ))

        return card.build()
    }

    private fun buildSupportCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(itemHelper(
                R.string.egab_contact_support,
                R.drawable.ic_email_black_24dp,
                MaterialAboutItemOnClickAction { EmailUtil.contactUs(this) }
        ))

        return card.build()
    }

    private fun buildShareCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(itemHelper(
                R.string.egab_leave_review,
                R.drawable.ic_star_black_24dp,
                MaterialAboutItemOnClickAction { openApp(this, config.buildType, config.packageName) }
        ))

        card.addItem(itemHelper(
                R.string.egab_share,
                R.drawable.ic_share_black_24dp,
                MaterialAboutItemOnClickAction { ShareUtil.share(this) }
        ))

        return card.build()
    }

    private fun buildLicenseCard(config: AboutConfig): MaterialAboutCard {
        val card = Builder()

        card.addItem(MaterialAboutActionItem.Builder()
                .text(R.string.egab_licenses)
                .icon(R.drawable.ic_copyleft_green_24dp)
                .setOnClickAction {
                    val intent = Intent(this, OssLicensesMenuActivity::class.java)
                    intent.putExtra("title", getString(R.string.menu_licenses))
                    startActivity(intent, null)
                }
                .setOnLongClickAction { openHTMLPage(this, config.webHomePage + "/blob/master/LICENSE") }
                .build())

        return card.build()
    }

    private fun itemHelper(name: Int, icon: Int, clickAction: MaterialAboutItemOnClickAction): MaterialAboutActionItem {
        return MaterialAboutActionItem.Builder()
                .text(name)
                .icon(icon)
                .setOnClickAction(clickAction)
                .build()
    }

    override fun getActivityTitle(): CharSequence? {
        return getString(R.string.egab_about_screen_title)
    }

    companion object {
        fun launch(activity: Activity) {
            val intent = Intent(activity, AboutActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            activity.startActivity(intent)
        }
    }
}
