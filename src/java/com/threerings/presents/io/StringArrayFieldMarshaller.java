//
// $Id: StringArrayFieldMarshaller.java,v 1.3 2002/02/01 23:26:49 mdb Exp $

package com.threerings.presents.dobj.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

public class StringArrayFieldMarshaller implements FieldMarshaller
{
    /** This is the sort of field that we marshall. */
    public String[] prototype;

    public void writeTo (DataOutputStream out, Field field, Object obj)
        throws IOException, IllegalAccessException
    {
        String[] value = (String[])field.get(obj);
        // we convert null string arrays to zero length arrays
        if (value == null) {
            out.writeInt(0);

        } else {
            out.writeInt(value.length);
            for (int i = 0; i < value.length; i++) {
                out.writeUTF((value[i] == null) ? "" : value[i]);
            }
        }
    }

    public void readFrom (DataInputStream in, Field field, Object obj)
        throws IOException, IllegalAccessException
    {
        String[] value = new String[in.readInt()];
        for (int i = 0; i < value.length; i++) {
            value[i] = in.readUTF();
        }
        field.set(obj, value);
    }
}
