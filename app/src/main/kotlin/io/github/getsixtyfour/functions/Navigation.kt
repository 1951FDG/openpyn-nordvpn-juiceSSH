package io.github.getsixtyfour.functions

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun <T : FragmentActivity> getCurrentNavigationFragment(activity: T): Fragment? {
    val navHostFragment = activity.supportFragmentManager.primaryNavigationFragment as? NavHostFragment
    val host = navHostFragment?.host
    if (host == null) {
        logger.error(IllegalStateException()) { "Fragment $navHostFragment has not been attached yet." }
    }

    return when (host) {
        null -> null
        else -> navHostFragment.childFragmentManager.primaryNavigationFragment
    }
}
