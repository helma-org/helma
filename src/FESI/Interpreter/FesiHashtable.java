/*
 * FesiHashtable, adapted from:
 * @(#)Hashtable.java    1.41 97/01/28
 * 
 * Copyright (c) 1995, 1996 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 * CopyrightVersion 1.1_beta
 *
 * Adapted by JM Lugrin for FESI
 * Synchronization removed (speed up, evaluator already synchronized)
 * Data type specific for object property list.
 * Externally provided hash code (to avoid recalculating the hash code.
 * Support the hidden and readonly properties (not enforced by this class).
 * Optimize compare for interned strings and poor hash function.
 * 
 */

package FESI.Interpreter;

import FESI.Data.ESValue;

import java.util.Enumeration;

/**
 * Hashtable collision list.
 */
class HashtableEntry {
    int hash;
    String key;
    ESValue value;
    HashtableEntry next;
    boolean hidden;
    boolean readonly;

    protected Object clone() {
        HashtableEntry entry = new HashtableEntry();
        entry.hash = hash;
        entry.key = key;
        entry.value = value;
        entry.hidden = hidden;
        entry.readonly = readonly;
        entry.next = (next != null) ? (HashtableEntry)next.clone() : null;
        return entry;
    }
}

/**
 * This class implements a hashtable, which maps keys to values. Any 
 * non-<code>null</code> object can be used as a key or as a value. 
 * <p>
 * To successfully store and retrieve objects from a hashtable, the 
 * objects used as keys must implement the <code>hashCode</code> 
 * method and the <code>equals</code> method. 
 * <p>
 * An instance of <code>Hashtable</code> has two parameters that 
 * affect its efficiency: its <i>capacity</i> and its <i>load 
 * factor</i>. The load factor should be between 0.0 and 1.0. When 
 * the number of entries in the hashtable exceeds the product of the 
 * load factor and the current capacity, the capacity is increased by 
 * calling the <code>rehash</code> method. Larger load factors use 
 * memory more efficiently, at the expense of larger expected time 
 * per lookup. 
 * <p>
 * If many entries are to be made into a <code>Hashtable</code>, 
 * creating it with a sufficiently large capacity may allow the 
 * entries to be inserted more efficiently than letting it perform 
 * automatic rehashing as needed to grow the table. 
 * <p>
 * This example creates a hashtable of numbers. It uses the names of 
 * the numbers as keys: 
 * <p><blockquote><pre>
 *     Hashtable numbers = new Hashtable();
 *     numbers.put("one", new Integer(1));
 *     numbers.put("two", new Integer(2));
 *     numbers.put("three", new Integer(3));
 * </pre></blockquote>
 * <p>
 * To retrieve a number, use the following code: 
 * <p><blockquote><pre>
 *     Integer n = (Integer)numbers.get("two");
 *     if (n != null) {
 *         System.out.println("two = " + n);
 *     }
 * </pre></blockquote>
 *
 * @author  Arthur van Hoff
 * @version 1.41, 01/28/97
 * @see     java.lang.Object#equals(java.lang.Object)
 * @see     java.lang.Object#hashCode()
 * @see     java.util.Hashtable#rehash()
 * @since   JDK1.0
 */
public
class FesiHashtable implements Cloneable {
    /**
     * The hash table data.
     */
    private transient HashtableEntry table[];

    /**
     * The total number of entries in the hash table.
     */
    private transient int count;

    /**
     * Rehashes the table when count exceeds this threshold.
     */
    private int threshold;

    /**
     * The load factor for the hashtable.
     */
    private float loadFactor;

    /**
     * Constructs a new, empty hashtable with the specified initial 
     * capacity and the specified load factor. 
     *
     * @param      initialCapacity   the initial capacity of the hashtable.
     * @param      loadFactor        a number between 0.0 and 1.0.
     * @exception  IllegalArgumentException  if the initial capacity is less
     *               than or equal to zero, or if the load factor is less than
     *               or equal to zero.
     * @since      JDK1.0
     */
    public FesiHashtable(int initialCapacity, float loadFactor) {
        if ((initialCapacity <= 0) || (loadFactor <= 0.0)) {
            throw new IllegalArgumentException();
        }
        this.loadFactor = loadFactor;
        table = new HashtableEntry[initialCapacity];
        threshold = (int)(initialCapacity * loadFactor);
    }

    /**
     * Constructs a new, empty hashtable with the specified initial capacity
     * and default load factor.
     *
     * @param   initialCapacity   the initial capacity of the hashtable.
     * @since   JDK1.0
     */
    public FesiHashtable(int initialCapacity) {
        this(initialCapacity, 0.75f);
    }

