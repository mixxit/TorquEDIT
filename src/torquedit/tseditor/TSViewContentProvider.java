package torquedit.tseditor;

import java.util.ArrayList;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

class TSViewContentProvider implements IStructuredContentProvider, 
ITreeContentProvider {
	private TreeParent invisibleRoot;

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
//		if (newInput != null) {
//		IDocument document= fDocumentProvider.getDocument(newInput);
//		if (document != null) {
//			document.addPositionCategory(SEGMENTS);
//			document.addPositionUpdater(fPositionUpdater);
//			parse(document);
//		}
//	}
		
	}
	public void dispose() {
	}
	public Object[] getElements(Object parent) {
//		if (parent.equals(getViewSite())) {
		if (invisibleRoot==null) {
			initialize();
		}
		return getChildren(invisibleRoot);
//		}
//		return getChildren(parent);
	}
	public Object getParent(Object child) {
		if (child instanceof TreeObject) {
			return ((TreeObject)child).getParent();
		}
		return null;
	}
	public Object [] getChildren(Object parent) {
		if (parent instanceof TreeParent) {
			return ((TreeParent)parent).getChildren();
		}
		return new Object[0];
	}
	public boolean hasChildren(Object parent) {
		if (parent instanceof TreeParent) {
			return ((TreeParent)parent).hasChildren();
		}
		return false;
	}
	/*
	 * We will set up a dummy model to initialize tree heararchy.
	 * In a real code, you will connect to a real model and
	 * expose its hierarchy.
	 */
	private void initialize() {
		TreeObject to1 = new TreeObject("Function 1");
		TreeObject to2 = new TreeObject("Function 2");
		TreeObject to3 = new TreeObject("Function 3");

		TreeParent root = new TreeParent("Root");
		root.addChild(to1);
		root.addChild(to2);
		root.addChild(to3);

		
		invisibleRoot = new TreeParent("");
		invisibleRoot.addChild(root);
	}
}

class TreeObject implements IAdaptable {
	private String name;
	private TreeParent parent;

	public TreeObject(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setParent(TreeParent parent) {
		this.parent = parent;
	}
	public TreeParent getParent() {
		return parent;
	}
	public String toString() {
		return getName();
	}
	public Object getAdapter(Class key) {
		return null;
	}
}

class TreeParent extends TreeObject {
	private ArrayList<TreeObject> children;
	public TreeParent(String name) {
		super(name);
		children = new ArrayList<TreeObject>();
	}
	public void addChild(TreeObject child) {
		children.add(child);
		child.setParent(this);
	}
	public void removeChild(TreeObject child) {
		children.remove(child);
		child.setParent(null);
	}
	public TreeObject [] getChildren() {
		return (TreeObject [])children.toArray(new TreeObject[children.size()]);
	}
	public boolean hasChildren() {
		return children.size()>0;
	}
}

class TSViewLabelProvider extends LabelProvider {

	public String getText(Object obj) {
		return obj.toString();
	}
	public Image getImage(Object obj) {
		String imageKey = ISharedImages.IMG_OBJ_ELEMENT;
		if (obj instanceof TreeParent)
		   imageKey = ISharedImages.IMG_OBJ_FOLDER;
		return PlatformUI.getWorkbench().getSharedImages().getImage(imageKey);
	}
}