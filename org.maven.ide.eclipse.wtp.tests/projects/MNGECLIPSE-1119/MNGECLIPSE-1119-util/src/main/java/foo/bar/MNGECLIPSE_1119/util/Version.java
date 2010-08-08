package foo.bar.MNGECLIPSE_1119.util;

import java.util.ResourceBundle;

/**
 * Class used to read the project version from a maven filtered resource bundle. 
 */
public class Version {
	
	private static ResourceBundle BUNDLE = ResourceBundle.getBundle("MNGECLIPSE-1119-util");
	
	public static String VALUE = BUNDLE.getString("version"); 
}
