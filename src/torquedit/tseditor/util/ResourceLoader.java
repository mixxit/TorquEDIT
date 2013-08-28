package torquedit.tseditor.util;

import java.io.InputStream;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

/**
 * Utility class for loading resources
 */
public abstract class ResourceLoader {
	
	/**
	 * Loads an xml resource
	 * @param resource
	 * @return
	 */
	public static List<?> loadXmlResource(String resource) {
		XmlResourceParser parser = new XmlResourceParser();
		return parser.parse(loadResource(resource));
	}
	
	/**
	 * Loads an image resource
	 * @param resource
	 * @return
	 */
	public static Image loadImageResource(String resource) {
		return new Image(Display.getCurrent(), loadResource(resource));
	}
	
	/**
	 * Loads a raw resources
	 * @param resource
	 * @return
	 */
	public static InputStream loadResource(String resource) {
		// get the CladdLoader
		ClassLoader cl = ResourceLoader.class.getClassLoader();
		
		// return it
		return cl.getResourceAsStream(resource);
	}
}



