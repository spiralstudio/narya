//
// $Id: IsoSceneViewModel.java,v 1.13 2001/10/11 16:21:09 shaper Exp $

package com.threerings.miso.scene;

import java.awt.Dimension;
import java.awt.Point;

import com.samskivert.util.Config;
import com.threerings.miso.Log;
import com.threerings.miso.scene.util.IsoUtil;
import com.threerings.miso.util.MisoUtil;

/**
 * This class provides a holding place for the myriad parameters and
 * bits of data that describe the details of an isometric view of a
 * scene.
 *
 * <p> The member data are public to facilitate speedy referencing by
 * the {@link IsoSceneView} class.  The model should only be
 * configured through the constructor's passed-in {@link
 * com.samskivert.util.Config} object and the accessor methods.
 */
public class IsoSceneViewModel
{
    /** Tile dimensions and half-dimensions in the view. */
    public int tilewid, tilehei, tilehwid, tilehhei;

    /** Fine coordinate dimensions. */
    public int finehwid, finehhei;

    /** Number of fine coordinates on each axis within a tile. */
    public int finegran;

    /** Scene dimensions in tile count. */
    public int scenewid, scenehei;

    /** The bounds dimensions for the view. */
    public Dimension bounds;

    /** The position in pixels at which tile (0, 0) is drawn. */ 
    public Point origin;

    /** The total number of tile rows to render the full view. */
    public int tilerows;

    /** The length of a tile edge in pixels. */
    public float tilelen;

    /** The y-intercept of the x-axis line. */
    public int bX;

    /** The slope of the x- and y-axis lines. */
    public float slopeX, slopeY;

    /** The x-axis line. */
    public Point lineX[];

    /** The length between fine coordinates in pixels. */
    public float finelen;

    /** The y-intercept of the x-axis line within a tile. */
    public float fineBX;

    /** The slope of the x- and y-axis lines within a tile. */
    public float fineSlopeX, fineSlopeY;

    /** Whether tile coordinates should be drawn. */
    public boolean showCoords;

    /** Whether locations in the scene should be drawn. */
    public boolean showLocs;

    /** Whether sprite paths should be drawn. */
    public boolean showPaths;

    /**
     * Construct an iso scene view model with view parameters as
     * specified in the given config object.
     *
     * @param config the config object.
     */
    public IsoSceneViewModel (Config config)
    {
	// set the scene tile dimensions
	scenewid = config.getValue(SCENE_WIDTH_KEY, DEF_SCENE_WIDTH);
	scenehei = config.getValue(SCENE_HEIGHT_KEY, DEF_SCENE_HEIGHT);

	// get the tile dimensions
	tilewid = config.getValue(TILE_WIDTH_KEY, DEF_TILE_WIDTH);
	tilehei = config.getValue(TILE_HEIGHT_KEY, DEF_TILE_HEIGHT);

	// set the fine coordinate granularity
	finegran = config.getValue(FINE_GRAN_KEY, DEF_FINE_GRAN);

	// set the desired scene view bounds
	int svwid = config.getValue(SCENE_VWIDTH_KEY, DEF_SCENE_VWIDTH);
	int svhei = config.getValue(SCENE_VHEIGHT_KEY, DEF_SCENE_VHEIGHT);
	bounds = new Dimension(svwid * tilewid, svhei * tilehei);

	// set the scene display origin
	int offy = config.getValue(SCENE_OFFSET_Y_KEY, DEF_OFFSET_Y);
        origin = new Point((bounds.width / 2), (offy * tilehei));

	// set our various flags
        showCoords = config.getValue(SHOW_COORDS_KEY, DEF_SHOW_COORDS);
	showLocs = config.getValue(SHOW_COORDS_KEY, DEF_SHOW_COORDS);
	showPaths = config.getValue(SHOW_PATHS_KEY, DEF_SHOW_PATHS);
    }

    /**
     * Returns whether the given tile coordinate is a valid coordinate
     * within the scene.
     */
    public boolean isValidCoordinate (int x, int y)
    {
        return (x >= 0 && x < scenewid &&
                y >= 0 && y < scenehei);
    }

    /**
     * Returns whether the given full coordinate is a valid coordinate
     * within the scene.
     */
    public boolean isValidFullCoordinate (int x, int y)
    {
        int tx = IsoUtil.fullToTile(x), ty = IsoUtil.fullToTile(y);
        int fx = IsoUtil.fullToFine(x), fy = IsoUtil.fullToFine(y);
        return (isValidCoordinate(tx, ty) &&
                fx >= 0 && fx < finegran &&
                fy >= 0 && fy < finegran);
    }

    /**
     * Return whether locations in the scene are currently drawn.
     */
    public boolean getShowLocations ()
    {
	return showLocs;
    }

    /**
     * Set whether locations in the scene should be drawn.
     *
     * @param show whether to show locations.
     */
    public void setShowLocations (boolean show)
    {
	showLocs = show;
    }

    /**
     * Return whether coordinates are currently drawn for each tile.
     */
    public boolean getShowCoordinates ()
    {
	return showCoords;
    }

