/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework;

/**
 * A handle for an asynchronous request execution. This allows to wait for
 * request termination, get the result or the exception of the execution.
 */
public interface FutureResult {
    /**
     * Get the result of the execution. If the execution is still active,
     * or if the invocation threw an exception, this method immediately returns null.
     * @return the result, or null
     */
    Object getResult();

    /**
     * Get the exception of the execution, if one was thrown. If the execution
     * is still active, or if no exception was thrown, this method immediately returns null.
     * @return the exception, or null
     */
    Exception getException();

    /**
     * Returns true if the execution is still active, and false if not.
     * @return true if the execution is still active
     */
    boolean getRunning();

    /**
     * Wait for execution to terminat, returning the execution result, if one is available.
     * @return the execution result, or null
     * @throws InterruptedException if we were interrupted by some other thread
     */
    Object waitForResult() throws InterruptedException;

    /**
     * Wait for a specific ammount of thime for the execution to terminate, returning
     * the execution result, if one is available.
     * @param timeout the number of milliseconds to wait
     * @return the execution result, or null
     * @throws InterruptedException if we were interrupted by some other thread
     */
    Object waitForResult(long timeout) throws InterruptedException;
}
