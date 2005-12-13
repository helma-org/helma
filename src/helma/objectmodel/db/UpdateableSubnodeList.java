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

import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;

public class UpdateableSubnodeList extends OrderedSubnodeList {

    // the update-criteria-fields
    private final String updateCriteria[];
    // the corresponding property to this update-criteria
    private final String updateProperty[];
    // records to fetch from the db will have a lower value?
    private final boolean updateTypeDesc[];
    
    // arrays representing the current borders for each update-criteria
    private Object highestValues[]=null;
    private Object lowestValues[]=null;

    /**
     * Construct a new UpdateableSubnodeList. The Relation is needed
     * to get the information about the ORDERING and the UPDATECriteriaS
     */
    public UpdateableSubnodeList (Relation rel) {
        super(rel);
        // check the update-criterias for updating this collection
        if (rel.updateCriteria == null) {
            // criteria-field muss vom criteria-operant getrennt werden
            // damit das update-criteria-rendering gut funktioniert
            updateCriteria = new String[1];
            updateCriteria[0]=rel.otherType.getIDField();
            updateProperty=null;
            updateTypeDesc = new boolean[1];
            updateTypeDesc[0] = false;
            highestValues = new Object[1];
            lowestValues = new Object[1];
        } else {
            String singleCriterias[] = rel.updateCriteria.split(",");
            updateCriteria = new String[singleCriterias.length];
            updateProperty = new String[singleCriterias.length];
            updateTypeDesc = new boolean[singleCriterias.length];
            highestValues = new Object[singleCriterias.length];
            lowestValues = new Object[singleCriterias.length];
            for (int i = 0; i < singleCriterias.length; i++) {
                parseCriteria (i, singleCriterias[i]);
            }
        }
    }

    /**
     * Utility-method for parsing criterias for updating this collection.
     */
    private void parseCriteria (int idx, String criteria) {
        String criteriaParts[] = criteria.trim().split(" ");
        updateCriteria[idx]=criteriaParts[0].trim();
        updateProperty[idx] = rel.otherType.columnNameToProperty(updateCriteria[idx]);
        if (updateProperty[idx] == null 
                && !updateCriteria[idx].equalsIgnoreCase(rel.otherType.getIDField())) {
            throw new RuntimeException("updateCriteria has to be mapped as Property of this Prototype (" + updateCriteria[idx] + ")");
        }
        if (criteriaParts.length < 2) {
            // default to INCREASING or to BIDIRECTIONAL?
            updateTypeDesc[idx]=false;
            return;
        }
        String direction = criteriaParts[1].trim().toLowerCase();
        if ("desc".equals(direction)) {
            updateTypeDesc[idx]=true;
        } else {
            updateTypeDesc[idx]=false;
        }
    }
    
    /**
     * render the criterias for fetching new nodes, which have been added to the
     * relational db.
     * @return the sql-criteria for updating this subnodelist
     * @throws ClassNotFoundException @see helma.objectmodel.db.DbMapping#getColumn(String)
     * @throws SQLException @see helma.objectmodel.db.DbMapping#getColumn @see helma.objectmodel.db.DbMapping#needsQuotes(String)
     */
    public String getUpdateCriteria() throws SQLException, ClassNotFoundException {
        StringBuffer criteria = new StringBuffer ();
        for (int i = 0; i < updateCriteria.length; i++) {
            // if we don't know the borders ignore this criteria
            if (!updateTypeDesc[i] && highestValues[i]==null)
                continue;
            if (updateTypeDesc[i]  && lowestValues[i]==null)
                continue;
            if (criteria.length() > 0) {
                criteria.append (" OR ");
            }
            renderUpdateCriteria(i, criteria);
        }
        if (criteria.length() < 1)
            return null;
        criteria.insert(0, "(");
        criteria.append(")");
        return criteria.toString();
    }

    /**
     * Render the current updatecriteria specified by idx and add the result to the given
     * StringBuffer (sb).
     * @param idx index of the current updatecriteria
     * @param sb the StringBuffer to append to
     * @throws ClassNotFoundException @see helma.objectmodel.db.DbMapping#getColumn(String)
     * @throws SQLException @see helma.objectmodel.db.DbMapping#getColumn @see helma.objectmodel.db.DbMapping#needsQuotes(String)
     */
    private void renderUpdateCriteria(int idx, StringBuffer sb) throws SQLException, ClassNotFoundException {
        if (!updateTypeDesc[idx]) {
            sb.append(updateCriteria[idx]);
            sb.append(" > ");
            renderValue(idx, highestValues, sb);
        } else {
            sb.append(updateCriteria[idx]);
            sb.append(" < ");
            renderValue(idx, lowestValues, sb);
        }
    }

