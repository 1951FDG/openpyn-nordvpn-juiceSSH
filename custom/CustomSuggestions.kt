package io.github.sdsstudios.nvidiagpumonitor.custom

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import com.abdeveloper.library.MultiSelectModel
//import com.lapism.searchview.widget.SearchItem
import io.github.sdsstudios.nvidiagpumonitor.R

@Suppress("MagicNumber")
object CustomSuggestions {
    private val countries: ArrayList<Country> = arrayListOf(
            Country("Albania", "al", R.drawable.flag_al),
            Country("Argentina", "ar", R.drawable.flag_ar),
            Country("Australia", "au", R.drawable.flag_au),
            Country("Austria", "at", R.drawable.flag_at),
            Country("Azerbaijan", "az", R.drawable.flag_az),
            Country("Belgium", "be", R.drawable.flag_be),
            Country("Bosnia and Herzegovina", "ba", R.drawable.flag_ba),
            Country("Brazil", "br", R.drawable.flag_br),
            Country("Bulgaria", "bg", R.drawable.flag_bg),
            Country("Canada", "ca", R.drawable.flag_ca),
            Country("Chile", "cl", R.drawable.flag_cl),
            Country("Costa Rica", "cr", R.drawable.flag_cr),
            Country("Croatia", "hr", R.drawable.flag_hr),
            Country("Cyprus", "cy", R.drawable.flag_cy),
            Country("Czech Republic", "cz", R.drawable.flag_cz),
            Country("Denmark", "dk", R.drawable.flag_dk),
            Country("Egypt", "eg", R.drawable.flag_eg),
            Country("Estonia", "ee", R.drawable.flag_ee),
            Country("Finland", "fi", R.drawable.flag_fi),
            Country("France", "fr", R.drawable.flag_fr),
            Country("Georgia", "ge", R.drawable.flag_ge),
            Country("Germany", "de", R.drawable.flag_de),
            Country("Greece", "gr", R.drawable.flag_gr),
            Country("Hong Kong", "hk", R.drawable.flag_hk),
            Country("Hungary", "hu", R.drawable.flag_hu),
            Country("Iceland", "is", R.drawable.flag_is),
            Country("India", "in", R.drawable.flag_in),
            Country("Indonesia", "id", R.drawable.flag_id),
            Country("Ireland", "ie", R.drawable.flag_ie),
            Country("Israel", "il", R.drawable.flag_il),
            Country("Italy", "it", R.drawable.flag_it),
            Country("Japan", "jp", R.drawable.flag_jp),
            Country("Latvia", "lv", R.drawable.flag_lv),
            Country("Luxembourg", "lu", R.drawable.flag_lu),
            Country("Macedonia", "mk", R.drawable.flag_mk),
            Country("Malaysia", "my", R.drawable.flag_my),
            Country("Mexico", "mx", R.drawable.flag_mx),
            Country("Moldova", "md", R.drawable.flag_md),
            Country("Netherlands", "nl", R.drawable.flag_nl),
            Country("New Zealand", "nz", R.drawable.flag_nz),
            Country("Norway", "no", R.drawable.flag_no),
            Country("Poland", "pl", R.drawable.flag_pl),
            Country("Portugal", "pt", R.drawable.flag_pt),
            Country("Romania", "ro", R.drawable.flag_ro),
            Country("Russia", "ru", R.drawable.flag_ru),
            Country("Serbia", "rs", R.drawable.flag_rs),
            Country("Singapore", "sg", R.drawable.flag_sg),
            Country("Slovakia", "sk", R.drawable.flag_sk),
            Country("Slovenia", "si", R.drawable.flag_si),
            Country("South Africa", "za", R.drawable.flag_za),
            Country("South Korea", "kr", R.drawable.flag_kr),
            Country("Spain", "es", R.drawable.flag_es),
            Country("Sweden", "se", R.drawable.flag_se),
            Country("Switzerland", "ch", R.drawable.flag_ch),
            Country("Taiwan", "tw", R.drawable.flag_tw),
            Country("Thailand", "th", R.drawable.flag_th),
            Country("Turkey", "tr", R.drawable.flag_tr),
            Country("Ukraine", "ua", R.drawable.flag_ua),
            Country("United Arab Emirates", "ae", R.drawable.flag_ae),
            Country("United Kingdom", "gb", R.drawable.flag_gb),
            Country("United States", "us", R.drawable.flag_us),
            Country("Vietnam", "vn", R.drawable.flag_vn)
    )

