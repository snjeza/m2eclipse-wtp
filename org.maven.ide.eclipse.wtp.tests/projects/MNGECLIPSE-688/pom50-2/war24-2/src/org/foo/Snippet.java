package org.foo;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Snippet {

	public static String test() throws NamingException {
		InitialContext ctx = new InitialContext();
		
		TestSessionBeanLocal test = (TestSessionBeanLocal) ctx
		        // .lookup("TestSessionBean/local");
				// .lookup("org/foo/TestSessionBeanLocal");
		        .lookup("custom/TestSession");
		
		return test.test();
	}
}
