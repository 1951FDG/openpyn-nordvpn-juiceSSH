package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions
import com.schibsted.spain.barista.rule.cleardata.ClearFilesRule
import com.schibsted.spain.barista.rule.cleardata.ClearPreferencesRule
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule
import tools.fastlane.screengrab.locale.LocaleUtil
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            PERMISSION_READ,
            PERMISSION_OPEN_SESSIONS,
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.CHANGE_CONFIGURATION"
        )
    // Clear all app's SharedPreferences
    @Rule
    @JvmField
    var clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()
    // Delete all files in getFilesDir() and getCacheDir()
    @Rule
    @JvmField
    var clearFilesRule: ClearFilesRule = ClearFilesRule()
    @Rule
    @JvmField
    var localeTestRule: LocaleTestRule = LocaleTestRule()
    @Rule
    @JvmField
    var mActivityTestRule: ActivityTestRule<MainActivity> = ActivityTestRule(MainActivity::class.java, true, false)

    companion object {
        @BeforeClass
        @JvmStatic
        fun beforeClass() {
            LocaleUtil.changeDeviceLocaleTo(LocaleUtil.getTestLocale())
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback { activity: Activity, stage: Stage ->
                if (stage == Stage.PRE_ON_CREATE) {
                    if (Build.VERSION.SDK_INT >= 27) {
                        activity.setShowWhenLocked(true)
                        activity.setTurnScreenOn(true)
                    } else {
                        val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        activity.window.addFlags(flags)
                    }
                }
            }
        }

        @AfterClass
        @JvmStatic
        fun afterClass() {
            LocaleUtil.changeDeviceLocaleTo(LocaleUtil.getEndingLocale())
        }
    }

    @Test
    fun testTakeScreenshot() {
        mActivityTestRule.launchActivity(null)

        val appContext = getInstrumentation().targetContext.applicationContext
        val screenshotStrategy = UiAutomatorScreenshotStrategy()

        BaristaSleepInteractions.sleep(7, TimeUnit.SECONDS)
        val checkableFloatingActionButton = onView(
            allOf(
                withId(R.id.fab3),
                isDisplayed()
            )
        )
        checkableFloatingActionButton.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)

        Screengrab.screenshot("screenshot_01", screenshotStrategy, FileWritingScreenshotCustomCallback(appContext))
        val floatingActionButton = onView(
            allOf(
                withId(R.id.fab2),
                isDisplayed()
            )
        )
        floatingActionButton.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)

        Screengrab.screenshot("screenshot_02", screenshotStrategy, FileWritingScreenshotCustomCallback(appContext))
        val appCompatTextView = onView(
            allOf(
                withId(R.id.cancel),
                withText("Cancel"),
                isDisplayed()
            )
        )
        appCompatTextView.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)
        val overflowMenuButton = onView(
            allOf(
                withContentDescription("More options"),
                childAtPosition(childAtPosition(withId(R.id.toolbar), 1), 1),
                isDisplayed()
            )
        )
        overflowMenuButton.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)
        val appCompatTextView2 = onView(
            allOf(
                withId(R.id.title),
                withText("Settings"),
                childAtPosition(childAtPosition(withId(R.id.content), 0), 0),
                isDisplayed()
            )
        )
        appCompatTextView2.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)

        Screengrab.screenshot("screenshot_03", screenshotStrategy, FileWritingScreenshotCustomCallback(appContext))
        val linearLayout = onView(
            allOf(
                childAtPosition(
                    allOf(
                        withId(R.id.recycler_view),
                        childAtPosition(withClassName(`is`("android.widget.FrameLayout")), 0)
                    ), 8
                ),
                isDisplayed()
            )
        )
        linearLayout.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)

        Screengrab.screenshot("screenshot_04", screenshotStrategy, FileWritingScreenshotCustomCallback(appContext))
        val appCompatImageButton = onView(
            allOf(
                withContentDescription("Navigate up"),
                childAtPosition(
                    allOf(
                        withId(R.id.action_bar),
                        childAtPosition(withId(R.id.action_bar_container), 0)
                    ), 1
                ),
                isDisplayed()
            )
        )
        appCompatImageButton.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)

        appCompatImageButton.perform(click())

        BaristaSleepInteractions.sleep(1, TimeUnit.SECONDS)
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) && view == parent.getChildAt(position)
            }
        }
    }
}