    private val countries2: ArrayList<MultiSelectModel> = arrayListOf(
            MultiSelectModel(0, "Albania", R.drawable.flag_al),
            MultiSelectModel(1, "Argentina", R.drawable.flag_ar),
            MultiSelectModel(2, "Australia", R.drawable.flag_au),
            MultiSelectModel(3, "Austria", R.drawable.flag_at),
            MultiSelectModel(4, "Azerbaijan", R.drawable.flag_az),
            MultiSelectModel(5, "Belgium", R.drawable.flag_be),
            MultiSelectModel(6, "Bosnia and Herzegovina", R.drawable.flag_ba),
            MultiSelectModel(7, "Brazil", R.drawable.flag_br),
            MultiSelectModel(8, "Bulgaria", R.drawable.flag_bg),
            MultiSelectModel(9, "Canada", R.drawable.flag_ca),
            MultiSelectModel(10, "Chile", R.drawable.flag_cl),
            MultiSelectModel(11, "Costa Rica", R.drawable.flag_cr),
            MultiSelectModel(12, "Croatia", R.drawable.flag_hr),
            MultiSelectModel(13, "Cyprus", R.drawable.flag_cy),
            MultiSelectModel(14, "Czech Republic", R.drawable.flag_cz),
            MultiSelectModel(15, "Denmark", R.drawable.flag_dk),
            MultiSelectModel(16, "Egypt", R.drawable.flag_eg),
            MultiSelectModel(17, "Estonia", R.drawable.flag_ee),
            MultiSelectModel(18, "Finland", R.drawable.flag_fi),
            MultiSelectModel(19, "France", R.drawable.flag_fr),
            MultiSelectModel(20, "Georgia", R.drawable.flag_ge),
            MultiSelectModel(21, "Germany", R.drawable.flag_de),
            MultiSelectModel(22, "Greece", R.drawable.flag_gr),
            MultiSelectModel(23, "Hong Kong", R.drawable.flag_hk),
            MultiSelectModel(24, "Hungary", R.drawable.flag_hu),
            MultiSelectModel(25, "Iceland", R.drawable.flag_is),
            MultiSelectModel(26, "India", R.drawable.flag_in),
            MultiSelectModel(27, "Indonesia", R.drawable.flag_id),
            MultiSelectModel(28, "Ireland", R.drawable.flag_ie),
            MultiSelectModel(29, "Israel", R.drawable.flag_il),
            MultiSelectModel(30, "Italy", R.drawable.flag_it),
            MultiSelectModel(31, "Japan", R.drawable.flag_jp),
            MultiSelectModel(32, "Latvia", R.drawable.flag_lv),
            MultiSelectModel(33, "Luxembourg", R.drawable.flag_lu),
            MultiSelectModel(34, "Macedonia", R.drawable.flag_mk),
            MultiSelectModel(35, "Malaysia", R.drawable.flag_my),
            MultiSelectModel(36, "Mexico", R.drawable.flag_mx),
            MultiSelectModel(37, "Moldova", R.drawable.flag_md),
            MultiSelectModel(38, "Netherlands", R.drawable.flag_nl),
            MultiSelectModel(39, "New Zealand", R.drawable.flag_nz),
            MultiSelectModel(40, "Norway", R.drawable.flag_no),
            MultiSelectModel(41, "Poland", R.drawable.flag_pl),
            MultiSelectModel(42, "Portugal", R.drawable.flag_pt),
            MultiSelectModel(43, "Romania", R.drawable.flag_ro),
            MultiSelectModel(44, "Russia", R.drawable.flag_ru),
            MultiSelectModel(45, "Serbia", R.drawable.flag_rs),
            MultiSelectModel(46, "Singapore", R.drawable.flag_sg),
            MultiSelectModel(47, "Slovakia", R.drawable.flag_sk),
            MultiSelectModel(48, "Slovenia", R.drawable.flag_si),
            MultiSelectModel(49, "South Africa", R.drawable.flag_za),
            MultiSelectModel(50, "South Korea", R.drawable.flag_kr),
            MultiSelectModel(51, "Spain", R.drawable.flag_es),
            MultiSelectModel(52, "Sweden", R.drawable.flag_se),
            MultiSelectModel(53, "Switzerland", R.drawable.flag_ch),
            MultiSelectModel(54, "Taiwan", R.drawable.flag_tw),
            MultiSelectModel(55, "Thailand", R.drawable.flag_th),
            MultiSelectModel(56, "Turkey", R.drawable.flag_tr),
            MultiSelectModel(57, "Ukraine", R.drawable.flag_ua),
            MultiSelectModel(58, "United Arab Emirates", R.drawable.flag_ae),
            MultiSelectModel(59, "United Kingdom", R.drawable.flag_gb),
            MultiSelectModel(60, "United States", R.drawable.flag_us),
            MultiSelectModel(61, "Vietnam", R.drawable.flag_vn)
    )

