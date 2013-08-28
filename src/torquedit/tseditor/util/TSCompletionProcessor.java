
package torquedit.tseditor.util;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.graphics.Image;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import torquedit.preferences.PreferenceConstants;
import torquedit.tseditor.TorquEDITPlugin;

/**
 * Does auto completion for functions and classes
 */
public class TSCompletionProcessor
	implements IContentAssistProcessor {

	/**
	 * Characters available to auto completion
	 */
	private static final char[] COMPLETION_START_CHARS 
		= ".:ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz_$%".toCharArray();
	
	
//	private List[] functions = new List[2];
//	private List[] classes = new List[2];
//	private List[] globals = new List[2];
//	private List[] members = new List[2];
	
	private Image functionImage = null;
	private Image classImage = null;
	private Image globalImage = null;
	private Image memberImage = null;

	private Document doc;
	private Element docRoot;
	
	private ISourceViewer sourceViewer = null;
	private IProject project = null;
	private String filename;

//	private MyChangeListener changeListener;
	private int currentLineNumber = 0;
	/**
	 * Creates the processor
	 */
	public TSCompletionProcessor() {
		functionImage		= ResourceLoader.loadImageResource("torquedit/tseditor/resources/function.gif");
		classImage			= ResourceLoader.loadImageResource("torquedit/tseditor/resources/class.gif");
		globalImage			= ResourceLoader.loadImageResource("torquedit/tseditor/resources/global.gif");
		memberImage			= ResourceLoader.loadImageResource("torquedit/tseditor/resources/member.gif");		
//		functiondetector 	= new TSFunctionsWordDetector();
//		classdetector 		= new TSClassesWordDetector();
		
	}

	class ElementComparator implements Comparator<Object> {

		public int compare(Object arg0, Object arg1) {
			// TODO Auto-generated method stub
			if(arg0 instanceof Element && arg1 instanceof Element) {
				Element a = (Element) arg0;
				Element b = (Element) arg1;
				return a.getAttribute("name").compareTo(b.getAttribute("name")); 
			}
			return 0;
		}
		
	}
	
	public void setFilename(String name) {
		filename = name;
		
	}
	
	public void setProject(IProject proj) {
		project = proj;
		
//		IPath projectPath = project.getLocation();
//		IPath indexPath = projectPath.append(".torquedit.xml");
//		
//		try {
//			doc = builder.parse(new File(indexPath.toOSString()));
//			docRoot = (Element)doc.getElementsByTagName("root").item(0);
//		} catch (SAXException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		doc = TorquEDITPlugin.getDefault().getAST(proj);
		if(doc != null) {
			docRoot = (Element)doc.getElementsByTagName("root").item(0);
		}
	}
	
	public void setSourceViewer(ISourceViewer sv) {
		sourceViewer = sv;
	}
	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeCompletionProposals(org.eclipse.jface.text.ITextViewer, int)
	 */
	public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset) {
		
		// get what they've typed
		String prefix = getPrefix(viewer.getDocument(), offset).trim();
		
		// prepare a list to store the completions in
		List<CompletionProposal> resultsList = new ArrayList<CompletionProposal>();

		if(project != null) {
			doc = TorquEDITPlugin.getDefault().getAST(project);
			if(doc != null) {
				docRoot = (Element)doc.getElementsByTagName("root").item(0);
			}
		}
		
		addDomToList(prefix, docRoot, offset, resultsList, null);		
		
		// put the list into the arrat
		ICompletionProposal[] retArray = new ICompletionProposal[resultsList.size()];
		resultsList.toArray(retArray);
		
		// return it
		return retArray;
	}

