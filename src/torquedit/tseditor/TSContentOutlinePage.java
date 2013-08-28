package torquedit.tseditor;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;




public class TSContentOutlinePage extends ContentOutlinePage {

	private IEditorInput fInput;
	private IDocumentProvider fDocumentProvider;
	private TreeViewer fViewer;
	private TSViewContentProvider fContentProvider;
	private TSViewLabelProvider fLabelProvider;
	
	public TSContentOutlinePage(IDocumentProvider doc) {
		fDocumentProvider = doc;
	}
	
	public void createControl(Composite parent) {
		   super.createControl(parent);
		   fViewer= getTreeViewer();
		   fContentProvider = new TSViewContentProvider();
		   fLabelProvider = new TSViewLabelProvider();
		   fViewer.setContentProvider(fContentProvider);
		   fViewer.setLabelProvider(fLabelProvider);
		   fViewer.addSelectionChangedListener(this);
		   fViewer.setInput(fInput);
		   fViewer.expandAll();
	}

	public void setInput(IEditorInput input) {
		fInput = input;
	
	}
	
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if(fContentProvider != null) {
			fContentProvider.inputChanged(viewer, oldInput, newInput);
		}
	}
	
	
	

	
		
}