    fun setCountries(countries: ArrayList<Country>) {
        this.countries.clear()
        this.countries.addAll(countries)
    }

    fun getCountries(): ArrayList<Country> {
        return countries
    }

    fun getCountries2(): ArrayList<MultiSelectModel> {
        return countries2
    }

    fun getCountries(context: Context, strings: Array<String>, strings2: Array<String>, bool: Boolean): ArrayList<Country> {
        val res = context.resources
        val countriesList = ArrayList<Country>()

        strings.indices.forEach { it ->
            val name = strings[it]
            val countryCode = strings2[it]
            // resId = 0 is not found
            val resId = res.getIdentifier("flag_$countryCode", "drawable", context.packageName)
            countriesList.add(Country(name, countryCode, resId))
        }

        return countriesList
    }

//    fun getCountries(context: Context, strings: Array<String>, strings2: Array<String>): ArrayList<SearchItem> {
//        val res = context.resources
//        val countriesList = ArrayList<SearchItem>()
//
//        strings.indices.forEach { it ->
//            val name = strings[it]
//            val countryCode = strings2[it]
//            // resId = 0 is not found
//            val resId = res.getIdentifier("flag_$countryCode", "drawable", context.packageName)
//            val suggestion = SearchItem(context)
//            suggestion.title = name
//            suggestion.icon1Resource = resId
////            suggestion.subtitle = countryCode
//            countriesList.add(suggestion)
//        }
//
//        return countriesList
//    }

    fun getCountries(arr1: Array<out String>, arr2: Array<out String>): String {
        val countriesList = StringBuilder()

        arr1.indices.forEach { it ->
            val name = arr1[it]
            val countryCode = arr2[it]
            val resId = "R.drawable.flag_$countryCode"
            countriesList.append("Country(\"$name\", \"$countryCode\", $resId)")
            countriesList.append("‚‗‚")
        }

        return countriesList.toString()
    }

    fun getCountries2(arr1: Array<out String>, arr2: Array<out String>): String {
        val countriesList = StringBuilder()

        arr1.indices.forEach { it ->
            val name = arr1[it]
            val countryCode = arr2[it]
            val resId = "R.drawable.flag_$countryCode"
            countriesList.append("MultiSelectModel($it, \"$name\", $resId)")
            countriesList.append("‚‗‚")
        }

        return countriesList.toString()
    }

    fun getDrawable(context: Context, countryCode: String): Drawable? {
        val res = context.resources
        val resId = res.getIdentifier("flag_$countryCode", "drawable", context.packageName)
        return ResourcesCompat.getDrawable(res, resId, null)
    }
}

