package foo.bar;

import junit.framework.TestCase;

public class DependOnPom extends TestCase {

	public static String whosDaddy() {
		return DependOnPom.class.getGenericSuperclass().toString();
	}
}
