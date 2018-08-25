package com.cocoahero.android.gmaps.addons.mapbox;

import android.database.Cursor;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.Closeable;
import java.io.File;

@MainThread
public class MapBoxOfflineTileProvider implements TileProvider, Closeable {
    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------
    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    private static final String TABLE_TILES = "tiles";
    private static final String TABLE_METADATA = "metadata";
    private static final String COL_TILES_TILE_DATA = "tile_data";
    private static final String COL_VALUE = "value";
    // Used to measure distances relative to the total world size.
    private static final double WORLD_WIDTH = 1;
    // Tile dimension, in pixels.
    private static final int TILE_DIM = 512;
    private static final String TAG = "MBTileProvider";

    static {
        System.loadLibrary("sqliteX");
        System.loadLibrary("sqlite3ndk");
    }

    private final SQLiteDatabase mDatabase;
    @Nullable
    private LatLngBounds mBounds;
    private float mMinimumZoom = 0.0f;
    private float mMaximumZoom = 22.0f;

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    @SuppressWarnings("unused")
    public MapBoxOfflineTileProvider(@NonNull File file) {
        this(file.getAbsolutePath());
    }

    @SuppressWarnings("unused")
    public MapBoxOfflineTileProvider(@NonNull String pathToFile) {
        int flags = SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS;
        this.mDatabase = SQLiteDatabase.openDatabase(pathToFile, null, flags);
        this.calculateMinZoomLevel();
        this.calculateMaxZoomLevel();
        this.calculateBounds();
    }

    @SuppressWarnings("unused")
    public MapBoxOfflineTileProvider(@NonNull String pathToFile, boolean debug) {
        this.mDatabase = SQLiteDatabase.create(null);
        this.mDatabase.execSQL("ATTACH DATABASE '" + pathToFile + "' AS db");
        this.mDatabase.execSQL("CREATE TABLE main." + "map" + " AS SELECT * FROM db." + "map");
        this.mDatabase.execSQL("CREATE TABLE main." + "metadata" + " AS SELECT * FROM db." + "metadata");
        this.mDatabase.execSQL("CREATE TABLE main." + "images" + " AS SELECT * FROM db." + "images");
        this.mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS map_index ON map (zoom_level, tile_column, tile_row);");
        this.mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS images_id ON images (tile_id);");
        this.mDatabase.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS name ON metadata (name);");
        this.mDatabase.execSQL("CREATE VIEW IF NOT EXISTS tiles AS " +
                "SELECT map.zoom_level AS zoom_level," +
                "map.tile_column AS tile_column," +
                "map.tile_row AS tile_row," +
                "images.tile_data AS tile_data " +
                "FROM map JOIN images ON images.tile_id = map.tile_id");
        this.mDatabase.execSQL("DETACH db");

        if (debug) {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' order by name";
            try (Cursor c = this.mDatabase.rawQuery(sql, null)) {
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        Log.d(TAG, c.getString(0));
                        c.moveToNext();
                    }
                }
            }

