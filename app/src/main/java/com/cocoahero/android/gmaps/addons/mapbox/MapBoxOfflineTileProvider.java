package com.cocoahero.android.gmaps.addons.mapbox;

import android.database.Cursor;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.geometry.Point;
import com.google.maps.android.projection.SphericalMercatorProjection;

import org.sqlite.database.sqlite.SQLiteCursor;
import org.sqlite.database.sqlite.SQLiteCursorDriver;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteDatabase.CursorFactory;
import org.sqlite.database.sqlite.SQLiteQuery;

import java.io.Closeable;
import java.io.File;

@MainThread
public class MapBoxOfflineTileProvider implements TileProvider, SQLiteCursorDriver, Closeable {

    //region Statics

    private static final String TABLE_METADATA = "metadata";

    private static final String COL_VALUE = "value";

    // Used to measure distances relative to the total world size.
    private static final double WORLD_WIDTH = 1.0;

    // Tile dimension, in pixels.
    private static final int TILE_DIM = 512;

    private static final String TAG = "MBTileProvider";

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    private static final String mEditTable = "tiles";

    private static final String mSql = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

    //endregion Statics

    //region Members

    @Nullable
    private LatLngBounds bounds;

    private final SQLiteDatabase mDatabase;

    private SQLiteQuery mQuery;

    private float maximumZoom;

    private float minimumZoom;

    //endregion Members

    //region Statics

