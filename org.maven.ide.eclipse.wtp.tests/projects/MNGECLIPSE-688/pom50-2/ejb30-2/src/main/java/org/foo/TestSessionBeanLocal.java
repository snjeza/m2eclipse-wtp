package org.foo;
import javax.ejb.Local;

@Local
public interface TestSessionBeanLocal {

	String test();
	
}
