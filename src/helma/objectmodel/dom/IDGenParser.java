package helma.objectmodel.dom;

import java.io.*;
import java.util.Date;
import org.w3c.dom.*;

import helma.objectmodel.ObjectNotFoundException;
import helma.objectmodel.db.IDGenerator;

public class IDGenParser	{

    public static IDGenerator getIDGenerator (File file) throws ObjectNotFoundException	{
		if ( ! file.exists() )
		    throw new ObjectNotFoundException ("IDGenerator not found in idgen.xml");
		try	{
			Document document = XmlUtil.parse(new FileInputStream (file));
			org.w3c.dom.Element tmp = (Element)document.getDocumentElement().getElementsByTagName("counter").item(0);
			return new IDGenerator( Long.parseLong (XmlUtil.getTextContent(tmp)) );
		}	catch (Exception e)		{
			throw new ObjectNotFoundException(e.toString());
		}
    }

	public static IDGenerator saveIDGenerator (IDGenerator idgen, File file) throws Exception	{
		OutputStreamWriter out = new OutputStreamWriter (new  FileOutputStream (file));
		out.write ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		out.write ("<!-- printed by helma object publisher     -->\n");
		out.write ("<!-- created " + (new Date()).toString() + " -->\n" );
		out.write ("<xmlroot>\n");
		out.write ("  <counter>" + idgen.getValue() + "</counter>\n");
		out.write ("</xmlroot>\n");
		out.close (); 
		return idgen;
	}
	
}

