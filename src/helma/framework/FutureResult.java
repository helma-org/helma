/*
 *  Copyright 2006 Hannes Wallnoefer <hannes@helma.at>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
