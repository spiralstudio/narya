//
// $Id: KeyTranslatorImpl.java,v 1.1 2001/12/12 16:55:06 shaper Exp $

package com.threerings.yohoho.puzzle.util;

import com.samskivert.util.HashIntMap;

/**
 * A simple implementation of the {@link KeyTranslator} interface that
 * provides facilities for mapping key codes to action command strings for
 * use by the {@link KeyboardManager}.
 */
public class KeyTranslatorImpl implements KeyTranslator
{
    /**
     * Adds a mapping from key press to action command string.
     */
    public void addPressCommand (int keyCode, String command)
    {
        _press.put(keyCode, command);
    }

    /**
     * Adds a mapping from key release to action command string.
     */
    public void addReleaseCommand (int keyCode, String command)
    {
        _release.put(keyCode, command);
    }

    // documentation inherited
    public boolean hasCommand (int keyCode)
    {
        return (_press.get(keyCode) != null ||
                _release.get(keyCode) != null);
    }

    // documentation inherited
    public String getPressCommand (int keyCode)
    {
        return (String)_press.get(keyCode);
    }

    // documentation inherited
    public String getReleaseCommand (int keyCode)
    {
        return (String)_release.get(keyCode);
    }
    
    /** The mapping for key presses from key codes to action commands. */
    protected HashIntMap _press = new HashIntMap();

    /** The mapping for key releases from key codes to action commands. */
    protected HashIntMap _release = new HashIntMap();
}
