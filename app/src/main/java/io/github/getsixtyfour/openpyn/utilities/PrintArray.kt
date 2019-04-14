package io.github.getsixtyfour.openpyn.utilities

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Size
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatActivity
import com.abdeveloper.library.MultiSelectDialog
import com.abdeveloper.library.MultiSelectable

interface SubmitCallbackListener {
    fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String)

    fun onCancel()
}

object PrintArray {
    private var hint = android.R.string.unknownName
    private var title = android.R.string.unknownName
    private var positiveTitle = android.R.string.ok
    private var negativeTitle = android.R.string.cancel
    private var neutralTitle = android.R.string.selectAll
    private var itemsList: ArrayList<MultiSelectable>? = null
    var checkedItemsList: ArrayList<Int>? = null
    const val delimiter: String = "‚‗‚"

    fun setHint(hint: Int): PrintArray {
        PrintArray.hint = hint
        return this
    }

    fun setTitle(title: Int): PrintArray {
        PrintArray.title = title
        return this
    }

    @Suppress("unused")
    fun setPositiveTitle(title: Int): PrintArray {
        positiveTitle = title
        return this
    }

    @Suppress("unused")
    fun setNegativeTitle(title: Int): PrintArray {
        negativeTitle = title
        return this
    }

    @Suppress("unused")
    fun setNeutralTitle(title: Int): PrintArray {
        neutralTitle = title
        return this
    }

    @Suppress("unused")
    fun setItems(items: ArrayList<MultiSelectable>): PrintArray {
        itemsList = items
        return this
    }

    @Suppress("unused")
    fun setCheckedItems(checkedItems: ArrayList<Int>): PrintArray {
        checkedItemsList = checkedItems
        return this
    }

    // AlertDialog
    fun show(
        @Size(min = 1) key: String,
        items: Array<CharSequence>,
        checkedItems: BooleanArray,
        context: Context,
        prefs: SharedPreferences?
    ) {
        fun save(selectedItems: ArrayList<Boolean>): Boolean {
            return when {
                prefs != null -> putListBoolean(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        Builder(context).apply {
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
            show()
        }
    }

    // AlertDialog
    fun show(
        @Size(min = 1) key: String,
        items: ArrayList<String>,
        checkedItems: ArrayList<Boolean>,
        context: Context,
        prefs: SharedPreferences?
    ) {
        fun save(selectedItems: ArrayList<Boolean>): Boolean {
            return when {
                prefs != null -> putListBoolean(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        Builder(context).apply {
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
            show()
        }
    }

    // MultiSelectDialog
    fun show(
        @Size(min = 1) key: String, context: AppCompatActivity, prefs: SharedPreferences?,
        listener: SubmitCallbackListener
    ) {
        show(key, checkNotNull(itemsList), checkNotNull(checkedItemsList), context, prefs, listener)
    }

    // MultiSelectDialog
    fun show(
        @Size(min = 1) key: String,
        items: ArrayList<MultiSelectable>,
        checkedItems: ArrayList<Int>,
        context: AppCompatActivity,
        prefs: SharedPreferences?,
        listener: SubmitCallbackListener
    ) {
        fun save(selectedItems: ArrayList<Int>): Boolean {
            return when {
                prefs != null -> putListInt(key, selectedItems, prefs).commit()
                else -> false
            }
        }

        MultiSelectDialog().apply {
            hint(context.getString(hint))
            title(context.getString(title))
            setMinSelectionLimit(1)
            setMaxSelectionLimit(items.size)
            preSelectIDsList(checkedItems)
            multiSelectList(items)
            onSubmit(object : MultiSelectDialog.SubmitCallbackListener {
                override fun onSelected(selectedIds: ArrayList<Int>, selectedNames: ArrayList<String>, dataString: String) {
                    if (save(selectedIds)) checkedItemsList = selectedIds
                    listener.onSelected(selectedIds, selectedNames, dataString)
                }

                override fun onCancel() {
                    listener.onCancel()
                }
            })

            show(context.supportFragmentManager, "multiSelectDialog")
        }
    }

    @Suppress("unused")
    fun putListInt(@Size(min = 1) key: String, intList: ArrayList<Int>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = intList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    @Suppress("unused")
    fun putListBoolean(@Size(min = 1) key: String, booleanList: ArrayList<Boolean>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = booleanList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    @Suppress("unused")
    fun putListString(@Size(min = 1) key: String, stringList: ArrayList<String>, prefs: SharedPreferences): SharedPreferences.Editor {
        val array = stringList.toTypedArray()
        val editor = prefs.edit()
        editor.putString(key, array.joinToString(separator = delimiter)).apply()
        return editor
    }

    @Suppress("unused")
    fun getListInt(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<Int> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.mapTo(ArrayList()) { it.toInt() }
    }

    @Suppress("unused")
    fun getListBoolean(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<Boolean> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.mapTo(ArrayList()) { it.toBoolean() }
    }

    @Suppress("unused")
    fun getListString(@Size(min = 1) key: String, defValue: String = "", prefs: SharedPreferences): ArrayList<String> {
        val array = prefs.getString(key, defValue)!!.split(delimiter)
        return array.toCollection(ArrayList())
    }
}
