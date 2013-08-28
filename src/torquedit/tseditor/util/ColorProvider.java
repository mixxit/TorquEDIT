package torquedit.tseditor.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

import torquedit.tseditor.TorquEDITPlugin;

/**
 * Manager for colors used in the editor
 */
public class ColorProvider {
	
	// where we cache the colors
	private Map<Object, Color> colorCache = new HashMap<Object, Color>(10);
    
	/**
	 * Release all of the color resources held onto by the receiver.
	 */	
	public void dispose() {
		Iterator<Color> e= colorCache.values().iterator();
		while (e.hasNext()) {
			 ((Color) e.next()).dispose();
		}
	}
	
	/**
	 * Return the color that is stored in the color table under the given RGB
	 * value.
	 * 
	 * @param rgb the RGB value
	 * @return the color stored in the color table for the given RGB value
	 */

	public Color getColor(String preferenceName) {
		IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
		RGB rgb = PreferenceConverter.getColor(store, preferenceName);
		Color color = null;
		try {
			
			if(colorCache.containsKey(preferenceName)) { 
					if (((Color)colorCache.get(preferenceName)).getRGB().equals(rgb)) {
						color= (Color) colorCache.get(preferenceName);		
					}
					else {
						((Color)colorCache.get(preferenceName)).dispose();
						color = null;
					}
			}
			if(color == null) {
				color= new Color(Display.getCurrent(), rgb);
				colorCache.put(preferenceName, color);
			}
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return color;
	}
	
	public void refreshColors() {
		Iterator<Color> e= colorCache.values().iterator();
		if(e!=null) {
			while (e.hasNext()) {
				 ((Color) e.next()).dispose();
			}
			colorCache.clear();
		}
	}
	
	public void refreshColor(PropertyChangeEvent event) {
		if(event.getNewValue() instanceof RGB) {
			if(colorCache.containsKey(event.getNewValue()) && !((Color)colorCache.get(event.getNewValue())).isDisposed())
				return;
			if(colorCache.containsKey(event.getOldValue())) {
				Color c = (Color)colorCache.get(event.getOldValue());
				c.dispose();
				c = new Color(Display.getCurrent(), (RGB)event.getNewValue());
				colorCache.put(event.getNewValue(), c);
			}
		}
	}
}