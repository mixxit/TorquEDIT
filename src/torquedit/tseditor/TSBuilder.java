package torquedit.tseditor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import torquedit.preferences.PreferenceConstants;

public class TSBuilder extends IncrementalProjectBuilder {

	private TSScriptBuildVisitor visitor;

	private DocumentBuilderFactory factory;

	private DocumentBuilder builder;

	private Document doc;

	private Element docRoot;

	private static TSBuilder instance = null;

	private String currentPrefix = "";

	public TSBuilder() {
		instance = this;
	}

	public static TSBuilder getInstance() {
		return instance;
	}

	public Element getDocRoot() {
		return docRoot;
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
	throws CoreException {
		if (kind == IncrementalProjectBuilder.FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
		
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
		try {
			doc = TorquEDITPlugin.getDefault().getAST(getProject());
			docRoot = (Element)doc.getFirstChild();
			IResourceDelta bob = delta;
			monitor.beginTask("Incremental build", 1000);
			DeltaPrinter dp = new DeltaPrinter(new SubProgressMonitor(monitor, 500), doc, docRoot);
			bob.accept(dp);
			
			// Write to file
			IPath projectPath = getProject().getLocation();
			IPath indexPath = projectPath.append(".torquedit.xml");

			FileOutputStream out = new FileOutputStream(indexPath.toOSString());

			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer aTransformer = tranFactory.newTransformer();

			Source src = new DOMSource(doc);
			Result dest = new StreamResult(out);

			aTransformer.transform(src, dest);
			// out.writeObject(doc);
			out.close();
			monitor.worked(500);
			// Update AST in plugin
			TorquEDITPlugin.getDefault().resfreshAST(getProject(), doc);
			
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			monitor.done();
		}
	}
	
	protected void fullBuild(final IProgressMonitor monitor)
			throws CoreException {
		try {

			monitor.beginTask("Source build", 1000);

			IPath projectPath = getProject().getLocation();
			IPath indexPath = projectPath.append(".torquedit.xml");

			FileOutputStream out = new FileOutputStream(indexPath.toOSString());

			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			doc = builder.newDocument();
			docRoot = doc.createElement("root");
			doc.appendChild(docRoot);

			parseTorqueSources(new SubProgressMonitor(monitor, 900));

			visitor = new TSScriptBuildVisitor(new SubProgressMonitor(monitor, 100),
					doc, docRoot);
			getProject().accept(visitor);

			
			
			
			TransformerFactory tranFactory = TransformerFactory.newInstance();
			Transformer aTransformer = tranFactory.newTransformer();

			Source src = new DOMSource(doc);
			Result dest = new StreamResult(out);

			aTransformer.transform(src, dest);
			// out.writeObject(doc);
			out.close();
			TorquEDITPlugin.getDefault().resfreshAST(getProject(), doc);

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			monitor.done();

		}
	}

	static public List<File> getFileListing(java.io.File aStartingDir)
			throws FileNotFoundException {
		validateDirectory(aStartingDir);
		List<File> result = new ArrayList<File>();

		java.io.File[] filesAndDirs = aStartingDir.listFiles();
		List<File> filesDirs = Arrays.asList(filesAndDirs);
		Iterator<File> filesIter = filesDirs.iterator();
		java.io.File file = null;
		while (filesIter.hasNext()) {
			file = (java.io.File) filesIter.next();
			result.add(file); // always add, even if directory
			if (!file.isFile()) {
				// must be a directory
				// recursive call!
				List<File> deeperList = getFileListing(file);
				result.addAll(deeperList);
			}

		}
		Collections.sort(result);
		return result;
	}

	/**
	 * Directory is valid if it exists, does not represent a file, and can be
	 * read.
	 */
	static private void validateDirectory(java.io.File aDirectory)
			throws FileNotFoundException {
		if (aDirectory == null) {
			throw new IllegalArgumentException("Directory should not be null.");
		}
		if (!aDirectory.exists()) {
			throw new FileNotFoundException("Directory does not exist: "
					+ aDirectory);
		}
		if (!aDirectory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: "
					+ aDirectory);
		}
		if (!aDirectory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: "
					+ aDirectory);
		}
	}