/*
val res = this.resources
val arr1 = res.getStringArray(R.array.pref_country_entries)
val arr2 = res.getStringArray(R.array.pref_country_values)
error(getCountries2(arr1, arr2))
*/

//    implementation 'com.alexmasson58.expensiblefabkotlin:expensiblefabkotlin:1.0.4'
//    implementation 'com.github.mancj:MaterialSearchBar:0.7.6'
//    implementation 'com.github.edsilfer:sticky-index:1.3.0'
//    implementation 'com.lapism:searchview:27.1.1.0.0'

//import io.github.droidkaigi.confsched.util.ResourceUtil
//import android.support.v7.widget.RecyclerView
//import android.view.inputmethod.InputMethodManager
//import br.com.stickyindex.view.StickyIndex
//import com.lapism.searchview.Search
//import com.lapism.searchview.database.SearchHistoryTable
//import com.lapism.searchview.widget.SearchItem
//import com.mancj.materialsearchbar.MaterialSearchBar
//import io.github.sdsstudios.nvidiagpumonitor.custom.CustomSuggestionsAdapter
//import io.github.sdsstudios.nvidiagpumonitor.custom.Country
//import io.github.sdsstudios.nvidiagpumonitor.custom.CustomSearchAdapter
//import io.github.sdsstudios.nvidiagpumonitor.custom.CustomSuggestions.getCountries
//import io.github.sdsstudios.nvidiagpumonitor.custom.CustomSuggestions.getCountries2


//      MaterialSearchBar.OnSearchActionListener,
//      CustomSuggestionsAdapter.OnItemViewClickListener,
//      Search.OnQueryTextListener, Search.OnOpenCloseListener,
//      CustomSearchAdapter.OnSearchItemClickListener,
//      Search.OnMenuClickListener,
//      Search.OnMicClickListener,
//      Search.OnLogoClickListener {

//    private lateinit var customSuggestionsAdapter: CustomSuggestionsAdapter
//    var recyclerView: RecyclerView? = null
//    var stickyIndex: StickyIndex? = null
//    var container: View? = null
//    private val mHistoryDatabase by lazy { SearchHistoryTable(this) }
//    private val searchAdapter by lazy { CustomSearchAdapter(this) }

//    @MainThread
//    fun setUpSearch() {
//        val res = this.resources
//        val arr1 = res.getStringArray(R.array.pref_country_entries)
//        val arr2 = res.getStringArray(R.array.pref_country_values)
//        //customSuggestionsAdapter.suggestions = getCountries(this, arr1, arr2)
//        //error(getCountries(arr1, arr2))
//
//
//        searchAdapter.suggestionsList = getCountries(this, arr1, arr2)
//        searchView.adapter = searchAdapter
//
//        searchView.setOnQueryTextListener(this)
//
//        searchView.setOnOpenCloseListener(this)
//
//        searchAdapter.setOnSearchItemClickListener(this)



//        searchView.setOnMenuClickListener(this)
//        searchView.setOnMicClickListener(this)
//        searchView.setOnLogoClickListener(this)

//        searchBar.setOnSearchActionListener(this)
//        searchBar.inflateMenu(menu_main)
//        searchBar.menu.setOnMenuItemClickListener {
//            val id = it.itemId
//            when (id) {
//                R.id.action_settings -> {
//                    onOptionsItemSelected(it)
//                }
//                else ->
//                    false
//            }
//        }

//        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE)
//        customSuggestionsAdapter =  CustomSuggestionsAdapter(inflater as LayoutInflater)
//        customSuggestionsAdapter.suggestions = getCountries()
//        //searchBar.updateLastSuggestions(getCountries())