    /**
     * Constructs a new, empty hashtable with a default capacity and load
     * factor. 
     *
     * @since   JDK1.0
     */
    public FesiHashtable() {
        this(27, 0.75f); // a smaller prime than the original 27
    }

    /**
     * Returns the number of keys in this hashtable.
     *
     * @return  the number of keys in this hashtable.
     * @since   JDK1.0
     */
    public int size() {
        return count;
    }

    /**
     * Tests if this hashtable maps no keys to values.
     *
     * @return  <code>true</code> if this hashtable maps no keys to values;
     *          <code>false</code> otherwise.
     * @since   JDK1.0
     */
    public boolean isEmpty() {
        return count == 0;
    }

    /**
     * Returns an enumeration of the keys in this hashtable.
     *
     * @return  an enumeration of the keys in this hashtable.
     * @see     java.util.Enumeration
     * @see     java.util.Hashtable#elements()
     * @since   JDK1.0
     */
    public Enumeration keys() {
        return new HashtableEnumerator(table,true);
    }

    /**
     * Returns an enumeration of the values in this hashtable.
     * Use the Enumeration methods on the returned object to fetch the elements
     * sequentially.
     *
     * @return  an enumeration of the values in this hashtable.
     * @see     java.util.Enumeration
     * @see     java.util.Hashtable#keys()
     * @since   JDK1.0
     */
    public Enumeration elements() {
        return new HashtableEnumerator(table, false);
    }

    /**
     * Tests if the specified object is a key in this hashtable.
     * 
     * @param   key   possible key.
     * @return  <code>true</code> if the specified object is a key in this
     *          hashtable; <code>false</code> otherwise.
     */

    public boolean containsKey(String key, int hash) {
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.key==key ) || ((e.hash == hash) && e.key.equals(key))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the value to which the specified key is mapped in this hashtable.
     *
     * @param   key   a key in the hashtable.
     * @param   hash  the key hashtable.
     * @return  the value to which the key is mapped in this hashtable;
     *          <code>null</code> if the key is not mapped to any value in
     *          this hashtable.
     */
    
