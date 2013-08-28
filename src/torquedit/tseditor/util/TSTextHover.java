package torquedit.tseditor.util;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;

/**
 * created on Feb 11, 2005
 * @author brian
 */
public class TSTextHover
	implements ITextHover {

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.ITextHover#getHoverInfo(org.eclipse.jface.text.ITextViewer, org.eclipse.jface.text.IRegion)
	 */
	public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion) {
		return null;
	}

	/**
	 * Inherited method.
	 * @see org.eclipse.jface.text.ITextHover#getHoverRegion(org.eclipse.jface.text.ITextViewer, int)
	 */
	public IRegion getHoverRegion(ITextViewer textViewer, int offset) {
		System.out.print(getHoverText(textViewer.getDocument(), offset));
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
    private String getHoverText(IDocument document, int offset) {
        try {
            int startPos = 0, endPos = 0;
            
            // get start
            for (int pos=offset; pos>0; pos--) {
            	char c = document.getChar(pos);
            	if (c=='\"') {
            		startPos = pos+1;
            		break;
            	} else if (c=='\n' || c=='\r') {
            		return "";
            	}
            }
            
             // get start
            for (int pos=offset; pos>0; pos++) {
            	char c = document.getChar(pos);
            	if (c=='\"') {
            		endPos = pos;
            		break;
            	} else if (c=='\n' || c=='\r') {
            		return "";
            	}
            }
            
            //String hover = document.get(startPos, endPos-startPos);
            String hover = "";
            //MessageDialog.openInformation(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "Preview", hover);
            
            return hover;
        } catch (BadLocationException e) {
            e.printStackTrace();
            return "";
        }
    }
	
}
