package filebarsearch;

import java.util.HashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;

public class OpenEditorCommandHandler extends AbstractHandler {
	public HashMap<IFile,FileElements> fileElementsMap;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		System.out.println("hohohoho");
		return null;
	}

}
