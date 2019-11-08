package io.github.getsixtyfour.openpyn.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import io.github.getsixtyfour.openpyn.R

class PreferenceDialog : AppCompatDialogFragment() {

    private val args: PreferenceDialogArgs by navArgs()
    // Use this instance of the interface to deliver action events
    internal lateinit var listener: NoticeDialogListener

    override fun onDestroyView() {
        super.onDestroyView()

        val fragmentManager = requireActivity().supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.fragment_container_view)
        fragment?.let { fragmentManager.beginTransaction().remove(it).commit() }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it, theme)
            builder.setTitle("VPN Connection")
            // Get the layout inflater
            val inflater = LayoutInflater.from(builder.context)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val customView = inflater.inflate(R.layout.abc_preference_dialog_material, null, false)
            customView.findViewById<TextView>(R.id.message).text = args.message
            builder.setView(customView)
            // Add action buttons
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                // Send the positive button event back to the host activity
                listener.onDialogPositiveClick(this)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ ->
                // Send the negative button event back to the host activity
                listener.onDialogNegativeClick(this)
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = context as NoticeDialogListener
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement NoticeDialogListener")
        }
    }

    /* The activity that creates an instance of this dialog fragment must
     * implement this interface in order to receive event callbacks.
     * Each method passes the DialogFragment in case the host needs to query it. */
    interface NoticeDialogListener {

        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }
}
