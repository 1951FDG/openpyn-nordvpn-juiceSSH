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

import org.jetbrains.annotations.NonNls;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import io.requery.android.database.sqlite.SQLiteCursor;
import io.requery.android.database.sqlite.SQLiteDatabase;
import io.requery.android.database.sqlite.SQLiteDatabase.CursorFactory;
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration;
import io.requery.android.database.sqlite.SQLiteDirectCursorDriver;
import io.requery.android.database.sqlite.SQLiteQuery;

/*import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseInputStream;
import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;
import com.almworks.sqlite4java.SQLiteStatement;
import org.sqlite.database.DatabaseUtils;
import org.sqlite.database.sqlite.SQLiteCursor;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteDatabase.CursorFactory;
import org.sqlite.database.sqlite.SQLiteDatabaseConfiguration;
import org.sqlite.database.sqlite.SQLiteDirectCursorDriver;
import org.sqlite.database.sqlite.SQLiteQuery;
import org.sqlite.database.sqlite.SQLiteStatement;*/

@MainThread
public class MapBoxOfflineTileProvider implements TileProvider, Closeable {
    //region Statics

    // Used to measure distances relative to the total world size.
    private static final double WORLD_WIDTH = 1.0;

    // Tile dimension, in pixels.
    private static final int TILE_DIM = 512;

    // sqlite3x, sqliteX, sqlite4java-android
    private static final String SQLITE = "sqlite3x";

    private static final String TAG = "MBTileProvider";

    // TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB);
    private static final String mSql = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";

    //endregion Statics

    //region Members

    @Nullable
    private LatLngBounds bounds;

    private final SQLiteDatabase mDatabase;

    private final SQLiteQuery mQuery;

    private final SQLiteCursor mCursor;

    // private final SQLiteQueue mQueue;

    private float maximumZoom;

    private float minimumZoom;

    //endregion Members

    //region Constructors

    public MapBoxOfflineTileProvider(@NonNull File file) {
        this(file.getAbsolutePath());
    }

    public MapBoxOfflineTileProvider(@NonNull String pathToFile) {
        // this(SQLiteDatabase.openDatabase(pathToFile, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS));
        this(SQLiteDatabase.openDatabase(pathToFile, null, SQLiteDatabase.OPEN_READONLY));
    }

    public MapBoxOfflineTileProvider(@Nullable CursorFactory factory, @NonNull String pathToFile) {
        this(create(factory, pathToFile));
    }

    private MapBoxOfflineTileProvider(@NonNull SQLiteDatabase database) {
        mDatabase = database;
        mQuery = new SQLiteQuery(mDatabase, mSql, null, null);
        mCursor = new SQLiteCursor(new SQLiteDirectCursorDriver(null, null, null, null), null, mQuery);

        /*mQueue = new SQLiteQueue();
        mQueue.start();*/

        calculateMinZoomLevel();
        calculateMaxZoomLevel();
        calculateBounds();
    }

    //endregion Constructors

    @NonNull
    @Override
    public String toString() {
        return "MapBoxOfflineTileProvider{" + "mDatabase='" + "'" + ", mSql='" + mSql + "'" + ", mBounds=" + bounds + ", mMinimumZoom="
                + minimumZoom + ", mMaximumZoom=" + maximumZoom + "}";
    }

