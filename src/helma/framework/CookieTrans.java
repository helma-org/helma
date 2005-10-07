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

package helma.framework;

import java.io.Serializable;
import javax.servlet.http.Cookie;

/**
 *  Cookie Transmitter. A simple, serializable representation
 *  of an HTTP cookie.
 */
public final class CookieTrans implements Serializable {
    String name;
    String value;
    String path;
    String domain;
    int days = -1;

    CookieTrans(String name, String value) {
        this.name = name;
        this.value = value;
    }

    void setValue(String value) {
        this.value = value;
    }

    void setDays(int days) {
        this.days = days;
    }

    void setPath(String path) {
        this.path = path;
    }

    void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getValue() {
        return value;
    }

    /**
     *
     *
     * @return ...
     */
    public int getDays() {
        return days;
    }

    /**
     *
     *
     * @return ...
     */
    public String getPath() {
        return path;
    }

    /**
     *
     *
     * @return ...
     */
    public String getDomain() {
        return domain;
    }

    /**
     *
     *
     * @param defaultPath ...
     * @param defaultDomain ...
     *
     * @return ...
     */
    public Cookie getCookie(String defaultPath, String defaultDomain) {
        Cookie c = new Cookie(name, value);

        // NOTE: If cookie version is set to 1, cookie values will be quoted.
        // c.setVersion(1);

        if (days > -1) {
            // Cookie time to live, days -> seconds
            c.setMaxAge(days * 60 * 60 * 24);
        }

        if (path != null) {
            c.setPath(path);
        } else if (defaultPath != null) {
            c.setPath(defaultPath);
        }

        if (domain != null) {
            c.setDomain(domain);
        } else if (defaultDomain != null) {
            c.setDomain(defaultDomain);
        }

        return c;
    }
}
