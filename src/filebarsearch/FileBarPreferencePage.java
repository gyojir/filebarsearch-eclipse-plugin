package filebarsearch;

import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class FileBarPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public FileBarPreferencePage() {
		super(GRID);
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
	}

	@Override
	protected void createFieldEditors() {
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_REFERENCE 					,"参照の色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_DECLARATION					,"宣言の色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_VARIABLE_TYPE_DECLARATION	,"変数の型の色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_SUPERCLASS_DECLARATION		,"親クラスの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_INTERFACE_DECLARATION		,"インタフェースの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_SUBCLASS_DECLARATION			,"子クラスの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_BROTHERCLASS_DECLARATION	,"兄弟クラスの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_OVERLOAD_METHOD				,"オーバーロードの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_INTERFACE_METHOD				,"メソッドの実装の色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_OVERRIDE_METHOD				,"オーバーライドの色",getFieldEditorParent()));
		addField(new ColorFieldEditor(PreferenceConstants.COLOR_OVERRIDED_METHOD				,"被オーバーライドメソッドの色",getFieldEditorParent()));

		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_REFERENCE,"参照の重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_DECLARATION,"定義の重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_VARIABLE_TYPE_DECLARATION,"変数の型の重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_SUPERCLASS_DECLARATION,"親クラスの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_INTERFACE_DECLARATION,"インタフェースの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_SUBCLASS_DECLARATION,"子クラスの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_BROTHERCLASS_DECLARATION,"兄弟クラスの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_OVERLOAD_METHOD,"オーバーロードの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_INTERFACE_METHOD,"メソッドの実装の重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_OVERRIDE_METHOD,"オーバーライドの重み",getFieldEditorParent()));
		addField(new IntegerFieldEditor(PreferenceConstants.WEIGHT_OVERRIDED_METHOD,"被オーバーライドメソッドの重み",getFieldEditorParent()));
	}

	@Override
	public void init(IWorkbench workbench) {

	}

}
