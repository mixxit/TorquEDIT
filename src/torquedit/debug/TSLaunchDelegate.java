package torquedit.debug;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.BackgroundResourceRefresher;
import org.eclipse.ui.externaltools.internal.program.launchConfigurations.ExternalToolsProgramMessages;


/**
 * @author Alan
 * Adapted from ProgramLaunchDelegate
 */
public class TSLaunchDelegate extends LaunchConfigurationDelegate {

	private static IWindowListener fWindowListener;

	/**
	 * A window listener that warns the user about any running programs when
	 * the workbench closes. Programs are killed when the VM exits.
	 */
	private class ProgramLaunchWindowListener implements IWindowListener {
		public void windowActivated(IWorkbenchWindow window) {
		}
		public void windowDeactivated(IWorkbenchWindow window) {
		}
		public void windowClosed(IWorkbenchWindow window) {
			IWorkbenchWindow windows[]= PlatformUI.getWorkbench().getWorkbenchWindows();
			if (windows.length > 1) {
				// There are more windows still open.
				return;
			}
			ILaunchManager manager= DebugPlugin.getDefault().getLaunchManager();
			ILaunchConfigurationType programType= manager.getLaunchConfigurationType(IExternalToolConstants.ID_PROGRAM_LAUNCH_CONFIGURATION_TYPE);
			if (programType == null) {
				return;
			}
			ILaunch launches[]= manager.getLaunches();
			ILaunchConfigurationType configType;
			ILaunchConfiguration config;
			for (int i = 0; i < launches.length; i++) {
				try {
					config= launches[i].getLaunchConfiguration();
					if (config == null) {
						continue;
					}
					configType= config.getType();
				} catch (CoreException e) {
					continue;
				}
				if (configType.equals(programType)) {
					if (!launches[i].isTerminated()) {
						MessageDialog.openWarning(window.getShell(), ExternalToolsProgramMessages.ProgramLaunchDelegate_Workbench_Closing_1, ExternalToolsProgramMessages.ProgramLaunchDelegate_The_workbench_is_exiting);
						break;
					}
				}
			}
		}
		public void windowOpened(IWorkbenchWindow window) {
		}
	}

	/**
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration, java.lang.String, org.eclipse.debug.core.ILaunch, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		if (monitor.isCanceled()) {
			return;
		}
		
		// resolve location
		IPath location = ExternalToolsUtil.getLocation(configuration);
		
		if (monitor.isCanceled()) {
			return;
		}		
		
		// resolve working directory
		IPath workingDirectory = ExternalToolsUtil.getWorkingDirectory(configuration);
		
		if (monitor.isCanceled()) {
			return;
		}
		
		// resolve arguments
		String[] arguments = ExternalToolsUtil.getArguments(configuration);
		
		if (monitor.isCanceled()) {
			return;
		}
		
		int cmdLineLength = 1;
		if (arguments != null) {
			cmdLineLength += arguments.length;
		}
		String[] cmdLine = new String[cmdLineLength];
		cmdLine[0] = location.toOSString();
		if (arguments != null) {
			System.arraycopy(arguments, 0, cmdLine, 1, arguments.length);
		}
		
		File workingDir = null;
		if (workingDirectory != null) {
			workingDir = workingDirectory.toFile();
		}
		
		if (monitor.isCanceled()) {
			return;
		}
		
		String[] envp = DebugPlugin.getDefault().getLaunchManager().getEnvironment(configuration);
		
		if (monitor.isCanceled()) {
			return;
		}
		
		if (fWindowListener == null) {
			fWindowListener= new ProgramLaunchWindowListener();
			PlatformUI.getWorkbench().addWindowListener(fWindowListener);
		}
		Process p = DebugPlugin.exec(cmdLine, workingDir, envp);
		IProcess process = null;
		
		// add process type to process attributes
		Map<String, String> processAttributes = new HashMap<String, String>();
		String programName = location.lastSegment();
		String extension = location.getFileExtension();
		if (extension != null) {
			programName = programName.substring(0, programName.length() - (extension.length() + 1));
		}
		programName = programName.toLowerCase();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);
		
		
		if (p != null) {
			monitor.beginTask(NLS.bind(ExternalToolsProgramMessages.ProgramLaunchDelegate_3, new String[] {configuration.getName()}), IProgressMonitor.UNKNOWN);
			process = DebugPlugin.newProcess(launch, p, location.toOSString(), processAttributes);
			if (process == null) {
				p.destroy();
				throw new CoreException(new Status(IStatus.ERROR, IExternalToolConstants.PLUGIN_ID, IExternalToolConstants.ERR_INTERNAL_ERROR, ExternalToolsProgramMessages.ProgramLaunchDelegate_4, null));
			}
			
			
		}
		process.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(cmdLine));

		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			IDebugTarget target = new TSDebugTarget(launch, process, 6060);
			launch.addDebugTarget(target);
		}		
		
		if (CommonTab.isLaunchInBackground(configuration)) {
			// refresh resources after process finishes
			if (RefreshTab.getRefreshScope(configuration) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, process);
				refresher.startBackgroundRefresh();
			}				
		} else {
			// wait for process to exit
			while (!process.isTerminated()) {
				try {
					if (monitor.isCanceled()) {
						process.terminate();
						break;
					}
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}
			
			// refresh resources
			RefreshTab.refreshResources(configuration, monitor);
		}
	}
	
	private String generateCommandLine(String[] commandLine) {
		if (commandLine.length < 1)
			return ""; //$NON-NLS-1$
		StringBuffer buf= new StringBuffer();
		for (int i= 0; i < commandLine.length; i++) {
			buf.append(' ');
			char[] characters= commandLine[i].toCharArray();
			StringBuffer command= new StringBuffer();
			boolean containsSpace= false;
			for (int j = 0; j < characters.length; j++) {
				char character= characters[j];
				if (character == '\"') {
					command.append('\\');
				} else if (character == ' ') {
					containsSpace = true;
				}
				command.append(character);
			}
			if (containsSpace) {
				buf.append('\"');
				buf.append(command);
				buf.append('\"');
			} else {
				buf.append(command);
			}
		}	
		return buf.toString();
	}	


}
