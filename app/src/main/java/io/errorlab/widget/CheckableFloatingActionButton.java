package io.errorlab.widget;

import android.content.Context;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.Checkable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class CheckableFloatingActionButton extends FloatingActionButton implements Checkable {
	private static final int[] CheckedStateSet = {
		android.R.attr.state_checked,
	};

	private boolean checked = false;

	public CheckableFloatingActionButton(Context ctx) {
		this(ctx, null);
	}

	public CheckableFloatingActionButton(Context ctx, AttributeSet attrs) {
		this(ctx, attrs, 0);
	}

	public CheckableFloatingActionButton(Context ctx, AttributeSet attrs, int defStyle) {
		super(ctx, attrs, defStyle);
	}

	@Override
	public int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CheckedStateSet);
		}
		return drawableState;
	}

	@Override
	public void setChecked(boolean checked) {
		if (checked == this.checked) {
			return;
		}
		this.checked = checked;
	}

	@Override
	public boolean isChecked() { return this.checked; }

	@Override
	public void toggle() { setChecked(!this.checked); }

	@Override
	public boolean performClick() {
		toggle();
		return super.performClick();
	}

	@Override
    protected Parcelable onSaveInstanceState() {
        CheckedSavedState result = new CheckedSavedState(super.onSaveInstanceState());
        result.checked = checked;
        return result;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof CheckedSavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        CheckedSavedState ss = (CheckedSavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setChecked(ss.checked);
    }
}
