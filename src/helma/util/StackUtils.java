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

package helma.util;

import java.util.List;
import java.util.ArrayList;

/**
 * Utility class to extract pure JavaScript stack trace from Java exceptions
 */
public class StackUtils {

    /**
     * Extract the JavaScript stack trace element from the source exception
     * and copy them over to the target exception.
     * @param source the source exception
     * @param target the target excepiton
     */
    public static void setJavaScriptStack(Throwable source, Throwable target) {
        List list = new ArrayList();
        StackTraceElement[] stack = source.getStackTrace();
        for (int i = 0; i < stack.length; i++) {
            StackTraceElement e = stack[i];
            String name = e.getFileName();
            if (e.getLineNumber() > -1 &&
                    (name.endsWith(".js") || name.endsWith(".hac"))) {
                list.add(e);
            }
        }
        target.setStackTrace((StackTraceElement[]) list.toArray(new StackTraceElement[list.size()]));
    }

}
