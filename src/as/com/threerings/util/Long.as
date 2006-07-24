package com.threerings.util {

/**
 * Equivalent to java.lang.Long.
 */
public class Long
    implements Equalable, Wrapped
{
    public var high :int;
    public var low :int;

    public static function valueOf (low :int, high :int = 0) :Long
    {
        return new Long(low, high);
    }

    public function Long (low :int, high :int = 0)
    {
        this.low = low;
        this.high = high;
    }

    // from Equalable
    public function equals (other :Object) :Boolean
    {
        if (!(other is Long)) {
            return false;
        }
        var that :Long = (other as Long);
        return (this.high == that.high) && (this.low == that.low);
    }

    // from Wrapped
    public function unwrap () :Object
    {
        return low; // TODO
    }
}
}
