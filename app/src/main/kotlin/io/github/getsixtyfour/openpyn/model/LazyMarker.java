package io.github.getsixtyfour.openpyn.model;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

@SuppressWarnings("FieldNotUsedInToString")
public final class LazyMarker {

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    @Nullable
    private transient OnMarkerCreateListener mListener;

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient GoogleMap mMap;

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    @Nullable
    private transient Marker mMarker;

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient MarkerOptions mMarkerOptions;

    private int mLevel;

    private LatLng mLocation;

    @Nullable
    private Object mTag;

    public LazyMarker(@NonNull GoogleMap googleMap, @NonNull MarkerOptions options) {
        this(googleMap, options, null, null);
    }

    public LazyMarker(@NonNull GoogleMap googleMap, @NonNull MarkerOptions options, @Nullable Object tag) {
        this(googleMap, options, tag, null);
    }

    public LazyMarker(@NonNull GoogleMap googleMap, @NonNull MarkerOptions options, @Nullable Object tag,
                      @Nullable OnMarkerCreateListener markerCreateListener) {
        mMap = googleMap;
        mMarkerOptions = copy(options);
        mTag = tag;
        mListener = markerCreateListener;
        mLocation = options.getPosition();
        if (options.isVisible()) {
            createMarker();
        }
    }

    private static MarkerOptions copy(MarkerOptions options) {
        MarkerOptions copy = new MarkerOptions();
        copy.alpha(options.getAlpha());
        copy.anchor(options.getAnchorU(), options.getAnchorV());
        copy.draggable(options.isDraggable());
        copy.flat(options.isFlat());
        copy.icon(options.getIcon());
        copy.infoWindowAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
        copy.position(options.getPosition());
        copy.rotation(options.getRotation());
        copy.snippet(options.getSnippet());
        copy.title(options.getTitle());
        copy.visible(options.isVisible());
        return copy;
    }