	protected void parseTorqueSources(IProgressMonitor monitor) {
		// TODO : handle tgb, tge, and tgea in different trees

		IPreferenceStore store = TorquEDITPlugin.getDefault()
				.getCombinedPreferenceStore();
		String[] folders = { store.getString(PreferenceConstants.P_TGB_FOLDER),
				store.getString(PreferenceConstants.P_TGE_FOLDER),
				store.getString(PreferenceConstants.P_TGEA_FOLDER) };
		
		boolean[] flags = { store.getBoolean(PreferenceConstants.P_TGB_CHECK), 
				store.getBoolean(PreferenceConstants.P_TGE_CHECK),
				store.getBoolean(PreferenceConstants.P_TGEA_CHECK) };
		
		int folderCount = 0;
		for (int i = 0; i < folders.length; i++) {
			if (!folders[i].equals("") && flags[i] == true) {
				folderCount++;
			}
		}
		monitor.beginTask("Building Source", 1000000 * folderCount);

		String[] prefixes = { "tgb", "tge", "tgea" };
		List<File> files = null;
		String curFolder;
		java.io.File curFile;
		try {
			for (int i = 0; i < prefixes.length; i++) {
				if(!folders[i].equals("") && flags[i] == true) {
					curFolder = folders[i];
					currentPrefix = prefixes[i];
					
					curFile = new java.io.File(curFolder);
					if (curFile.exists()) {
						files = getFileListing(curFile);
						int workScale = 1000000 / files.size();
						Iterator<File> filesIter = files.iterator();
						while (filesIter.hasNext() && !monitor.isCanceled()) {
							curFile = (java.io.File) (filesIter.next());
							if (curFile.getName().endsWith(".cc")
									|| curFile.getName().endsWith(".h")) {
								parseTorqueFile(curFile, curFolder);
							}
							monitor.worked(workScale);
						}
					}
					
					if(currentPrefix.equals("tgb")) {
						Element vecClass = doc.createElement("tgbClass");
						vecClass.setAttribute("name", "t2dVector");
						vecClass.setAttribute("class", "t2dVector");
						docRoot.appendChild(vecClass);
						
						String[] funcs = {"t2dVectorAdd", "t2dVectorSub", "t2dVectorMult", "t2dVectorScale",
								"t2dVectorNormalise", "t2dVectorDot", "t2dVectorCompare", "t2dAngleBetween",
								"t2dVectorDistance", "t2dVectorLength", "t2dRectNormalise"
						};
						for(int f = 0; f < funcs.length; f++) {
							String n = funcs[f];
							Element func = doc.createElement("tgbMethod");
							func.setAttribute("name", n);
							func.setAttribute("replacementString", n+"(%s, )");
							func.setAttribute("replacementOffset", "0");
							func.setAttribute("replacementLength", "0");
							func.setAttribute("cursorPosition", Integer.toString(-1));
							Element realFunc = findElementByAttribute(docRoot, "tgbFunction", "name", n);
							if(realFunc != null) {
								func.setAttribute("description", realFunc.getAttribute("description"));
							}
							if(n.equals("t2dVectorNormalise")) {
								func.setAttribute("replacementString", n+"(%s);");
								func.setAttribute("cursorPosition", "0");
							}
							
							vecClass.appendChild(func);
						}
						
					}

				}
				
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			System.out.println("Folder not found for parsing "
					+ currentPrefix.toUpperCase() + "sources.");
		} finally {
			monitor.done();
		}

	}

	protected void parseTorqueFile(java.io.File file, String baseFolder) {
		try {
			FileReader reader = new FileReader(file);
			BufferedReader in = new BufferedReader(reader);
			StreamTokenizer token = new StreamTokenizer(in);

			String filename = file.getPath().substring(baseFolder.length()+1);
			
			
			token.commentChar('#');
			token.slashStarComments(true);
			token.slashSlashComments(true);
			token.eolIsSignificant(true);
			token.parseNumbers();
			token.quoteChar('"');
			token.ordinaryChar('.');
			token.wordChars('_', '_');

			String str, name, className, description;

			int tokType = token.nextToken();
			int prevTokType = StreamTokenizer.TT_EOF;
			String prevString = "";
			while (tokType != StreamTokenizer.TT_EOF) {
				switch (tokType) {

				case StreamTokenizer.TT_WORD:
					str = token.sval;

					// ConsoleFunction(alxSourcei, void, 4, 4, "(handle, ALenum,
					// value)")
					if (str.equals("ConsoleFunction")) {
						token.nextToken();
						token.nextToken();
						name = token.sval;
						tokType = token.nextToken();
						while (tokType != '"') {
							tokType = token.nextToken();
						}
						description = token.sval;
						tokType = token.nextToken();
						while (tokType != ')') {
							description += "\n" + token.sval;
							tokType = token.nextToken();
						}
						if (description == null) {
							description = "";
						}

						description = description.replaceAll("@", "");
						// description = description.replaceAll("\n", "");
						description = description.replaceAll("<", "&lt;");
						description = description.replaceAll(">", "&gt;");
						description = description.replaceAll("&", "&amp;");
						description = description.replaceAll("\"", "&quot;");
						Element funcNode = doc.createElement(currentPrefix
								+ "Function");
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);
						funcNode.setAttribute("description", description);
						docRoot.appendChild(funcNode);

					}

					// addVariable("$laska::guwhgo", TypeF32,
					if (str.equals("addVariable")) {
						token.nextToken();
						tokType = token.nextToken();
						if (tokType != '"')
							break;
						name = token.sval;
						if (!name.startsWith("$")) {
							name = "$" + name;
						}
						tokType = token.nextToken();
						tokType = token.nextToken();
						description = token.sval;
						if (description == null) {
							description = "";
						}

						description = description.replaceAll("@", "");
						// description = description.replaceAll("\n", "");
						description = description.replaceAll("<", "&lt;");
						description = description.replaceAll(">", "&gt;");
						description = description.replaceAll("&", "&amp;");
						description = description.replaceAll("\"", "&quot;");
						Element funcNode = doc.createElement(currentPrefix
								+ "Global");
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);
						funcNode.setAttribute("description", description);
						docRoot.appendChild(funcNode);

					}

					// class Garbledygook : public Garble, public Gook
					if (str.equals("class")) {
						if (prevTokType == StreamTokenizer.TT_EOL
								|| prevTokType == StreamTokenizer.TT_EOF) {
							tokType = token.nextToken();
							name = token.sval;
							tokType = token.nextToken();
							if (tokType != ':') {
								break;
							}

							Element funcNode = findElementByName(docRoot,
									currentPrefix + "Class", name);
							if (funcNode == null) {
								funcNode = doc.createElement(currentPrefix
										+ "Class");
								docRoot.appendChild(funcNode);
							}

							funcNode.setAttribute("name", name);
							funcNode.setAttribute("class", name);
							funcNode.setAttribute("startLine", Integer
									.toString(token.lineno()));
							funcNode.setAttribute("file", filename);

							tokType = ',';
							while (tokType == ',') {

								tokType = token.nextToken();
								tokType = token.nextToken();
								//Element par = doc.createElement("parent");								
//								par.setAttribute("parentname", token.sval);
//								if (findElementByName(funcNode, "parent", token.sval) == null) {
//									funcNode.appendChild(par);
//								}
								addParent(funcNode, token.sval);
								tokType = token.nextToken();
							}

						}
					}

					// IMPLEMENT_CONOBJECT(t2dSceneObject);
					if (str.equals("IMPLEMENT_CONOBJECT")) {

						token.nextToken();
						tokType = token.nextToken();
						name = token.sval;
						description = "";
						Element funcNode = findElementByName(docRoot,
								currentPrefix + "Class", name);
						if (funcNode == null) {
							funcNode = doc.createElement(currentPrefix
									+ "Class");
							docRoot.appendChild(funcNode);
						}
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("class", name);
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);

					}

					// IMPLEMENT_CO_DATABLOCK_V1(AudioEnvironment);
					if (str.equals("IMPLEMENT_CO_DATABLOCK_V1")) {
						token.nextToken();
						tokType = token.nextToken();
						name = token.sval;
						description = "";
						Element funcNode = doc.createElement(currentPrefix
								+ "Datablock");
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("class", name);
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);
						docRoot.appendChild(funcNode);
					}

					// ConsoleMethod(t2dSceneObject, addToScene, void, 3, 3,
					// "(t2dSceneGraph) - Add to SceneGraph.")
					if (str.equals("ConsoleMethod")) {

						token.nextToken();
						token.nextToken();
						className = token.sval;

						tokType = token.nextToken();
						tokType = token.nextToken();
						name = token.sval;

						tokType = token.nextToken();
						tokType = token.nextToken();
						String returnType = token.sval;

						tokType = token.nextToken();
						tokType = token.nextToken();
						int minArgs = (int) token.nval - 2;

						tokType = token.nextToken();
						tokType = token.nextToken();
						int maxArgs = (int) token.nval - 2;

						tokType = token.nextToken();
						tokType = token.nextToken();
						description = token.sval;
						tokType = token.nextToken();
						while (tokType != ')') {
							description += "\n" + token.sval;
							tokType = token.nextToken();
						}
						if (description == null) {
							description = "";
						}

						description = description.replaceAll("@", "");
						// description = description.replaceAll("\n", "");
						description = description.replaceAll("<", "&lt;");
						description = description.replaceAll(">", "&gt;");
						description = description.replaceAll("&", "&amp;");
						description = description.replaceAll("\"", "&quot;");
						Element parentNode = findElementByName(docRoot,
								currentPrefix + "Class", className);
						if (parentNode == null) {
							parentNode = doc.createElement(currentPrefix + "Class");
							parentNode.setAttribute("name", className);
							parentNode.setAttribute("class", className);
							docRoot.appendChild(parentNode);
						}

						Element funcNode = doc.createElement(currentPrefix + "Method");
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("returnType", returnType);
						funcNode.setAttribute("minArgs", new Integer(minArgs)
								.toString());
						funcNode.setAttribute("maxArgs", new Integer(maxArgs)
								.toString());
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);
						funcNode.setAttribute("description", description);

						parentNode.appendChild(funcNode);

					}

