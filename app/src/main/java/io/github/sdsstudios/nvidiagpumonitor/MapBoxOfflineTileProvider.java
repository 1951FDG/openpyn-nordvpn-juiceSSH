package io.github.sdsstudios.nvidiagpumonitor;

import java.io.Closeable;
import java.io.File;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

@MainThread
public class MapBoxOfflineTileProvider implements TileProvider, Closeable {

    // ------------------------------------------------------------------------
    // Instance Variables
    // ------------------------------------------------------------------------

    private float mMinimumZoom = 0.0f;

    private float mMaximumZoom = 22.0f;

    @Nullable
    private LatLngBounds mBounds;

    private final SQLiteDatabase mDatabase;

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    private static final String TABLE_TILES = "tiles";
    private static final String TABLE_METADATA = "metadata";
    private static final String COL_TILES_TILE_DATA = "tile_data";
    private static final String COL_VALUE = "value";

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
            try (Cursor c = this.mDatabase.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' order by name", null)) {
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
        String[] columns = { COL_TILES_TILE_DATA };
        String[] selectionArgs = { String.valueOf(z), String.valueOf(x), String.valueOf((1 << z) - 1 - y) };

        try (Cursor c = this.mDatabase.query(TABLE_TILES, columns, "zoom_level = ? AND tile_column = ? AND tile_row = ?", selectionArgs, null, null, null)) {
            if (c.moveToFirst()) return new Tile(256, 256, c.getBlob(0));
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
     * The minimum zoom level supported by this provider.
     * 
     * @return the minimum zoom level supported or {@link #mMinimumZoom} if
     *         it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMinimumZoom() {
        return this.mMinimumZoom;
    }

    /**
     * The maximum zoom level supported by this provider.
     * 
     * @return the maximum zoom level supported or {@link #mMaximumZoom} if
     *         it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMaximumZoom() {
        return this.mMaximumZoom;
    }

    /**
     * The geographic bounds available from this provider.
     * 
     * @return the geographic bounds available or {@link null} if it could not
     *         be determined.
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
     *         provider.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public boolean isZoomLevelAvailable(float zoom) {
        return (zoom >= this.mMinimumZoom) && (zoom <= this.mMaximumZoom);
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getName() { return getStringValue("name"); }

    @Nullable
    @SuppressWarnings("unused")
    public String getType() { return getStringValue("template"); }

    @Nullable
    @SuppressWarnings("unused")
    public String getVersion() { return getStringValue("version"); }

    @Nullable
    @SuppressWarnings("unused")
    public String getDescription() { return getStringValue("description"); }

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

    private static final String TAG = "MBTileProvider";
}
