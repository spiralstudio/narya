//
// $Id: AnimatedPanel.java,v 1.21 2002/04/18 22:38:24 mdb Exp $

package com.threerings.media.animation;

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.ImageCapabilities;
import java.awt.Rectangle;

import java.awt.image.BufferStrategy;
import java.awt.image.VolatileImage;

import javax.swing.event.AncestorEvent;
import javax.swing.JComponent;

import java.util.ArrayList;
import java.util.List;

import com.samskivert.swing.event.AncestorAdapter;
import com.samskivert.util.Histogram;
import com.samskivert.util.StringUtil;

import com.threerings.media.Log;
import com.threerings.media.sprite.SpriteManager;

/**
 * The animated panel provides a useful extensible implementation of a
 * {@link JComponent} that implements the {@link AnimatedView} interface.
 * It takes care of automatically creating an animation manager and a
 * sprite manager to manage the animations that take place within this
 * panel. If a sprite manager is not needed, {@link #needsSpriteManager}
 * can be overridden to prevent it from being created.
 *
 * <p> Users of the animated panel must be sure to call {@link #stop} when
 * their panel is no longer being displayed and {@link #start} to start it
 * back up again when it is once again being displayed. These methods
 * deactivate and reactivate the underlying animation manager.
 *
 * <p> Sub-classes should override {@link #render} to draw their
 * panel-specific contents.
 */