    //region Accessors

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
        return bounds;
    }

    @Nullable
    public String getDescription() {
        return getStringValue("description");
    }

    /**
     * The maximum zoom level supported by this provider.
     *
     * @return the maximum zoom level supported or {@link #maximumZoom} if
     * it could not be determined.
     */
    public float getMaximumZoom() {
        return maximumZoom;
    }

    /**
     * The minimum zoom level supported by this provider.
     *
     * @return the minimum zoom level supported or {@link #minimumZoom} if
     * it could not be determined.
     */
    public float getMinimumZoom() {
        return minimumZoom;
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
        return (zoom >= minimumZoom) && (zoom <= maximumZoom);
    }

    //endregion Accessors

    //region TileProvider

    @SuppressWarnings({ "ParameterNameDiffersFromOverriddenParameter", "SynchronizedMethod" })
    @Override
    @NonNull
    public synchronized Tile getTile(int x, int y, int zoom) {
        // Log.e(TAG, String.format("%d %d %d", zoom, x, ((1 << zoom) - 1 - y)));
        String[] bindArgs = { Integer.toString(zoom), Integer.toString(x), Integer.toString((1 << zoom) - 1 - y) };
        mQuery.bindString(3, bindArgs[2]); // row
        mQuery.bindString(2, bindArgs[1]); // column
        mQuery.bindString(1, bindArgs[0]); // zoom
        mCursor.requery();
        return mCursor.moveToPosition(0) ? new Tile(TILE_DIM, TILE_DIM, mCursor.getBlob(0)) : NO_TILE;

        //region sqlite4java-android
        /*SQLiteJob<Tile> job = mQueue.execute(new TileSQLiteJob(x, y, zoom));
        return job.complete();*/
        //endregion sqlite4java-android

        //region sqliteX
        /*Tile tile = NO_TILE;
        SQLiteStatement st = mDatabase.compileStatement(mSql);
        st.bindString(3, bindArgs[2]);
        st.bindString(2, bindArgs[1]);
        st.bindString(1, bindArgs[0]);
        ParcelFileDescriptor pfd = DatabaseUtils.blobFileDescriptorForQuery(mDatabase, mSql, bindArgs);
        try (AutoCloseInputStream fis = new AutoCloseInputStream(pfd)) {
            tile = new Tile(TILE_DIM, TILE_DIM, read(fis, BUFFER_SIZE << 4));
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }*/
        //endregion sqliteX
    }

    //endregion TileProvider

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
        if ((mCursor != null) && (!mCursor.isClosed())) {
            mCursor.close();
        }
        if ((mDatabase != null) && (mDatabase.isOpen())) {
            mDatabase.close();
        }
    }

    //endregion Closeable

    //region Instance Methods

    @SuppressWarnings({ "StandardVariableNames", "DynamicRegexReplaceableByCompiledPattern" })
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

    @Nullable
    private String getStringValue(@NonNull String key) {
        @NonNls String sql = "SELECT value FROM metadata WHERE name = ?";
        String[] bindArgs = { key };
        try (Cursor cursor = mDatabase.rawQueryWithFactory(null, sql, bindArgs, null, null)) {
            return cursor.moveToPosition(0) ? cursor.getString(0) : null;
        }
    }

    @Nullable
    private String getSQliteVersion() {
        @NonNls String sql = "SELECT sqlite_version() AS sqlite_version";
        // return DatabaseUtils.stringForQuery(mDatabase, sql, null); // sqliteX
        try (Cursor cursor = mDatabase.rawQueryWithFactory(null, sql, null, null, null)) {
            return cursor.moveToPosition(0) ? cursor.getString(0) : null;
        }
    }

    //endregion Instance Methods

    //region Statics

    /**
     * Convert tile coordinates and zoom into Bounds format.
     *
     * @param x The requested x coordinate.
     * @param y The requested y coordinate.
     * @param z The requested zoom level.
     * @return the geographic bounds of the tile
     */
    @SuppressWarnings("MagicNumber")
    @NonNull
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

    /**
     * Create a memory backed SQLite database.
     *
     * @param path    to database file to open and/or create
     * @param factory an optional factory class that is called to instantiate a
     *                cursor when query is called, or null for default
     * @return the newly opened database
     * @throws android.database.SQLException if the database cannot be opened
     */
    @SuppressWarnings({ "DuplicateStringLiteralInspection", "HardCodedStringLiteral", "MethodCallInLoopCondition" })
    private static SQLiteDatabase create(@Nullable CursorFactory factory, @NonNull String path) {
        SQLiteDatabase database = SQLiteDatabase
                .openDatabase(SQLiteDatabaseConfiguration.MEMORY_DB_PATH, factory, SQLiteDatabase.CREATE_IF_NECESSARY);
        database.execSQL("ATTACH DATABASE '" + path + "' AS db");
        database.execSQL("CREATE TABLE main." + "map" + " AS SELECT * FROM db." + "map");
        database.execSQL("CREATE TABLE main." + "metadata" + " AS SELECT * FROM db." + "metadata");
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

    /**
     * buffer size used for reading and writing
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    /**
     * Reads all the bytes from an input stream. Uses {@code initialSize} as a hint
     * about how many bytes the stream will have.
     *
     * @param source      the input stream to read from
     * @param initialSize the initial size of the byte array to allocate
     * @return a byte array containing the bytes read from the file
     * @throws IOException      if an I/O error occurs reading from the stream
     * @throws OutOfMemoryError if an array of the required size cannot be allocated
     */
    @SuppressWarnings({ "ForLoopWithMissingComponent", "MethodCallInLoopCondition", "NestedAssignment", "ValueOfIncrementOrDecrementUsed",
            "NumericCastThatLosesPrecision" })
    private static byte[] read(InputStream source, int initialSize) throws IOException {
        int capacity = initialSize;
        byte[] buf = new byte[capacity];
        int nread = 0;
        int n;
        for (; ; ) {
            // read to EOF which may read more or less than initialSize (eg: file
            // is truncated while we are reading)
            while ((n = source.read(buf, nread, capacity - nread)) > 0) {
                nread += n;
            }
            // if last call to source.read() returned -1, we are done
            // otherwise, try to read one more byte; if that failed we're done too
            if ((n < 0) || (((n = source.read())) < 0)) {
                break;
            }
            // one more byte was read; need to allocate a larger buffer
            if (capacity <= (MAX_BUFFER_SIZE - capacity)) {
                capacity = Math.max(capacity << 1, BUFFER_SIZE);
            } else {
                if (capacity == MAX_BUFFER_SIZE) {
                    throw new OutOfMemoryError("Required array size too large");
                }
                capacity = MAX_BUFFER_SIZE;
            }
            buf = Arrays.copyOf(buf, capacity);
            buf[nread++] = (byte) n;
        }
        return (capacity == nread) ? buf : Arrays.copyOf(buf, nread);
    }

    /*private static class TileSQLiteJob extends SQLiteJob<Tile> {

        private final int mX;

        private final int mY;

        private final int mZoom;

        public TileSQLiteJob(int x, int y, int zoom) {
            mX = x;
            mY = y;
            mZoom = zoom;
        }

        protected Tile job(SQLiteConnection connection) throws SQLiteException {
            SQLiteStatement st = connection.prepare(mSql);
            st.bind(1, mZoom);
            st.bind(2, mX);
            st.bind(3, ((1 << mZoom) - 1 - mY));
            try {
                return st.step() ? new Tile(TILE_DIM, TILE_DIM, st.columnBlob(0)) : NO_TILE;
            } finally {
                st.dispose();
            }
        }
    }*/

    //endregion Statics

    static {
        // sqlite3ndk should be loaded first
        System.loadLibrary("sqlite3ndk");
        System.loadLibrary(SQLITE);
    }
}
