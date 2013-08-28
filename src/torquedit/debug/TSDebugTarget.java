/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package torquedit.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.eclipse.debug.ui.console.ConsoleColorProvider;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.ui.externaltools.internal.model.IExternalToolConstants;

import torquedit.tseditor.ITSConstants;

/**
 * PDA Debug Target
 */
public class TSDebugTarget extends TSDebugElement implements IDebugTarget {

	// associated system process (VM)
	private IProcess fProcess;

	// containing launch object
	private ILaunch fLaunch;

	// program name
	private String fName;

	// sockets to communicate with VM
	private Socket fRequestSocket;
	private PrintWriter fRequestWriter;
	private BufferedReader fRequestReader;
	//private Socket fEventSocket;
	//private BufferedReader fEventReader;

	// suspend state
	private boolean fSuspended = true;

	// terminated state
	private boolean fTerminated = false;

	// threads
	private TSThread fThread;
	private IThread[] fThreads;

	// event dispatch job
	private EventDispatchJob fEventDispatch;

	// message output console
	private MessageConsoleStream fConsole;

	// stack frames
	private IStackFrame[] fStackFrames;
	
	// watch expressions
	private Map<String, IWatchExpressionListener> fWatchExpressionListeners;
	private Map<String, String> fWatchExpressions;
	private int fWatchExpressionCounter;
	
	/**
	 * Listens to events from the PDA VM and fires corresponding 
	 * debug events.
	 */
	class EventDispatchJob extends Job {

		public EventDispatchJob() {
			super("TS Debug Event Dispatch");
			setSystem(true);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		protected IStatus run(IProgressMonitor monitor) {
			String event = "";

			while (!fTerminated && !isTerminated() && event != null) {
				try {
					event = fRequestReader.readLine();
					if (event != null) {
						System.out.println(event);
						fThread.setBreakpoints(null);
						fThread.setStepping(false);
						if (event.equals("PASS Connected.")) {
							started();
						} 
						else if (event.equals("PASS WrongPassword.")) {
							terminated();
						} 
						else if (event.startsWith("COUT")) {
							handleConsoleOutput(event);
						} 
						else if (event.startsWith("BREAK")) {

							/*if (event.endsWith("client")) {
								suspended(DebugEvent.CLIENT_REQUEST);
							} else if (event.endsWith("step")) {
								suspended(DebugEvent.STEP_END);
							} else if (event.indexOf("breakpoint") >= 0) {
								breakpointHit(event);
							}*/
							breakpointHit(event);
						}
						else if (event.startsWith("EVALOUT")) {
							evalHit(event);
						}
					}
				} catch (IOException e) {
					//e.printStackTrace();
					terminated();
				}
			}
			return Status.OK_STATUS;
		}

	}


