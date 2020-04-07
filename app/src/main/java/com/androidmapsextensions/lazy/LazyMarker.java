/*
 * Copyright (C) 2013 Maciej Górski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.androidmapsextensions.lazy;

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
public class LazyMarker {

    @Nullable
    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient OnMarkerCreateListener mListener;

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient GoogleMap mMap;

    @Nullable
    @SuppressWarnings("TransientFieldInNonSerializableClass")
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
        if (options.isVisible()) {
            createMarker(googleMap, options, tag, markerCreateListener);
        } else {
            mMap = googleMap;
            mMarkerOptions = copy(options);
            mListener = markerCreateListener;
        }
        mTag = tag;
        mLocation = options.getPosition();
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
        if ((obj == null) || (getClass() != obj.getClass())) {
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

    public void setAlpha(float alpha) {
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

    public void setRotation(float rotation) {
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

    public void setAnchor(float anchorU, float anchorV) {
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

    public void setInfoWindowAnchor(float anchorU, float anchorV) {
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

    private void createMarker(GoogleMap googleMap, MarkerOptions options, Object tag, OnMarkerCreateListener markerCreateListener) {
        mMarker = googleMap.addMarker(options);
        if (tag != null) {
            mMarker.setTag(tag);
        }
        if (markerCreateListener != null) {
            markerCreateListener.onMarkerCreate(this);
        }
    }

    @FunctionalInterface
    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    public interface OnMarkerCreateListener {

        void onMarkerCreate(@NonNull LazyMarker marker);
    }

    @FunctionalInterface
    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    public interface OnLevelChangeCallback {

        void onLevelChange(@NonNull LazyMarker marker, int level);
    }
}