            Log.d(TAG, "[mDatabase=" + mDatabase.getPath() + "]");
        }

        this.calculateMinZoomLevel();
        this.calculateMaxZoomLevel();
        this.calculateBounds();
    }

    // ------------------------------------------------------------------------
    // TileProvider Interface
    // ------------------------------------------------------------------------

    @NonNull
    @Override
    @SuppressWarnings("unused")
    public Tile getTile(int x, int y, int z) {
        //Log.e(TAG, String.format("%d %d", x, y));
        String[] columns = { COL_TILES_TILE_DATA };
        String[] selectionArgs = { String.valueOf(z), String.valueOf(x), String.valueOf((1 << z) - 1 - y) };
        String selection = "zoom_level = ? AND tile_column = ? AND tile_row = ?";

        try (Cursor c = this.mDatabase.query(TABLE_TILES, columns, selection, selectionArgs, null, null, null)) {
            if (c.moveToFirst()) return new Tile(TILE_DIM, TILE_DIM, c.getBlob(0));
            else return NO_TILE;
        }
    }

    // ------------------------------------------------------------------------
    // Closeable Interface
    // ------------------------------------------------------------------------

    /**
     * Closes the provider, cleaning up any background resources.
     *
     * <p>
     * You must call {@code close()} when you are finished using an instance of
     * this provider. Failing to do so may leak resources, such as the backing
     * SQLiteDatabase.
     * </p>
     */
    @Override
    public void close() {
        if (this.isDatabaseAvailable()) {
            this.mDatabase.close();
        }
    }

    // ------------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------------

    /**
     * Convert tile coordinates and zoom into Bounds format.
     *
     * @param x The requested x coordinate.
     * @param y The requested y coordinate.
     * @param z The requested zoom level.
     * @return the geographic bounds of the tile
     */
    @NonNull
    public LatLngBounds calculateTileBounds(int x, int y, int z) {
        // Width of the world = WORLD_WIDTH = 1

        // Calculate width of one tile, given there are 2 ^ zoom tiles in that zoom level
        // In terms of world width units
        double tileWidth = WORLD_WIDTH / Math.pow(2, z);

        // Make bounds: minX, maxX, minY, maxY
        double minX = x * tileWidth;
        double maxX = (x + 1) * tileWidth;
        double minY = y * tileWidth;
        double maxY = (y + 1) * tileWidth;

        SphericalMercatorProjection sProjection = new SphericalMercatorProjection(WORLD_WIDTH);

        LatLng sw = sProjection.toLatLng(new Point(minX, maxY));
        LatLng ne = sProjection.toLatLng(new Point(maxX, minY));

        return new LatLngBounds(sw, ne);
    }

    /**
     * The minimum zoom level supported by this provider.
     *
     * @return the minimum zoom level supported or {@link #mMinimumZoom} if
     * it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMinimumZoom() {
        return this.mMinimumZoom;
    }

    /**
     * The maximum zoom level supported by this provider.
     *
     * @return the maximum zoom level supported or {@link #mMaximumZoom} if
     * it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMaximumZoom() {
        return this.mMaximumZoom;
    }

    /**
     * The geographic bounds available from this provider.
     *
     * @return the geographic bounds available or {@link null} if it could not
     * be determined.
     */
    @Nullable
    @SuppressWarnings("unused")
    public LatLngBounds getBounds() {
        return this.mBounds;
    }

    /**
     * Determines if the requested zoom level is supported by this provider.
     *
     * @param zoom The requested zoom level.
     * @return {@code true} if the requested zoom level is supported by this
     * provider.
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    public boolean isZoomLevelAvailable(float zoom) {
        return (zoom >= this.mMinimumZoom) && (zoom <= this.mMaximumZoom);
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getName() {
        return getStringValue("name");
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getType() {
        return getStringValue("template");
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getVersion() {
        return getStringValue("version");
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getDescription() {
        return getStringValue("description");
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getAttribution() {
        return getStringValue("attribution");
    }

    // ------------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------------

    private String getStringValue(String key) {
        String[] columns = { COL_VALUE };
        String[] selectionArgs = { key };

        try (Cursor c = this.mDatabase.query(TABLE_METADATA, columns, "name = ?", selectionArgs, null, null, null)) {
            if (c.moveToFirst()) return c.getString(0);
            else return null;
        }
    }

    private void calculateMinZoomLevel() {
        String result = getStringValue("minzoom");
        if (result != null) {
            this.mMinimumZoom = Float.parseFloat(result);
        }
    }

    private void calculateMaxZoomLevel() {
        String result = getStringValue("maxzoom");
        if (result != null) {
            this.mMaximumZoom = Float.parseFloat(result);
        }
    }

    private void calculateBounds() {
        String result = getStringValue("bounds");
        if (result != null) {
            String[] parts = result.split(",\\s*");

            double w = Double.parseDouble(parts[0]);
            double s = Double.parseDouble(parts[1]);
            double e = Double.parseDouble(parts[2]);
            double n = Double.parseDouble(parts[3]);

            LatLng sw = new LatLng(s, w);
            LatLng ne = new LatLng(n, e);

            this.mBounds = new LatLngBounds(sw, ne);
        }
    }

    private boolean isDatabaseAvailable() {
        return (this.mDatabase != null) && (this.mDatabase.isOpen());
    }
}