					// addField("useRoom", TypeBool, Offset(mUseRoom,
					// AudioEnvironment));
					// static void addField(const char* in_pFieldname,
					// const U32 in_fieldType,
					// const dsize_t in_fieldOffset,
					// const U32 in_elementCount = 1, //TODO : Z9) array!
					// EnumTable * in_table = NULL,
					// const char* in_pFieldDocs = NULL);
					// addField("class", TypeString, Offset(mClassName,
					// ScriptObject), "Class of object.");
					// addField("fieldName", TypeName, Offset(mMember,
					// className), 5, NULL, "Description");

					if (str.equals("addField")) {
						token.nextToken();
						tokType = token.nextToken();
						if (tokType != '"')
							break;

						name = token.sval;

						tokType = token.nextToken();
						tokType = token.nextToken();
						String type = token.sval;

						tokType = token.nextToken();
						tokType = token.nextToken();
						while (tokType != ',') {
							tokType = token.nextToken();

						}
						tokType = token.nextToken();
						className = token.sval;

						description = "";
						// TODO: fill in description
						// description = token.sval;
						// tokType = token.nextToken();
						// while(tokType != ')' ) {
						// description += "\n" + token.sval;
						// tokType = token.nextToken();
						// }
						// if(description == null) {
						// description = "";
						// }
						//						
						// description = description.replaceAll("@", "");
						// //description = description.replaceAll("\n", "");
						// description = description.replaceAll("<", "&lt;");
						// description = description.replaceAll(">", "&gt;");
						// description = description.replaceAll("&", "&amp;");
						// description = description.replaceAll("\"", "&quot;");

						Element parentNode = findElementByName(docRoot,
								currentPrefix + "Class", className);
						if (parentNode == null) {
							parentNode = findElementByName(docRoot,
									currentPrefix + "Datablock", className);
							if (parentNode == null) {
								parentNode = doc.createElement(currentPrefix
										+ "Class");
								parentNode.setAttribute("name", className);
								parentNode.setAttribute("class", className);
								docRoot.appendChild(parentNode);
							}
						}

						Element funcNode = doc.createElement(currentPrefix
								+ "Field");
						funcNode.setAttribute("name", name);
						funcNode.setAttribute("type", type);
						funcNode.setAttribute("startLine", Integer
								.toString(token.lineno()));
						funcNode.setAttribute("file", filename);
						funcNode.setAttribute("description", description);

						parentNode.appendChild(funcNode);

					}