    @SuppressWarnings("NonFinalFieldReferenceInEquals")
    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LazyMarker)) {
            return false;
        }
        return Objects.equals(mLocation, ((LazyMarker) obj).mLocation);
    }

    @SuppressWarnings("NonFinalFieldReferencedInHashCode")
    @Override
    public int hashCode() {
        return mLocation.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "LazyMarker{" + "tag=" + mTag + ", level=" + mLevel + ", location=" + mLocation + "}";
    }

    public float getAlpha() {
        if (mMarker != null) {
            return mMarker.getAlpha();
        }
        return mMarkerOptions.getAlpha();
    }

    public void setAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        if (mMarker != null) {
            mMarker.setAlpha(alpha);
        } else {
            mMarkerOptions.alpha(alpha);
        }
    }

    @Nullable
    public String getId() {
        if (mMarker != null) {
            return mMarker.getId();
        }
        return null;
    }

    @IntRange(from = 0, to = 9)
    public int getLevel() {
        return mLevel;
    }

    @NonNull
    public LatLng getPosition() {
        if (mMarker != null) {
            return mMarker.getPosition();
        }
        return mMarkerOptions.getPosition();
    }

    public void setPosition(@NonNull LatLng position) {
        mLocation = position;
        if (mMarker != null) {
            mMarker.setPosition(position);
        } else {
            mMarkerOptions.position(position);
        }
    }

    public float getRotation() {
        if (mMarker != null) {
            return mMarker.getRotation();
        }
        return mMarkerOptions.getRotation();
    }

    public void setRotation(@FloatRange(from = 0.0, to = 360.0) float rotation) {
        if (mMarker != null) {
            mMarker.setRotation(rotation);
        } else {
            mMarkerOptions.rotation(rotation);
        }
    }

    @Nullable
    public String getSnippet() {
        if (mMarker != null) {
            return mMarker.getSnippet();
        }
        return mMarkerOptions.getSnippet();
    }

    public void setSnippet(@NonNull String snippet) {
        if (mMarker != null) {
            mMarker.setSnippet(snippet);
        } else {
            mMarkerOptions.snippet(snippet);
        }
    }

    @Nullable
    public Object getTag() {
        return mTag;
    }

    public void setTag(@Nullable Object tag) {
        mTag = tag;
        if (mMarker != null) {
            mMarker.setTag(tag);
        }
    }

    @Nullable
    public String getTitle() {
        if (mMarker != null) {
            return mMarker.getTitle();
        }
        return mMarkerOptions.getTitle();
    }

    public void setTitle(@NonNull String title) {
        if (mMarker != null) {
            mMarker.setTitle(title);
        } else {
            mMarkerOptions.title(title);
        }
    }

    public float getZIndex() {
        if (mMarker != null) {
            return mMarker.getZIndex();
        }
        return mMarkerOptions.getZIndex();
    }

    public void setZIndex(float zIndex) {
        if (mMarker != null) {
            mMarker.setZIndex(zIndex);
        } else {
            mMarkerOptions.zIndex(zIndex);
        }
    }

    public void hideInfoWindow() {
        if (mMarker != null) {
            mMarker.hideInfoWindow();
        }
    }

    public boolean isDraggable() {
        if (mMarker != null) {
            return mMarker.isDraggable();
        }
        return mMarkerOptions.isDraggable();
    }

    public void setDraggable(boolean draggable) {
        if (mMarker != null) {
            mMarker.setDraggable(draggable);
        } else {
            mMarkerOptions.draggable(draggable);
        }
    }

    public boolean isFlat() {
        if (mMarker != null) {
            return mMarker.isFlat();
        }
        return mMarkerOptions.isFlat();
    }

    public void setFlat(boolean flat) {
        if (mMarker != null) {
            mMarker.setFlat(flat);
        } else {
            mMarkerOptions.flat(flat);
        }
    }

    public boolean isInfoWindowShown() {
        if (mMarker != null) {
            return mMarker.isInfoWindowShown();
        }
        return false;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVisible() {
        if (mMarker != null) {
            return mMarker.isVisible();
        }
        return false;
    }

    public void setVisible(boolean visible) {
        if (mMarker != null) {
            mMarker.setVisible(visible);
        } else if (visible) {
            mMarkerOptions.visible(true);
            createMarker();
        }
    }

    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    public void remove() {
        if (mMarker != null) {
            mMarker.remove();
            mMarker = null;
        }
    }

    public void setAnchor(@FloatRange(from = 0.0, to = 1.0) float anchorU, @FloatRange(from = 0.0, to = 1.0) float anchorV) {
        if (mMarker != null) {
            mMarker.setAnchor(anchorU, anchorV);
        } else {
            mMarkerOptions.anchor(anchorU, anchorV);
        }
    }

    public void setIcon(@Nullable BitmapDescriptor icon) {
        if (mMarker != null) {
            mMarker.setIcon(icon);
        } else {
            mMarkerOptions.icon(icon);
        }
    }

    public void setInfoWindowAnchor(@FloatRange(from = 0.0, to = 1.0) float anchorU, @FloatRange(from = 0.0, to = 1.0) float anchorV) {
        if (mMarker != null) {
            mMarker.setInfoWindowAnchor(anchorU, anchorV);
        } else {
            mMarkerOptions.infoWindowAnchor(anchorU, anchorV);
        }
    }

    public void setLevel(@IntRange(from = 0, to = 9) int level, @Nullable OnLevelChangeCallback callback) {
        mLevel = level;
        if (callback != null) {
            callback.onLevelChange(this, level);
        }
    }

    public void showInfoWindow() {
        if (mMarker != null) {
            mMarker.showInfoWindow();
        }
    }

    private void createMarker() {
        if (mMarker == null) {
            createMarker(mMap, mMarkerOptions, mTag, mListener);
            mListener = null;
        }
    }

    private void createMarker(GoogleMap googleMap, MarkerOptions options, @Nullable Object tag,
                              @Nullable OnMarkerCreateListener markerCreateListener) {
        mMarker = googleMap.addMarker(options);
        if (tag != null) {
            mMarker.setTag(tag);
        }
        if (markerCreateListener != null) {
            markerCreateListener.onMarkerCreate(this);
        }
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface OnMarkerCreateListener {

        void onMarkerCreate(@NonNull LazyMarker marker);
    }

    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    @FunctionalInterface
    public interface OnLevelChangeCallback {

        void onLevelChange(@NonNull LazyMarker marker, int level);
    }
}
