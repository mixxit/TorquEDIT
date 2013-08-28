package torquedit.debug;

import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;

public class TSWatchExpressionDelegate implements IWatchExpressionDelegate {

	
	public void evaluateExpression(String expression, IDebugElement context,
			IWatchExpressionListener listener) {
		// TODO Auto-generated method stub
		System.out.println("Evaluate expression called... " + expression + " " + context.getDebugTarget().getModelIdentifier());
		TSDebugTarget target = (TSDebugTarget)context.getDebugTarget();
		target.evaluateExpression(expression, context, listener);
	}

}