//        customSuggestionsAdapter.setSuggestionsClickListener(this)
//        searchBar.setCustomSuggestionAdapter(customSuggestionsAdapter)
//
//        container = findViewById<RelativeLayout>(com.mancj.materialsearchbar.R.id.last)
//        stickyIndex = layoutInflater.inflate(R.layout.sticky_index, null, true) as StickyIndex
//        recyclerView = findViewById(com.mancj.materialsearchbar.R.id.mt_recycler)
//        // Binds the RecyclerViews to synchronize the scroll event between the them
//        stickyIndex!!.bindRecyclerView(recyclerView!!)
//        // Adds the sticky index content to the stickyIndex. This must be called whenever the content changes
//        stickyIndex!!.refresh(convertToIndexList(customSuggestionsAdapter.suggestions))
//        (container as RelativeLayout).addView(stickyIndex)

//        searchBar.addTextChangeListener(object : TextWatcher, Filter.FilterListener {
//            val destiny = resources.displayMetrics.density
//
//            override fun onFilterComplete(count: Int) {
////                fun getListHeight(isSubtraction: Boolean): Int {
////                    return if (!isSubtraction) (customSuggestionsAdapter.listHeight * destiny).toInt() else ((customSuggestionsAdapter.itemCount - 1) * customSuggestionsAdapter.singleViewHeight * destiny).toInt()
////                }
////                fun animateSuggestions(from: Int, to: Int) {
////                    val last = findViewById<View>(com.mancj.materialsearchbar.R.id.last) as RelativeLayout
////                    val lp = last.layoutParams
////                    if (to == 0 && lp.height == 0)
////                        return
////                    val animator = ValueAnimator.ofInt(from, to)
////                    animator.duration = 200
////                    animator.addUpdateListener { animation ->
////                        lp.height = animation.animatedValue as Int
////                        last.layoutParams = lp
////                    }
////                    if (searchBar.isSearchEnabled)
////                        animator.start()
////                }
////
////                fun updateLastSuggestions(suggestions: List<*>) {
////                    val startHeight = getListHeight(false)
////                    if (suggestions.isNotEmpty()) {
////                        animateSuggestions(startHeight, getListHeight(false))
////                    } else {
////                        animateSuggestions(startHeight, 0)
////                    }
////                }
////
////                if (searchBar.isSearchEnabled) {
////                    updateLastSuggestions(customSuggestionsAdapter.suggestions)
////                }
//
//                if (searchBar.isSearchEnabled) {
//                    stickyIndex!!.refresh(convertToIndexList(customSuggestionsAdapter.suggestions))
//                }
//            }
//
//            override fun beforeTextChanged(charSequence: CharSequence, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(charSequence: CharSequence, start: Int, before: Int, count: Int) {
//                customSuggestionsAdapter.filter.filter(searchBar.text, this)
//            }
//
//            override fun afterTextChanged(editable: Editable) {}
//        })
//    }


//    // SearchLayout
//    override fun onMicClick() {
////        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onMenuClick() {
////        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onQueryTextSubmit(query: CharSequence?): Boolean {
//        val item = SearchItem(this)
//        item.title = query
//
//        mHistoryDatabase.addItem(item)
//
////        searchView.close()
//        searchAdapter.notifyDataSetChanged()
//
//        return false
//    }
//
//    override fun onQueryTextChange(newText: CharSequence?) {
////        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    // SearchView
//    override fun onLogoClick() {
////        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onOpen() {
////        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override fun onClose() {
////        searchView.setText("")
//    }
//
//    // SearchAdapter
//    override fun onSearchItemClick(position: Int, title: CharSequence?, subtitle: CharSequence?) {
//        val item = SearchItem(this)
//        item.title = title
//        item.subtitle = subtitle
//
//        mHistoryDatabase.addItem(item)
//
////        searchView.close()
//        searchAdapter.notifyDataSetChanged()
//    }
//
//    /**
//     * Maps the RecyclerView content to a {@link CharArray} to be used as sticky-indexes
//     */
//    private fun convertToIndexList(list: List<Country>) = list.map { country -> country.name.toUpperCase()[0] }
//            .toCollection(ArrayList())
//            .toCharArray()
//
//    private fun onCountryClicked(country: Country) {
//        toast(country.name)
//    }
//
//    override fun onItemClickListener(position: Int, v: View) {
//        onCountryClicked(customSuggestionsAdapter.suggestions[position])
//    }
//
//    override fun onSearchStateChanged(enabled: Boolean) {
//    }
//
//    override fun onSearchConfirmed(text: CharSequence) {
////        startSearch(text.toString(), true, null, false)
//        // Close Keyboard
//        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
////        imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
////        searchBar.disableSearch()
//    }
//
//    override fun onButtonClicked(buttonCode: Int) {
//        when (buttonCode) {
//            MaterialSearchBar.BUTTON_SPEECH -> { }
//            MaterialSearchBar.BUTTON_NAVIGATION -> { }
////            MaterialSearchBar.BUTTON_BACK -> { searchBar.hideSuggestionsList() }
//        }
//    }

