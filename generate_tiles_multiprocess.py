#!/usr/bin/env python
#pylint: disable=C0111,C0301

import json
import logging
import os
import sys
import threading
from argparse import ArgumentParser
from math import atan, exp, log, pi, sin
from Queue import Queue

import mapnik
from mbutil import disk_to_mbtiles


def minmax(a, b, c):
    a = max(a, b)
    a = min(a, c)
    return a


class GoogleProjection(object):
    def __init__(self, levels, tile_size):
        self.Bc = []
        self.Cc = []
        self.zc = []
        self.Ac = []
        c = tile_size
        for _ in range(0, levels):
            e = c / 2
            self.Bc.append(c / 360.0)
            self.Cc.append(c / (2 * pi))
            self.zc.append((e, e))
            self.Ac.append(c)
            c *= 2

    def from_ll_to_pixel(self, ll, zoom):
        d = self.zc[zoom]
        e = round(d[0] + ll[0] * self.Bc[zoom])
        f = minmax(sin(pi / 180 * ll[1]), -0.9999, 0.9999)
        g = round(d[1] + 0.5 * log((1 + f) / (1 - f)) * -self.Cc[zoom])
        return e, g

    def from_pixel_to_ll(self, px, zoom):
        e = self.zc[zoom]
        f = (px[0] - e[0]) / self.Bc[zoom]
        g = (px[1] - e[1]) / -self.Cc[zoom]
        h = 180 / pi * (2 * atan(exp(g)) - 0.5 * pi)
        return f, h


class RenderThread(object):
    def __init__(self, q, map_file, max_zoom, tile_size, tile_format):
        self.q = q
        # Create a Map
        self.m = mapnik.Map(tile_size, tile_size)
        self.m.aspect_fix_mode = mapnik.aspect_fix_mode.RESPECT
        # Load style XML
        mapnik.load_map(self.m, map_file, True)
        # Obtain <Map> projection
        self.prj = mapnik.Projection(self.m.srs)
        # Projects between tile pixel co-ordinates and LatLong (EPSG:4326)
        self.tileproj = GoogleProjection(max_zoom + 1, tile_size)
        self.tilesize = tile_size
        self.tileformat = tile_format

    def render_tile(self, tile_uri, x, y, z):
        # Calculate pixel positions of bottom-left & top-right
        p0 = (x * self.tilesize, (y + 1) * self.tilesize)
        p1 = ((x + 1) * self.tilesize, y * self.tilesize)
        # Convert to LatLong (EPSG:4326)
        l0 = self.tileproj.from_pixel_to_ll(p0, z)
        l1 = self.tileproj.from_pixel_to_ll(p1, z)
        # Convert to map projection (e.g. mercator coordinate system EPSG:900913)
        c0 = self.prj.forward(mapnik.Coord(l0[0], l0[1]))
        c1 = self.prj.forward(mapnik.Coord(l1[0], l1[1]))
        # Bounding box for the tile
        bbox = mapnik.Box2d(c0.x, c0.y, c1.x, c1.y)
        self.m.resize(self.tilesize, self.tilesize)
        self.m.zoom_to_box(bbox)
        if self.m.buffer_size < 128:
            self.m.buffer_size = 128
        # Render image with default AGG renderer
        im = mapnik.Image(self.tilesize, self.tilesize)
        mapnik.render(self.m, im)

        if self.tileformat == "jpg":
            im.save(tile_uri, "jpeg100")
        if self.tileformat == "png":
            im.save(tile_uri, "png256:z=9:t=0:m=h:s=filtered")
        if self.tileformat == "webp":
            im.save(tile_uri, "webp:lossless=1:quality=100:method=4:image_hint=3")

    def loop(self):
        while True:
            # Fetch a tile from the queue and render it
            r = self.q.get()
            if r is None:
                self.q.task_done()
                break
            else:
                (name, tile_uri, x, y, z) = r

            exists = ""
            if os.path.isfile(tile_uri):
                exists = "exists"
            else:
                self.render_tile(tile_uri, x, y, z)

            empty = ""
            if logger.isEnabledFor(logging.DEBUG):
                data = os.stat(tile_uri)[6]
                if data in (103, 126, 222):
                    empty = "empty"

            logger.debug(self.m.scale_denominator())
            logger.debug("(%s : %s, %s, %s, %s, %s)", name, z, x, y, exists,
                         empty)
            self.q.task_done()


