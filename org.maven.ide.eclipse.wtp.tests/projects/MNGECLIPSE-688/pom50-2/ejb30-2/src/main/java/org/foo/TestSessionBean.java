package org.foo;

import javax.ejb.Stateless;

import org.jboss.annotation.ejb.LocalBinding;

/**
 * Session Bean implementation class TestSessionBean
 */
@Stateless(name = "TestSessionBean")
// @LocalHome(TestSessionBeanLocalHome.class)
@LocalBinding(jndiBinding="custom/TestSession")
public class TestSessionBean implements TestSessionBeanLocal {

	/**
	 * Default constructor.
	 */
	public TestSessionBean() {
	}

	public String test() {
		FooUtil fooUtil = new FooUtil();
		return fooUtil.hello();
	}

}
