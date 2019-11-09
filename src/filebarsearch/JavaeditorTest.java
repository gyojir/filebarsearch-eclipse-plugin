package filebarsearch;

import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitEditor;


// JavaSourceEditorを使えるようにするためにeditorプラグインを動かす
@SuppressWarnings("restriction")
public class JavaeditorTest extends CompilationUnitEditor {

    /**
     * デフォルトコンストラクタ、必須
     */
    public JavaeditorTest() {
        super();
    }

    /**
     * エディタを終了した時に実行されるメソッド、必須
     */
    @Override
    public void dispose() {
        super.dispose();
    }
}
