// CookieTrans.java

package helma.framework;

import java.io.Serializable;
import java.util.HashMap;
import javax.servlet.http.Cookie;

/**
 *  Cookie Transmitter. A simple, serializable representation 
 *  of an HTTP cookie.
 */
public final class CookieTrans implements Serializable {

    String name, value, path, domain;
    int days;

    CookieTrans (String name, String value) {
	this.name = name;
	this.value = value;
    }

    void setValue (String value) {
	this.value = value;
    }

    void setDays (int days) {
	this.days = days;
    }

    void setPath (String path) {
	this.path = path;
    }

    void setDomain (String domain) {
	this.domain = domain;
    }

    public String getName() {
	return name;
    }

    public String getValue() {
	return value;
    }

    public int getDays() {
	return days;
    }

    public String getPath () {
	return path;
    }

    public String getDomain () {
	return domain;
    }

    public Cookie getCookie (String defaultPath, String defaultDomain) {
	Cookie c = new Cookie (name, value);
	if (days > 0)
	    // Cookie time to live, days -> seconds
	    c.setMaxAge (days*60*60*24);
	if (path != null)
	    c.setPath (path);
	else if (defaultPath != null)
	    c.setPath (defaultPath);
	if (domain != null)
	    c.setDomain (domain);
	else if (defaultDomain != null)
	    c.setDomain (defaultDomain);
	return c;
    }
}