def render_tiles(bbox, map_file, min_zoom, max_zoom, threads, name, tile_dir, tile_size, tile_format, mbtiles_path):
    logger.info("render_tiles(%s, %s, %s, %s, %s, %s, %s, %s, %s)", bbox,
                map_file, tile_dir, min_zoom, max_zoom, threads, name, tile_size,
                mbtiles_path)
    # Launch rendering threads
    queue = Queue(32)
    renderers = {}
    for i in range(threads):
        renderer = RenderThread(queue, map_file, max_zoom, tile_size, tile_format)
        render_thread = threading.Thread(target=renderer.loop)
        render_thread.start()
        logger.info("Started render thread %s", render_thread.getName())
        renderers[i] = render_thread

    if not os.path.isdir(tile_dir):
        os.mkdir(tile_dir)

    gprj = GoogleProjection(max_zoom + 1, tile_size)
    ll0 = (bbox[0], bbox[3])
    ll1 = (bbox[2], bbox[1])
    for z in range(min_zoom, max_zoom + 1):
        px0 = gprj.from_ll_to_pixel(ll0, z)
        px1 = gprj.from_ll_to_pixel(ll1, z)
        # check if we have directories in place
        str_z = "%s" % z
        tile_sizef = float(tile_size)
        if not os.path.isdir(os.path.join(tile_dir, str_z)):
            os.mkdir(os.path.join(tile_dir, str_z))

        for x in range(int(px0[0] / tile_sizef), int(px1[0] / tile_sizef) + 1):
            # Validate x co-ordinate
            if (x < 0) or (x >= 2**z):
                continue
            # Check if we have directories in place
            str_x = "%s" % x
            if not os.path.isdir(os.path.join(tile_dir, str_z, str_x)):
                os.mkdir(os.path.join(tile_dir, str_z, str_x))

            for y in range(
                    int(px1[1] / tile_sizef),
                    int(px0[1] / tile_sizef) + 1):
                # Validate y co-ordinate
                if (y < 0) or (y >= 2**z):
                    continue
                # Submit tile to be rendered into the queue
                str_y = "%s" % y
                tile_uri = os.path.join(tile_dir, str_z, str_x, str_y + "." + tile_format)
                t = (name, tile_uri, x, y, z)
                try:
                    queue.put(t)
                except KeyboardInterrupt:
                    raise SystemExit("Ctrl-C detected, exiting...")

    # Signal render threads to exit by sending empty request to queue
    for _ in range(threads):
        queue.put(None)
    # Wait for pending rendering jobs to complete
    queue.join()
    for i in range(threads):
        renderers[i].join()
    # Import `tiles` directory into a `MBTiles` file
    if mbtiles_path:
        if os.path.isfile(mbtiles_path):
            # `MBTiles` file must not already exist
            sys.stderr.write("Importing tiles into already-existing MBTiles is not yet supported\n")
            sys.exit(1)
        else:
            data = {}
            # data["bounds"] = str(bbox[0]) + ", " + str(bbox[1]) + ", " + str(bbox[2]) + ", " + str(bbox[3])
            data["maxzoom"] = str(max_zoom)
            data["minzoom"] = str(min_zoom)
            # data["version"] = "1.0"
            with open(os.path.join(tile_dir, "metadata.json"), "w") as outfile:
                json.dump(data, outfile, sort_keys=True, indent=4)

            disk_to_mbtiles(tile_dir, mbtiles_path, **args.__dict__)


if __name__ == "__main__":
    parser = ArgumentParser(
        usage="%(prog)s [options] input output 1..18 1..18")
    # Positional arguments
    parser.add_argument("input", help="mapnik XML file")
    parser.add_argument("output", help="a MBTiles file", default=None)
    parser.add_argument(
        "min",
        help="minimum zoom level to render",
        type=int,
        metavar="1..18",
        choices=range(1, 18),
        default="1")
    parser.add_argument(
        "max",
        help="maximum zoom level to render",
        type=int,
        metavar="1..18",
        choices=range(1, 18),
        default="18")
    # Optional arguments
    parser.add_argument(
        "--bbox",
        help="bounding box that will be rendered",
        nargs=4,
        type=float,
        metavar="f",
        default=[
            -20037508.342789244, -20037508.342789244, 20037508.342789244,
            20037508.342789244
        ])
    parser.add_argument(
        "--cores", help="# of rendering threads to spawn", type=int, default=4)
    parser.add_argument(
        "--name", help="name for each renderer", default="unknown")
    parser.add_argument(
        "--size",
        help="resolution of the tile image",
        type=int,
        metavar="SIZE",
        choices=[1024, 512, 256],
        default=512)
    # MBUtil arguments
    parser.add_argument(
        "--format",
        help="format of the image tiles",
        dest="format",
        type=str,
        metavar="FORMAT",
        choices=["jpg", "png", "webp"],
        default="png")
    parser.add_argument(
        "--scheme",
        help="tiling scheme of the tiles",
        dest="scheme",
        type=str,
        metavar="SCHEME",
        choices=["xyz"],
        default="xyz")
    parser.add_argument(
        "--no_compression",
        help="disable MBTiles compression",
        dest="compression",
        action="store_false",
        default=True)
    parser.add_argument(
        "--verbose", dest="silent", action="store_false", default=True)

    args = parser.parse_args()
    if not args.silent:
        logging.basicConfig(level=logging.DEBUG)

    logger = logging.getLogger(__name__)
    base_dir = os.path.dirname(args.input)
    tiles_dir = os.path.join(base_dir, "tiles")
    render_tiles(args.bbox, args.input, args.min, args.max, args.cores,
                 args.name, tiles_dir, args.size, args.format, args.output)
