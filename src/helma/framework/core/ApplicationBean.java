package helma.framework.core;

import java.io.Serializable;
import java.util.Date;

public class ApplicationBean implements Serializable {

	Application app;

	public ApplicationBean(Application app)	{
		this.app = app;
	}

	public Date getupSince () {
		return new Date (app.starttime);
	}

	public long getrequestCount ()	{
		return app.getRequestCount ();
	}

	public long getxmlrpcCount ()	{
		return app.getXmlrpcCount ();
	}

	public long geterrorCount () {
		return app.getErrorCount ();
	}

	public Application get__app__ () {
		return app;
	}

	public void clearCache () {
	}

}



