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

import java.sql.Types;

/**
 * A class that encapsulates the Column name and data type of a column in a
 * relational table.
 */
public final class DbColumn {
    private final String name;
    private final int type;
    private final Relation relation;
    
    private final boolean isId;
    private final boolean isPrototype;
    private final boolean isName;

    /**
     * Constructor
     */
    public DbColumn(String name, int type, Relation rel, DbMapping dbmap) {
        this.name = name;
        this.type = type;
        this.relation = rel;

        if (relation != null) {
            relation.setColumnType(type);
        }

        isId = name.equalsIgnoreCase(dbmap.getIDField());
        isPrototype = name.equalsIgnoreCase(dbmap.getPrototypeField());
        isName = name.equalsIgnoreCase(dbmap.getNameField());
    }

    /**
     * Get the column name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get this columns SQL data type.
     */
    public int getType() {
        return type;
    }

    /**
     * Return the relation associated with this column. May be null.
     */
    public Relation getRelation() {
        return relation;
    }

    /**
     * Returns true if this column serves as ID field for the prototype.
     */
    public boolean isIdField() {
        return isId;
    }

    /**
     * Returns true if this column serves as prototype field for the prototype.
     */
    public boolean isPrototypeField() {
        return isPrototype;
    }

    /**
     * Returns true if this column serves as name field for the prototype.
     */
    public boolean isNameField() {
        return isName;
    }

    /**
     * Returns true if this field is mapped by the prototype's db mapping.
     */
    public boolean isMapped() {
        // Note: not sure if check for primitive or reference relation is really
        // needed, but we did it before, so we leave it in for safety.
        return isId || isPrototype || isName || 
               (relation != null && relation.isPrimitiveOrReference());
    }

    /**
     * Checks whether values for this column need to be quoted in insert/update
     * stmts
     * 
     * @return true if values need to be wrapped in quotes
     */
    public boolean needsQuotes() {
        switch (type) {
        case Types.CHAR:
        case Types.VARCHAR:
        case Types.LONGVARCHAR:
        case Types.BINARY:
        case Types.VARBINARY:
        case Types.LONGVARBINARY:
        case Types.DATE:
        case Types.TIME:
        case Types.TIMESTAMP:
            return true;
        default:
            return false;
        }
    }

}
