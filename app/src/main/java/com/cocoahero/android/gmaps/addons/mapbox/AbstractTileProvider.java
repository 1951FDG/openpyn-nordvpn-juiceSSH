package com.cocoahero.android.gmaps.addons.mapbox;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;

import java.util.Objects;

import org.jetbrains.annotations.NonNls;

@MainThread
abstract class AbstractTileProvider implements TileProvider {

    // Tile dimension, in pixels
    private static final int TILE_DIM = 512;

    @Nullable
    protected LatLngBounds mBounds;

    protected float mMinimumZoom;

    protected float mMaximumZoom;

    /**
     * Convert tile coordinates and zoom into Bounds format.
     *
     * @param x The requested x coordinate.
     * @param y The requested y coordinate.
     * @param z The requested zoom level.
     * @return the geographic bounds of the tile
     */
    @SuppressWarnings({ "MagicNumber", "ImplicitNumericConversion" })
    @NonNull
    public static LatLngBounds calculateTileBounds(int x, int y, int z) {
        // Width of the world = 1
        double worldWidth = 1.0;
        // Calculate width of one tile, given there are 2 ^ zoom tiles in that zoom level
        // In terms of world width units
        double tileWidth = worldWidth / Math.pow(2.0, z);
        // Make bounds: minX, maxX, minY, maxY
        double minX = x * tileWidth;
        double maxX = (x + 1) * tileWidth;
        double minY = y * tileWidth;
        double maxY = (y + 1) * tileWidth;
        SphericalMercatorProjection projection = new SphericalMercatorProjection(worldWidth);
        LatLng sw = projection.toLatLng(new Point(minX, maxY));
        LatLng ne = projection.toLatLng(new Point(maxX, minY));
        return new LatLngBounds(sw, ne);
    }

    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Override
    public Tile getTile(int x, int y, int zoom) {
        byte[] bytes = getBytes(x, y, zoom);
        return (bytes != null) ? new Tile(TILE_DIM, TILE_DIM, bytes) : NO_TILE;
    }

    /**
     * The geographic bounds available from this provider.
     *
     * @return the geographic bounds available or {@link null} if it could not
     * be determined.
     */
    @Nullable
    public LatLngBounds getBounds() {
        return mBounds;
    }

    /**
     * The minimum zoom level supported by this provider.
     *
     * @return the minimum zoom level supported or {@link #mMinimumZoom} if
     * it could not be determined.
     */
    public float getMinimumZoom() {
        return mMinimumZoom;
    }

    /**
     * The maximum zoom level supported by this provider.
     *
     * @return the maximum zoom level supported or {@link #mMaximumZoom} if
     * it could not be determined.
     */
    public float getMaximumZoom() {
        return mMaximumZoom;
    }

    /**
     * Determines if the requested zoom level is supported by this provider.
     *
     * @param zoom The requested zoom level.
     * @return {@code true} if the requested zoom level is supported by this
     * provider.
     */
    public boolean isZoomLevelAvailable(float zoom) {
        return (zoom >= mMinimumZoom) && (zoom <= mMaximumZoom);
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getName() {
        return getStringValue("name");
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getFormat() {
        return getStringValue("format");
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getAttribution() {
        return getStringValue("attribution");
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getDescription() {
        return getStringValue("description");
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getType() {
        return getStringValue("type");
    }

    @SuppressWarnings("unused")
    @Nullable
    public String getVersion() {
        return getStringValue("version");
    }

    @SuppressWarnings("StandardVariableNames")
    protected LatLngBounds calculateBounds() {
        String result = Objects.requireNonNull(getStringValue("bounds"));
        String[] parts = result.split(",");
        // OpenLayers Bounds format (left, bottom, right, top)
        double w = Double.parseDouble(parts[0]);
        double s = Double.parseDouble(parts[1]);
        double e = Double.parseDouble(parts[2]);
        double n = Double.parseDouble(parts[3]);
        LatLng sw = new LatLng(s, w);
        LatLng ne = new LatLng(n, e);
        return new LatLngBounds(sw, ne);
    }

    protected float calculateMinZoomLevel() {
        String result = Objects.requireNonNull(getStringValue("minzoom"));
        return Float.parseFloat(result);
    }

    protected float calculateMaxZoomLevel() {
        String result = Objects.requireNonNull(getStringValue("maxzoom"));
        return Float.parseFloat(result);
    }

    @Nullable
    protected abstract byte[] getBytes(int x, int y, int zoom);

    @Nullable
    protected abstract String getSQliteVersion();

    @Nullable
    protected abstract String getStringValue(@NonNls @NonNull String key);
}
