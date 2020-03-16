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

package helma.scripting.rhino;

import java.util.Set;

/**
 * An interface that allows us to keep track of changed properties in
 * JavaScript objects. This is used when compiling prototypes in order
 * to be able to remove properties from prototypes that haven't been
 * renewed in the compilation step.
 */
public interface PropertyRecorder {

    /**
     * Tell this PropertyRecorder to start recording changes to properties
     */
    public void startRecording();

    /**
     * Tell this PropertyRecorder to stop recording changes to properties
     */
    public void stopRecording();

    /**
     * Returns a set containing the names of properties changed since
     * the last time startRecording() was called.
     *
     * @return a Set containing the names of changed properties
     */
    public Set getChangeSet();

    /**
     * Clear the set of changed properties.
     */
    public void clearChangeSet();
}
