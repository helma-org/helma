/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.objectmodel.db;


/**
 * A class that encapsulates the Column name and data type of a
 * column in a relational table.
 */
public final class DbColumn {
    private final String name;
    private final int type;
    private final Relation relation;

    /**
     *  Constructor
     */
    public DbColumn(String name, int type, Relation rel) {
        this.name = name;
        this.type = type;
        this.relation = rel;

        if (relation != null) {
            relation.setColumnType(type);
        }
    }

    /**
     *  Get the column name.
     */
    public String getName() {
        return name;
    }

    /**
     *  Get this columns SQL data type.
     */
    public int getType() {
        return type;
    }

    /**
     *  Return the relation associated with this column. May be null.
     */
    public Relation getRelation() {
        return relation;
    }
}
