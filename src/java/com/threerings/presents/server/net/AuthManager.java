//
// $Id: AuthManager.java,v 1.6 2001/12/03 20:14:51 mdb Exp $

package com.threerings.presents.server.net;

import com.samskivert.util.LoopingThread;
import com.samskivert.util.Queue;

import com.threerings.presents.Log;
import com.threerings.presents.net.AuthRequest;
import com.threerings.presents.net.AuthResponse;

/**
 * The authentication manager takes care of the authentication process.
 * Authentication happens on multiple threads. The conmgr thread parses
 * the authentication request and passes it on to the authmgr thread. The
 * authmgr thread invokes the (pluggable) authenticator to do the actual
 * authentication. Then the response message is queued up to be delivered
 * by the conmgr thread.
 *
 * <p> This structure prevents authentication to take place
 * asynchronously, but in a controlled manner. Only one authentication
 * will be processed at a time, but the dobj and conmgr threads will
 * continue to operate independent of the authentication process.
 */
public class AuthManager extends LoopingThread
{
    public AuthManager (Authenticator author)
    {
        _author = author;
    }

    /**
     * Puts an authenticating connection on the queue. The connection will
     * be authenticated and an auth response delivered.
     */
    public void postAuthingConnection (AuthingConnection aconn)
    {
        _authq.append(aconn);
    }

    /**
     * The connection manager introduces itself to the auth manager so
     * that the auth manager can let it know when it has authorized
     * connections.
     */
    public void setConnectionManager (ConnectionManager conmgr)
    {
        _conmgr = conmgr;
    }

    /**
     * Process auth requests.
     */
    protected void iterate ()
    {
        // grab the next authing connection from the queue
        Object item = _authq.get();

        // if we're being kicked and requested to exit, we'll just post
        // some bogus item on the queue to wake up the auth manager and
        // get him the hell out of dodge
        if (!(item instanceof AuthingConnection)) {
            return;
        }

        AuthingConnection aconn = (AuthingConnection)item;
        try {
            // instruct the authenticator to process the auth request
            AuthResponse rsp = _author.process(aconn.getAuthRequest());

            // now ship the response back
            aconn.postMessage(rsp);

            // stuff a reference to the auth response into the connection
            // so that we have access to it later in the authentication
            // process
            aconn.setAuthResponse(rsp);

            // if the authentication request was granted, let the
            // connection manager know that we just authed
            _conmgr.connectionDidAuthenticate(aconn);

        } catch (Exception e) {
            Log.warning("Failure processing authreq [conn=" + aconn + "].");
            Log.logStackTrace(e);
        }
    }

    protected void kick ()
    {
        // we post something bogus to the queue to wake up the authmgr
        _authq.append(new Integer(0));
    }

    protected Authenticator _author;
    protected ConnectionManager _conmgr;
    protected Queue _authq = new Queue();
}
