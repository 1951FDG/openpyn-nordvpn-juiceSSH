package io.github.getsixtyfour.openpyn

import android.content.Context
import com.github.moduth.blockcanary.BlockCanaryContext
import com.github.moduth.blockcanary.internal.BlockInfo
import mu.KLogging

class AppBlockCanaryContext : BlockCanaryContext() {

    override fun provideMonitorDuration(): Int {
        return 9999
    }

    override fun provideBlockThreshold(): Int {
        return 500
    }

    override fun displayNotification(): Boolean {
        return true
    }

    override fun provideWhiteList(): List<String> {
        return emptyList()
    }

    override fun onBlock(context: Context?, blockInfo: BlockInfo?) {
        super.onBlock(context, blockInfo)
        blockInfo?.buildException()?.let { logger.error(it) { "" } }
    }

    companion object : KLogging()
}
