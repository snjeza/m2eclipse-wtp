package foo.bar.core;

import junit.framework.TestCase;

public class NameFormatTest extends TestCase {

	public void testFormat() {
		assertNull(NameFormat.format(null));
		assertEquals("  ",NameFormat.format("  "));
		assertEquals("Hello World",NameFormat.format(" hello world "));
	}

}
