package foo.bar.core;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

public class NameFormat {

	public static String format(String name) {
		return (StringUtils.isBlank(name))?name:WordUtils.capitalize(name.trim());
	}
	
}
