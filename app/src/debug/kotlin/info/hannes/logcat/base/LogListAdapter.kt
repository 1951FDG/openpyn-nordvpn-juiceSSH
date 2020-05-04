package info.hannes.logcat.base

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import info.hannes.logcat.R
import java.util.ArrayList

class LogListAdapter(private val completeLogs: MutableList<String>, filter: String) : RecyclerView.Adapter<LogListAdapter.LogViewHolder>() {

    private var currentFilter: Array<out String>? = null
    var filterLogs: List<String> = ArrayList()

    init {
        setFilter(filter)
    }

    fun setFilter(vararg filters: String) {
        currentFilter = filters
        filterLogs = completeLogs.filter { line ->
            var include = false
            for (filter in filters)
                if (!include && line.contains(filter, true))
                    include = true
            include
        }
        notifyDataSetChanged()
    }

    fun addLine(addLine: String) {

        completeLogs.add(completeLogs.size, addLine)

        filterLogs = completeLogs.filter { line ->
            var include = false
            currentFilter?.let {
                for (filter in it)
                    if (!include && line.contains(filter, true))
                        include = true
            }
            include
        }
        notifyDataSetChanged()
    }

    /**
     * Define the view for each log in the list
     */
    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val logContent: TextView = view.findViewById(R.id.logLine)
    }

    /**
     * Create the view for each log in the list
     *
     * @param viewGroup
     * @param i
     * @return
     */
    override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): LogViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_log, viewGroup, false)
        return LogViewHolder(view)
    }

    /**
     * Fill in each log in the list
     *
     * @param holder
     * @param position
     */
    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.logContent.text = filterLogs[position].also {
            if (it.contains(" ${LogBaseFragment.ERROR_LINE}") || it.startsWith(LogBaseFragment.ERROR_LINE)) {
                holder.logContent.setTextColor(Color.RED)
            } else if (it.contains(" ${LogBaseFragment.ASSERT_LINE}") || it.startsWith(LogBaseFragment.ASSERT_LINE)) {
                holder.logContent.setTextColor(Color.RED)
            } else if (it.contains(" ${LogBaseFragment.INFO_LINE}") || it.startsWith(LogBaseFragment.INFO_LINE)) {
                holder.logContent.setTextColor(Color.BLACK)
            } else if (it.contains(" ${LogBaseFragment.WARNING_LINE}") || it.startsWith(LogBaseFragment.WARNING_LINE)) {
                holder.logContent.setTextColor(Color.MAGENTA)
            } else if (it.contains(" ${LogBaseFragment.VERBOSE_LINE}") || it.startsWith(LogBaseFragment.VERBOSE_LINE)) {
                holder.logContent.setTextColor(Color.GRAY)
            } else {
                holder.logContent.setTextColor(getColorAttr(holder.logContent.context, android.R.attr.textColorSecondary))
            }
        }
    }

    override fun getItemCount(): Int = filterLogs.size

    companion object {
        fun getColorAttr(context: Context, attr: Int): ColorStateList? {
            val ta: TypedArray = context.obtainStyledAttributes(intArrayOf(attr))
            return try {
                ta.getColorStateList(0)
            } finally {
                ta.recycle()
            }
        }
    }

}
