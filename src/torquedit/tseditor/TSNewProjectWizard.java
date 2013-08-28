package torquedit.tseditor;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;



public class TSNewProjectWizard extends Wizard implements INewWizard {

   /**
    * The main page on the wizard: collects the project name and location.
    */
   private WizardNewProjectCreationPage namePage;

   /**
    * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
    */
   public void init(IWorkbench workbench,IStructuredSelection selection)
   {
      setNeedsProgressMonitor(true);
   }

   /**
    * @see org.eclipse.jface.wizard.IWizard#addPages()
    */
   public void addPages()
   {
   	try
   	{
      super.addPages();
      namePage = new WizardNewProjectCreationPage("NewTorqueProjectWizard");
      namePage.setTitle("New Torque Project");
      namePage.setDescription("Create a new torque project");
      namePage.setImageDescriptor(ImageDescriptor.createFromFile(getClass(),
    		  "torquedit/tseditor/resources/torque20.png"));
      addPage(namePage);
   	}
   	catch(Exception x)
   	{
   		reportError(x);
   	}
   }

   /**
    * User has clicked "Finish", we create the project.
    * In practice, it calls the createProject() method in the appropriate thread.
    * @see #createProject(IProjectMonitor)
    * @see org.eclipse.jface.wizard.IWizard#performFinish()
    */
   public boolean performFinish()
   {
      try
      {
         getContainer().run(false,true,new WorkspaceModifyOperation()
         {
            protected void execute(IProgressMonitor monitor)
            {
               createProject(monitor != null ? monitor : new NullProgressMonitor());
            }
         });
      }
      catch(InvocationTargetException x)
      {
         reportError(x);
         return false;
      }
      catch(InterruptedException x)
      {
         reportError(x);
         return false;
      }
      return true; 
   }

   /**
    * This is the actual implementation for project creation.
    * @param monitor reports progress on this object
    */
   protected void createProject(IProgressMonitor monitor)
   {
      monitor.beginTask("Creating project",20);
      try
      {
         IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
         monitor.subTask("Creating directories");
         IProject project = root.getProject(namePage.getProjectName());
         IProjectDescription description = ResourcesPlugin.getWorkspace().newProjectDescription(project.getName());
         if(!Platform.getLocation().equals(namePage.getLocationPath()))
            description.setLocation(namePage.getLocationPath());
         project.create(description,monitor);
         monitor.worked(10);
         project.open(monitor);
         description = project.getDescription();
         description.setNatureIds(new String[] { "torquedit.TSNature" });
         
         project.setDescription(description,new SubProgressMonitor(monitor,10));
         
//         IPath projectPath = project.getFullPath(),
//               srcPath = projectPath.append("src"),
//               rulesPath = projectPath.append("rules"),
//               publishPath = projectPath.append("publish");
//         IFolder srcFolder = root.getFolder(srcPath),
//                 rulesFolder = root.getFolder(rulesPath),
//                 publishFolder = root.getFolder(publishPath);
//         createFolderHelper(srcFolder,monitor);
//         createFolderHelper(rulesFolder,monitor);
//         createFolderHelper(publishFolder,monitor);
//         monitor.worked(10);
//         monitor.subTask("Creating files");
//         IPath indexPath = srcPath.append("index.xml"),
//               defaultPath = rulesPath.append("default.xsl");
//         IFile indexFile = root.getFile(indexPath),
//               defaultFile = root.getFile(defaultPath);
//         Class clasz = getClass();
//         InputStream indexIS = clasz.getResourceAsStream("/org/ananas/xm/eclipse/resources/index.xml"),
//                     defaultIS = clasz.getResourceAsStream("/org/ananas/xm/eclipse/resources/default.xsl");
//         indexFile.create(indexIS,false,new SubProgressMonitor(monitor,10));
//         defaultFile.create(defaultIS,false,new SubProgressMonitor(monitor,10));
      }
      catch(CoreException x)
      {
         reportError(x);
      }
      finally
      {
         monitor.done();
      }
   }

   /**
    * Displays an error that occurred during the project creation.
    * @param x details on the error
    */
   private void reportError(Exception x)
   {
      ErrorDialog.openError(getShell(),
                            "Error",
                            "Project creation error",
                            makeStatus(x));
   }

   /**
    * Helper method: it recursively creates a folder path.
    * @param folder
    * @param monitor
    * @throws CoreException
    * @see java.io.File#mkdirs()
    */
/*   private void createFolderHelper(IFolder folder,IProgressMonitor monitor)
      throws CoreException
   {
      if(!folder.exists())
      {
         IContainer parent = folder.getParent();
         if(parent instanceof IFolder
            && (!((IFolder)parent).exists()))
            createFolderHelper((IFolder)parent,monitor);
         folder.create(false,true,monitor);
      }
   }
*/   
   public static IStatus makeStatus(Exception x)
   {
      
      if(x instanceof CoreException)
         return ((CoreException)x).getStatus();
      else
         return new Status(IStatus.ERROR,
                            "torquedit.tseditor.TorquEDIT",
                            IStatus.ERROR,
                            x.getMessage() != null ? x.getMessage()
                                                   : x.toString(),
                            x);
   }
}