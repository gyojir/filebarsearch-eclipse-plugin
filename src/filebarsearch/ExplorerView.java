package filebarsearch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Sash;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class ExplorerView extends ViewPart implements PaintListener{
	private List<FileComposite> fileList = new ArrayList<>();
	private List<Control> controlList = new ArrayList<>();
	private Composite parent = null;
	private ScrolledComposite scrollbar = null;
	private Menu popupMenu;

	private List<IFile> findAllProjectFiles(IContainer container) throws CoreException {
		IResource[] members = container.members();
		List<IFile> list = new ArrayList<>();

		for (IResource member : members) {
			if (member instanceof IContainer) {
				IContainer c = (IContainer) member;
				list.addAll(findAllProjectFiles(c));
			} else if (member instanceof IFile) {
				list.add((IFile) member);
			}
		}
		return list;
	}

	public void createPartControl(Composite parent) {

		// アクティブエディタ（編集中のファイル）の情報を取得する。
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = window.getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor)editor;

		// エディタ情報をもとに、編集中のファイルが属するプロジェクト情報を取得する。
		IFileEditorInput editorInput = (IFileEditorInput)textEditor.getEditorInput();
		IFile file = editorInput.getFile();
		IProject project = file.getProject();

		scrollbar = new ScrolledComposite(parent, SWT.H_SCROLL);
		scrollbar.setExpandHorizontal(true);
		scrollbar.setExpandVertical(true);

		Composite composite = new Composite(scrollbar, SWT.NULL);
		scrollbar.setContent(composite);

		//this.parent = sc;
		//this.parent = parent;
		this.parent = composite;
		this.parent.setLayout(new FormLayout());


		try {
			List<IFile> list = findAllProjectFiles(project);
			int count = 0;
			for (IFile file1 : list) {
				String ext = file1.getFileExtension();
				if(ext != null && ext.equals("java")){
					count ++;
				}
			}
			System.out.println("file:"+count);
		} catch (CoreException e1) {
			// TODO 自動生成された catch ブロック
			e1.printStackTrace();
		}

		List<FileElements> fileElementsList = new ArrayList<>();
		fileElementsList.add(new FileElements(file));
		setFileElementsList(fileElementsList);

		// ビューの幅が変更されたとき(いらないかも??)
		scrollbar.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				scrollbar.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
		});

		// 中身の幅が変更されたとき
		composite.addControlListener(new ControlAdapter() {
			public void controlResized(ControlEvent e) {
				scrollbar.setMinSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

				// ファイルが展開されたらそれ全体が表示されるようにする
				if(e.data != null){
					FileComposite fileComposite = (FileComposite) e.data;
					scrollbar.showControl(fileComposite);
					parent.layout(); //再描画
				}
			}
		});

		Runnable runnable = new Runnable(){
			public void run() {
				// timerExecで繰り返すバージョン
				if(parent != null && !parent.isDisposed()){
					parent.redraw();

					Display display = parent.getDisplay();
					if (!display.isDisposed())
						display.timerExec(50, this);
				}
			}
		};

		Display display = parent.getDisplay();
		if (!display.isDisposed())
			display.timerExec(50, runnable);

		getSite().setSelectionProvider(new SelectionProviderAdapter());
		//createPopupMenu();


		 /*Menu menu = new Menu(parent);
		 new MenuItem(menu, SWT.NONE).setText("MenuIteam");
		 this.parent.setMenu(menu);

		 parent.addListener(SWT.MenuDetect, new Listener()
		 {
			 @Override
			 public void handleEvent(Event event)
			 {
			 }
		 });*/

	}

	// ファイルと要素を格納したクラスのリストを渡す
	public void setFileElementsList(List<FileElements> list){
		fileList = new ArrayList<>();
		controlList = new ArrayList<>();
		for (Widget c : parent.getChildren()) {
			c.dispose();
		}

		//FileBar left = null;
		Control left = null;
		for (FileElements fileElements : list) {
			IResource resource = fileElements.file;

			String ext = resource.getFileExtension();
			if(ext != null && ext.equals("java")){
				FileComposite composite = new FileComposite(parent, SWT.DOUBLE_BUFFERED);
				fileList.add(composite);
				controlList.add(composite);

				{
					FormData formData = new FormData();
					formData.top   = new FormAttachment(0,0);   // ウィンドウの上側にはりつく
					formData.bottom = new FormAttachment(100,0); // ウィンドウの下側にはりつく
					if(left != null){
						formData.left = new FormAttachment(left,0,0); // 前のウィジェットの右側にはりつく
					}
					composite.setLayoutData(formData);
					//composite.setFile((IFile) resource);
					composite.setFileElements(fileElements);
					left = composite;
				}

				// サイズ変更のためのサッシ（スプリッタ）
				final Sash sash = new Sash(parent, SWT.VERTICAL);
				{
					FormData formData = new FormData();
					formData.top   = new FormAttachment(0,0);   // ウィンドウの上側にはりつく
					formData.bottom = new FormAttachment(100,0); // ウィンドウの下側にはりつく
					formData.left = new FormAttachment(left,0,0); // ファイルバーの右側にはりつく

					sash.setLayoutData(formData);
					sash.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
					left = sash;
				}
				controlList.add(sash);

				// サッシを動かしたときのリスナー
				sash.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						// マウスのx座標(変更先) - コンポジットの現在の右端の座標
						int x = event.x - (composite.getLocation().x + composite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x);
						composite.resizeTextArea(x);

						sash.getParent().layout();
					}
				});
			}
		}

		scrollbar.setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		parent.layout(); //再描画
	}

	public void setFocus() {
	}



	@Override
	public void paintControl(PaintEvent e) {
	}

	public void dispose(){
		//popupMenu.dispose();
		super.dispose();
	}

	/** ポップアップメニューを作成 */
	private void createPopupMenu(){
		MenuManager menuMgr= new MenuManager("#PopupMenu");
		menuMgr.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		popupMenu = menuMgr.createContextMenu(parent);
		parent.setMenu(popupMenu);
		getSite().registerContextMenu(menuMgr, getSite().getSelectionProvider());
	}

	public class SelectionProviderAdapter implements ISelectionProvider {

		private final List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();

		private ISelection theSelection = StructuredSelection.EMPTY;
		@Override
		public void addSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.add(listener);
		}

		@Override
		public void removeSelectionChangedListener(ISelectionChangedListener listener) {
			listeners.remove(listener);
		}
		@Override
		public void setSelection(ISelection selection) {
			theSelection = selection;

			final SelectionChangedEvent event = new SelectionChangedEvent(this, selection);

			for (final ISelectionChangedListener listener : listeners) {
				SafeRunner.run(new SafeRunnable() {
					@Override
					public void run() {
						listener.selectionChanged(event);
					}
				});
			}
		}

		@Override
		public ISelection getSelection() {
			return theSelection;
		}
	}
}
