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

package helma.scripting;

import helma.framework.repository.Resource;
import helma.framework.repository.Resource;
import helma.util.Updatable;

/**
 * Represents an updatable that was read from some resource
 */
public interface ScriptingResource extends Updatable {

    /**
     * Returns the resource updatable was read from
     * @return resource updatable was read from
     */
    public Resource getResource();

}
