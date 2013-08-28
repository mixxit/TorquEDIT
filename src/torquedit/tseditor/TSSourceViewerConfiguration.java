package torquedit.tseditor;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import torquedit.preferences.PreferenceConstants;
import torquedit.tseditor.util.ColorProvider;
import torquedit.tseditor.util.TSAutoIndentStrategy;
import torquedit.tseditor.util.TSCompletionProcessor;
import torquedit.tseditor.util.TSTextHover;


/**
 * The SourceViewerConfiguration object
 */
public class TSSourceViewerConfiguration
	extends TextSourceViewerConfiguration {
	
	/**
	 * Stuff :P
	 */
	private TSRuleScanner 			scanner 			= null;
	private IAutoEditStrategy		autoEditStrategy 	= null;
	private ColorProvider			cp					= null;
	private TSCompletionProcessor	completionProcessor	= null;
	private TSTextHover				textHover			= null;
	private ContentAssistant		assistant			= null;
	
	DefaultDamagerRepairer			damageRepairer		= null;
	PresentationReconciler			reconciler			= null;
	
	
	/**
	 * Creates the soruce viewer configuration.
	 */
	public TSSourceViewerConfiguration(TSCompletionProcessor tscp, ColorProvider colorp, TSRuleScanner rs) {
		super();
		
		autoEditStrategy 		= new TSAutoIndentStrategy();
		completionProcessor		= tscp;
		cp 						= colorp;
		scanner 				= rs;
		textHover				= new TSTextHover();

	}
	
	/**
	 * Returns the PresentationReconciler
	 */
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		reconciler = new PresentationReconciler();
		damageRepairer = new DefaultDamagerRepairer(scanner);
		
		reconciler.setDamager(damageRepairer, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(damageRepairer, IDocument.DEFAULT_CONTENT_TYPE);
		
		return reconciler;
	}
	
	/**
	 * Returns the auto edit strategy, we use this for indention
	 */
	public IAutoEditStrategy getAutoEditStrategy(ISourceViewer sourceViewer, String contentType) {
		return autoEditStrategy;
	}

	/**
	 * Returns the content assistant, we use this for
	 * auto completion
	 */
	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
		if(assistant != null){
			return assistant;
		}
		else {
			IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
			assistant = new ContentAssistant();
			assistant.setContentAssistProcessor(completionProcessor, IDocument.DEFAULT_CONTENT_TYPE);
			assistant.setInformationControlCreator(getInformationControlCreator(sourceViewer));
			assistant.enableAutoInsert(true);
			assistant.enableAutoActivation(true);
			assistant.setAutoActivationDelay(store.getInt(PreferenceConstants.P_POPUP_DELAY));
			assistant.setProposalSelectorBackground(cp.getColor(PreferenceConstants.P_AUTO_COMPLETE_BG));
			assistant.setProposalSelectorForeground(cp.getColor(PreferenceConstants.P_AUTO_COMPLETE_FG));
			assistant.setProposalPopupOrientation(IContentAssistant.CONTEXT_INFO_BELOW);
			assistant.setProposalPopupOrientation(IContentAssistant.PROPOSAL_STACKED);
			
			return assistant;
		}
	}

	public void updateSettings(ISourceViewer sourceViewer, PropertyChangeEvent event) {
		try { 
			IPreferenceStore store = TorquEDITPlugin.getDefault().getPreferenceStore();
			//assistant.uninstall();
			//assistant = null;
			if(event.getNewValue() instanceof RGB) {
				cp.refreshColor(event);
				scanner.refreshColor(event);
				assistant.setProposalSelectorBackground(cp.getColor(PreferenceConstants.P_AUTO_COMPLETE_BG));
				assistant.setProposalSelectorForeground(cp.getColor(PreferenceConstants.P_AUTO_COMPLETE_FG));
				sourceViewer.invalidateTextPresentation();
			}
			if(event.getNewValue() instanceof Boolean) {
				scanner.refreshRules();
			}
			if(event.getNewValue() instanceof Integer) {
				assistant.setAutoActivationDelay(store.getInt(PreferenceConstants.P_POPUP_DELAY));		
			}
			
			//getContentAssistant(sourceViewer);
			//assistant.install(sourceViewer);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * For events when the cursor hovers over text
	 */
	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
		return textHover;
	}
	
}
