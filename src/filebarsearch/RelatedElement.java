package filebarsearch;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

public class RelatedElement{
	public int topLine;
	public int bottomLine;
	public int startOffset;
	public int length;
	public Type type;
	public IJavaElement element;


	public enum Type {
	    REFERENCE,
	    DECLARATION,
		VARIABLE_TYPE_DECLARATION,
		SUPERCLASS_DECLARATION,
		INTERFACE_DECLARATION,
		SUBCLASS_DECLARATION,
		BROTHERCLASS_DECLARATION,
		INTERNAL_INVOCATION,
		OVERLOAD_METHOD,
		INTERFACE_METHOD,
		OVERRIDE_METHOD,
		OVERRIDED_METHOD,
	}

	public RelatedElement(int topLine, int bottomLine, int startOffset, int length, Type type, IJavaElement element) {
		super();
		this.topLine = topLine;
		this.bottomLine = bottomLine;
		this.startOffset = startOffset;
		this.length = length;
		this.type = type;
		this.element = element;
	}


	public RelatedElement(IDocument document, ISourceRange range, Type type, IJavaElement element) {
		super();

		int top = 0;
		int bottom = 0;
		try {
			top = document.getLineOfOffset(range.getOffset());
			bottom = document.getLineOfOffset(range.getOffset()+range.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

		this.topLine = top;
		this.bottomLine = bottom;
		this.startOffset = range.getOffset();
		this.length = range.getLength();
		this.type = type;
		this.element = element;
	}

	public double getWeight(){
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		switch(type){
		case REFERENCE:
			return store.getDouble(PreferenceConstants.WEIGHT_REFERENCE);
		case DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_DECLARATION);
		case VARIABLE_TYPE_DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_VARIABLE_TYPE_DECLARATION);
		case SUPERCLASS_DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_SUPERCLASS_DECLARATION);
		case INTERFACE_DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_INTERFACE_DECLARATION);
		case SUBCLASS_DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_SUBCLASS_DECLARATION);
		case BROTHERCLASS_DECLARATION:
			return store.getDouble(PreferenceConstants.WEIGHT_BROTHERCLASS_DECLARATION);
		case INTERNAL_INVOCATION:
			return store.getDouble(PreferenceConstants.WEIGHT_REFERENCE);
		case OVERLOAD_METHOD:
			return store.getDouble(PreferenceConstants.WEIGHT_OVERLOAD_METHOD);
		case INTERFACE_METHOD:
			return store.getDouble(PreferenceConstants.WEIGHT_INTERFACE_METHOD);
		case OVERRIDE_METHOD:
			return store.getDouble(PreferenceConstants.WEIGHT_OVERRIDE_METHOD);
		case OVERRIDED_METHOD:
			return store.getDouble(PreferenceConstants.WEIGHT_OVERRIDED_METHOD);
		}
		return 1.0;
	}
}