					break;
				}
				prevTokType = tokType;
				prevString = token.sval;
				tokType = token.nextToken();

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public Element addParent(Element base, String name) {
		Element type = findElementByAttribute(docRoot, null , "name", name);
		Element par = findElementByAttribute(base, "parent", "parentname", name);
		if(par == null) {
			par = doc.createElement("parent");
			par.setAttribute("parentname", name);
			if(type != null) {
				par.setAttribute("parenttype", type.getNodeName());
			}
			base.appendChild(par);
		}
		return par;
		
	}
	
	public Element findElementByAttribute(Element base, String type, String attribute, String name) {
		if(base == null) {
			return null;
		}
		NodeList classes;
		classes = base.getChildNodes();
		
		if (classes == null)
			return null;
		Element iterNode, resultNode = null;
		
		// Element classNode = doc.createElement("class");
		for (int i = 0; i < classes.getLength(); i++) {
			
			iterNode = (Element) classes.item(i);
			if(type == null || iterNode.getNodeName().equals(type)) {			
				if (iterNode.getAttribute(attribute).equals(name)) {
					resultNode = iterNode;
				}
			}
		}
		return resultNode;
	}
	
	public Element findElementByName(Element base, String type, String name) {
		NodeList classes = base.getElementsByTagName(type);
		Element iterNode, resultNode = null;

		// Element classNode = doc.createElement("class");
		for (int i = 0; i < classes.getLength(); i++) {
			iterNode = (Element) classes.item(i);
			if (iterNode.getAttribute("name").equals(name)) {
				resultNode = iterNode;
			}
		}
		return resultNode;
	}

	// protected void startupOnInitialize() {
	//		
	// }

}

