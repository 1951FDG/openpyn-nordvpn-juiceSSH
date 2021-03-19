package io.github.getsixtyfour.openpyn.maps;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.util.Objects;

import org.jetbrains.annotations.NonNls;

@MainThread
abstract class AbstractTileProvider implements TileProvider {

    // Tile dimension, in pixels
    private static final int TILE_DIM = 512;

    /**
     * Convert tile coordinates and zoom into LatLngBounds format.
     *
     * @param x    The requested x coordinate.
     * @param y    The requested y coordinate.
     * @param zoom The requested zoom level.
     * @return the geographic bounds of the tile
     */
    @SuppressWarnings({ "MagicNumber", "ImplicitNumericConversion" })
    @NonNull
    public static LatLngBounds calculateTileBounds(int x, int y, int zoom) {
        // Calculate width of one tile, given there are 2 ^ zoom tiles in that zoom level
        double tileWidth = 1.0 / Math.pow(2.0, zoom);
        LatLng southwest = toLatLng(x * tileWidth, (y + 1) * tileWidth);
        LatLng northeast = toLatLng((x + 1) * tileWidth, y * tileWidth);
        return new LatLngBounds(southwest, northeast);
    }

    @NonNull
    @SuppressWarnings("MagicNumber")
    private static LatLng toLatLng(double x, double y) {
        double latitude = 90.0 - Math.toDegrees(Math.atan(Math.exp(-(0.5 - y) * 2.0 * Math.PI)) * 2.0);
        double longitude = (x - 0.5) * 360.0;
        return new LatLng(latitude, longitude);
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
        // OpenLayers Bounds format (left, bottom, right, top)
        String result = getBounds();
        String[] parts = result.split(",", -1);
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
