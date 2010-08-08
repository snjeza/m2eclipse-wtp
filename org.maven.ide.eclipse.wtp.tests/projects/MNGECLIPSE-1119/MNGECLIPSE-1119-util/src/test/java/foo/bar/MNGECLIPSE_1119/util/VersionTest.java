package foo.bar.MNGECLIPSE_1119.util;

import org.apache.log4j.Logger;
import org.junit.Test;
import static org.junit.Assert.*;


public class VersionTest {

	private static Logger LOG = Logger.getLogger(VersionTest.class);
	
	@Test
	public void testVersion()
	{
		try {
			LOG.debug("Version from filtered properties :"+ Version.VALUE);
			assertFalse("MNGECLIPSE-1119-util.properties is not filtered", "${app.version}".equals(Version.VALUE));
		} catch (ExceptionInInitializerError e) {
			LOG.error("MNGECLIPSE-1119-util.properties could not be read");
			fail("Filtering is buggy under M2Eclipse, try cleaning the project or doing a 'touch' on MNGECLIPSE-1119-util.properties");
		}
	}
}
