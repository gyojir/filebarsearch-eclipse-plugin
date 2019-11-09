package filebarsearch;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.CompilationUnitDocumentProvider;
import org.eclipse.jdt.ui.text.IJavaPartitions;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.AbstractTextEditor;

@SuppressWarnings("restriction")
public class FileComposite extends Composite {
	StyledText styledText;
	FileBar fileBar;
	IFile file;
	private Color white;
	private Color rangeColor;
	private boolean isVisible = false;
	private int textAreaWidth = 200; // テキストの幅
	private SourceViewer sourceViewer;
	private Menu popupMenu;
	private Point popupPoint;

	public FileComposite(Composite parent, int style) {
		super(parent, style);

		Logger logger = Logger.getLogger("experiment_logger");

		white = new Color(null, 255, 255, 255);
		rangeColor = new Color(null, 200, 200, 200);
		setBackground(white);


		this.setLayout(new FormLayout());

		// ファイルバー
		fileBar = new FileBar(this, style);
		{
			FormData formData = new FormData();
			formData.top   = new FormAttachment(0,0);   // ウィンドウの上側にはりつく
			formData.bottom = new FormAttachment(100,0); // ウィンドウの下側にはりつく
			formData.left = new FormAttachment(0,0); // ウィンドウの右側にはりつく
			formData.left.offset = 0;//isVisible? 0: -scrollbar.width;
			formData.width = 200;

			fileBar.setLayoutData(formData);
		}

		// テキストエリア
		createStyledText(this);
		{
			FormData formData = new FormData();
			formData.top   = new FormAttachment(0,0);   // ウィンドウの上側にはりつく
			formData.bottom = new FormAttachment(100,0); // ウィンドウの下側にはりつく
			formData.left = new FormAttachment(fileBar,0,0); // ウィンドウの右側にはりつく
			formData.width = isVisible? textAreaWidth: 0; // テキストの幅はここで決める
			styledText.setLayoutData(formData);
			styledText.setVisible(isVisible);
			//styledText.setFont(new Font(getDisplay(), "ＭＳ ゴシック", 9, SWT.NORMAL));
			//styledText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
		}
		Rectangle scrollbar = styledText.computeTrim(0, 0, 0, 0);


		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
				popupMenu.dispose();
				white.dispose();
				rangeColor.dispose();
				fileBar.dispose();
			}
		});

		addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				FileComposite.this.controlResized(e);
			}
		});

		// マウスクリックのリスナー
		fileBar.addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if(e.button==1){
					setVisibleText(!isVisible);
					if(isVisible){
						logger.info("VISIBLE_TEXT, " + fileBar.getText() + ", NONE, NONE");
					}else{
						logger.info("HIDE_TEXT, " + fileBar.getText());
					}
				}
			}

			@Override
			public void mouseDown(MouseEvent e) {
				// クリックした部分の要素を取得
				if(e.button==1){
					RelatedElement relatedElement = fileBar.getRelatedElementOfPixel(e.y);
					if(relatedElement != null){
						if(isVisible){
							logger.info("MOVE_TEXT, " + fileBar.getText() + ", " + relatedElement.element.getElementName() + ", " + relatedElement.type.toString());
						}else{
							logger.info("VISIBLE_TEXT, " + fileBar.getText() + ", " + relatedElement.element.getElementName() + ", " + relatedElement.type.toString());
						}

						int start = styledText.getLineAtOffset(sourceViewer.getTopIndexStartOffset());
						int end = styledText.getLineAtOffset(sourceViewer.getBottomIndexEndOffset());
						styledText.setTopIndex(relatedElement.topLine - (end-start)/2);
						setVisibleText(true);
					}
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {
			}
		});


		// 右クリックメニュー
		popupMenu = new Menu(getShell(),SWT.POP_UP);
		MenuItem item = new MenuItem(popupMenu, SWT.PUSH);
		item.setText("OpenEditor");
		fileBar.setMenu(popupMenu);

		item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {

				if(file != null && popupPoint != null){
					int line = fileBar.getLineOfPixel(popupPoint.y);

					IWorkbench workbench = PlatformUI.getWorkbench();
					IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
					IWorkbenchPage page = window.getActivePage();

					try {
						logger.info("OPEN_EDITOR, " + fileBar.getText() + ", " + line);

						IEditorPart editorPart = IDE.openEditor(page, file);
						ISourceViewer viewer = getSourceViewer(editorPart);
						StyledText text = viewer.getTextWidget();
						ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;

						line = extension.modelLine2WidgetLine(line); // エディタのFoldingを考慮する
						int offset = text.getOffsetAtLine(line);
						//offset = extension.modelOffset2WidgetOffset(offset);

						text.setTopIndex(line);
						text.setCaretOffset(offset);
					} catch (PartInitException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		//		 fileBar.addMenuDetectListener(new MenuDetectListener() {
		//	            public void menuDetected(MenuDetectEvent e) {
		//	                popupPoint = new Point(e.x, e.y);
		//	            }
		//	        });

		fileBar.addListener(SWT.MenuDetect, new Listener(){
			@Override
			public void handleEvent(Event e) {
				popupPoint = new Point(e.x, e.y);
				popupPoint = fileBar.toControl(popupPoint);
			}
		});

	}

	void setVisibleText(boolean visible){
		isVisible = visible;
		{
			FormData data = (FormData) styledText.getLayoutData();
			data.width = isVisible? textAreaWidth: 0;
			styledText.setVisible(isVisible);
		}

		/*{
			FormData data = (FormData) fileBar.getLayoutData();
			Rectangle scrollbar = styledText.computeTrim(0, 0, 0, 0);
			data.left.offset = isVisible? 0: -scrollbar.width;
		}*/
		layout(true);
		getParent().layout(true);
		Event e = new Event();
		e.data = this;
		getParent().notifyListeners(SWT.Resize, e);
	}

	void createStyledText(Composite parent) {
		sourceViewer = new SourceViewer(parent, null, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		//sourceViewer = new JavaSourceViewer(parent, null, null, false, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL, JavaPlugin.getDefault().getPreferenceStore());

		styledText = sourceViewer.getTextWidget();
		styledText.setEditable(false);
		styledText.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
	}

	void controlResized(ControlEvent e) {
	}

	public void setFile(IFile file) {
		this.file = file;
		open(file);
		fileBar.setText(file.getName());
	}

	// ファイルと関連要素をセット
	public void setFileElements(FileElements elements){
		file = elements.file;
		open(elements.file);
		fileBar.setText(JavaCore.createCompilationUnitFrom(elements.file).findPrimaryType().getFullyQualifiedName());
		fileBar.setRelatedElementList(elements.relatedElementList);
		fileBar.setDocument(sourceViewer.getDocument());
		textAreaWidth = Math.min(styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, 500);

		for(RelatedElement relatedElement: elements.relatedElementList){
			StyleRange styleRange = new StyleRange();
			styleRange.start =relatedElement.startOffset;
			styleRange.length = relatedElement.length;
			styleRange.background = getDisplay().getSystemColor(SWT.COLOR_GRAY);
			styleRange.foreground = fileBar.getElementColor(relatedElement.type);//rangeColor;
			styleRange.fontStyle = SWT.BOLD;
			styledText.setStyleRange(styleRange);
		}
		redraw();
	}

	public int getTextAreaWidth(){
		return textAreaWidth;
	}

	public void resizeTextArea(int offset){
		if(!isVisible){
			return;
		}

		textAreaWidth = Math.max(0, textAreaWidth + offset);

		FormData data = (FormData)styledText.getLayoutData();
		data.width = textAreaWidth;

		layout(true);
		getParent().layout(true);
		getParent().notifyListeners(SWT.Resize, new Event() );
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		//Point sExtent = styledText.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		Point fExtent = fileBar.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
		Rectangle scrollbar = styledText.computeTrim(0, 0, 0, 0); // スクロールバーのサイズ（StyledTextのクライアントエリア以外の部分）

		int width = ((FormData)styledText.getLayoutData()).width;
		width += fExtent.x;
		width += isVisible? scrollbar.width: 0;

		int height = fExtent.y;
		if (wHint != SWT.DEFAULT) width = wHint;
		if (hHint != SWT.DEFAULT) height = hHint;
		return new Point(width + 2, height + 2);
	}

	public void open(IFile file) {
		JavaTextTools javaTools = JavaPlugin.getDefault().getJavaTextTools();
		IPreferenceStore store = JavaPlugin.getDefault().getPreferenceStore();
		CompilationUnitDocumentProvider provider = new CompilationUnitDocumentProvider();

		try {
			provider.connect(file);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}

		IDocument document = provider.getDocument(file);
		//javaTools.setupJavaDocumentPartitioner(document, IJavaPartitions.JAVA_PARTITIONING);

		sourceViewer.setDocument(document);
		sourceViewer.configure(new JavaSourceViewerConfiguration(javaTools.getColorManager(), store, null, IJavaPartitions.JAVA_PARTITIONING));
		sourceViewer.unconfigure(); // よくわからないが、これをしないと色付けが
	}

	// EditorからISourceViewerへ
	// minimapよりコピペ
	public ISourceViewer getSourceViewer(IEditorPart editor) {
		AbstractTextEditor abstEditor = (AbstractTextEditor) editor;
		try {
			Method m = AbstractTextEditor.class.getDeclaredMethod("getSourceViewer");
			m.setAccessible(true);
			return (ISourceViewer) m.invoke(abstEditor);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
