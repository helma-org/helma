// IProperty.java
// Copyright (c) Hannes Wallnöfer 1997-2000

package helma.objectmodel;

import java.util.Date;

/**
 * Interface that is implemented by node properties.
 */
 
public interface IProperty {

    public static final int STRING = 1;
    public static final int BOOLEAN = 2;
    public static final int DATE = 3;
    public static final int INTEGER = 4;
    public static final int FLOAT = 5;
    public static final int NODE = 6;
    public static final int JAVAOBJECT = 7;

    public String getName ();
    public int getType ();
    public Object getValue ();

    public INode getNodeValue ();
    public String getStringValue ();
    public boolean getBooleanValue ();
    public long getIntegerValue ();
    public double getFloatValue ();
    public Date getDateValue ();
    public Object getJavaObjectValue ();

  }