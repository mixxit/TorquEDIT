package torquedit.tseditor;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class TSProjectNature implements IProjectNature {

	private IProject project;
	
	public void configure() throws CoreException {

		   final String BUILDER_ID = "torquedit.builder";
		   IProjectDescription desc = project.getDescription();
		   ICommand[] commands = desc.getBuildSpec();
		   boolean found = false;

		   for (int i = 0; i < commands.length; ++i) {
		      if (commands[i].getBuilderName().equals(BUILDER_ID)) {
		         found = true;
		         break;
		      }
		   }
		   if (!found) { 
		      //add builder to project
		      ICommand command = desc.newCommand();
		      command.setBuilderName(BUILDER_ID);
		      ICommand[] newCommands = new ICommand[commands.length + 1];

		      // Add it before other builders.
		      System.arraycopy(commands, 0, newCommands, 1, commands.length);
		      newCommands[0] = command;
		      desc.setBuildSpec(newCommands);
		      project.setDescription(desc, null);
		   }


	}

	public void deconfigure() throws CoreException {
		// TODO Auto-generated method stub

	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject value) {
		project = value;

	}

}
