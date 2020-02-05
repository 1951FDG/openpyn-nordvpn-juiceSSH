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

@MainThread
abstract class AbstractTileProvider implements TileProvider {
    //region Class variables

    // Tile dimension, in pixels.
    private static final int TILE_DIM = 512;

    //endregion

    //region Instance variables

    @Nullable
    protected LatLngBounds mBounds;

    protected float mMaximumZoom;

    protected float mMinimumZoom;

    //endregion

    //region Methods

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
        SphericalMercatorProjection sProjection = new SphericalMercatorProjection(worldWidth);
        LatLng sw = sProjection.toLatLng(new Point(minX, maxY));
        LatLng ne = sProjection.toLatLng(new Point(maxX, minY));
        return new LatLngBounds(sw, ne);
    }

    @SuppressWarnings("ParameterNameDiffersFromOverriddenParameter")
    @Override
    public Tile getTile(int x, int y, int zoom) {
        byte[] bytes = getBytes(x, y, zoom);
        return (bytes != null) ? new Tile(TILE_DIM, TILE_DIM, bytes) : NO_TILE;
    }

    @Nullable
    public String getAttribution() {
        return getStringValue("attribution");
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

    @Nullable
    public String getDescription() {
        return getStringValue("description");
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
     * The minimum zoom level supported by this provider.
     *
     * @return the minimum zoom level supported or {@link #mMinimumZoom} if
     * it could not be determined.
     */
    public float getMinimumZoom() {
        return mMinimumZoom;
    }

    @Nullable
    public String getName() {
        return getStringValue("name");
    }

    @Nullable
    public String getType() {
        return getStringValue("template");
    }

    @Nullable
    public String getVersion() {
        return getStringValue("version");
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

    @SuppressWarnings({ "StandardVariableNames", "DynamicRegexReplaceableByCompiledPattern" })
    protected void calculateBounds() {
        String result = getStringValue("bounds");
        if (result != null) {
            String[] parts = result.split(",\\s*");
            double w = Double.parseDouble(parts[0]);
            double s = Double.parseDouble(parts[1]);
            double e = Double.parseDouble(parts[2]);
            double n = Double.parseDouble(parts[3]);
            LatLng sw = new LatLng(s, w);
            LatLng ne = new LatLng(n, e);
            mBounds = new LatLngBounds(sw, ne);
        }
    }

    protected void calculateMaxZoomLevel() {
        String result = getStringValue("maxzoom");
        if (result != null) {
            mMaximumZoom = Float.parseFloat(result);
        }
    }

    protected void calculateMinZoomLevel() {
        String result = getStringValue("minzoom");
        if (result != null) {
            mMinimumZoom = Float.parseFloat(result);
        }
    }

    @Nullable
    protected abstract byte[] getBytes(int x, int y, int zoom);

    @Nullable
    protected abstract String getSQliteVersion();

    @Nullable
    protected abstract String getStringValue(@NonNull String key);

    //endregion
}