    /**
     * Set whether coordinates should be drawn for each tile.
     *
     * @param show whether to show coordinates.
     */
    public void setShowCoordinates (boolean show)
    {
	showCoords = show;
    }

    /**
     * Set whether sprite paths should be drawn.
     *
     * @param show whether to show paths.
     */
    public void setShowSpritePaths (boolean show)
    {
	showPaths = show;
    }

    /**
     * Pre-calculate various member data that are commonly used in
     * working with an isometric view.
     */
    public void precalculate ()
    {
	// pre-calculate tile-related data
	precalculateTiles();

	// calculate scene-based x-axis line for conversion from
	// screen to tile coordinates

        // create the x- and y-axis lines
	lineX = new Point[2];
	for (int ii = 0; ii < 2; ii++) {
	    lineX[ii] = new Point();
	}

        // determine the starting point
        lineX[0].setLocation(origin.x, origin.y);
	bX = (int)-(slopeX * origin.x);

        // determine the ending point
	lineX[1].x = lineX[0].x + (tilehwid * scenewid);
	lineX[1].y = lineX[0].y + (int)((slopeX * lineX[1].x) + bX);

	// calculate tile-based x-axis line for conversion from
	// tile-based pixel to fine coordinates

	// calculate the edge length separating each fine coordinate
	finelen = tilelen / (float)finegran;

	// calculate the fine-coordinate x-axis line
	fineSlopeX = (float)tilehei / (float)tilewid;
	fineBX = -(fineSlopeX * (float)tilehwid);
	fineSlopeY = -fineSlopeX;

	// calculate the fine coordinate dimensions
	finehwid = (int)((float)tilehwid / (float)finegran);
	finehhei = (int)((float)tilehhei / (float)finegran);
    }

    /**
     * Pre-calculate various tile-related member data.
     */
    protected void precalculateTiles ()
    {
        // halve the dimensions
        tilehwid = (tilewid / 2);
        tilehhei = (tilehei / 2);

        // calculate the length of a tile edge in pixels
        tilelen = (float) Math.sqrt(
            (tilehwid * tilehwid) + (tilehhei * tilehhei));

        // calculate the number of tile rows to render
        tilerows = (scenewid * scenehei) - 1;

        // calculate the slope of the x- and y-axis lines
        slopeX = (float)tilehei / (float)tilewid;
        slopeY = -slopeX;
    }

    /** The config key for tile width in pixels. */
    protected static final String TILE_WIDTH_KEY =
	MisoUtil.CONFIG_KEY + ".tile_width";

    /** The config key for tile height in pixels. */
    protected static final String TILE_HEIGHT_KEY =
	MisoUtil.CONFIG_KEY + ".tile_height";

    /** The config key for tile fine coordinate granularity. */
    protected static final String FINE_GRAN_KEY =
	MisoUtil.CONFIG_KEY + ".fine_granularity";

    /** The config key for scene view width in tile count. */
    protected static final String SCENE_VWIDTH_KEY =
	MisoUtil.CONFIG_KEY + ".scene_view_width";

    /** The config key for scene view height in tile count. */
    protected static final String SCENE_VHEIGHT_KEY =
	MisoUtil.CONFIG_KEY + ".scene_view_height";

    /** The config key for scene width in tile count. */
    protected static final String SCENE_WIDTH_KEY =
	MisoUtil.CONFIG_KEY + ".scene_width";

    /** The config key for scene height in tile count. */
    protected static final String SCENE_HEIGHT_KEY =
	MisoUtil.CONFIG_KEY + ".scene_height";

    /** The config key for scene origin vertical offset in tile count. */
    protected static final String SCENE_OFFSET_Y_KEY =
	MisoUtil.CONFIG_KEY + ".scene_offset_y";

    /** The config key for whether to show tile coordinates. */
    protected static final String SHOW_COORDS_KEY =
	MisoUtil.CONFIG_KEY + ".show_coords";

    /** The config key for whether to show locations. */
    protected static final String SHOW_LOCS_KEY =
	MisoUtil.CONFIG_KEY + ".show_locs";

    /** The config key for whether to show sprite paths. */
    protected static final String SHOW_PATHS_KEY =
	MisoUtil.CONFIG_KEY + ".show_paths";

    /** Default scene view parameters. */
    protected static final int DEF_TILE_WIDTH = 64;
    protected static final int DEF_TILE_HEIGHT = 48;
    protected static final int DEF_FINE_GRAN = 4;
    protected static final int DEF_SCENE_VWIDTH = 10;
    protected static final int DEF_SCENE_VHEIGHT = 12;
    protected static final int DEF_SCENE_WIDTH = 22;
    protected static final int DEF_SCENE_HEIGHT = 22;
    protected static final int DEF_OFFSET_Y = -5;
    protected static final boolean DEF_SHOW_COORDS = false;
    protected static final boolean DEF_SHOW_LOCS = false;
    protected static final boolean DEF_SHOW_PATHS = false;
}
