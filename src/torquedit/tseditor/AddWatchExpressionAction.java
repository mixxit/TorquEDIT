package torquedit.tseditor;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;

public class AddWatchExpressionAction implements IEditorActionDelegate {

	IEditorPart activeEditor;
	ISelection activeSelection;
	
	public void setActiveEditor(IAction action, IEditorPart targetEditor) {
		activeEditor = targetEditor;
	}

	public void run(IAction action) {
		IExpression[] exps = DebugPlugin.getDefault().getExpressionManager().getExpressions();
		IExpressionManager expMan = DebugPlugin.getDefault().getExpressionManager();
		if(activeSelection instanceof TextSelection) {
			TextSelection s = (TextSelection)activeSelection;
			String text = s.getText();
			if(text.length() > 0) {
				if(text.endsWith(";")) {
					text = text.substring(0, text.length()-1);
				}
				IExpression e = expMan.newWatchExpression(text);
				expMan.addExpression(e);
			}
		}
	}

	public void selectionChanged(IAction action, ISelection selection) {
		activeSelection = selection;
	}

}
