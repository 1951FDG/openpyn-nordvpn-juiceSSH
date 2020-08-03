package io.github.getsixtyfour.openpyn.settings

import android.os.Bundle
import android.system.Os
import android.system.OsConstants
import android.text.InputFilter
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.textfield.TextInputLayout
import io.github.getsixtyfour.ktextension.setTitle
import io.github.getsixtyfour.openpyn.R
import org.apache.hadoop.hdfs.util.PosixPathNameChecker
import java.net.InetAddress

/**
 * This fragment shows Management settings preferences only.
 */
class ManagementPreferenceFragment : PreferenceFragmentCompat() {

    override fun onDetach() {
        super.onDetach()

        (activity as? AppCompatActivity)?.supportActionBar?.title = getString(R.string.title_settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(false)

        findPreference<EditTextPreference>(getString(R.string.pref_openvpnmgmt_host_key))?.apply {
            setOnBindEditTextListener {
                // IPv4
                it.keyListener = DigitsKeyListener.getInstance("0123456789.")
                it.filters += InputFilter.LengthFilter(15)
                // IPv4, IPv6
                /*it.keyListener = object : NumberKeyListener() {
                    override fun getInputType(): Int {
                        return InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_NORMAL
                    }

                    override fun getAcceptedChars(): CharArray {
                        return charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ':', 'a', 'b', 'c', 'd', 'e', 'f')
                    }
                }
                it.filters += InputFilter.LengthFilter(45)*/
                it.addTextChangedListener(it.textInputLayout()?.hostErrorTextWatcher)
            }
        }

        findPreference<EditTextPreference>(getString(R.string.pref_openvpnmgmt_port_key))?.apply {
            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_NORMAL
                it.filters += InputFilter.LengthFilter(5)
                it.addTextChangedListener(it.textInputLayout()?.portErrorTextWatcher)
            }
        }

        findPreference<EditTextPreference>(getString(R.string.pref_openvpnmgmt_password_file_key))?.apply {
            setOnBindEditTextListener {
                /*it.filters += InputFilter { source, start, end, dest, dstart, dend ->
                    val char = PosixPathNameChecker.SEPARATOR_CHAR
                    when {
                        dest.isEmpty() || source.isEmpty() -> null
                        dstart >= dest.length && dest[dstart - 1] == char && source[start] == char -> ""
                        dstart < dest.length && dest[dstart] == char && source[start] == char -> ""
                        dest[dstart - 1] == char && source[start] == char -> ""
                        else -> null
                    }
                }*/
                it.filters += InputFilter.LengthFilter(PosixPathNameChecker.MAX_PATH_LENGTH)
                it.addTextChangedListener(it.textInputLayout()?.pathErrorTextWatcher)
            }
        }

        findPreference<EditTextPreference>(getString(R.string.pref_openvpnmgmt_password_key))?.apply {
            setSummaryProvider(::provideSummary)
        }

        findPreference<EditTextPreference>(getString(R.string.pref_openvpnmgmt_userpass_key))?.apply {
            setSummaryProvider(::provideSummary)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.pref_openvpnmgmt, rootKey)
        setTitle(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.fitsSystemWindows = true
        setDivider(null)

        super.onViewCreated(view, savedInstanceState)
    }

    override fun getCallbackFragment(): PreferenceFragmentCompat = this

    companion object {
        internal val TextInputLayout.hostErrorTextWatcher: AbstractTextWatcher
            get() = object : AbstractTextWatcher(this) {
                val message = ctx.getString(R.string.pref_openvpnmgmt_host_error)
                override val submitButtonId: Int
                    get() = android.R.id.button1

                init {
                    textInputLayout.helperText = ctx.getString(R.string.pref_openvpnmgmt_host_helper_text)
                }

                override fun validate(s: String): String? = if (s.isEmpty()) null else try {
                    val address = parseNumericAddressNoThrow(s)
                    requireNotNull(address) { ctx.getString(R.string.pref_openvpnmgmt_host_error_2) }
                    require(isLocalAddress(address)) { message }
                    null
                } catch (e: Exception) {
                    e.message
                }

                fun isLocalAddress(address: InetAddress): Boolean {
                    return address.isSiteLocalAddress || address.address[0] == 0xfd.toByte()
                }

                @Suppress("unused")
                fun isNumericAddress(address: String): Boolean {
                    return parseNumericAddressNoThrow(address) != null
                }

                @Suppress("unused")
                fun parseNumericAddress(address: String): InetAddress? {
                    return parseNumericAddressNoThrow(address) ?: throw IllegalArgumentException("Not a numeric address: $address")
                }

                fun parseNumericAddressNoThrow(address: String): InetAddress? {
                    return Os.inet_pton(OsConstants.AF_INET, address) // IPv4
                    // return Os.inet_pton(OsConstants.AF_INET, address) ?: Os.inet_pton(OsConstants.AF_INET6, address) // IPv4, IPv6
                }
            }
        internal val TextInputLayout.portErrorTextWatcher: AbstractTextWatcher
            get() = object : AbstractTextWatcher(this) {
                val message = ctx.getString(R.string.pref_openvpnmgmt_port_error)
                override val submitButtonId: Int
                    get() = android.R.id.button1

                init {
                    textInputLayout.helperText = ctx.getString(R.string.pref_openvpnmgmt_port_helper_text)
                }

                override fun validate(s: String): String? = if (s.isEmpty()) null else try {
                    require(isNumericPort(s)) { message }
                    // require(s != "80") { message }
                    null
                } catch (e: Exception) {
                    e.message
                }

                fun isNumericPort(port: String): Boolean = try {
                    port.toInt() in 1..65535
                } catch (e: NumberFormatException) {
                    false
                }
            }
        internal val TextInputLayout.pathErrorTextWatcher: AbstractTextWatcher
            get() = object : AbstractTextWatcher(this) {
                val message = ctx.getString(R.string.pref_openvpnmgmt_path_error)
                override val submitButtonId: Int
                    get() = android.R.id.button1

                init {
                    textInputLayout.helperText = ctx.getString(R.string.pref_openvpnmgmt_path_helper_text)
                }

                override fun validate(s: String): String? = if (s.isEmpty()) null else try {
                    require(isPosixFilePath(s)) { message }
                    null
                } catch (e: Exception) {
                    e.message
                }

                fun isPosixFilePath(path: String): Boolean =
                    PosixPathNameChecker().isValidPath(path) && path[path.length - 1] != PosixPathNameChecker.SEPARATOR_CHAR
            }

        fun EditText.textInputLayout(): TextInputLayout? = (parent.parent as? TextInputLayout)

        fun provideSummary(preference: Preference): CharSequence {
            return when (preference.sharedPreferences.getString(preference.key, null)) {
                null, "" -> preference.context.getString(R.string.not_set)
                else -> preference.context.getString(R.string.password_set)
            }
        }
    }
}