//	private void addResourcesToList(String prefix, List resources, int offset, List addTo, Image img) {
//		boolean addMe;
//		for (int i=0;i<resources.size();i++) {
//			addMe = false;
//			XmlResource resource = (XmlResource)resources.get(i);
//			
//			if (resource.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
//				if(prefix.lastIndexOf(".") < 0 && resource.getName().lastIndexOf(".") < 0) {
//					addMe = true;
//				}
//				else if (prefix.lastIndexOf(".") >= 0 && resource.getName().lastIndexOf(".") >= 0) {
//					addMe = true;
//				}
//				
//				if(addMe) {
//					CompletionProposal cp = new CompletionProposal(
//							resource.getName(),
//							offset-prefix.length(),
//							prefix.length(),
//							resource.getName().length(),
//							img,
//							resource.getName(),
//							null,
//							resource.getDescription()
//							
//						);
//					addTo.add(cp);
//					
//				}
//			}
//		}
//	}

	private void addDomToList(String prefix, Element rootNode, int offset, List<CompletionProposal> addTo, Image img) {
		if(rootNode == null)
		{
			return;
		}
		IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();		
//		TorqueScriptEditor editor = TorqueScriptEditor.getInstance();
//		IEditorInput input = editor.getEditorInput();
//		String filename = input.getName();
		
//		ArrayList engines = new ArrayList();
//		if(store.getBoolean(PreferenceConstants.P_TGB_CHECK)) {
//			engines.add("tgb");
//		}
//
//		if(store.getBoolean(PreferenceConstants.P_TGE_CHECK)) {
//			engines.add("tge");
//		}
//
//		if(store.getBoolean(PreferenceConstants.P_TGEA_CHECK)) {
//			engines.add("tgea");
//		}
		
		String[] prefixParts = prefix.split("\\.");
		int numPrefixes = prefixParts.length;
		String lastPrefix = prefixParts[numPrefixes-1];
		if(prefix.endsWith("."))
			lastPrefix = "";
		
		try {
			currentLineNumber = sourceViewer.getDocument().getLineOfOffset(offset);
		}
		catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		Element cur, par;
		
		par = docRoot;
		Element testNode;
		ArrayList<Element> candidates = new ArrayList<Element>();

		if(lastPrefix.equals(""))
			numPrefixes++;
		for(int j=0; j < numPrefixes && par != null; j++) {
			if(j < numPrefixes-1) {
				testNode = findElementInHierarchy(par, null, "name", prefixParts[j]);
				par = testNode;
			}
			else {
				addParentsToList(par, candidates, new ArrayList<Element>());
				ArrayList<Element> result = findElementsInHierarchyPrefix(par, null, "name", lastPrefix);
				if(result != null) {
					candidates.addAll(result);
				}
			}
			
		}

		
		Comparator comp = new ElementComparator();
		Collections.sort(candidates, comp);
		String prevName = "";
		String n;
		for(int i = 0; i < candidates.size(); i++) {
			cur = (Element)candidates.get(i);
			n = cur.getAttribute("name");
			String replacementString = n;
			int replacementOffset = offset-lastPrefix.length();
			int replacementLength = lastPrefix.length();
			int cursorPosition = n.length();
			String displayString = n;

			if(!cur.getAttribute("displayString").equals("")) {
				displayString = cur.getAttribute("displayString");
			}
			
			if(!cur.getAttribute("replacementString").equals("")) {
				replacementString = cur.getAttribute("replacementString");
				replacementString = replacementString.replaceAll("\\x25s", prefix.substring(0, prefix.length()-lastPrefix.length()-1));
			}
			
			if(!cur.getAttribute("replacementOffset").equals("")) {
				replacementOffset = Integer.parseInt(cur.getAttribute("replacementOffset"));
				replacementOffset += offset - prefix.length();
			}

			if(!cur.getAttribute("replacementLength").equals("")) {
				replacementLength = Integer.parseInt(cur.getAttribute("replacementLength"));
				replacementLength += prefix.length();
			}

			if(!cur.getAttribute("cursorPosition").equals("")) {
				cursorPosition = Integer.parseInt(cur.getAttribute("cursorPosition"));
				cursorPosition += replacementString.length();
			}
			
			
			if(cur != null &&  !n.equals(prevName) && !n.equals("") && (prefix.endsWith(".") || n.toLowerCase().startsWith(lastPrefix.toLowerCase()))) {
			
				CompletionProposal cp = new CompletionProposal(
						replacementString,
						replacementOffset,
						replacementLength,
						cursorPosition,
						selectImage(cur.getNodeName()),
						displayString,
						null,
						cur.getAttribute("description") + "\n" + cur.getAttribute("file") + ": " + cur.getAttribute("startLine")  

				);
				addTo.add(cp);
				prevName = n;
			}
		}

	}

	protected void addChildrenToList(Element rootNode, List<Element> addTo) {
		if(rootNode == null || rootNode == docRoot)
			return;
		NodeList children = rootNode.getChildNodes();
		Element par;
		ArrayList<String> existing = new ArrayList<String>();
		for(int j = 0; j < children.getLength(); j++) {
			Element e = (Element) children.item(j);
			if(e.getParentNode() == rootNode) {
				String n = e.getAttribute("name");
				boolean exists = false;
				for (int k = 0; k < existing.size(); k++) {
					String comp = (String)existing.get(k);
					if(comp.toLowerCase().equals(n.toLowerCase())) 
					{
						exists = true;
					}
				}
				if(!exists) {
					par = (Element)e.getParentNode();
					while(par != null) {
						if(par.getParentNode() instanceof Element) {
							par = (Element) par.getParentNode();
						}
						else {
							par = null;
						}
					}
					existing.add(n);
					addTo.add(e);
				}
			}					
		}
	}

