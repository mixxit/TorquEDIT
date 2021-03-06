package torquedit.tseditor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.rules.EndOfLineRule;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordPatternRule;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import torquedit.preferences.PreferenceConstants;
import torquedit.tseditor.util.CaseInsensitiveWordRule;
import torquedit.tseditor.util.ColorProvider;
import torquedit.tseditor.util.ResourceLoader;
import torquedit.tseditor.util.TSClassesWordDetector;
import torquedit.tseditor.util.TSFunctionsWordDetector;
import torquedit.tseditor.util.TSGlobalVariableWordDetector;
import torquedit.tseditor.util.TSKeywordDetector;
import torquedit.tseditor.util.TSLocalVariableWordDetector;
import torquedit.tseditor.util.TSWhiteSpaceDetector;
import torquedit.tseditor.util.XmlResource;



/**
 * Sets rules for syntax highlighting
 */
public class TSRuleScanner
	extends RuleBasedScanner {
	
	/**
	 * Stuff
	 */
	ColorProvider cp;
	Map<String, Token> tokens = new HashMap<String, Token>();
	IProject project;

	/**
	 * Creates an instance of the RuleScanner
	 */
	public TSRuleScanner(ColorProvider colp) {
			
		cp = colp;
	
	}

	public void setProject(IProject proj) {
		project = proj;
		
	}
	
	/**
	 * Utility method for adding words to a CaseInsensitiveWordRule
	 */
	private void addWordsToWordRule(CaseInsensitiveWordRule wordRule, List<?> words, IToken token) {
		for (int i=0; i<words.size(); i++) {
			XmlResource resource = (XmlResource)words.get(i);
			wordRule.addWord(resource.getName(), token);
		}
	}

	/**
	 * Utility method for adding words to a rule using the runtime-generated AST
	 * @param wordRule
	 * @param words
	 * @param token
	 */
	private void addWordsToWordRuleLive(CaseInsensitiveWordRule wordRule, NodeList words, IToken token) {
		if(words != null && words.getLength() > 0) {
			for(int i = 0; i < words.getLength(); i++) {
				Element e = (Element)words.item(i);
				wordRule.addWord(e.getAttribute("name"), token);
			}
		}
	}
	
	/**
	 * Creates colored text attribute tokens
	 */
	private Token coloredTxtAttrToken(String name) {
		Token t = null;
		if (tokens.containsKey(name)) {
			t = (Token)tokens.get(name);
		}
		if(t == null) {
			t = new Token(new TextAttribute(cp.getColor(name)));
			tokens.put(name, t);
		}
		return t;
	}
	
	public void refreshColor(PropertyChangeEvent event) {
		if(event.getNewValue() instanceof RGB) {
			Token t = (Token) tokens.get(event.getProperty());

			if(t==null)
				return;

			Color c = cp.getColor(event.getProperty());
			try {
			//if(c != null) {
				RGB rgb = c.getRGB();
				RGB ergb = (RGB)event.getNewValue();
				TextAttribute oldAttr = (TextAttribute)t.getData();
				t.setData( new TextAttribute(c, oldAttr.getBackground(), oldAttr.getStyle()));
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public void refreshRules() {
		try {
			// create some tokens
			coloredTxtAttrToken(PreferenceConstants.P_STRING);
			coloredTxtAttrToken(PreferenceConstants.P_COMMENT);
			coloredTxtAttrToken(PreferenceConstants.P_LOCAL);
			coloredTxtAttrToken(PreferenceConstants.P_GLOBAL);
			coloredTxtAttrToken(PreferenceConstants.P_KEYWORD);
			coloredTxtAttrToken(PreferenceConstants.P_CLASSES);
			coloredTxtAttrToken(PreferenceConstants.P_FUNCTIONS);
			coloredTxtAttrToken(PreferenceConstants.P_DEFAULT);
			
			IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
			boolean tgb = store.getBoolean(PreferenceConstants.P_TGB_CHECK);
			boolean tge = store.getBoolean(PreferenceConstants.P_TGE_CHECK);
			boolean tgea = store.getBoolean(PreferenceConstants.P_TGEA_CHECK);

			Document doc;
			Element docRoot = null;
			doc = TorquEDITPlugin.getDefault().getAST(project);
			if(doc != null) {
				docRoot = (Element)doc.getElementsByTagName("root").item(0);
			}
			
			// List for ease of addition
			List<IRule> rules = new ArrayList<IRule>();
			
			// default token
			setDefaultReturnToken((Token)tokens.get(PreferenceConstants.P_DEFAULT));
			
			// add rule for white space
			rules.add(new WhitespaceRule(new TSWhiteSpaceDetector()));
			
			// add a rule for comments
			rules.add(new EndOfLineRule("//", (Token)tokens.get(PreferenceConstants.P_COMMENT)));
			
			// add rule for strings
			rules.add(new SingleLineRule("\"", "\"", (Token)tokens.get(PreferenceConstants.P_STRING), '\\'));
			rules.add(new SingleLineRule("'", "'", (Token)tokens.get(PreferenceConstants.P_STRING), '\\'));
			
			//rules.add(new PatternRule("function ", "(", classes, '\\', true));
			
			// local variables
			WordPatternRule localVarsRule = new WordPatternRule(
					new TSLocalVariableWordDetector(), "%", "", (Token)tokens.get(PreferenceConstants.P_LOCAL));
			rules.add(localVarsRule);
			
			// global variables
			WordPatternRule globalVarsRule = new WordPatternRule(
					new TSGlobalVariableWordDetector(), "$", "", (Token)tokens.get(PreferenceConstants.P_GLOBAL));
			rules.add(globalVarsRule);

			// add rule for classes
			CaseInsensitiveWordRule classesRule = new CaseInsensitiveWordRule(new TSClassesWordDetector());

			// add rule for functions
			CaseInsensitiveWordRule functionsRule = new CaseInsensitiveWordRule(new TSFunctionsWordDetector());

			CaseInsensitiveWordRule scriptGlobalsRule = new CaseInsensitiveWordRule(new TSClassesWordDetector());

			CaseInsensitiveWordRule scriptLocalsRule = new CaseInsensitiveWordRule(new TSClassesWordDetector());
			
			NodeList list;
			
			if(docRoot != null) {
				list = docRoot.getElementsByTagName("class");
				addWordsToWordRuleLive(classesRule, list, (Token)tokens.get(PreferenceConstants.P_CLASSES));

				list = docRoot.getElementsByTagName("function");
				addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));

				list = docRoot.getElementsByTagName("method");
				addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));
				
				list = docRoot.getElementsByTagName("global");
				addWordsToWordRuleLive(scriptGlobalsRule, list, (Token)tokens.get(PreferenceConstants.P_GLOBAL));

				list = docRoot.getElementsByTagName("local");
				addWordsToWordRuleLive(scriptLocalsRule, list, (Token)tokens.get(PreferenceConstants.P_LOCAL));

				list = docRoot.getElementsByTagName("field");
				addWordsToWordRuleLive(scriptLocalsRule, list, (Token)tokens.get(PreferenceConstants.P_LOCAL));
				
				if(tgb) {
					list = docRoot.getElementsByTagName("tgbClass");
					addWordsToWordRuleLive(classesRule, list, (Token)tokens.get(PreferenceConstants.P_CLASSES));
					
					list = docRoot.getElementsByTagName("tgbFunction");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));
					
					list = docRoot.getElementsByTagName("tgbMethod");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));

				}

				if(tge) {
					list = docRoot.getElementsByTagName("tgeClass");
					addWordsToWordRuleLive(classesRule, list, (Token)tokens.get(PreferenceConstants.P_CLASSES));
					
					list = docRoot.getElementsByTagName("tgeFunction");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));
					
					list = docRoot.getElementsByTagName("tgeMethod");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));

				}

				if(tgea) {
					list = docRoot.getElementsByTagName("tgeaClass");
					addWordsToWordRuleLive(classesRule, list, (Token)tokens.get(PreferenceConstants.P_CLASSES));
					
					list = docRoot.getElementsByTagName("tgeaFunction");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));
					
					list = docRoot.getElementsByTagName("tgeaMethod");
					addWordsToWordRuleLive(functionsRule, list, (Token)tokens.get(PreferenceConstants.P_FUNCTIONS));

				}
				
			}
			
			rules.add(classesRule);
			rules.add(functionsRule);
			rules.add(scriptGlobalsRule);
			rules.add(scriptLocalsRule);
			
			// add rule for keywords
			CaseInsensitiveWordRule keywordsRule = new CaseInsensitiveWordRule(new TSKeywordDetector());
			addWordsToWordRule(
					keywordsRule,
					ResourceLoader.loadXmlResource("torquedit/tseditor/resources/keywords.xml"),
					(Token)tokens.get(PreferenceConstants.P_KEYWORD)
			);
			rules.add(keywordsRule);
			
			// create an array of IRules and copy the
			// rules from the List into the array
			IRule[] retRules = new IRule[rules.size()];
			rules.toArray(retRules);
			
			// set the rules
			setRules(retRules);
			
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
