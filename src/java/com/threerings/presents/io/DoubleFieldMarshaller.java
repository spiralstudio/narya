//
// $Id: DoubleFieldMarshaller.java,v 1.5 2002/02/01 23:26:49 mdb Exp $

package com.threerings.presents.dobj.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class DoubleFieldMarshaller implements FieldMarshaller
{
    /** This is the sort of field that we marshall. */
    public double prototype;

    public void writeTo (DataOutputStream out, Field field, Object obj)
        throws IOException, IllegalAccessException
    {
        out.writeDouble(field.getDouble(obj));
    }

    public void readFrom (DataInputStream in, Field field, Object obj)
        throws IOException, IllegalAccessException
    {
        field.setDouble(obj, in.readDouble());
    }
}
