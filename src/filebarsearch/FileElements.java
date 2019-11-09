package filebarsearch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;

// ファイルとその中に含まれる関連要素
public class FileElements {
	public IFile file;
	public List<RelatedElement> relatedElementList = new ArrayList<RelatedElement>();

	FileElements(IFile file){
		this.file = file;
	}

	public int getScore(){
		int score = 0;
		for(RelatedElement relatedElement: relatedElementList){
			score += relatedElement.getWeight();
		}
		return score;

	}
}