    /**
     * Convert tile coordinates and zoom into Bounds format.
     *
     * @param x The requested x coordinate.
     * @param y The requested y coordinate.
     * @param z The requested zoom level.
     * @return the geographic bounds of the tile
     */
    @NonNull
    @SuppressWarnings("MagicNumber")
    public static LatLngBounds calculateTileBounds(int x, int y, int z) {
        // Width of the world = WORLD_WIDTH = 1
        // Calculate width of one tile, given there are 2 ^ zoom tiles in that zoom level
        // In terms of world width units
        double tileWidth = WORLD_WIDTH / Math.pow(2.0, z);
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

    //endregion Statics

    //region Constructors

    @SuppressWarnings("unused")
    public MapBoxOfflineTileProvider(@NonNull File file) {
        this(file.getAbsolutePath());
    }

    @SuppressWarnings({ "WeakerAccess", "unused" })
    public MapBoxOfflineTileProvider(@NonNull String pathToFile) {
        this(SQLiteDatabase.openDatabase(pathToFile, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS));
    }

    @SuppressWarnings({ "LambdaLast", "unused" })
    public MapBoxOfflineTileProvider(@Nullable CursorFactory factory, @NonNull String pathToFile) {
        this(create(factory, pathToFile));
    }

    private MapBoxOfflineTileProvider(@NonNull SQLiteDatabase database) {
        mDatabase = database;
        calculateMinZoomLevel();
        calculateMaxZoomLevel();
        calculateBounds();
    }

    //endregion Constructors

    @NonNull
    @Override
    public String toString() {
        return "MapBoxOfflineTileProvider{" + "mDatabase='" + mDatabase.getPath() + '\'' + ", mEditTable='" + mEditTable + '\'' + ", mSql='"
                + mSql + '\'' + ", mBounds=" + bounds + ", mMinimumZoom=" + minimumZoom + ", mMaximumZoom=" + maximumZoom + '}';
    }

    //region Accessors

    @Nullable
    @SuppressWarnings("unused")
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
    @SuppressWarnings("unused")
    public LatLngBounds getBounds() {
        return bounds;
    }

    @Nullable
    @SuppressWarnings("unused")
    public String getDescription() {
        return getStringValue("description");
    }

    /**
     * The maximum zoom level supported by this provider.
     *
     * @return the maximum zoom level supported or {@link #maximumZoom} if
     * it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMaximumZoom() {
        return maximumZoom;
    }

    /**
     * The minimum zoom level supported by this provider.
     *
     * @return the minimum zoom level supported or {@link #minimumZoom} if
     * it could not be determined.
     */
    @SuppressWarnings("unused")
    public float getMinimumZoom() {
        return minimumZoom;
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

    /**
     * Determines if the requested zoom level is supported by this provider.
     *
     * @param zoom The requested zoom level.
     * @return {@code true} if the requested zoom level is supported by this
     * provider.
     */
    @SuppressWarnings({ "WeakerAccess", "unused" })
    public boolean isZoomLevelAvailable(float zoom) {
        return (zoom >= minimumZoom) && (zoom <= maximumZoom);
    }

    //endregion Accessors

    //region TileProvider

    @NonNull
    @Override
    @SuppressWarnings({ "unused", "ParameterNameDiffersFromOverriddenParameter" })
    public Tile getTile(int x, int y, int z) {
        //Log.e(TAG, String.format("%d %d", x, y));
        //String[] columns = { "tile_data" };
        //String selection = "zoom_level = ? AND tile_column = ? AND tile_row = ?";
        //String sql = SQLiteQueryBuilder.buildQueryString(false, TABLE_TILES, columns, selection, null, null, null, null);
        //Log.e(TAG, sql);
        String[] selectionArgs = { Integer.toString(z), Integer.toString(x), Integer.toString((1 << z) - 1 - y) };
        try (Cursor cursor = query(null, selectionArgs)) {
            return cursor.moveToFirst() ? new Tile(TILE_DIM, TILE_DIM, cursor.getBlob(0)) : NO_TILE;
        }
    }

    //endregion TileProvider

    //region SQLiteCursorDriver

    /**
     * Executes the query returning a Cursor over the result set.
     *
     * @param factory The CursorFactory to use when creating the Cursors, or
     *                null if standard SQLiteCursors should be returned.
     * @return a Cursor over the result set
     */
    @NonNull
    @SuppressWarnings("LambdaLast")
    public Cursor query(@Nullable CursorFactory factory, @NonNull String[] bindArgs) {
        SQLiteQuery query = new SQLiteQuery(mDatabase, mSql, null);
        query.bindAllArgsAsStrings(bindArgs);
        Cursor cursor = new SQLiteCursor(this, mEditTable, query);
        mQuery = query;
        return cursor;
    }

    /**
     * Called by a SQLiteCursor when it is released.
     */
    public void cursorDeactivated() {
        // Do nothing
    }

    /**
     * Called by a SQLiteCursor when it is requeried.
     */
    public void cursorRequeried(@NonNull Cursor cursor) {
        // Do nothing
    }

    /**
     * Called by a SQLiteCursor when it it closed to destroy this object as well.
     */
    public void cursorClosed() {
        // Do nothing
    }

    /**
     * Set new bind arguments. These will take effect in cursorRequeried().
     *
     * @param bindArgs the new arguments
     */
    public void setBindArguments(@NonNull String[] bindArgs) {
        mQuery.bindAllArgsAsStrings(bindArgs);
    }

    //endregion SQLiteCursorDriver

    //region Closeable

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
        if ((mDatabase != null) && (mDatabase.isOpen())) {
            mDatabase.close();
        }
    }

    //endregion Closeable

    //region Instance Methods

    @SuppressWarnings("StandardVariableNames")
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
            bounds = new LatLngBounds(sw, ne);
        }
    }

    private void calculateMaxZoomLevel() {
        String result = getStringValue("maxzoom");
        if (result != null) {
            maximumZoom = Float.parseFloat(result);
        }
    }

    private void calculateMinZoomLevel() {
        String result = getStringValue("minzoom");
        if (result != null) {
            minimumZoom = Float.parseFloat(result);
        }
    }

    private String getStringValue(@NonNull String key) {
        String[] columns = { COL_VALUE };
        String[] selectionArgs = { key };
        try (Cursor cursor = mDatabase.query(TABLE_METADATA, columns, "name = ?", selectionArgs, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    /**
     * Create a memory backed SQLite database.
     *
     * @param factory an optional factory class that is called to instantiate a
     *                cursor when query is called
     * @return a SQLiteDatabase object, or null if the database can't be created
     */
    @SuppressWarnings("DuplicateStringLiteralInspection")
    private static SQLiteDatabase create(@Nullable CursorFactory factory, @NonNull String pathToFile) {
        SQLiteDatabase database = SQLiteDatabase.create(factory);
        database.execSQL("ATTACH DATABASE '" + pathToFile + "' AS db");
        database.execSQL("CREATE TABLE main." + "map" + " AS SELECT * FROM db." + "map");
        database.execSQL("CREATE TABLE main." + TABLE_METADATA + " AS SELECT * FROM db." + TABLE_METADATA);
        database.execSQL("CREATE TABLE main." + "images" + " AS SELECT * FROM db." + "images");
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS map_index ON map (zoom_level, tile_column, tile_row);");
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS images_id ON images (tile_id);");
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS name ON metadata (name);");
        database.execSQL("CREATE VIEW IF NOT EXISTS tiles AS " + "SELECT map.zoom_level AS zoom_level," + "map.tile_column AS tile_column,"
                + "map.tile_row AS tile_row," + "images.tile_data AS tile_data " + "FROM map JOIN images ON images.tile_id = map.tile_id");
        database.execSQL("DETACH db");
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name!='android_metadata' order by name";
            try (Cursor cursor = database.rawQuery(sql, null)) {
                if (cursor.moveToFirst()) {
                    while (!cursor.isAfterLast()) {
                        Log.d(TAG, cursor.getString(0));
                        cursor.moveToNext();
                    }
                }
            }
        }
        return database;
    }

    //endregion Instance Methods

    static {
        System.loadLibrary("sqliteX");
        System.loadLibrary("sqlite3ndk");
    }
}
