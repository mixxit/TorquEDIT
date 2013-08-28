/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package torquedit.debug;

import org.eclipse.core.internal.variables.DynamicVariable;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.core.sourcelookup.containers.FolderSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.ui.externaltools.internal.launchConfigurations.ExternalToolsUtil;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

/**
 * Computes the default source lookup path for a PDA launch configuration.
 * The default source lookup path is the folder or project containing 
 * the PDA program being launched. If the program is not specified, the workspace
 * is searched by default.
 */
public class TSSourcePathComputerDelegate implements ISourcePathComputerDelegate {
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourcePathComputerDelegate#computeSourceContainers(org.eclipse.debug.core.ILaunchConfiguration, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		//String path = configuration.getAttribute(ITSConstants.ATTR_PDA_PROGRAM, (String)null);
		
		ISourceContainer sourceContainer = null;
	    //String dir = configuration.getAttribute(IExternalToolConstants.ATTR_WORKING_DIRECTORY, (String)null);
	    IPath workingDirectory = ExternalToolsUtil.getWorkingDirectory(configuration); 
	    
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().getContainerForLocation(workingDirectory);
		if (resource != null) {
			IContainer container;
			if(resource.getType() == IResource.FILE) {
				container = resource.getParent();
			}
			else {
				container = (IContainer) resource;
			}
			
			if (container.getType() == IResource.PROJECT) {
				sourceContainer = new ProjectSourceContainer((IProject)container, false);
			} else if (container.getType() == IResource.FOLDER) {
				sourceContainer = new FolderSourceContainer(container, false);
			}
		}
		
		if (sourceContainer == null) {
			sourceContainer = new WorkspaceSourceContainer();
		}
		return new ISourceContainer[]{sourceContainer};
	}
}
