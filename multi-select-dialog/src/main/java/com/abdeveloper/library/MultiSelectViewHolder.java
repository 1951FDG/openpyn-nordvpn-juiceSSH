package com.abdeveloper.library;

import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.Spannable.Factory;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.widget.CompoundButtonCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import java.util.List;

class MultiSelectViewHolder extends ViewHolder implements View.OnClickListener {

    private static final StyleSpan BOLD_STYLE_SPAN = new StyleSpan(Typeface.BOLD);

    private static final Factory SPANNABLE_FACTORY = new MultiSelectFactory();

    private final ImageView mImageView;

    private final SelectionCallbackListener mListener;

    private final TextView mTitleView;

    MultiSelectViewHolder(@NonNull View view, SelectionCallbackListener listener) {
        super(view);
        mListener = listener;
        View checkboxView = view.findViewById(R.id.checkbox);
        mImageView = view.findViewById(R.id.image);
        mTitleView = view.findViewById(R.id.text);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            if (checkboxView instanceof ImageView) {
                ImageViewCompat.setImageTintList((ImageView) checkboxView,
                        AppCompatResources.getColorStateList(checkboxView.getContext(), R.color.control_checkable_material));
            } else if (checkboxView instanceof CompoundButton) {
                CompoundButtonCompat.setButtonTintList((CompoundButton) checkboxView,
                        AppCompatResources.getColorStateList(checkboxView.getContext(), R.color.control_checkable_material));
            }
        } else {
            mImageView.setClipToOutline(true);
        }
        mTitleView.setSpannableFactory(SPANNABLE_FACTORY);
        //noinspection ThisEscapedInObjectConstruction
        view.setOnClickListener(this);
    }

    @Override
    public void onClick(@NonNull View v) {
        //noinspection NumericCastThatLosesPrecision
        int id = (int) getItemId();
        boolean checked = ((Checkable) v).isChecked();
        if (mListener != null) {
            if (checked) {
                mListener.removeFromSelection(id);
            } else {
                mListener.addToSelection(id);
            }
        }
        ((Checkable) v).setChecked(!checked);
    }

    void bind(MultiSelectable model) {
        int resId = 0;
        if (model instanceof Iconable) {
            resId = ((Iconable) model).getResId();
        }
        if (resId == 0) {
            if (mImageView.getVisibility() != View.GONE) {
                mImageView.setVisibility(View.GONE);
            }
        } else {
            mImageView.setImageResource(resId);
        }
        CharSequence name = model.getName();
        if (name instanceof Spannable) {
            mTitleView.setText(name, TextView.BufferType.SPANNABLE);
            if (model instanceof Range) {
                CharSequence text = mTitleView.getText();
                int start = ((Range) model).getStart();
                int end = ((Range) model).getEnd();
                //noinspection StaticFieldReferencedViaSubclass
                ((Spannable) text).setSpan(BOLD_STYLE_SPAN, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        } else {
            mTitleView.setText(name, TextView.BufferType.NORMAL);
        }
        if (mListener != null) {
            int id = model.getId();
            boolean checked = mListener.isSelected(id);
            ((Checkable) itemView).setChecked(checked);
        }
    }

    void bind(MultiSelectable model, List<Object> payloads) {
        Bundle bundle = (Bundle) payloads.get(0);
        int[] array = bundle.getIntArray(MultiSelectItemCallback.BUNDLE_INT_ARRAY);
        if ((array != null) && (array.length != 0)) {
            CharSequence text = mTitleView.getText();
            if (text instanceof Spannable) {
                StyleSpan[] spans = ((Spannable) text).getSpans(0, text.length(), StyleSpan.class);
                if (spans.length > 0) {
                    //noinspection StaticFieldReferencedViaSubclass
                    ((Spannable) text).setSpan(spans[0], array[0], array[1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public interface SelectionCallbackListener {

        boolean addToSelection(@NonNull Integer id);

        boolean isSelected(@NonNull Integer id);

        boolean removeFromSelection(@NonNull Integer id);
    }
}
