//
// $Id: TableLobbyObject.java,v 1.6 2003/02/26 17:54:56 mdb Exp $

package com.threerings.micasa.lobby.table;

import com.threerings.presents.dobj.DSet;
import com.threerings.parlor.data.Table;
import com.threerings.micasa.lobby.LobbyObject;

public class TableLobbyObject
    extends LobbyObject
    implements com.threerings.parlor.data.TableLobbyObject
{
    /** The field name of the <code>tableSet</code> field. */
    public static final String TABLE_SET = "tableSet";

    /** A set containing all of the tables being managed by this lobby. */
    public DSet tableSet = new DSet();

    // documentation inherited
    public DSet getTables ()
    {
        return tableSet;
    }

    // documentation inherited from interface
    public void addToTables (Table table)
    {
        addToTableSet(table);
    }

    // documentation inherited from interface
    public void updateTables (Table table)
    {
        updateTableSet(table);
    }

    // documentation inherited from interface
    public void removeFromTables (Comparable key)
    {
        removeFromTableSet(key);
    }

    /**
     * Requests that the specified entry be added to the
     * <code>tableSet</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void addToTableSet (DSet.Entry elem)
    {
        requestEntryAdd(TABLE_SET, elem);
    }

    /**
     * Requests that the entry matching the supplied key be removed from
     * the <code>tableSet</code> set. The set will not change until the
     * event is actually propagated through the system.
     */
    public void removeFromTableSet (Comparable key)
    {
        requestEntryRemove(TABLE_SET, key);
    }

    /**
     * Requests that the specified entry be updated in the
     * <code>tableSet</code> set. The set will not change until the event is
     * actually propagated through the system.
     */
    public void updateTableSet (DSet.Entry elem)
    {
        requestEntryUpdate(TABLE_SET, elem);
    }

    /**
     * Requests that the <code>tableSet</code> field be set to the
     * specified value. Generally one only adds, updates and removes
     * entries of a distributed set, but certain situations call for a
     * complete replacement of the set value. The local value will be
     * updated immediately and an event will be propagated through the
     * system to notify all listeners that the attribute did
     * change. Proxied copies of this object (on clients) will apply the
     * value change when they received the attribute changed notification.
     */
    public void setTableSet (DSet tableSet)
    {
        this.tableSet = tableSet;
        requestAttributeChange(TABLE_SET, tableSet);
    }
}
