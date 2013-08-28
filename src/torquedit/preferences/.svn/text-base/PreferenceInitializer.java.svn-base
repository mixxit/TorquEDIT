package torquedit.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.RGB;

import org.eclipse.jface.preference.PreferenceConverter;

import torquedit.tseditor.TorquEDITPlugin;
/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	public void initializeDefaultPreferences() {
		IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
		store.setDefault(PreferenceConstants.P_TGB_CHECK, true);
		store.setDefault(PreferenceConstants.P_TGE_CHECK, true);
		store.setDefault(PreferenceConstants.P_TGEA_CHECK, true);
		store.setDefault(PreferenceConstants.P_DELETE_DSOS, false);
		store.setDefault(PreferenceConstants.P_POPUP_DELAY, 500);
		store.setDefault(PreferenceConstants.P_TGB_FOLDER, "");
		store.setDefault(PreferenceConstants.P_TGE_FOLDER, "");
		store.setDefault(PreferenceConstants.P_TGEA_FOLDER, "");
		
		PreferenceConverter.setDefault(store, PreferenceConstants.P_COMMENT,			new RGB(55,150,88));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_STRING,				new RGB(190,190,190));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_KEYWORD,			new RGB(0,0,100));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_GLOBAL,				new RGB(153,39,52));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_LOCAL,				new RGB(51,9,138));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_CLASSES,			new RGB(250,18,46));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_FUNCTIONS,			new RGB(58,114,209));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_DEFAULT,			new RGB(0,0,0));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_AUTO_COMPLETE_BG,	new RGB(255,255,255));
		PreferenceConverter.setDefault(store, PreferenceConstants.P_AUTO_COMPLETE_FG,	new RGB(0,0,0));
		
//		store.setDefault(PreferenceConstants.P_CHOICE, "choice2");
//		store.setDefault(PreferenceConstants.P_STRING,
//				"Default value");
	}

}
