package helma.framework;

import java.io.Serializable;
import java.util.Map;

import helma.framework.core.Application;

public class ResponseBean implements Serializable {

    ResponseTrans res;

    public ResponseBean(ResponseTrans res)	{
	this.res = res;
    }

    public void encode (Object what) {
	res.encode (what);
    }

    public void encodeXml (Object what) {
	res.encodeXml (what);
    }

    public void format (Object what) {
	res.format (what);
    }

    public void pushStringBuffer  () {
	res.pushStringBuffer ();
    }

    public String popStringBuffer () {
	return res.popStringBuffer ();
    }

    public void redirect (String url) throws RedirectException {
	res.redirect (url);
    }

    public void reset () {
	res.reset ();
    }

    public void setCookie (String key, String value) {
	res.setCookie (key, value, -1);
    }

    public void setCookie (String key, String value, int days) {
	res.setCookie (key, value, days);
    }

    public void write (Object what) {
	res.write (what);
    }

    public void writeln (Object what) {
	res.writeln (what);
    }

    public void writeBinary (byte[] what) {
	res.writeBinary (what);
    }

    public String toString() {
	return "[Response]";
    }


	// property-related methods:

    public boolean getcache () {
	return res.cache;
    }

    public void setcache (boolean cache) {
	res.cache = cache;
    }

    public String getcharset () {
	return res.charset;
    }

    public void setcharset (String charset) {
	res.charset = charset;
    }

    public String getcontentType () {
	return res.contentType;
    }

    public void setcontentType (String contentType) {
	res.contentType = contentType;
    }

    public Map getdata () {
	return res.getResponseData ();
    }

    public String geterror () {
	return res.error;
    }

    public String getmessage () {
	return res.message;
    }

    public void setmessage (String message) {
	res.message = message;
    }

    public String getrealm () {
	return res.realm;
    }

    public void setrealm (String realm) {
	res.realm = realm;
    }

    public void setskinpath (Object[] arr) {
	res.setTranslatedSkinpath (arr);
    }

    public Object[] getskinpath () {
	return res.getTranslatedSkinpath ();
    }

    public int getstatus () {
	return res.status;
    }

    public void setstatus (int status) {
	res.status = status;
    }

}



