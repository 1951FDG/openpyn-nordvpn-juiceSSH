package com.abdeveloper.library;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.PluralsRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("ReturnOfThis")
public class MultiSelectDialog extends AppCompatDialogFragment
        implements DialogInterface.OnClickListener, ViewTreeObserver.OnGlobalLayoutListener, SearchView.OnQueryTextListener,
        MultiSelectViewHolder.SelectionCallbackListener, Filterable {

    // Default Values
    private String mHint = "";

    // Use this instance of the interface to deliver action events
    @Nullable
    private SubmitCallbackListener mListener = null;

    private int mMaxRecycledViews = Integer.MAX_VALUE;

    private int mMaxSelectionLimit = 0;

    private String mMaxSelectionMessage = "";

    private int mMinSelectionLimit = 1;

    private String mMinSelectionMessage = "";

    private MultiSelectAdapter mMultiSelectAdapter = null;

    private MultiSelectFilter mMultiSelectFilter = null;

    private List<MultiSelectable> mMultiSelectItems = null;

    @StringRes
    private int mNegativeText = android.R.string.cancel;

    @StringRes
    private int mPositiveText = android.R.string.ok;

    private Set<Integer> mPostSelectedIds = Collections.emptySet();

    private Set<Integer> mPreSelectedIds = Collections.emptySet();

    private String mTitle = "";

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListener = (SubmitCallbackListener) context;
        } catch (ClassCastException ignored) {
            throw new ClassCastException(context + " must implement SubmitCallbackListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mTitle.isEmpty()) {
            setStyle(DialogFragment.STYLE_NO_TITLE, getTheme());
        } else {
            setStyle(DialogFragment.STYLE_NORMAL, getTheme());
        }

        mMultiSelectAdapter = new MultiSelectAdapter(this);
        mMultiSelectAdapter.setHasStableIds(true);
        mMultiSelectAdapter.submitList(mMultiSelectItems);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), getTheme());
        if (!mTitle.isEmpty()) {
            builder.setTitle(mTitle);
        }
        LayoutInflater inflater = LayoutInflater.from(builder.getContext());
        View view = inflater.inflate(R.layout.multi_select_dialog, null, false);
        onViewCreated(view, savedInstanceState);
        builder.setView(view);
        builder.setPositiveButton(mPositiveText, this);
        builder.setNegativeButton(mNegativeText, this);

        Dialog dialog = builder.create();

        // Workaround to resize the dialog for the input method
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        MultiSelectRecyclerView recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setEmptyView(view.findViewById(R.id.stub));
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(recyclerView.getContext()));

        RecycledViewPool recycledViewPool = recyclerView.getRecycledViewPool();
        recycledViewPool.setMaxRecycledViews(R.layout.multi_select_item, mMaxRecycledViews);

        recyclerView.setAdapter(mMultiSelectAdapter);

        ViewTreeObserver observer = recyclerView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(this);

        SearchView searchView = view.findViewById(R.id.search);
        searchView.setOnQueryTextListener(this);
        searchView.onActionViewExpanded();
        searchView.clearFocus();
        if (!mHint.isEmpty()) {
            searchView.setQueryHint(mHint);
        }
    }

    @Override
    public void onStart() {
        mPostSelectedIds = Collections.checkedSortedSet(new TreeSet<>(mPreSelectedIds), Integer.class);
        if (BuildConfig.DEBUG && !mPostSelectedIds.equals(mPreSelectedIds)) {
            throw new AssertionError(String.format("expected same:<%s> was not:<%s>", mPreSelectedIds, mPostSelectedIds));
        }

        super.onStart();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mListener = null;
    }

    @Override
    public void onClick(@Nullable DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            Resources res = getResources();
            int size = mPostSelectedIds.size();
            if (size >= mMinSelectionLimit) {
                if (size <= mMaxSelectionLimit) {
                    mPreSelectedIds = Collections.checkedSortedSet(new TreeSet<>(mPostSelectedIds), Integer.class);
                    if (mListener != null) {
                        ArrayList<Integer> selectedIds = new ArrayList<>(mPostSelectedIds);
                        ArrayList<String> selectedNames = getSelectNameList(mMultiSelectItems);
                        String dataString = getSelectedDataString(mMultiSelectItems);
                        mListener.onSelected(selectedIds, selectedNames, dataString);
                    }
                } else {
                    showMessage(res, R.plurals.max_selection_message, mMaxSelectionMessage, mMaxSelectionLimit);
                }
            } else {
                showMessage(res, R.plurals.min_selection_message, mMinSelectionMessage, mMinSelectionLimit);
            }
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            if (mListener != null) {
                mListener.onCancel();
            }
        }
    }

    @Override
    public void onGlobalLayout() {
        Dialog dialog = requireDialog();
        View view = dialog.findViewById(R.id.recycler);
        if (view == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Dialog");
        }
        int minHeight = view.getHeight();
        view.setMinimumHeight(minHeight);
        ViewTreeObserver observer = view.getViewTreeObserver();
        observer.removeOnGlobalLayoutListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(@NonNull String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(@NonNull String newText) {
        String lowerCaseQuery = newText.toLowerCase(Locale.ENGLISH);
        Filter filter = getFilter();
        filter.filter(lowerCaseQuery, null);
        return true;
    }

    @Override
    public boolean addToSelection(@NonNull Integer id) {
        return mPostSelectedIds.add(id);
    }

    @Override
    public boolean isSelected(@NonNull Integer id) {
        return mPostSelectedIds.contains(id);
    }

    @Override
    public boolean removeFromSelection(@NonNull Integer id) {
        return mPostSelectedIds.remove(id);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (mMultiSelectFilter == null) {
            mMultiSelectFilter = new MultiSelectFilter();
        }
        return mMultiSelectFilter;
    }

    @Nullable
    public MultiSelectAdapter getAdapter() {
        return mMultiSelectAdapter;
    }

    @NonNull
    public List<MultiSelectable> getMultiSelectItems() {
        return (mMultiSelectItems != null) ? mMultiSelectItems : new ArrayList<>(0);
    }

    @NonNull
    public MultiSelectDialog setHint(@NonNull String str) {
        mHint = str;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMaxRecycledViews(@IntRange(from = 5, to = Integer.MAX_VALUE) int max) {
        mMaxRecycledViews = max;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMaxSelectionLimit(@IntRange(from = 1, to = Integer.MAX_VALUE) int limit) {
        mMaxSelectionLimit = limit;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMaxSelectionMessage(@NonNull String message) {
        mMaxSelectionMessage = message;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMinSelectionLimit(@IntRange(from = 1, to = Integer.MAX_VALUE) int limit) {
        mMinSelectionLimit = limit;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMinSelectionMessage(@NonNull String message) {
        mMinSelectionMessage = message;
        return this;
    }

    @NonNull
    public MultiSelectDialog setMultiSelectList(@NonNull Collection<MultiSelectable> list) {
        List<MultiSelectable> newList = new ArrayList<>(list.size());
        for (MultiSelectable model : list) {
            MultiSelectable clone = model.clone();
            newList.add(clone);
        }
        mMultiSelectItems = Collections.unmodifiableList(newList);
        if (mMaxSelectionLimit == 0) {
            mMaxSelectionLimit = list.size();
        }
        return this;
    }

    @NonNull
    public MultiSelectDialog setNegativeText(@StringRes int message) {
        mNegativeText = message;
        return this;
    }

    @NonNull
    public MultiSelectDialog setPositiveText(@StringRes int message) {
        mPositiveText = message;
        return this;
    }

    @NonNull
    public MultiSelectDialog setPreSelectIDsList(@NonNull Collection<Integer> list) {
        mPreSelectedIds = Collections.checkedSortedSet(new TreeSet<>(list), Integer.class);
        return this;
    }

    @NonNull
    public MultiSelectDialog setTitle(@NonNull String str) {
        mTitle = str;
        return this;
    }

    private ArrayList<String> getSelectNameList(Collection<MultiSelectable> list) {
        ArrayList<String> names = new ArrayList<>(list.size());
        for (MultiSelectable model : list) {
            int id = model.getId();
            if (mPostSelectedIds.contains(id)) {
                names.add(model.getName().toString());
            }
        }
        return names;
    }

    @SuppressWarnings("MagicNumber")
    private String getSelectedDataString(Collection<MultiSelectable> list) {
        StringBuilder data = new StringBuilder(256);
        for (MultiSelectable model : list) {
            int id = model.getId();
            if (mPostSelectedIds.contains(id)) {
                data.append(", ");
                data.append(model.getName());
            }
        }
        return (data.length() > 0) ? data.substring(1) : "";
    }

    private void showMessage(Resources res, @PluralsRes int id, String selectionMessage, int selectionLimit) {
        String message = selectionMessage;
        if (message.isEmpty()) {
            message = res.getQuantityString(id, selectionLimit, selectionLimit);
        }
        Toast toast = Toast.makeText(getActivity(), message, Toast.LENGTH_LONG);
        toast.show();
    }

    public interface SubmitCallbackListener {

        void onCancel();

        void onSelected(@NonNull ArrayList<Integer> selectedIds, @NonNull ArrayList<String> selectedNames, @NonNull String dataString);
    }

    protected class MultiSelectFilter extends Filter {

        private static final int INDEX_NOT_FOUND = -1;

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<MultiSelectable> list = null;
            int length = constraint.length();
            if (length > 0) {
                List<MultiSelectable> items = getMultiSelectItems();
                List<MultiSelectable> newList = new ArrayList<>(items.size());
                for (MultiSelectable model : items) {
                    CharSequence name = model.getName();
                    int queryStart = indexOfIgnoreCase(name, constraint);
                    int queryEnd = queryStart + length;
                    if (queryStart > INDEX_NOT_FOUND) {
                        MultiSelectable clone = model.clone();
                        if (length > 1) {
                            if (clone instanceof Range) {
                                ((Range) clone).setStart(queryStart);
                                ((Range) clone).setEnd(queryEnd);
                            }
                        }
                        newList.add(clone);
                    }
                }
                if (!newList.isEmpty()) {
                    list = newList;
                }
            } else {
                list = getMultiSelectItems();
            }
            FilterResults results = new FilterResults();
            results.values = list;
            results.count = (list == null) ? 0 : list.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            MultiSelectAdapter adapter = getAdapter();
            if (adapter != null) {
                adapter.submitList((List<MultiSelectable>) results.values);
            }
        }

        private int indexOfIgnoreCase(@NonNull CharSequence seq, @NonNull CharSequence searchSeq) {
            int endLimit = (seq.length() - searchSeq.length()) + 1;
            if (endLimit < 0) {
                return INDEX_NOT_FOUND;
            }
            for (int i = 0; i < endLimit; i++) {
                if (regionMatches(seq, i, searchSeq, 0, searchSeq.length())) {
                    return i;
                }
            }
            return INDEX_NOT_FOUND;
        }

        @SuppressWarnings({ "ContinueStatement", "ImplicitNumericConversion", "ValueOfIncrementOrDecrementUsed" })
        private boolean regionMatches(@NonNull CharSequence seq, int toffset, @NonNull CharSequence searchSeq, int ooffset, int len) {
            int to = toffset;
            int po = ooffset;
            while (len-- > 0) {
                char c1 = seq.charAt(to++);
                char c2 = searchSeq.charAt(po++);
                if (c1 == c2) {
                    continue;
                }
                char u1 = Character.toLowerCase(c1);
                char u2 = Character.toLowerCase(c2);
                if (u1 == u2) {
                    continue;
                }
                return false;
            }
            return true;
        }
    }
}
