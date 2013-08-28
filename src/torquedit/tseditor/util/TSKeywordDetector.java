package torquedit.tseditor.util;

import org.eclipse.jface.text.rules.IWordDetector;


/**
 * Detects words that are keywords
 */
public class TSKeywordDetector
	implements IWordDetector {

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordStart(char)
	 */
	public boolean isWordStart(char c) {
		return Character.isLetter(c);
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.rules.IWordDetector#isWordPart(char)
	 */
	public boolean isWordPart(char c) {
		return Character.isLetter(c) || c=='$';
	}

}