class DeltaPrinter implements IResourceDeltaVisitor {
	
	private IProgressMonitor monitor;
	private Document doc;
	private Element docRoot;
	
	public DeltaPrinter(IProgressMonitor monitorv, Document docv, Element docRootv) {
		monitor = monitorv;
		doc = docv;
		docRoot = docRootv;
	}
	
    public boolean visit(IResourceDelta delta) {
       IResource res = delta.getResource();
       String path = res.getFullPath().toString();
       IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
       boolean deleteDso = store.getBoolean(PreferenceConstants.P_DELETE_DSOS);
       if(res.getName().endsWith(".cs") ||
    		   res.getName().endsWith(".t2d") ||
    		   res.getName().endsWith(".gui") ||
    		   res.getName().endsWith(".mis")) {
    	   try {
	    	   TSScriptBuildVisitor visitor;
	    	   switch (delta.getKind()) {
	    	   case IResourceDelta.ADDED:
	    		   if(deleteDso) {
		    		   IPath dsoPath = res.getFullPath().addFileExtension("dso");
		    		   IFile dso = res.getWorkspace().getRoot().getFile(dsoPath);
		    		   if(dso.exists()) {
		    			   dso.delete(false, new NullProgressMonitor());
		    		   }
	    		   }
	    		   visitor = new TSScriptBuildVisitor(new SubProgressMonitor(monitor, 100),	doc, docRoot);
	    		   res.accept(visitor);
	    		   break;
	    	   case IResourceDelta.REMOVED:
	    		   if(deleteDso) {
		    		   IPath dsoPath = res.getFullPath().addFileExtension("dso");
		    		   IFile dso = res.getWorkspace().getRoot().getFile(dsoPath);
		    		   if(dso.exists()) {
		    			   dso.delete(false, new NullProgressMonitor());
		    		   }
	    		   }	    		   
	    		   removeEntriesFromTree(path);
	    		   break;
	    	   case IResourceDelta.CHANGED:
	    		   removeEntriesFromTree(path);
	    		   if(deleteDso) {
		    		   IPath dsoPath = res.getFullPath().addFileExtension("dso");
		    		   IFile dso = res.getWorkspace().getRoot().getFile(dsoPath);
		    		   if(dso.exists()) {
		    			   dso.delete(false, new NullProgressMonitor());
		    		   }
	    		   }
	    		   visitor = new TSScriptBuildVisitor(new SubProgressMonitor(monitor, 100),	doc, docRoot);
	    		   res.accept(visitor);
	    		   break;
	    	   }
    	   }
    	   catch (Exception e) {
    		   e.printStackTrace();
    	   }
       }
       return true; // visit the children
    }
    
    public void removeEntriesFromTree(String filename) {
    	ArrayList<Element> allEntries = findElementsByAttribute(docRoot, null, "file", filename);

    	for(int i = 0; i < allEntries.size(); i++) {
    		try {    			
    			Element e = (Element)allEntries.get(i);

    			docRoot.removeChild(e.getParentNode());
    		}
    		catch (DOMException d) {

    		}

    	}

    }
    
    public ArrayList<Element> findElementsByAttribute(Element base, String type, String attribute, String name) {
    	if(base == null) {
    		return null;
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
					ArrayList<Element> tempAdd = findElementsByAttribute(iterNode, type, attribute, name);
					if(tempAdd != null) {
						results.addAll(tempAdd);
					}
				}
			}
		}
		return results;
    }
    
 }
