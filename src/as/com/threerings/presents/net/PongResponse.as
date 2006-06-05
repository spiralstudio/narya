package com.threerings.presents.net {

import flash.utils.getTimer;

import com.threerings.util.Long;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

public class PongResponse extends DownstreamMessage
{
    public function PongResponse ()
    {
        super();
    }

    public function getPackStamp () :Long
    {
        return _packStamp;
    }

    public function getProcessDelay () :int
    {
        return _processDelay;
    }

    public function getUnpackStamp () :uint
    {
        return _unpackStamp;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        _unpackStamp = getTimer();
        super.readObject(ins);

        // TODO: Figure out how we're really going to cope with longs
        _packStamp = new Long(0);
        ins.readBareObject(_packStamp);

        _processDelay = ins.readInt();
    }

    protected var _packStamp :Long;

    protected var _processDelay :int;

    protected var _unpackStamp :uint;
}
}
