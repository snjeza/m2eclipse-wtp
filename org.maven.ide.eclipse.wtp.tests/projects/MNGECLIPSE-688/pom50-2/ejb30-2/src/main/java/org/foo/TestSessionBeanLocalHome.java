package org.foo;
import java.rmi.RemoteException;

import javax.ejb.CreateException;
import javax.ejb.EJBLocalHome;

public interface TestSessionBeanLocalHome extends EJBLocalHome {

	public TestSessionBeanLocalComponent create() throws CreateException, RemoteException;
	
}
