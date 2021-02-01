package io.github.getsixtyfour.openpyn.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Size
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.abdeveloper.library.MultiSelectDialog
import com.abdeveloper.library.MultiSelectable
import io.github.getsixtyfour.openpyn.R

@Suppress("unused")
object PrintArray {

    private var hint = R.string.empty
    private var title = R.string.empty
    private var positiveTitle = android.R.string.ok
    private var negativeTitle = android.R.string.cancel
    private var neutralTitle = android.R.string.selectAll
    private lateinit var itemsList: ArrayList<MultiSelectable>
    lateinit var checkedItemsList: ArrayList<Int>
    const val delimiter: String = "‚‗‚"

    fun setHint(@StringRes hint: Int): PrintArray {
        PrintArray.hint = hint
        return this
    }

    fun setTitle(@StringRes title: Int): PrintArray {
        PrintArray.title = title
        return this
    }

    fun setPositiveTitle(@StringRes title: Int): PrintArray {
        positiveTitle = title
        return this
    }

    fun setNegativeTitle(@StringRes title: Int): PrintArray {
        negativeTitle = title
        return this
    }

    fun setNeutralTitle(@StringRes title: Int): PrintArray {
        neutralTitle = title
        return this
    }

    fun setItems(items: ArrayList<MultiSelectable>): PrintArray {
        itemsList = items
        return this
    }

    fun setCheckedItems(checkedItems: ArrayList<Int>): PrintArray {
        checkedItemsList = checkedItems
        return this
    }

    // AlertDialog
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun show(
        @Size(min = 1) key: String, items: Array<CharSequence>, checkedItems: BooleanArray, context: Context
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        fun save(selectedItems: ArrayList<Boolean>): Boolean {
            return when {
                prefs != null -> putListBoolean(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        AlertDialog.Builder(context).apply {
            setTitle(title)
            setMultiChoiceItems(items, checkedItems) { dialog, which, isChecked ->
                checkedItems[which] = isChecked
            }
            setCancelable(false)
            setPositiveButton(positiveTitle) { dialog, which ->
                save(checkedItems.toCollection(ArrayList()))
            }
            setNegativeButton(negativeTitle) { dialog, which ->
                dialog.dismiss()
            }
            setNeutralButton(neutralTitle) { dialog, which ->
                checkedItems.indices.forEach {
                    checkedItems[it] = true
                }
                save(checkedItems.toCollection(ArrayList()))
            }
        }.show()
    }

    // AlertDialog
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    fun show(
        @Size(min = 1) key: String, items: ArrayList<String>, checkedItems: ArrayList<Boolean>, context: Context
    ) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        fun save(selectedItems: ArrayList<Boolean>): Boolean {
            return when {
                prefs != null -> putListBoolean(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        AlertDialog.Builder(context).apply {
            setTitle(title)
            setMultiChoiceItems(items.toTypedArray(), checkedItems.toBooleanArray()) { dialog, which, isChecked ->
                checkedItems[which] = isChecked
            }
            setCancelable(false)
            setPositiveButton(positiveTitle) { dialog, which ->
                save(checkedItems)
            }
            setNegativeButton(negativeTitle) { dialog, which ->
                dialog.dismiss()
            }
            setNeutralButton(neutralTitle) { dialog, which ->
                checkedItems.indices.forEach {
                    checkedItems[it] = true
                }
                save(checkedItems)
            }
        }.show()
    }

    // MultiSelectDialog
    fun show(@Size(min = 1) key: String, context: FragmentActivity) {
        show(key, itemsList, checkedItemsList, context)
    }

    // MultiSelectDialog
    fun show(@Size(min = 1) key: String, items: ArrayList<MultiSelectable>, checkedItems: ArrayList<Int>, context: FragmentActivity) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
        fun save(selectedItems: ArrayList<Int>): Boolean {
            return when {
                prefs != null -> putListInt(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        MultiSelectDialog().apply {
            setHint(context.getString(hint))
            setTitle(context.getString(title))
            setMinSelectionLimit(1)
            setMaxSelectionLimit(items.size)
            setPreSelectIDsList(checkedItems)
            setMultiSelectList(items)
        }.show(context.supportFragmentManager, null)
        // TODO: change to nav, setup onAttachFragment in activity or setup onAttach in class itself, or ?
        /*val action = MapFragmentDirections.actionMapFragmentToMultiSelectDialogFragment()
        Navigation.findNavController(context.findViewById(R.id.map)).navigate(action)*/
    }

    fun putListInt(@Size(min = 1) key: String, intList: ArrayList<Int>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = intList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    fun putListBoolean(@Size(min = 1) key: String, booleanList: ArrayList<Boolean>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = booleanList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    fun putListString(@Size(min = 1) key: String, stringList: ArrayList<String>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = stringList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    fun getListInt(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<Int> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.mapTo(ArrayList()) { it.toInt() }
    }

    fun getListBoolean(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<Boolean> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.mapTo(ArrayList()) { it.toBoolean() }
    }

    fun getListString(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<String> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.toCollection(ArrayList())
    }
}