    /**
     * Renders the value contained inside the given Array (values) at the given 
     * index (idx) depending on it's SQL-Type and add the result to the given 
     * StringBuffer (sb).
     * @param idx index-position of the value to render
     * @param values the values-array to operate on
     * @param sb the StringBuffer to append to
     * @throws ClassNotFoundException @see helma.objectmodel.db.DbMapping#getColumn(String)
     * @throws SQLException @see helma.objectmodel.db.DbMapping#getColumn @see helma.objectmodel.db.DbMapping#needsQuotes(String)
     */
    private void renderValue(int idx, Object[] values, StringBuffer sb) throws SQLException, ClassNotFoundException {
        DbColumn dbc = rel.otherType.getColumn(updateCriteria[idx]);
        if (rel.otherType.getIDField().equalsIgnoreCase(updateCriteria[idx])) {
            if (rel.otherType.needsQuotes(updateCriteria[idx])) {
                sb.append ("'").append (values[idx]).append("'");
            } else {
                sb.append (values[idx]);
            }
            return;
        }
        Property p = (Property) values[idx];
        String strgVal = p.getStringValue();

        switch (dbc.getType()) {
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP:
                // use SQL Escape Sequences for JDBC Timestamps 
                // (http://www.oreilly.com/catalog/jentnut2/chapter/ch02.html)
                Timestamp ts = p.getTimestampValue();
                sb.append ("{ts '");
                sb.append (ts.toString());
                sb.append ("'}");
                return;

            case Types.BIT:
            case Types.TINYINT:
            case Types.BIGINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.REAL:
            case Types.FLOAT:
            case Types.DOUBLE:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.VARBINARY:
            case Types.BINARY:
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
            case Types.CHAR:
            case Types.VARCHAR:
            case Types.OTHER:
            case Types.NULL:
            case Types.CLOB:
            default:
                if (rel.otherType.needsQuotes(updateCriteria[idx])) {
                    sb.append ("'").append (strgVal).append ("'");
                } else {
                    sb.append (strgVal);
                }
        }
    }

    /**
     * add a new node honoring the Nodes SQL-Order and check if the borders for 
     * the updateCriterias have changed by adding this node
     * @param obj the object to add
     */
    public boolean add(Object obj) {
        // we do not have a SQL-Order and add this node on top of the list
        NodeHandle nh = (NodeHandle) obj;
        updateBorders(nh);
        return super.add(nh);
    }

    /**
     * add a new node at the given index position and check if the 
     * borders for the updateCriterias have changed
     * NOTE: this overrules the ordering (should we disallowe this?)
     * @param idx the index-position this node should be added at
     * @param obj the NodeHandle of the node which should be added
     */
    public void add (int idx, Object obj) {
        NodeHandle nh = (NodeHandle) obj;
        super.add(idx, nh);
        updateBorders(nh);
    }

    /**
     * Check if the currently added node changes the borers for the updateCriterias and
     * update them if neccessary.
     * @param nh the added Node
     */
    private void updateBorders(NodeHandle nh) {
        Node node=null;
        for (int i = 0; i < updateCriteria.length; i++) {
            node=updateBorder(i, nh, node);
        }
    }
    
    /**
     * Check if the given NodeHandle's node is outside of the criteria's border
     * having the given index-position (idx) and update the border if neccessary.
     * @param idx the index-position
     * @param nh the NodeHandle possible changing the border
     * @param node optional the node handled by this NodeHandler
     * @return The Node given or the Node retrieved of this NodeHandle
     */
    private Node updateBorder(int idx, NodeHandle nh, Node node) {
        String cret = updateCriteria[idx];
        if (rel.otherType.getIDField().equals(cret)) {
            String nid = nh.getID();
            if (updateTypeDesc[idx]
                    && OrderedSubnodeList.compareNumericString(nid, (String) lowestValues[idx]) < 0) {
                lowestValues[idx]=nid;
            } else if (!updateTypeDesc[idx]
                    && OrderedSubnodeList.compareNumericString(nid, (String) highestValues[idx]) > 0) {
                highestValues[idx]=nid;
            }
        } else {
            if (node == null)
                node = nh.getNode(rel.otherType.getWrappedNodeManager());
            Property np = node.getProperty(
                    rel.otherType.columnNameToProperty(cret));
            if (updateTypeDesc[idx]) {
                Property lp = (Property) lowestValues[idx];
                if (lp==null || np.compareTo(lp)<0)
                    lowestValues[idx]=np;
            } else {
                Property hp = (Property) highestValues[idx];
                if (hp==null || np.compareTo(hp)>0)
                    highestValues[idx]=np;
            }
        }
        return node;
    }

