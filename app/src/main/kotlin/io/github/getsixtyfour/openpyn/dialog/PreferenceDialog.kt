package io.github.getsixtyfour.openpyn.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import io.github.getsixtyfour.openpyn.R

class PreferenceDialog : AppCompatDialogFragment(), DialogInterface.OnClickListener {

    private val mArgs: PreferenceDialogArgs by navArgs()

    // Use this instance of the interface to deliver action events
    private var mListener: NoticeDialogListener? = null

    override fun onDestroyView() {
        super.onDestroyView()

        val fragmentManager = requireActivity().supportFragmentManager
        val fragment = fragmentManager.findFragmentById(R.id.fragment_container_view)
        fragment?.let { fragmentManager.beginTransaction().remove(it).commit() }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), theme)
        builder.setTitle(R.string.vpn_title_connect)
        // Get the layout inflater
        val inflater = LayoutInflater.from(builder.context)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val view = inflater.inflate(R.layout.abc_preference_dialog_material, null, false)
        view.findViewById<TextView>(R.id.message).text = mArgs.message
        builder.setView(view)
        // Add action buttons
        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(android.R.string.cancel, null)
        return builder.create()
    }

    // Override the Fragment.onAttach() method to instantiate the NoticeDialogListener
    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = context as NoticeDialogListener?
        } catch (e: ClassCastException) {
            // The activity doesn't implement the interface, throw exception
            throw ClassCastException("$context must implement NoticeDialogListener")
        }
    }

    override fun onDetach() {
        super.onDetach()

        mListener = null
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        when (which) {
            DialogInterface.BUTTON_POSITIVE -> {
                // Send the positive button event back to the host activity
                mListener?.onDialogPositiveClick(this)
            }
            DialogInterface.BUTTON_NEGATIVE -> {
                // Send the negative button event back to the host activity
                mListener?.onDialogNegativeClick(this)
            }
        }
    }

    // The activity that creates an instance of this dialog fragment must implement this interface
    interface NoticeDialogListener {

        fun onDialogPositiveClick(dialog: DialogFragment)
        fun onDialogNegativeClick(dialog: DialogFragment)
    }
}
