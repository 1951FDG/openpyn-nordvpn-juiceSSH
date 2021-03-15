package com.abdeveloper.library;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;

import java.util.List;

class MultiSelectAdapter extends ListAdapter<MultiSelectable, MultiSelectViewHolder> {

    private final MultiSelectViewHolder.SelectionCallbackListener mListener;

    MultiSelectAdapter(MultiSelectViewHolder.SelectionCallbackListener listener) {
        super(new MultiSelectItemCallback());
        mListener = listener;
    }

    @NonNull
    @Override
    public MultiSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View root = inflater.inflate(viewType, parent, false);
        return new MultiSelectViewHolder(root, mListener);
    }

    @Override
    public void onBindViewHolder(@NonNull MultiSelectViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public void onBindViewHolder(@NonNull MultiSelectViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            holder.bind(getItem(position), payloads);
        }
    }

    @Override
    public long getItemId(int position) {
        MultiSelectable model = getItem(position);
        //noinspection ImplicitNumericConversion
        return model.getId();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.multi_select_item;
    }
}
