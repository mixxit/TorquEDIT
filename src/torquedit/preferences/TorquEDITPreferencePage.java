package torquedit.preferences;

import org.eclipse.jface.preference.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

import torquedit.tseditor.TorquEDITPlugin;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class TorquEDITPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public TorquEDITPreferencePage() {
		super(GRID);
		setPreferenceStore(TorquEDITPlugin.getDefault().getPreferenceStore());
		//setDescription("Code completion");
	}
	
    private Label createLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.LEFT);
        label.setText(text);
        GridData data = new GridData();
        data.horizontalSpan = 2;
        data.horizontalAlignment = GridData.FILL;
        label.setLayoutData(data);
        return label;
    }
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
//		addField(new DirectoryFieldEditor(PreferenceConstants.P_PATH, 
//				"&Directory preference:", getFieldEditorParent()));
		
		//createLabel(getFieldEditorParent(), "Code Completion Settings");
		//createLabel(getFieldEditorParent(), " ");

		addField( new DirectoryFieldEditor( PreferenceConstants.P_TGB_FOLDER, "TGB Source Folder", getFieldEditorParent()));
		addField( new DirectoryFieldEditor( PreferenceConstants.P_TGE_FOLDER, "TGE Source Folder", getFieldEditorParent()));
		addField( new DirectoryFieldEditor( PreferenceConstants.P_TGEA_FOLDER, "TGEA Source Folder", getFieldEditorParent()));
		
		addField( new BooleanFieldEditor( PreferenceConstants.P_TGB_CHECK, "Use TGB", getFieldEditorParent()));
		addField( new BooleanFieldEditor( PreferenceConstants.P_TGE_CHECK, "Use TGE", getFieldEditorParent()));
		addField( new BooleanFieldEditor( PreferenceConstants.P_TGEA_CHECK, "Use TGEA", getFieldEditorParent()));

		addField( new IntegerFieldEditor( PreferenceConstants.P_POPUP_DELAY, "Popup delay (milliseconds)",	getFieldEditorParent()));		
		addField( new BooleanFieldEditor( PreferenceConstants.P_DELETE_DSOS, "Auto-delete .dso files", getFieldEditorParent()));
		
		//createLabel(getFieldEditorParent(), "");
		//createLabel(getFieldEditorParent(), "Syntax Coloring Settings");
		
		addField(new ColorFieldEditor(PreferenceConstants.P_COMMENT, "Comments", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_AUTO_COMPLETE_BG, "Code Completion popup background", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_AUTO_COMPLETE_FG, "Code Completion popup foreground", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_CLASSES, "Classes", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_DEFAULT, "Default", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_FUNCTIONS, "Functions", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_GLOBAL, "Global variables", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_KEYWORD, "Keywords", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_LOCAL, "Local variables", getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.P_STRING, "Strings", getFieldEditorParent()));
		
//		addField(new RadioGroupFieldEditor(
//				PreferenceConstants.P_CHOICE,
//			"An example of a multiple-choice preference",
//			1,
//			new String[][] { { "&Choice 1", "choice1" }, {
//				"C&hoice 2", "choice2" }
//		}, getFieldEditorParent()));
//		addField(
//			new StringFieldEditor(PreferenceConstants.P_STRING, "A &text preference:", getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
	public boolean performOk() {
		
		return super.performOk();
	}
}