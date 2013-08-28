package torquedit.debug;

import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsMainTab;

public class TSLaunchMainTab extends ExternalToolsMainTab {
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		configuration.setAttribute(FIRST_EDIT, true);
	}


}
