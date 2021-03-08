package io.github.getsixtyfour.openpyn.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import io.github.getsixtyfour.openpyn.initEmulatorPreferences
import io.github.getsixtyfour.openpyn.initStrictMode
import io.github.getsixtyfour.openpyn.initTimber

class DebugAppInitProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        context?.let {
            initEmulatorPreferences(it)
            initTimber(it)
        }

        initStrictMode()

        return false
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? {
        return null
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        return 0
    }
}
