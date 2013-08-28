package torquedit.debug;

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;

public class TSLaunchTabGroup extends
		AbstractLaunchConfigurationTabGroup implements
		ILaunchConfigurationTabGroup {

	
	
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		ILaunchConfigurationTab[] tabs = new ILaunchConfigurationTab[] {	
				new CommonTab(),
				new TSLaunchMainTab()

		};
		setTabs(tabs);
	}

	
	
	
}
