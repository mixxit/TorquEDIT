package torquedit.tseditor.util;

import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.PatternRule;

public class TSScriptClassPatternRule extends PatternRule {

	public TSScriptClassPatternRule(String startSequence, String endSequence,
			IToken token, char escapeCharacter, boolean breaksOnEOL) {
		super(startSequence, endSequence, token, escapeCharacter, breaksOnEOL);
		// TODO Auto-generated constructor stub
	}

	public TSScriptClassPatternRule(String startSequence, String endSequence,
			IToken token, char escapeCharacter, boolean breaksOnEOL,
			boolean breaksOnEOF) {
		super(startSequence, endSequence, token, escapeCharacter, breaksOnEOL,
				breaksOnEOF);
		// TODO Auto-generated constructor stub
	}

	public TSScriptClassPatternRule(String startSequence, String endSequence,
			IToken token, char escapeCharacter, boolean breaksOnEOL,
			boolean breaksOnEOF, boolean escapeContinuesLine) {
		super(startSequence, endSequence, token, escapeCharacter, breaksOnEOL,
				breaksOnEOF, escapeContinuesLine);
		// TODO Auto-generated constructor stub
	}

	
	
}
