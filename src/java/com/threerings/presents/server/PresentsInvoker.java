//
// $Id$
//
// Narya library - tools for developing networked games
// Copyright (C) 2002-2007 Three Rings Design, Inc., All Rights Reserved
// http://www.threerings.net/code/narya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.presents.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;

import static com.threerings.presents.Log.log;

/**
 * Extends the generic {@link Invoker} and integrates it a bit more into the Presents system.
 */
@Singleton
public class PresentsInvoker extends Invoker
    implements ShutdownManager.Shutdowner, ReportManager.Reporter
{
    @Inject public PresentsInvoker (PresentsDObjectMgr omgr, ShutdownManager shutmgr,
                                    ReportManager repmgr)
    {
        super("presents.Invoker", omgr);
        _omgr = omgr;
        shutmgr.registerShutdowner(this);
        if (PERF_TRACK) {
            repmgr.registerReporter(this);
        }
    }

    @Override // from Invoker
    public void shutdown ()
    {
        // this will do a sophisticated shutdown of both ourself and the dobjmgr
        _queue.append(new ShutdownUnit());
    }

    // from interface ReportManager.Reporter
    public void appendReport (StringBuilder buf, long now, long sinceLast, boolean reset)
    {
        buf.append("* presents.util.Invoker:\n");
        int qsize = _queue.size();
        buf.append("- Queue size: ").append(qsize).append("\n");
        synchronized (this) {
            buf.append("- Max queue size: ").append(_maxQueueSize).append("\n");
            buf.append("- Units executed: ").append(_unitsRun).append("\n");
            if (_currentUnit != null) {
                String uname = StringUtil.safeToString(_currentUnit);
                buf.append("- Current unit: ").append(uname).append(" ");
                buf.append(now-_currentUnitStart).append("ms\n");
            }
            if (reset) {
                _maxQueueSize = qsize;
                _unitsRun = 0;
            }
        }

        if (PresentsDObjectMgr.UNIT_PROF_ENABLED) {
            for (Object key : _tracker.keySet()) {
                UnitProfile profile = _tracker.get(key);
                if (key instanceof Class) {
                    key = StringUtil.shortClassName((Class<?>)key);
                }
                buf.append("  ").append(key).append(" ");
                buf.append(profile).append("\n");
                if (reset) {
                    profile.clear();
                }
            }
        }
    }

    @Override // from Invoker
    protected void willInvokeUnit (Unit unit, long start)
    {
        super.willInvokeUnit(unit, start);

        int queueSize = _queue.size();
        synchronized (this) {
            // keep track of the largest queue size we've seen
            if (queueSize > _maxQueueSize) {
                _maxQueueSize = queueSize;
            }

            // note the currently invoking unit
            _currentUnit = unit;
            _currentUnitStart = start;
        }
    }

    @Override // from Invoker
    protected void didInvokeUnit (Unit unit, long start)
    {
        super.didInvokeUnit(unit, start);

        synchronized (this) {
            // clear out our currently invoking unit
            _currentUnit = null;
            _currentUnitStart = 0L;
        }
    }

    @Override // from Invoker
    protected void didShutdown ()
    {
        _server.invokerDidShutdown();
    }

    /**
     * This gets posted back and forth between the invoker and DObjectMgr until both of their
     * queues are empty and they can both be safely shutdown.
     */
    protected class ShutdownUnit extends Unit
    {
        // run on the invoker thread
        @Override
        public boolean invoke ()
        {
            if (checkLoops()) {
                return false;

            // if the invoker queue is not empty, we put ourselves back on it
            } else if (_queue.hasElements()) {
                _loopCount++;
                postUnit(this);
                return false;

            } else {
                // the invoker is empty, let's go over to the omgr
                _loopCount = 0;
                _passCount++;
                return true;
            }
        }

        // run on the dobj thread
        @Override
        public void handleResult ()
        {
            if (checkLoops()) {
                return;

            // if the queue isn't empty, re-post
            } else if (!_omgr.queueIsEmpty()) {
                _loopCount++;
                _omgr.postRunnable(this);

            // if the invoker still has stuff and we're still under the pass
            // limit, go ahead and pass it back to the invoker
            } else if (_queue.hasElements() && (_passCount < MAX_PASSES)) {
                // pass the buck back to the invoker
                _loopCount = 0;
                postUnit(this);

            } else {
                // otherwise end it, and complain if we're ending it
                // because of passes
                if (_passCount >= MAX_PASSES) {
                    log.warning("Shutdown Unit passed 50 times without " +
                                "finishing, shutting down harshly.");
                }
                doShutdown();
            }
        }

        /**
         * Check to make sure we haven't looped too many times.
         */
        protected boolean checkLoops ()
        {
            if (_loopCount > MAX_LOOPS) {
                log.warning("Shutdown Unit looped on one thread 10000 times " +
                            "without finishing, shutting down harshly.");
                doShutdown();
                return true;
            }

            return false;
        }

        /**
         * Do the actual shutdown.
         */
        protected void doShutdown ()
        {
            _omgr.harshShutdown(); // end the dobj thread

            // end the invoker thread
            postUnit(new Unit() {
                @Override
                public boolean invoke () {
                    _running = false;
                    return false;
                }
            });
        }

        /** The number of times we've been passed to the object manager. */
        protected int _passCount = 0;

        /** How many times we've looped on the thread we're currently on. */
        protected int _loopCount = 0;

        /** The maximum number of passes we allow before just ending things. */
        protected static final int MAX_PASSES = 50;

        /** The maximum number of loops we allow before just ending things. */
        protected static final int MAX_LOOPS = 10000;
    }

    /** The distributed object manager with which we interoperate. */
    protected PresentsDObjectMgr _omgr;

    /** The server we're working for. */
    @Inject protected PresentsServer _server;

    /** The largest queue size since our last report. */
    protected long _maxQueueSize;

    /** Records the currently invoking unit. */
    protected Object _currentUnit;

    /** The time at which our current unit started. */
    protected long _currentUnitStart;
}