    /**
     * First check which borders this node will change and rebuild
     * these if neccessary.
     * @param nh the NodeHandle of the removed Node
     */
    private void rebuildBorders(NodeHandle nh) {
        boolean[] check = new boolean[updateCriteria.length];
        Node node = nh.getNode(rel.otherType.getWrappedNodeManager());
        for (int i = 0; i < updateCriteria.length; i++) {
            String cret = updateCriteria[i];
            if (cret.equals(rel.otherType.getIDField())) {
                check[i] = (updateTypeDesc[i]
                                && nh.getID().equals(lowestValues[i]))
                        || (!updateTypeDesc[i]
                                && nh.getID().equals(highestValues[i]));
            } else {
                Property p = node.getProperty(updateProperty[i]);
                check[i] = (updateTypeDesc[i]
                                && p.equals(lowestValues[i]))
                        || (!updateTypeDesc[i]
                                && p.equals(highestValues[i]));
            }
        }
        for (int i = 0; i < updateCriteria.length; i++) {
            if (!check[i])
                continue;
            rebuildBorder(i);
        }
    }

    /**
     * Rebuild all borders for all the updateCriterias
     */
    public void rebuildBorders() {
        for (int i = 0; i < updateCriteria.length; i++) {
            rebuildBorder(i);
        }
    }

    /**
     * Only rebuild the border for the update-criteria specified by the given
     * index-position (idx).
     * @param idx the index-position of the updateCriteria to rebuild the border for
     */
    private void rebuildBorder(int idx) {
        if (updateTypeDesc[idx]) {
            lowestValues[idx]=null;
        } else {
            highestValues[idx]=null;
        }
        for (int i = 0; i < this.size(); i++) {
            updateBorder(idx, (NodeHandle) this.get(i), null);
        }
    }

    /**
     * remove the object specified by the given index-position
     * and update the borders if neccesary
     * @param idx the index-position of the NodeHandle to remove
     */
    public Object remove (int idx) {
        Object obj = super.remove(idx);
        if (obj == null)
            return null;
        rebuildBorders((NodeHandle) obj);
        return obj;
    }

    /**
     * remove the given Object from this List and update the borders if neccesary
     * @param obj the NodeHandle to remove
     */
    public boolean remove (Object obj) {
        if (!super.remove(obj))
            return false;
        rebuildBorders((NodeHandle) obj);
        return true;
    }

    /**
     * remove all elements conteined inside the specified collection
     * from this List and update the borders if neccesary
     * @param c the Collection containing all Objects to remove from this List
     * @return true if the List has been modified
     */
    public boolean removeAll(Collection c) {
        if (!super.removeAll(c))
            return false;
        for (Iterator i = c.iterator(); i.hasNext(); )
            rebuildBorders((NodeHandle) i.next());
        return true;
    }

    /**
     * remove all elements from this List, which are NOT specified
     * inside the specified Collecion and update the borders if neccesary
     * @param c the Collection containing all Objects to keep on the List 
     * @return true if the List has been modified
     */
    public boolean retainAll (Collection c) {
        if (!super.retainAll(c))
            return false;
        rebuildBorders();
        return true;
    }

    /**
     * checks if the borders have to be rebuilt because of the removed or 
     * the added NodeHandle.
     */
    public Object set(int idx, Object obj) {
        Object prevObj = super.set(idx, obj);
        rebuildBorders((NodeHandle) prevObj);
        updateBorders((NodeHandle) obj);
        return prevObj;
    }

    /**
     * if the wrapped List is an instance of OrderedSubnodeList,
     * the sortIn-method will be used.
     */
    public boolean addAll(Collection col) {
        return sortIn(col, true) > 0;
    }
}
