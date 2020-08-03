package io.github.getsixtyfour.openpyn

import android.content.Context
import com.github.moduth.blockcanary.BlockCanaryContext
import com.github.moduth.blockcanary.internal.BlockInfo
import mu.KotlinLogging

class AppBlockCanaryContext : BlockCanaryContext() {

    private val logger = KotlinLogging.logger {}

    override fun provideMonitorDuration(): Int = 9999

    override fun provideBlockThreshold(): Int = 500

    override fun displayNotification(): Boolean = true

    override fun provideWhiteList(): List<String> = emptyList()

    override fun onBlock(context: Context?, blockInfo: BlockInfo?) {
        blockInfo?.buildException()?.let { logger.error(it) { "" } }
    }
}