//    private fun startRotateIconAnimation() {
//        val animator = ObjectAnimator.ofFloat(drawableWrapper!!, "rotation", 0f, 360f)
//        animator.addListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) {}
//
//            override fun onAnimationEnd(animation: Animator) {}
//
//            override fun onAnimationCancel(animation: Animator) {}
//
//            override fun onAnimationRepeat(animation: Animator) {}
//        })
//        animator.interpolator = AccelerateDecelerateInterpolator()
//        animator.duration = 1000
//        animator.start()
//
//        val animator = AnimatorInflater.loadAnimator(this, R.animator.rotation)
//        val interpolator = AnimationUtils.loadInterpolator(this, R.anim.linear_interpolator)
//        animator.interpolator = interpolator
//        animator.duration = 1000
//        animator.setTarget(item.icon)
//        animator.start()
//        val actionBar = supportActionBar
//        if (actionBar != null) {
//            if (actionBar.isShowing) {
//                actionBar.hide()
//            } else {
//                actionBar.show()
//            }
//        }
//    }

//    class MainActivityUI : AnkoComponent<MainActivity> {
//        override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
//            verticalLayout {
//                relativeLayout {
//                    textView {
//                        text = "23"
//                        textSize = 24f
//                        //gravity = Gravity.START
//                    }.lparams(width = wrapContent, height = wrapContent) {
//                        alignParentLeft()
//
//                    }
//
//                    textView {
//                        text = "Enter your request"
//                        textSize = 24f
//                        //gravity = Gravity.END
//                    }.lparams(width = wrapContent, height = wrapContent) {
//                        //margin = dip(20)
//                        //gravity = Gravity.END
//                        alignParentRight()
//                    }
//                }
//                gravity = Gravity.CENTER
//                padding = dip(40)
//
//
//
//                textView {
//                    gravity = Gravity.CENTER
//                    text = "Enter your request"
//                    textColor = Color.BLACK
//                    textSize = 24f
//                }.lparams(width = matchParent) {
//                    margin = dip(40)
//                }
//
//                val name = editText {
//                hint = "What is your name?"
//            }
//
//                editText {
//                    hint = "What is your message?"
//                    lines = 3
//                }
//
//                button("Enter") {
//                    }
//                }
//
//            }
//        }

//   def filter_by_protocol(json_res_list, tcp):
//        remaining_servers = []
//
//        for res in json_res_list:
//        # when connecting using TCP only append if it supports OpenVPN-TCP
//        if tcp is True and res["features"]["openvpn_tcp"] is True:
//        remaining_servers.append([res["domain"][:res["domain"].find(".")], res["load"]])
//        # when connecting using TCP only append if it supports OpenVPN-TCP
//        elif tcp is False and res["features"]["openvpn_udp"] is True:
//        remaining_servers.append([res["domain"][:res["domain"].find(".")], res["load"]])
//        # logger.debug("TCP SERVESR :", res["feature"], res["feature"]["openvpn_tcp"])
//        return remaining_servers

