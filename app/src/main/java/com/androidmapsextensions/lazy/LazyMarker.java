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

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

public class LazyMarker {

    @FunctionalInterface
    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    public interface OnMarkerCreateListener {

        @NonNull
        Marker onMarkerCreate(@NonNull MarkerOptions options, @Nullable Object aTag);
    }

    @FunctionalInterface
    @SuppressWarnings({ "WeakerAccess", "PublicInnerClass" })
    public interface OnLevelChangeCallback {

        void onLevelChange(@NonNull LazyMarker marker, int level);
    }

    private int level;

    @Nullable
    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient OnMarkerCreateListener listener;

    private LatLng location;

    @Nullable
    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient Marker marker;

    @SuppressWarnings("TransientFieldInNonSerializableClass")
    private transient MarkerOptions markerOptions;

    @Nullable
    private Object tag;

    @SuppressWarnings("unused")
    public LazyMarker(@NonNull MarkerOptions options) {
        this(options, null, null);
    }

    @SuppressWarnings("unused")
    public LazyMarker(@NonNull MarkerOptions options, @Nullable Object aTag) {
        this(options, aTag, null);
    }

    @SuppressWarnings("WeakerAccess")
    public LazyMarker(@NonNull MarkerOptions options, @Nullable Object aTag,
                      @Nullable OnMarkerCreateListener markerCreateListener) {
        if (options.isVisible()) {
            createMarker(options, aTag, markerCreateListener);
        } else {
            markerOptions = copy(options);
            listener = markerCreateListener;
        }
        tag = aTag;
        location = options.getPosition();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        return Objects.equals(location, ((LazyMarker) obj).location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    @NonNull
    @Override
    public String toString() {
        return "LazyMarker{" + "tag=" + tag + ", level=" + level + ", location=" + location + "}";
    }

    public float getAlpha() {
        if (marker != null) {
            return marker.getAlpha();
        } else {
            return markerOptions.getAlpha();
        }
    }

    public void setAlpha(float alpha) {
        if (marker != null) {
            marker.setAlpha(alpha);
        } else {
            markerOptions.alpha(alpha);
        }
    }

    @NonNull
    public String getId() {
        createMarker();
        if (marker == null) {
            throw new AssertionError();
        }
        return marker.getId();
    }

    @IntRange(from = 0, to = 9)
    public int getLevel() {
        return level;
    }

    @NonNull
    public LatLng getPosition() {
        if (marker != null) {
            return marker.getPosition();
        } else {
            return markerOptions.getPosition();
        }
    }

    public void setPosition(@NonNull LatLng position) {
        location = position;
        if (marker != null) {
            marker.setPosition(position);
        } else {
            markerOptions.position(position);
        }
    }

    public float getRotation() {
        if (marker != null) {
            return marker.getRotation();
        } else {
            return markerOptions.getRotation();
        }
    }

    public void setRotation(float rotation) {
        if (marker != null) {
            marker.setRotation(rotation);
        } else {
            markerOptions.rotation(rotation);
        }
    }

    @Nullable
    public String getSnippet() {
        if (marker != null) {
            return marker.getSnippet();
        } else {
            return markerOptions.getSnippet();
        }
    }

    public void setSnippet(@NonNull String snippet) {
        if (marker != null) {
            marker.setSnippet(snippet);
        } else {
            markerOptions.snippet(snippet);
        }
    }

    @Nullable
    public Object getTag() {
        return tag;
    }

    public void setTag(@Nullable Object aTag) {
        tag = aTag;
        if (marker != null) {
            marker.setTag(aTag);
        }
    }

    @Nullable
    public String getTitle() {
        if (marker != null) {
            return marker.getTitle();
        } else {
            return markerOptions.getTitle();
        }
    }

    public void setTitle(@NonNull String title) {
        if (marker != null) {
            marker.setTitle(title);
        } else {
            markerOptions.title(title);
        }
    }

    public float getZIndex() {
        if (marker != null) {
            return marker.getZIndex();
        } else {
            return markerOptions.getZIndex();
        }
    }

    public void setZIndex(float zIndex) {
        if (marker != null) {
            marker.setZIndex(zIndex);
        } else {
            markerOptions.zIndex(zIndex);
        }
    }

    public void hideInfoWindow() {
        if (marker != null) {
            marker.hideInfoWindow();
        }
    }

    public boolean isDraggable() {
        if (marker != null) {
            return marker.isDraggable();
        } else {
            return markerOptions.isDraggable();
        }
    }

    public void setDraggable(boolean draggable) {
        if (marker != null) {
            marker.setDraggable(draggable);
        } else {
            markerOptions.draggable(draggable);
        }
    }

    public boolean isFlat() {
        if (marker != null) {
            return marker.isFlat();
        } else {
            return markerOptions.isFlat();
        }
    }

    public void setFlat(boolean flat) {
        if (marker != null) {
            marker.setFlat(flat);
        } else {
            markerOptions.flat(flat);
        }
    }

    public boolean isInfoWindowShown() {
        if (marker != null) {
            return marker.isInfoWindowShown();
        } else {
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isVisible() {
        if (marker != null) {
            return marker.isVisible();
        } else {
            return false;
        }
    }

    public void setVisible(boolean visible) {
        if (marker != null) {
            marker.setVisible(visible);
        } else if (visible) {
            markerOptions.visible(true);
            createMarker();
        }
    }

    public void remove() {
        if (marker != null) {
            marker.remove();
            marker = null;
        }
    }

    public void setAnchor(float anchorU, float anchorV) {
        if (marker != null) {
            marker.setAnchor(anchorU, anchorV);
        } else {
            markerOptions.anchor(anchorU, anchorV);
        }
    }

    public void setIcon(@Nullable BitmapDescriptor icon) {
        if (marker != null) {
            marker.setIcon(icon);
        } else {
            markerOptions.icon(icon);
        }
    }

    public void setInfoWindowAnchor(float anchorU, float anchorV) {
        if (marker != null) {
            marker.setInfoWindowAnchor(anchorU, anchorV);
        } else {
            markerOptions.infoWindowAnchor(anchorU, anchorV);
        }
    }

    public void setLevel(@IntRange(from = 0, to = 9) int aLevel, @Nullable OnLevelChangeCallback callback) {
        level = aLevel;
        if (callback != null) {
            callback.onLevelChange(this, aLevel);
        }
    }

    public void showInfoWindow() {
        if (marker != null) {
            marker.showInfoWindow();
        }
    }

    private void createMarker() {
        if (marker == null) {
            createMarker(markerOptions, tag, listener);
            markerOptions = null;
            listener = null;
        }
    }

    private void createMarker(MarkerOptions options, Object aTag, OnMarkerCreateListener markerCreateListener) {
        if (markerCreateListener != null) {
            marker = markerCreateListener.onMarkerCreate(options, aTag);
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
}
