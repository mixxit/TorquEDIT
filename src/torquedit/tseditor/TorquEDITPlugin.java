package torquedit.tseditor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.BundleContext;
import org.w3c.dom.Document;

// TODO : 3) Optimize speed-wise for large projects
// TODO : indent preferences (tabs, spaces, width)
// TODO : handle c++ loop/sprintf case?
// TODO : bold/italic color preferences.
// TODO : AST augmentation file... xml that user can edit to make return type, custom entries that are not refreshed on build/clean.  Load this first for fresh builds.
// TODO : Show problem markers by parsing console output

/**
 * The main plugin class to be used in the desktop.
 */
public class TorquEDITPlugin
	extends AbstractUIPlugin {
	
	//The shared instance.
	private static TorquEDITPlugin plugin;
	private IPreferenceStore fCombinedPreferenceStore;
	
	private Map<IProject, Document> projectAST;
	
	/**
	 * The constructor.
	 */
	public TorquEDITPlugin() {
		super();
		plugin = this;
		projectAST = new HashMap<IProject, Document>();

	}
	
	public Document getAST(IProject project)
	{
		Document doc = null;
		if(projectAST.containsKey(project)) {
			doc = (Document) projectAST.get(project);
		}
		if(doc == null) {
			try {
				doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().
					parse(new File(project.getLocation().append(".torquedit.xml").toOSString()));
				projectAST.put(project, doc);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			} 
		}
		return doc;
	}
	
	public void resfreshAST(IProject project, Document doc) {
		projectAST.remove(project);
		projectAST.put(project, doc);
	}
	
	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	/**
	 * Returns the shared instance.
	 */
	public static TorquEDITPlugin getDefault() {
		
		return plugin;
	}
	
	
    public IPreferenceStore getCombinedPreferenceStore() {
        if (fCombinedPreferenceStore == null) {
            IPreferenceStore generalTextStore= EditorsUI.getPreferenceStore(); 
            fCombinedPreferenceStore= new ChainedPreferenceStore(new IPreferenceStore[] { getPreferenceStore(), generalTextStore });
        }
        return fCombinedPreferenceStore;
    }

    public static IWorkspace getWorkspace() {
    	
        return ResourcesPlugin.getWorkspace();
    }
    
}
