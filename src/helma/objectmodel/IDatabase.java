// IDatabase.java

package helma.objectmodel;

import helma.objectmodel.db.IDGenerator;
import helma.objectmodel.INode;

/**
 * Interface that is implemented by Database wrappers
 */
 
public interface IDatabase {

	// db-related
    public void shutdown ();

	// id-related
	public String      nextID();
    public IDGenerator getIDGenerator  (ITransaction transaction) throws Exception;
	public void        saveIDGenerator (ITransaction transaction, IDGenerator idgen) throws Exception;

	// node-related
    public INode getNode    (ITransaction transaction, String key) throws Exception;
    public void  saveNode   (ITransaction transaction, String key, INode node) throws Exception;
    public void  deleteNode (ITransaction transaction, String key) throws Exception;

	// transaction-related
    public ITransaction beginTransaction  ();
    public void         commitTransaction (ITransaction transaction) throws DatabaseException;
    public void         abortTransaction  (ITransaction transaction) throws DatabaseException;

}