    public ESValue get(String key, int hash) {
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.key==key ) || ((e.hash == hash) && e.key.equals(key))) {
                return e.value;
            }
        }
        return null;
    }

    /**
     * Check if a property is hidden (return false if not present).
     *
     * @param   key   a key in the hashtable.
     * @param   hash  the key hashtable.
     * @return  true if hidden.
     */
    
    public boolean isHidden(String key, int hash) {
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.key==key ) || ((e.hash == hash) && e.key.equals(key))) {
                return e.hidden;
            }
        }
        return false;
    }


    /**
     * Check if a property is readonly (return false if not present).
     *
     * @param   key   a key in the hashtable.
     * @param   hash  the key hashtable.
     * @return  true if hidden.
     */
    
    public boolean isReadonly(String key, int hash) {
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.key==key ) || ((e.hash == hash) && e.key.equals(key))) {
                return e.readonly;
            }
        }
        return false;
    }

    /**
     * Rehashes the contents of the hashtable into a hashtable with a 
     * larger capacity. This method is called automatically when the 
     * number of keys in the hashtable exceeds this hashtable's capacity 
     * and load factor. 
     *
     * @since   JDK1.0
     */
    protected void rehash() {
        int oldCapacity = table.length;
        HashtableEntry oldTable[] = table;
    
        int newCapacity = oldCapacity * 2 + 1;
        if (newCapacity<101) newCapacity = 101; // Ensure a prime
        HashtableEntry newTable[] = new HashtableEntry[newCapacity];
    
        threshold = (int)(newCapacity * loadFactor);
        table = newTable;
    
        //System.out.println("rehash old=" + oldCapacity + ", new=" + newCapacity + ", thresh=" + threshold + ", count=" + count);
    
        for (int i = oldCapacity ; i-- > 0 ;) {
            for (HashtableEntry old = oldTable[i] ; old != null ; ) {
            HashtableEntry e = old;
            old = old.next;
    
            int index = (e.hash & 0x7FFFFFFF) % newCapacity;
            e.next = newTable[index];
            newTable[index] = e;
            }
        }
    }

    /**
     * Maps the specified <code>key</code> to the specified 
     * <code>value</code> in this hashtable. Neither the key nor the 
     * value can be <code>null</code>. 
     * <p>
     * The value can be retrieved by calling the <code>get</code> method 
     * with a key that is equal to the original key. 
     *
     * @param      key     the hashtable key.
     * @param      hash    the hash value.
     * @param      hidden  true if the entry must not be enumerated.
     * @param      readonly  true if the entry must not be deleted.
     * @param      value   the value.
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one.
     * @exception  NullPointerException  if the key or value is
     *               <code>null</code>.
     * @see     java.lang.Object#equals(java.lang.Object)
     * @see     java.util.Hashtable#get(java.lang.Object)
     * @since   JDK1.0
     */
    public ESValue put(String key, int hash, boolean hidden, boolean readonly, ESValue value) {
    // Make sure the value is not null
        if (value == null) {
            throw new NullPointerException("value");
        }
    
        // Makes sure the key is not already in the hashtable.
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index] ; e != null ; e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
            ESValue old = e.value;
            e.value = value;
            return old;
            }
        }
    
        if (count >= threshold) {
            // Rehash the table if the threshold is exceeded
            rehash();
            return put(key, hash, hidden, readonly, value);
        } 
    
        // Creates the new entry.
        HashtableEntry e = new HashtableEntry();
        e.hash = hash;
        e.key = key;
        e.value = value;
        e.hidden = hidden;
        e.readonly = readonly;
        e.next = tab[index];
        tab[index] = e;
        count++;
        return null;
    }

    /**
     * Removes the key (and its corresponding value) from this 
     * hashtable. This method does nothing if the key is not in the hashtable.
     *
     * @param   key   the key that needs to be removed.
     * @return  the value to which the key had been mapped in this hashtable,
     *          or <code>null</code> if the key did not have a mapping.
     * @since   JDK1.0
     */
    public ESValue remove(String key, int hash) {
        HashtableEntry tab[] = table;
        int index = (hash & 0x7FFFFFFF) % tab.length;
        for (HashtableEntry e = tab[index], prev = null ; e != null ; prev = e, e = e.next) {
            if ((e.hash == hash) && e.key.equals(key)) {
            if (prev != null) {
                prev.next = e.next;
            } else {
                tab[index] = e.next;
            }
            count--;
            return e.value;
            }
        }
        return null;
    }

    /**
     * Clears this hashtable so that it contains no keys. 
     *
     * @since   JDK1.0
     */
    public void clear() {
        HashtableEntry tab[] = table;
        for (int index = tab.length; --index >= 0; )
            tab[index] = null;
        count = 0;
    }

    /**
     * Creates a shallow copy of this hashtable. The keys and values 
     * themselves are not cloned. 
     * This is a relatively expensive operation.
     *
     * @return  a clone of the hashtable.
     * @since   JDK1.0
     */
    public Object clone() {
        try { 
            FesiHashtable t = (FesiHashtable)super.clone();
            t.table = new HashtableEntry[table.length];
            for (int i = table.length ; i-- > 0 ; ) {
            t.table[i] = (table[i] != null) 
                ? (HashtableEntry)table[i].clone() : null;
            }
            return t;
        } catch (CloneNotSupportedException e) { 
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * Returns a rather long string representation of this hashtable.
     *
     * @return  a string representation of this hashtable.
     * @since   JDK1.0
     */
    public String toString() {
        int max = size() - 1;
        StringBuffer buf = new StringBuffer();
        Enumeration k = keys();
        Enumeration e = elements();
        buf.append("{");
    
        for (int i = 0; i <= max; i++) {
            String s1 = k.nextElement().toString();
            String s2 = e.nextElement().toString();
            buf.append(s1 + "=" + s2);
            if (i < max) {
            buf.append(", ");
            }
        }
        buf.append("}");
        return buf.toString();
    }
}


/**
 * A hashtable enumerator class.  This class should remain opaque 
 * to the client. It will use the Enumeration interface. 
 */
class HashtableEnumerator implements Enumeration {
    boolean keys;
    int index;
    HashtableEntry table[];
    HashtableEntry entry;

    HashtableEnumerator(HashtableEntry table[], boolean keys) {
        this.table = table;
        this.keys = keys;
        this.index = table.length;
    }
    
    public boolean hasMoreElements() {
        if (entry != null) {
            return true;
        }
        while (index-- > 0) {
            if ((entry = table[index]) != null) {
            return true;
            }
        }
        return false;
    }

    public Object nextElement() {
        if (entry == null) {
            while ((index-- > 0) && ((entry = table[index]) == null));
        }
        if (entry != null) {
            HashtableEntry e = entry;
            entry = e.next;
            return keys ? ((Object) e.key) : ((Object) e.value);
        }
        throw new java.util.NoSuchElementException("FesiHashtableEnumerator");
    }
}