public class AnimatedPanel extends JComponent
    implements AnimatedView
{
    /**
     * Constructs an animated panel.
     */
    public AnimatedPanel ()
    {
        // create our animation manager
        _animmgr = new AnimationManager(this);

        // create a sprite manager if we haven't been requested not to
        if (needsSpriteManager()) {
            _spritemgr = new SpriteManager();
            _animmgr.setSpriteManager(_spritemgr);
        }

        // turn off double buffering because we handle our own rendering;
        // it's not enough to turn it off for this component, we need to
        // climb all the way up the chain, otherwise one of our parents
        // might be painting and decide to do its own double buffering
        addAncestorListener(new AncestorAdapter() {
            public void ancestorAdded (AncestorEvent event) {
                Component c = AnimatedPanel.this;
                while (c != null) {
                    if (c instanceof JComponent) {
                        ((JComponent)c).setDoubleBuffered(false);
                    }
                    c = c.getParent();
                }
                setOpaque(true);
            }
        });
    }

    /**
     * Derived classes can override this method to prevent the automatic
     * creation of a sprite manager to work with the animation manager
     * that is managing this panel.
     */
    protected boolean needsSpriteManager ()
    {
        return true;
    }        

    /**
     * Starts up the animation manager associated with this panel.
     */
    public void start ()
    {
        _animmgr.start();
    }

    /**
     * Shuts down the animation manager associated with this panel.
     */
    public void stop ()
    {
        _animmgr.stop();
    }

    /**
     * Instructs the view to scroll by the specified number of pixels
     * (which can be negative if it should scroll in the negative x or y
     * direction) in the specified number of milliseconds. While the view
     * is scrolling, derived classes can hear about the scrolled
     * increments by overriding {@link #viewWillScroll} and they can find
     * out when scrolling is complete by overriding {@link
     * #viewFinishedScrolling}.
     *
     * @param dx the number of pixels in the x direction to scroll.
     * @param dy the number of pixels in the y direction to scroll.
     * @param millis the number of milliseconds in which to do it. The
     * scrolling is calculated such that we will "drop frames" in order to
     * scroll the necessary distance by the requested time.
     */
    public void setScrolling (int dx, int dy, long millis)
    {
        // if dx and dy are zero, we've got nothing to do
        if (dx == 0 && dy == 0) {
            return;
        }

        // set our scrolling parameters
        _scrollx = dx;
        _scrolly = dy;
        _last = System.currentTimeMillis();
        _ttime = _last + millis;

        // figure out the lesser (but non-zero) of the two scroll deltas
        int absx = Math.abs(dx), absy = Math.abs(dy);
        int mindist = absx;
        if (absx == 0) {
            mindist = absy;
        } else if (absy == 0) {
            mindist = absx;
        } else {
            mindist = Math.min(absx, absy);
        }

        // let the animation manager know how "fast" we'll be scrolling so
        // that it can determine its frame rate
        int mspp = (int)(millis/mindist);
        _animmgr.setScrolling(mspp);

//         Log.info("Scrolling [dx=" + dx + ", dy=" + dy +
//                  ", millis=" + millis + "ms, mspp=" + mspp + "].");
    }

    // documentation inherited
    public void paint (Graphics g)
    {
        // convert the clipping rectangle into the proper coordinates and
        // call into our rendering system
        Rectangle r = g.getClipBounds();
        r.translate(_tx, _ty);
        _paintList.add(r);
        // we have to tell paintImmediately() to use the graphics object
        // that we were passed, otherwise Swing will fuck everything to
        // the high heavens
        paintImmediately(g, _paintList);
        _paintList.clear();
    }

    // documentation inherited
    public void paintImmediately (int x, int y, int w, int h)
    {
        paintImmediately(new Rectangle(x, y, w, h));
    }

    // documentation inherited
    public void paintImmediately (Rectangle r)
    {
        // convert the clipping rectangle into the proper coordinates and
        // call into our rendering system
        r.translate(_tx, _ty);
        _paintList.add(r);
        paintImmediately(_paintList);
        _paintList.clear();
    }

    // documentation inherited
    public void doLayout ()
    {
        super.doLayout();

        // if we change size, clear out our old back buffer
        _backimg = null;

        // figure out our viewport offsets
        Dimension size = getSize(), vsize = getViewSize();
        _tx = (vsize.width - size.width)/2;
        _ty = (vsize.height - size.height)/2;

//         Log.info("Size: " + size + ", vsize: " + vsize +
//                  ", tx: " + _ty + ", ty: " + _ty + ".");
    }

    // documentation inherited
    public void invalidateRect (Rectangle invalidRect)
    {
        // pass it on to the animation manager
        _animmgr.addDirtyRect(invalidRect);
    }

    // documentation inherited
    public void paintImmediately (List invalidRects)
    {
        paintImmediately(null, invalidRects);
    }

    /**
     * Renders this animated panel. If the supplied graphics reference is
     * non-null, we will render to that instance, otherwise a graphics
     * object will be obtained via a call to {@link #getGraphics}. This
     * method, of course, must be called from the AWT thread or mayhem
     * will ensue.
     */
    protected void paintImmediately (Graphics gfx, List invalidRects)
    {
        // no use in painting if we're not showing or if we've not yet
        // been validated
        if (!isValid() || !isShowing()) {
            return;
        }

        // track how long it was since we were last painted
        long now = System.currentTimeMillis();
//         if (_paint != 0) {
//             int delta = (int)(now-_paint);
//             _histo.addValue(delta);

//             // dump the histogram every ten seconds
//             if (_paint % (10*1000) < 50) {
//                 Log.info("Render histogram.");
//                 Log.info(StringUtil.toMatrixString(_histo.getBuckets(), 10, 3));
//             }
//         }

        int width = getWidth(), height = getHeight();

        // if scrolling is enabled, determine the scrolling delta to be
        // used and do the business
        int dx = 0, dy = 0;
        if (_ttime != 0) {
            // if we've blown past our allotted time, we want to scroll
            // the rest of the way
            if (now > _ttime) {
                dx = _scrollx;
                dy = _scrolly;

//                 Log.info("Scrolling rest [dx=" + dx + ", dy=" + dy + "].");

            } else {
                // otherwise figure out how many milliseconds have gone by
                // since we last scrolled and scroll the requisite amount
                float dt = (float)(now - _last);
                float rt = (float)(_ttime - _last);

                // our delta is the remaining distance multiplied by the
                // time delta divided by the remaining time
                dx = Math.round((float)(_scrollx * dt) / rt);
                dy = Math.round((float)(_scrolly * dt) / rt);

//                 Log.info("Scrolling delta [dt=" + dt + ", rt=" + rt +
//                          ", dx=" + dx + ", dy=" + dy + "].");
            }

            // and add invalid rectangles for the exposed areas
            if (dx > 0) {
                Rectangle dirty = new Rectangle(width - dx, 0, dx, height);
                dirty.translate(_tx, _ty);
                invalidRects.add(dirty);
            } else if (dx < 0) {
                Rectangle dirty = new Rectangle(0, 0, -dx, height);
                dirty.translate(_tx, _ty);
                invalidRects.add(dirty);
            }
            if (dy > 0) {
                Rectangle dirty = new Rectangle(0, height - dy, width, dy);
                dirty.translate(_tx, _ty);
                invalidRects.add(dirty);
            } else if (dy < 0) {
                Rectangle dirty = new Rectangle(0, 0, width, -dy);
                dirty.translate(_tx, _ty);
                invalidRects.add(dirty);
            }

            // make sure we're actually scrolling before telling people
            // about it
            if (dx != 0 || dy != 0) {
                // if we are working with a sprite manager, let it know
                // that we're about to scroll out from under its sprites
                // and allow it to provide us with more dirty rects
                if (_spritemgr != null) {
                    _spritemgr.viewWillScroll(dx, dy, invalidRects);
                }

                // let our derived classes do whatever they need to do to
                // prepare to be scrolled
                viewWillScroll(dx, dy, now, invalidRects);

                // keep track of the last time we scrolled
                _last = now;

                // subtract our scrolled deltas from the distance remaining
                _scrollx -= dx;
                _scrolly -= dy;

                // if we've reached our desired position, finish the job
                if (_scrollx == 0 && _scrolly == 0) {
                    _ttime = 0;
                    viewFinishedScrolling();
                }
            }
        }

        // if we didn't scroll and have no invalid rects, there's no need
        // to repaint anything
        if (invalidRects.size() == 0 && dx == 0 && dy == 0) {
            return;
        }

        // create our off-screen buffer if necessary
        GraphicsConfiguration gc = getGraphicsConfiguration();
        if (_backimg == null) {
            createBackBuffer(gc);
        }

        // render into our back buffer
        do {
            // make sure our back buffer hasn't disappeared
            int valres = _backimg.validate(gc);

            // if we've changed resolutions, recreate the buffer
            if (valres == VolatileImage.IMAGE_INCOMPATIBLE) {
                Log.info("Back buffer incompatible, recreating.");
                createBackBuffer(gc);
            }

            Graphics g = null;
            try {
                g = _backimg.getGraphics();

                // if the image wasn't A-OK, we need to rerender the whole
                // business rather than just the dirty parts
                if (valres != VolatileImage.IMAGE_OK) {
                    invalidRects.clear();
                    invalidRects.add(new Rectangle(_tx, _ty, width, height));
                    Log.info("Lost back buffer, redrawing.");

                } else if (dx != 0 || dy != 0) {
                    // if it was OK, we may need to do some scrolling
                    g.copyArea(0, 0, width, height, -dx, -dy);
                }

                // translate into happy space
                g.translate(-_tx, -_ty);

                // now do our actual rendering
                render((Graphics2D)g, invalidRects);

                // translate back out of happy space
                g.translate(_tx, _ty);

            } finally {
                g.dispose();
            }

            // draw the back buffer to the screen
            try {
                if (gfx == null) {
                    g = getGraphics();
                } else {
                    g = gfx;
                }

                // if we're scrolling, we've got to copy the whole image
                // to the screen, otherwise we can just copy the dirty
                // regions
                if (dx != 0 || dy != 0) {
                    g.drawImage(_backimg, 0, 0, null);

                } else {
                    // iterate through the invalid rectangles, copying
                    // those areas from the back buffer to the display
                    int isize = invalidRects.size();
                    for (int i = 0; i < isize; i++) {
                        Rectangle rect = (Rectangle)invalidRects.get(i);
                        // we have to translate out of view coordinates
                        // before doing the actual copy
                        rect.translate(-_tx, -_ty);
                        g.setClip(rect);
                        g.drawImage(_backimg, 0, 0, null);
                    }
                }

            } finally {
                if (g != null) {
                    g.dispose();
                }
            }

        } while (_backimg.contentsLost());
    }

    /**
     * This is called, when the view is scrolling, just before a call to
     * {@link #render}. The animated panel will take care of scrolling the
     * contents of the offscreen buffer, but the derived class will need
     * to do whatever is necessary to prepare to repaint the exposed
     * regions as well as update it's own internal state accordingly.
     *
     * @param dx the distance (in pixels) that the view will scroll in the
     * x direction.
     * @param dy the distance (in pixels) that the view will scroll in the
     * y direction.
     * @param now the current time, provided because we have it and
     * scrolling views are likely to want to use it in calculating stuff.
     * @param invalidRects the list of invalid rectangles which will be
     * redrawn; rectangles can be added to this list if necessary.
     */
    protected void viewWillScroll (int dx, int dy, long now, List invalidRects)
    {
        // nothing to do here
    }

    /**
     * Called during the same frame that we scrolled into the final
     * desired position. This method is called after {@link
     * #viewWillScroll} is called with the final scrolling deltas.
     */
    protected void viewFinishedScrolling ()
    {
//         Log.info("viewFinishedScrolling");
    }

    /**
     * Derived classes that wish to operate in a coordinate system based
     * on a view size that is larger or smaller than the viewport size
     * (the actual dimensions of the animated panel) can override this
     * method and return the desired size of the view. The animated panel
     * will take this size into account and translate into the view
     * coordinate system before calling {@link #render}.
     */
    protected Dimension getViewSize ()
    {
        return getSize();
    }

    /**
     * Requests that the supplied list of invalid rectangles be redrawn in
     * the supplied graphics context. The rectangles will be in the view
     * coordinate system (which may differ from screen coordinates, see
     * {@link #getViewSize}. Sub-classes should override this method to do
     * the actual rendering for their display.
     */
    protected void render (Graphics2D gfx, List invalidRects)
    {
        // nothing for now
    }

    /**
     * Creates the off-screen buffer used to perform double buffered
     * rendering of the animated panel.
     */
    protected void createBackBuffer (GraphicsConfiguration gc)
    {
        _backimg = gc.createCompatibleVolatileImage(getWidth(), getHeight());
    }

    /** The animation manager we use in this panel. */
    protected AnimationManager _animmgr;

    /** The sprite manager in use by this panel. */
    protected SpriteManager _spritemgr;

    /** The image used to render off-screen. */
    protected VolatileImage _backimg;

    /** Our viewport offsets. */
    protected int _tx, _ty;

    /** The time at which we expect to stop scrolling. */
    protected long _ttime;

    /** Used to determine how many pixels we have left to scroll. */
    protected int _scrollx, _scrolly;

    /** The last time we were rendered. */
    protected long _last;

    /** A histogram for tracking how frequently we're rendered. */
    protected Histogram _histo = new Histogram(0, 1, 100);

    /** Used to pass dirty regions supplied by the AWT to the rendering
     * system. */
    protected ArrayList _paintList = new ArrayList();
}
