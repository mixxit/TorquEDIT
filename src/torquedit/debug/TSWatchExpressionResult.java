package torquedit.debug;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionResult;

public class TSWatchExpressionResult implements IWatchExpressionResult {

	private String text;
	private String value;
	private TSDebugTarget target;
	
	public TSWatchExpressionResult(String exp, String val, TSDebugTarget targ) {
		text = exp;
		value = val;
		target = targ;
	}
	
	
	public String[] getErrorMessages() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public DebugException getException() {
		// TODO Auto-generated method stub
		return null;
	}

	
	public String getExpressionText() {
		return text;
	}

	
	public IValue getValue() {
		return new TSValue(target,value);
	}


	public boolean hasErrors() {
		// TODO Auto-generated method stub
		return false;
	}

}
