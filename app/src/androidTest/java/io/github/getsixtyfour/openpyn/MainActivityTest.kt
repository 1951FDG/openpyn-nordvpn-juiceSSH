package io.github.getsixtyfour.openpyn

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.schibsted.spain.barista.interaction.BaristaSleepInteractions
import com.schibsted.spain.barista.rule.cleardata.ClearFilesRule
import com.schibsted.spain.barista.rule.cleardata.ClearPreferencesRule
import com.sonelli.juicessh.pluginlibrary.PluginContract.Connections.PERMISSION_READ
import com.sonelli.juicessh.pluginlibrary.PluginContract.PERMISSION_OPEN_SESSIONS
import org.hamcrest.*
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.runner.*
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    var mGrantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(PERMISSION_OPEN_SESSIONS, PERMISSION_READ)

    // Clear all app's SharedPreferences
    @Rule
    @JvmField
    var clearPreferencesRule: ClearPreferencesRule = ClearPreferencesRule()

    // Delete all files in getFilesDir() and getCacheDir()
    @Rule
    @JvmField
    var clearFilesRule: ClearFilesRule = ClearFilesRule()

    companion object {

        @BeforeClass
        @JvmStatic
        fun oneTimeSetUp() {
            /*CleanStatusBar.enableWithDefaults()*/

            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback { activity: Activity, stage: Stage ->
                if (stage == Stage.PRE_ON_CREATE) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        activity.setShowWhenLocked(true)
                        activity.setTurnScreenOn(true)
                    } else {
                        val flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        activity.window.addFlags(flags)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testTakeScreenshot() {
        val screenshotStrategy = UiAutomatorScreenshotStrategy()
        val screenshotCallback = FileWritingScreenshotCustomCallback(getInstrumentation().targetContext.applicationContext)

        launchActivity<MainActivity>()

        BaristaSleepInteractions.sleep(5.seconds.toLongMilliseconds())

        onView(allOf(withId(R.id.fab3), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_00", screenshotCallback)

        onView(allOf(withId(R.id.fab0), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_01", screenshotCallback)

        onView(allOf(withText(android.R.string.cancel), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(withId(R.id.fab2), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_02", screenshotCallback)

        onView(allOf(withText(android.R.string.cancel), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(withId(R.id.settingsfab), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(withText(R.string.title_settings), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_03", screenshotCallback)

        onView(allOf(childAtPosition(withId(R.id.recycler_view), 6), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_04", screenshotCallback)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(childAtPosition(withId(R.id.recycler_view), 7), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_05", screenshotCallback)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(childAtPosition(withId(R.id.recycler_view), 8), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        screenshotStrategy.takeScreenshot("screenshot_06", screenshotCallback)

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())

        onView(allOf(withContentDescription("Navigate up"), isDisplayed())).perform(click())

        BaristaSleepInteractions.sleep(1.seconds.toLongMilliseconds())
    }

    private fun childAtPosition(parentMatcher: Matcher<View>, position: Int) = object : TypeSafeMatcher<View>() {
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
