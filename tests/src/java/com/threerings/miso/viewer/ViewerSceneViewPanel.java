//
// $Id: ViewerSceneViewPanel.java,v 1.58 2003/04/25 15:52:25 mdb Exp $

package com.threerings.miso.viewer;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.threerings.util.RandomUtil;
import com.threerings.media.FrameManager;

import com.threerings.cast.CharacterDescriptor;
import com.threerings.cast.CharacterManager;
import com.threerings.cast.CharacterSprite;
import com.threerings.cast.ComponentRepository;
import com.threerings.cast.util.CastUtil;

import com.threerings.media.sprite.PathCompletedEvent;
import com.threerings.media.sprite.SpriteEvent;
import com.threerings.media.sprite.SpriteManager;
import com.threerings.media.sprite.SpriteObserver;
import com.threerings.media.util.LineSegmentPath;

import com.threerings.media.util.PerformanceMonitor;
import com.threerings.media.util.PerformanceObserver;

import com.threerings.miso.Log;
import com.threerings.miso.MisoConfig;
import com.threerings.miso.client.MisoScenePanel;
import com.threerings.miso.data.MisoSceneModel;
import com.threerings.miso.util.MisoContext;

public class ViewerSceneViewPanel extends MisoScenePanel
    implements PerformanceObserver, SpriteObserver
{
    /**
     * Construct the panel and initialize it with a context.
     */
    public ViewerSceneViewPanel (MisoContext ctx,
                                 CharacterManager charmgr,
                                 ComponentRepository crepo)
    {
	super(ctx, MisoConfig.getSceneMetrics());

        // create the character descriptors
        _descUser = CastUtil.getRandomDescriptor("female", crepo);
        _descDecoy = CastUtil.getRandomDescriptor("male", crepo);

        // create the manipulable sprite
        _sprite = createSprite(_spritemgr, charmgr, _descUser);
        setFollowsPathable(_sprite);

        // create the decoy sprites
        createDecoys(_spritemgr, charmgr);

	PerformanceMonitor.register(this, "paint", 1000);
    }

    // documentation inherited
    public void setSceneModel (MisoSceneModel model)
    {
        super.setSceneModel(model);
        Log.info("Using " + model + ".");

        // now that we have a scene, we can create valid paths for our
        // decoy sprites
        createDecoyPaths();
    }

    /**
     * Creates a new sprite.
     */
    protected CharacterSprite createSprite (
        SpriteManager spritemgr, CharacterManager charmgr,
        CharacterDescriptor desc)
    {
        CharacterSprite s = charmgr.getCharacter(desc);
        if (s != null) {
            // start 'em out standing
            s.setActionSequence(CharacterSprite.STANDING);
            s.setLocation(300, 300);
            s.addSpriteObserver(this);
            spritemgr.addSprite(s);
        }

        return s;
    }

    /**
     * Creates the decoy sprites.
     */
    protected void createDecoys (
        SpriteManager spritemgr, CharacterManager charmgr)
    {
        _decoys = new CharacterSprite[NUM_DECOYS];
        for (int ii = 0; ii < NUM_DECOYS; ii++) {
            _decoys[ii] = createSprite(spritemgr, charmgr, _descDecoy);
        }
    }

    /**
     * Creates paths for the decoy sprites.
     */
    protected void createDecoyPaths ()
    {
        for (int ii = 0; ii < NUM_DECOYS; ii++) {
            if (_decoys[ii] != null) {
                createRandomPath(_decoys[ii]);
            }
        }
    }

    // documentation inherited
    public void paint (Graphics g)
    {
	super.paint(g);
	PerformanceMonitor.tick(this, "paint");
    }

    // documentation inherited
    public void checkpoint (String name, int ticks)
    {
        Log.info(name + " [ticks=" + ticks + "].");
    }

    // documentation inherited
    public void mousePressed (MouseEvent e)
    {
        super.mousePressed(e);

        int x = e.getX(), y = e.getY();
        Log.info("Mouse pressed +" + x + "+" + y);

        switch (e.getModifiers()) {
        case MouseEvent.BUTTON1_MASK:
            createPath(_sprite, x, y);
            break;

        case MouseEvent.BUTTON2_MASK:
            for (int ii = 0; ii < NUM_DECOYS; ii++) {
                createPath(_decoys[ii], x, y);
            }
            break;
        }
    }

    /**
     * Assigns the sprite a path leading to the given destination
     * screen coordinates.  Returns whether a path was successfully
     * assigned.
     */
    protected boolean createPath (CharacterSprite s, int x, int y)
    {
        // get the path from here to there
        LineSegmentPath path = (LineSegmentPath)getPath(s, x, y);
	if (path == null) {
	    s.cancelMove();
	    return false;
	}

        // start the sprite moving along the path
	path.setVelocity(100f/1000f);
	s.move(path);
        return true;
    }

    /**
     * Assigns a new random path to the given sprite.
     */
    protected void createRandomPath (CharacterSprite s)
    {
        Dimension d = _vbounds.getSize();
        int x, y;
        do {
            x = RandomUtil.getInt(d.width);
            y = RandomUtil.getInt(d.height);
        } while (!createPath(s, x, y));
    }

    // documentation inherited
    public void handleEvent (SpriteEvent event)
    {
        if (event instanceof PathCompletedEvent) {
            CharacterSprite s = (CharacterSprite)event.getSprite();

            if (s != _sprite) {
                // move the sprite to a new random location
                createRandomPath(s);
            }
        }
    }

    /** The number of decoy characters milling about. */
    protected static final int NUM_DECOYS = 5;

    /** The character descriptor for the user character. */
    protected CharacterDescriptor _descUser;

    /** The character descriptor for the decoy characters. */
    protected CharacterDescriptor _descDecoy;

    /** The sprite we're manipulating within the view. */
    protected CharacterSprite _sprite;

    /** The test sprites that meander about aimlessly. */
    protected CharacterSprite _decoys[];
}
