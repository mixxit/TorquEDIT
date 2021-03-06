package torquedit.tseditor.util;

import org.eclipse.jface.text.rules.IWordDetector;


/**
 * Detects words that are global variables
 */
public class TSGlobalVariableWordDetector
	implements IWordDetector {

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		return (c=='$');
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		return Character.isLetter(c) || Character.isDigit(c) || c=='_' || c==':';
	}

}