	public void handleConsoleOutput(String event) {

		
		
		fConsole.println(event.substring(5));
		if(event.contains("parse error")) {
			//Color oldColor = fConsole.getColor();
			
			//fConsole.setColor();
			
			String filePath = event.substring(5, event.indexOf("Line:")-1);
			IPath path = new Path(filePath);
			IResource[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(path);

			if(files.length > 0) {
//				Full path.cs Line: 34 - parse error
				String line = event.substring(event.indexOf("Line:")+6, event.lastIndexOf('-')-1);
				int lineNum = Integer.parseInt(line);
				IMarker m;
				try {
					m = files[0].createMarker(IMarker.PROBLEM);
					m.setAttribute(IMarker.LINE_NUMBER, lineNum);
					m.setAttribute(IMarker.MESSAGE, "Parse error");
					m.setAttribute(IMarker.PRIORITY,IMarker.PRIORITY_HIGH);
					m.setAttribute(IMarker.SEVERITY,IMarker.SEVERITY_ERROR);
					
				} catch (CoreException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
	
	private MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		//no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[]{myConsole});
		return myConsole;
	}	
	
	public void evalHit(String event) {
		String[] tokens = event.split(" "); 
		
		IWatchExpressionListener listen = fWatchExpressionListeners.get(tokens[1]);
		String exp = fWatchExpressions.get(tokens[1]);
		IWatchExpressionResult result = new TSWatchExpressionResult(exp, tokens[2], this);
		listen.watchEvaluationFinished(result);
		
	}
	/**
	 * Constructs a new debug target in the given launch for the 
	 * associated PDA VM process.
	 * 
	 * @param launch containing launch
	 * @param process PDA VM
	 * @param requestPort port to send requests to the VM
	 * @param eventPort port to read events from
	 * @exception CoreException if unable to connect to host
	 */
	public TSDebugTarget(ILaunch launch, IProcess process, int requestPort) throws CoreException {
		super(null);
		fLaunch = launch;
		fTarget = this;
		fProcess = process;
		int numRetries = 10;
		Exception err = null;
		
		MessageConsole myConsole = findConsole("torquedit");
		fConsole = myConsole.newMessageStream();
		fConsole.println("Console started.");
				
	
		// TODO inject dbgSetParameters(port, password, wait); into main.cs file.

		for(int i = 0; i < numRetries; i++) {

			try {
				Thread.sleep(1000);
				fRequestSocket = new Socket("localhost", requestPort);
				fRequestWriter = new PrintWriter(fRequestSocket.getOutputStream());
				fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
				//fEventSocket = new Socket("localhost", eventPort);
				//fEventReader = new BufferedReader(new InputStreamReader(fEventSocket.getInputStream()));
				break;


			} catch (UnknownHostException e) {
				abort("Unable to connect to Torque, unknown host", e);
			} catch (IOException e) {
				System.out.println("Could not connect to torque, retrying ");
				err = e;
			} catch (Exception e) {
				abort("Error while attempting to connect to Torque",e);
			}
		}
		if(!fRequestSocket.isConnected()) {
			abort("FAIL: Unable to connect to Torque.", err);
		}
		fThread = new TSThread(this);
		fThreads = new IThread[] {fThread};
		fEventDispatch = new EventDispatchJob();
		fEventDispatch.schedule();
		fWatchExpressions = new TreeMap();
		fWatchExpressionListeners = new TreeMap();
		fWatchExpressionCounter = 0;
		// Send password... the first step in the connection protocol
		sendRequest("torquedit");

		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		//DebugPlugin.getDefault().getExpressionManager().newWatchExpressionDelegate(ITSConstants.ID);
		//DebugPlugin.getDefault().addDebugEventListener(this);
		
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess() {
		return fProcess;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
	 */
	public IThread[] getThreads() throws DebugException {
		return fThreads;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	public boolean hasThreads() throws DebugException {
		return true; // WTB Changed per bug #138600
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException {
		if (fName == null) {
			fName = "Program";
			try {
				fName = getLaunch().getLaunchConfiguration().getAttribute(IExternalToolConstants.ATTR_LOCATION, "Torque Executable");
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
		return fName;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (breakpoint.getModelIdentifier().equals(ITSConstants.ID)) {
			/*try {
				String program = getLaunch().getLaunchConfiguration().getAttribute(IExternalToolConstants.ATTR_LOCATION, (String)null);
				if (program != null) {
					IMarker marker = breakpoint.getMarker();
					if (marker != null) {
						IPath p = new Path(program);
						return marker.getResource().getFullPath().equals(p);
					}
				}
			} catch (CoreException e) {
			}	*/
			return true;
		}
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getDebugTarget()
	 */
	public IDebugTarget getDebugTarget() {
		return this;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() {
		return fLaunch;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate() {
		return getProcess().canTerminate();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated() {
		return getProcess().isTerminated();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException {
		sendRequest("CONTINUE");
		sendRequest("CEVAL quit();");
		
		try {
			fRequestSocket.close();
			fRequestReader.close();
			fRequestWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		terminated();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended() {
		return fSuspended;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException {
		sendRequest("CONTINUE");
		resumed(DebugEvent.RESUME);
	}

	/**
	 * Notification the target has resumed for the given reason
	 * 
	 * @param detail reason for the resume
	 */
	private void resumed(int detail) {
		fSuspended = false;
		fThread.fireResumeEvent(detail);
	}

	/**
	 * Notification the target has suspended for the given reason
	 * 
	 * @param detail reason for the suspend
	 */
	private void suspended(int detail) {
		fSuspended = true;
		fThread.fireSuspendEvent(detail);
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException {
		
		sendRequest("BRKNEXT");
		suspended(DebugEvent.SUSPEND);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(org.eclipse.debug.core.model.IBreakpoint)
	 */
	public void breakpointAdded(IBreakpoint breakpoint) {
//		BRKSET file line clear passCount expr
//		This sets a breakpoint on a particular file and line. It must pass passCount number times for it to break. If clear is true, the breakpoint will be cleared after hit. Use passCount 0 to break always. The expr will be evaluated when the breakpoint is reached and break if the result is true.
//		If the protocol version is 1 and the file is not currently loaded or the line is not a breakable line then the command is ignored and no breakpoint is set.
//		If the protocol version is 2 and the line is not a breakable line a BRKMOV or BRKCLR message is sent back to the client.
//		The expression is not optional, if you want to always break, pass true.
		System.out.println("Entered breakpointAdded");
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled()) {
					try {
						//IMarker marker = ((IFile) breakpoint.getMarker().getResource()).createMarker("torquedit.breakmarker");
						//marker.setAttribute(IMarker.LINE_NUMBER,((ILineBreakpoint)breakpoint).getLineNumber());
						
						sendRequest("BRKSET " +
								((ILineBreakpoint)breakpoint).getMarker().getResource().getProjectRelativePath().toString() + " " +
								(((ILineBreakpoint)breakpoint).getLineNumber() - 0) + " " +
								"false " +
								"0 " +
								"true"
						);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				sendRequest("BRKCLR " + 
						((ILineBreakpoint)breakpoint).getMarker().getResource().getProjectRelativePath().toString() + " " + 
						((ILineBreakpoint)breakpoint).getLineNumber());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(org.eclipse.debug.core.model.IBreakpoint, org.eclipse.core.resources.IMarkerDelta)
	 */
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#canDisconnect()
	 */
	public boolean canDisconnect() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
	 */
	public void disconnect() throws DebugException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#isDisconnected()
	 */
	public boolean isDisconnected() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#supportsStorageRetrieval()
	 */
	public boolean supportsStorageRetrieval() {
		return false;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long, long)
	 */
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}

	/**
	 * Notification we have connected to the VM and it has started.
	 * Resume the VM.
	 */
	private void started() {
		fireCreationEvent();
		installDeferredBreakpoints();
		try {
			resume();
		} catch (DebugException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint
	 * manager.
	 */
	private void installDeferredBreakpoints() {
		IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ITSConstants.ID);
		for (int i = 0; i < breakpoints.length; i++) {
			breakpointAdded(breakpoints[i]);
		}
	}

	/**
	 * Called when this debug target terminates.
	 */
	private void terminated() {
		fTerminated = true;
		fSuspended = false;
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		fireTerminateEvent();
		try {
			if(fRequestSocket.isConnected()) {
				fRequestSocket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the current stack frames in the target.
	 * 
	 * @return the current stack frames in the target
	 * @throws DebugException if unable to perform the request
	 */
	protected IStackFrame[] getStackFrames() throws DebugException {
		/*synchronized (fRequestSocket) {
			fRequestWriter.println("stack");
			fRequestWriter.flush();
			try {
				String framesData = fRequestReader.readLine();
				if (framesData != null) {
					String[] frames = framesData.split("#");
					IStackFrame[] theFrames = new IStackFrame[frames.length];
					for (int i = 0; i < frames.length; i++) {
						String data = frames[i];
						theFrames[frames.length - i - 1] = new TSStackFrame(fThread, data, i);
					}
					return theFrames;
				}
			} catch (IOException e) {
				abort("Unable to retrieve stack frames", e);
			}
		}*/
		if(fStackFrames != null) {
			return fStackFrames;
		}
		return new IStackFrame[0];
	}

	/**
	 * Single step the interpreter.
	 * 
	 * @throws DebugException if the request fails
	 */
	protected void stepOver() throws DebugException {

		sendRequest("STEPOVER");
		fThread.fireSuspendEvent(DebugEvent.STEP_OVER);
	}

	/**
	 * Single step the interpreter.
	 * 
	 * @throws DebugException if the request fails
	 */
	protected void stepInto() throws DebugException {

		sendRequest("STEPIN");
		fThread.fireSuspendEvent(DebugEvent.STEP_INTO);
	}

	/**
	 * Single step the interpreter.
	 * 
	 * @throws DebugException if the request fails
	 */
	protected void stepReturn() throws DebugException {

		sendRequest("STEPOUT");
		fThread.fireSuspendEvent(DebugEvent.STEP_RETURN);
	}
	

	/**
	 * Returns the current value of the given variable.
	 * 
	 * @param variable
	 * @return variable value
	 * @throws DebugException if the request fails
	 */
	protected IValue getVariableValue(TSVariable variable) throws DebugException {
		/*synchronized (fRequestSocket) {
			fRequestWriter.println("var " + variable.getStackFrame().getIdentifier() + " " + variable.getName());
			fRequestWriter.flush();
			try {
				String value = fRequestReader.readLine();
				return new TSValue(this, value);
			} catch (IOException e) {
				abort(MessageFormat.format("Unable to retrieve value for variable {0}", new String[]{variable.getName()}), e);
			}
		}*/
		return null;
	}

	/**
	 * Returns the values on the data stack (top down)
	 * 
	 * @return the values on the data stack (top down)
	 */
	public IValue[] getDataStack() throws DebugException {
/*		synchronized (fRequestSocket) {
			fRequestWriter.println("data");
			fRequestWriter.flush();
			try {
				String valueString = fRequestReader.readLine();
				if (valueString != null && valueString.length() > 0) {
					String[] values = valueString.split("\\|");
					IValue[] theValues = new IValue[values.length];
					for (int i = 0; i < values.length; i++) {
						String value = values[values.length - i - 1];
						theValues[i] = new TSValue(this, value);
					}
					return theValues;
				}
			} catch (IOException e) {
				abort("Unable to retrieve data stack", e);
			}
		}*/
		return new IValue[0];		
	}

	/**
	 * Sends a request to the PDA VM and waits for an OK.
	 * 
	 * @param request debug command
	 * @throws DebugException if the request fails
	 */
	private void sendRequest(String request) throws DebugException {
		System.out.println("Sending request: " + request);
		synchronized (fRequestSocket) {
			fRequestWriter.println(request);
			fRequestWriter.flush();
			/*
			try {
				// wait for "ok"
				String response = fRequestReader.readLine();
				System.out.println("Received response: " + response);
			} catch (IOException e) {
				abort("Request failed: " + request, e);
			}
			*/
		}		
	}

	
	public void sendEvalRequest(String expression, String tag, int stack) throws DebugException {
		sendRequest("EVAL " + tag + " " + stack + " " + expression);
	}
	
	
	/**
	 * Notification a breakpoint was encountered. Determine
	 * which breakpoint was hit and fire a suspend event.
	 * 
	 * @param event debug event
	 */
	private void breakpointHit(String event) {
		// determine which breakpoint was hit, and set the thread's breakpoint
		//BREAK file1 line1 function1 file2 line2 function2 ...
		//Sent when the debugger hits a breakpoint. 
		//It lists out one file/line/function triplet for each stack level. 
		//The first one is the top of the stack.

		try {

			
			String[] tokens = event.split(" ");
			int numFrames = (tokens.length-1)/3;
			fStackFrames = new IStackFrame[numFrames];
			for(int t = 0; t < fStackFrames.length; t++) {
				fStackFrames[t] = 
					new TSStackFrame(fThread, 
							tokens[t*3+1], 
							Integer.parseInt(tokens[t*3+2]),
							tokens[t*3+3],
							t);
			}
			String file = tokens[1];
			int line = Integer.parseInt(tokens[2]);
		
			IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(ITSConstants.ID);
			for (int i = 0; i < breakpoints.length; i++) {
				IBreakpoint breakpoint = breakpoints[i];
				if (supportsBreakpoint(breakpoint)) {
					if (breakpoint instanceof ILineBreakpoint) {
						ILineBreakpoint lineBreakpoint = (ILineBreakpoint) breakpoint;
						try {
							String relPath = lineBreakpoint.getMarker().getResource().getProjectRelativePath().toString();
							if(relPath.equals(file)) {
								if (lineBreakpoint.getLineNumber() == line) {
									fThread.setBreakpoints(new IBreakpoint[]{breakpoint});
									break;
								}
							}
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		suspended(DebugEvent.BREAKPOINT);
	}
	public void evaluateExpression(String expression, IDebugElement context,
			IWatchExpressionListener listener) {
		IExpression[] exps = DebugPlugin.getDefault().getExpressionManager().getExpressions();
		IExpression we;
		for(int i = 0; i < exps.length; i++) {
			we = exps[i];
			if(we.getExpressionText().equals(expression)) {
				try {
					fWatchExpressionCounter++;
					String tag = "watch"+fWatchExpressionCounter;
					sendEvalRequest(expression, tag, 0);
					fWatchExpressions.put(tag, expression);
					fWatchExpressionListeners.put(tag, listener);
				}
				catch (DebugException e) {
					System.out.println("Watch expression exception");
					e.printStackTrace();
				}
				
			}
		}
		
	}
}
