package io.github.getsixtyfour.openpyn.maps;

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

    @NonNull
    public String getName() {
        return Objects.requireNonNull(getStringValue("name"));
    }

    @NonNull
    public String getFormat() {
        return Objects.requireNonNull(getStringValue("format"));
    }

    @NonNull
    public String getBounds() {
        return Objects.requireNonNull(getStringValue("bounds"));
    }

    public float getMinZoom() {
        return Float.parseFloat(Objects.requireNonNull(getStringValue("minzoom")));
    }

    public float getMaxZoom() {
        return Float.parseFloat(Objects.requireNonNull(getStringValue("maxzoom")));
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

    @SuppressWarnings({ "StandardVariableNames", "unused" })
    @NonNull
    public LatLngBounds getLatLngBounds() {
        String result = Objects.requireNonNull(getStringValue("bounds"));
        String[] parts = result.split(",", -1);
        // OpenLayers Bounds format (left, bottom, right, top)
        double w = Double.parseDouble(parts[0]);
        double s = Double.parseDouble(parts[1]);
        double e = Double.parseDouble(parts[2]);
        double n = Double.parseDouble(parts[3]);
        LatLng sw = new LatLng(s, w);
        LatLng ne = new LatLng(n, e);
        return new LatLngBounds(sw, ne);
    }

    @Nullable
    protected abstract byte[] getBytes(int x, int y, int zoom);

    @Nullable
    protected abstract String getStringValue(@NonNls @NonNull String key);
}
