package io.github.getsixtyfour.openpyn

import com.github.moduth.blockcanary.BlockCanaryContext

class AppBlockCanaryContext : BlockCanaryContext() {
    override fun provideQualifier(): String {
        return BuildConfig.VERSION_CODE.toString() + "_" + BuildConfig.VERSION_NAME + "_YYB"
    }

    override fun provideUid(): String {
        return "87224330"
    }

    override fun provideNetworkType(): String {
        return "4G"
    }

    override fun provideMonitorDuration(): Int {
        return 9999
    }

    override fun provideBlockThreshold(): Int {
        return 500
    }

    override fun displayNotification(): Boolean {
        return true
    }

    override fun concernPackages(): List<String> {
        val list = super.provideWhiteList()
        return list
    }

    override fun provideWhiteList(): List<String> {
        val list = super.provideWhiteList()
        return list
    }
}