//it?.backgroundTintList = ContextCompat.getColorStateList(fab0.context, R.color.colorPrimary)
//ViewCompat.setBackgroundTintList(it, ContextCompat.getColorStateList(fab0.context, R.color.colorPrimary))

//        private var drawableWrapper: AnimationDrawableWrapper? = null
//        val menuItem = menu.findItem(R.id.action_refresh)
//        drawableWrapper = AnimationDrawableWrapper(resources, menuItem.icon)
//        menuItem.icon = drawableWrapper
//        startRotateIconAnimation()
//every scale is 1.0
//18 stops
// 3 21
//<uses-sdk tools:overrideLibrary="com.lapism.searchview" />
//        <!--android:icon="@drawable/ic_refresh"-->

//<!--<com.mancj.materialsearchbar.MaterialSearchBar-->
//<!--android:id="@+id/searchBar"-->
//<!--style="@style/MaterialSearchBarLight"-->
//<!--android:layout_width="match_parent"-->
//<!--android:layout_height="wrap_content"-->
//<!--android:layout_gravity="start"-->
//<!--android:layout_marginBottom="@dimen/layout_margin"-->
//<!--android:layout_marginStart="@dimen/layout_margin"-->
//<!--android:layout_marginTop="@dimen/layout_margin"-->
//<!--android:layout_marginEnd="@dimen/layout_margin"-->
//<!--android:fitsSystemWindows="true"-->
//<!--android:paddingStart="@dimen/fab_margin"-->
//<!--android:paddingTop="@dimen/fab_margin"-->
//<!--android:paddingEnd="@dimen/fab_margin"-->
//<!--android:paddingBottom="@dimen/fab_margin"-->
//<!--app:mt_hint="Select Your Country"-->
//<!--app:mt_navIconEnabled="true"-->
//<!--app:mt_placeholder="Search" />-->
//
//<!--<com.lapism.searchview.widget.SearchView-->
//<!--android:id="@+id/searchView"-->
//<!--android:layout_width="match_parent"-->
//<!--android:layout_height="match_parent"-->
//<!--android:fitsSystemWindows="true"-->
//<!--app:search_logo="arrow"-->
//<!--app:search_shape="classic"-->
//<!--app:search_theme="light"-->
//<!--app:search_version_margins="menu_item"-->
//<!--app:search_version="menu_item"-->
//<!--app:search_hint="Select Your Country"-->
//<!--app:search_shadow="true"-->
//<!--app:layout_behavior="@string/search_behavior" />-->

//<!--<dimen name="search_height_bar">44dp</dimen>-->
//<!--<dimen name="search_height_view">48dp</dimen>-->
//
//<!--<dimen name="search_shape_classic">@dimen/cardview_default_radius</dimen>-->
//<!--<dimen name="search_shape_rounded">8dp</dimen>-->
//<!--<dimen name="search_shape_oval">24dp</dimen>-->
//
//<!--<dimen name="search_key_line_8">8dp</dimen>-->
//<!--<dimen name="search_divider">1dp</dimen>-->
//
//<!--<dimen name="search_item_height">72dp</dimen>-->
//<!--<dimen name="search_icon_24">24dp</dimen>-->
//<!--<dimen name="search_icon_48">48dp</dimen>-->
//<!--<dimen name="search_icon_56">56dp</dimen>-->
//
//<!--<dimen name="search_text_12">12sp</dimen>-->
//<!--<dimen name="search_text_14">14sp</dimen>-->
//<!--<dimen name="search_text_16">16sp</dimen>-->
//<!--<dimen name="search_reveal">24dp</dimen>-->
//
//<!--<dimen name="search_menu_margin_right">10dp</dimen>-->
//
//<!--<dimen name="search_icon_40">40dp</dimen>-->

//<item
//android:id="@+id/action_search"
//android:icon="@drawable/ic_search_black_24dp"
//android:orderInCategory="98"
//android:title="@android:string/search_go"
//app:showAsAction="ifRoom" />

//            R.id.action_search -> {
//                searchView.open(item)
//                true
//            }