//	protected Element getParent(Element rootNode, String parentName) {
//		if(rootNode == null)
//			return null;
//		NodeList parents = rootNode.getElementsByTagName("parent");
//		Element par;
//		String n;
//		for(int j = 0; j < parents.getLength(); j++) {
//			Element e = (Element) parents.item(j);
//			n = e.getAttribute("parentname");
//			if(parentName.equals(n)) {
//				par = findElementByAttribute(docRoot, null, "name", n);
//				return par;
//			}
//		}
//		return null;
//	}
	
	protected void addParentsToList(Element rootNode, List<Element> addTo, List<Element> visited) {
		if(rootNode == null || visited.contains(rootNode))
			return;
		visited.add(rootNode);
		NodeList parents = rootNode.getChildNodes();
		Element par;
		String n, t;
		for(int j = 0; j < parents.getLength(); j++) {
			Element e = (Element) parents.item(j);
			if(e.getNodeName().equals("parent")) {
				n = e.getAttribute("parentname");
				t = e.getAttribute("parenttype");
				if(t.equals("")) {
					t = null;
				}
				if(!n.equals("")) {
					par = findElementByAttribute(docRoot, t, "name", n);
					if(par != null) {
							addChildrenToList(par, addTo);
							addParentsToList( par, addTo, visited);
					}						
				}
			}
		}
	}
	
	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#computeContextInformation(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IContextInformation[] computeContextInformation(ITextViewer viewer,
			int offset) {
		return null;
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getCompletionProposalAutoActivationCharacters()
	 */
	public char[] getCompletionProposalAutoActivationCharacters() {
		return COMPLETION_START_CHARS;
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationAutoActivationCharacters()
	 */
	public char[] getContextInformationAutoActivationCharacters() {
		return null;
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getErrorMessage()
	 */
	public String getErrorMessage() {
		return null;
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.contentassist.IContentAssistProcessor#getContextInformationValidator()
	 */
	public IContextInformationValidator getContextInformationValidator() {
		return null;
	}
	
    /**
     * Returns the part of the word immediately before the position at which
     * content assist was requested. The prefix is lower-cased before it is 
     * returned, to enable case-insensitive matching.
     * 
     * @param document the document
     * @param offset the offset into the document
     * @return the prefix
     */
    private String getPrefix(IDocument document, int offset) {
        try {
            int startPos = offset;
            while (startPos > 0) {
                char c = document.getChar(startPos - 1);
                startPos--;
                //if (!functiondetector.isWordPart(c) || !classdetector.isWordPart(c)) {
                //if (c < '!' || c > '~') {
                if(!Character.isLetter(c) && !Character.isDigit(c) && c != '.' && c != ':') {
                    break;
                }
            }
            if (startPos < offset) {
                return document.get(startPos, offset - startPos).toLowerCase();
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        return "";
    }

    private Image selectImage(String type) {
    	if(type.contains("lass") || type.contains("atablock"))
    		return classImage;
    	if(type.contains("unction"))
    		return functionImage;
    	if(type.contains("ethod") || type.contains("ield"))
    		return memberImage;
    	if(type.contains("lobal"))
    		return globalImage;
    	if(type.contains("ocal"))
    		return memberImage;
    	
    	
    	return null;
    }
    
    public ArrayList<Element> findElementsByAttributePrefix(Element base, String type, String attribute, String name) {
    	if(base == null) {
    		return null;
    	}
    	if( !base.getAttribute("endLine").equals("") && !base.getAttribute("startLine").equals("")) {
    		if(!base.getAttribute("file").equals(filename) || 
    				!(Integer.parseInt(base.getAttribute("startLine")) <= currentLineNumber && 
    						Integer.parseInt(base.getAttribute("endLine")) >= currentLineNumber)) {
    			return null;

			}
    		
    	}
		NodeList classes;
		ArrayList<Element> results = new ArrayList<Element>();
		classes = base.getChildNodes();
		Element iterNode;
		
		for(int i = 0; i < classes.getLength(); i++) {
			iterNode = (Element)classes.item(i);
			if(type == null || iterNode.getNodeName().equals(type)) {			
				if(iterNode.getAttribute(attribute).toLowerCase().startsWith(name.toLowerCase())) {
					results.add(iterNode);
				}
				// Only worry about script scoping
				if(iterNode.getNodeName().equals("class") || iterNode.getNodeName().equals("method") || iterNode.getNodeName().equals("function")) {
					ArrayList<Element> tempAdd = findElementsByAttributePrefix(iterNode, type, attribute, name);
					if(tempAdd != null) {
						results.addAll(tempAdd);
					}
				}
			}
		}
		return results;
    }
    
    public Element findElementByAttribute(Element base, String type, String attribute, String name) {
    	if(base == null)
    		return null;
    	if( !base.getAttribute("endLine").equals("") && !base.getAttribute("startLine").equals("")) {
    		if(!base.getAttribute("file").equals(filename) || 
    				!(Integer.parseInt(base.getAttribute("startLine")) <= currentLineNumber && 
    						Integer.parseInt(base.getAttribute("endLine")) >= currentLineNumber)) {
    			return null;

			}
    		
    	}
		NodeList classes;
		classes = base.getChildNodes();

		Element iterNode, resultNode = null;
		
		//Element classNode = doc.createElement("class");
		for(int i = 0; i < classes.getLength() && resultNode == null; i++) {
			iterNode = (Element)classes.item(i);
			if(type == null || iterNode.getNodeName().equals(type)) {			
				if(iterNode.getAttribute(attribute).toLowerCase().equals(name.toLowerCase())) {
					resultNode = iterNode;
				}
				else {
					resultNode = findElementByAttribute(iterNode, type, attribute, name);
				}
			}			
		}
		return resultNode;
    }

    public ArrayList<Element> findElementsInHierarchyPrefix(Element base, String type, String attribute, String name) {
    	if(base == null)
    		return null;
    	
    	ArrayList<Element> result = new ArrayList<Element>();
    	ArrayList<Element> output =findElementsByAttributePrefix(base, type, attribute, name);
    	if(output != null) {
    		result.addAll(output);
    	}
    	
		NodeList parents;
		parents = base.getChildNodes();
		Element iterNode;
		Element parNode;
		for(int i = 0; i < parents.getLength() && result == null; i++) {
			iterNode = (Element)parents.item(i);
			if(type == null || iterNode.getNodeName().equals("parent")) {
				parNode = findElementByAttribute(docRoot, null, "name", iterNode.getAttribute("parentname"));
				if(parNode != null) {
					output = findElementsByAttributePrefix(parNode, type, attribute, name);
					if(output != null) {
						result.addAll(output);
					}
				}

			}
		}
		return result;
    }

    public Element findElementInHierarchy(Element base, String type, String attribute, String name) {
    	if(base == null)
    		return null;
    	
    	Element result;
    	result = findElementByAttribute(base, type, attribute, name);
    	if(result != null)
    		return result;
    	
		NodeList parents;
		parents = base.getChildNodes();
		Element iterNode;
		Element parNode;
		for(int i = 0; i < parents.getLength() && result == null; i++) {

			iterNode = (Element)parents.item(i);
			if(type == null || iterNode.getNodeName().equals("parent")) {
				parNode = findElementByAttribute(docRoot, null, "name", iterNode.getAttribute("parentname"));
				if(parNode != null) {
					result = findElementByAttribute(parNode, type, attribute, name);
				}
			}
		}
		return result;
    }
    
//    public class MyChangeListener implements IResourceChangeListener {
//        public void resourceChanged(IResourceChangeEvent event) {
//           IResource res = event.getResource();
//           switch (event.getType()) {
//              case IResourceChangeEvent.POST_BUILD:
//                 System.out.println("Build complete.");
//                 if(project != null) {
//                	 doc = TorquEDITPlugin.getDefault().getAST(project);
//                 }
//                 break;
//           }
//        }
//     }
    
}
