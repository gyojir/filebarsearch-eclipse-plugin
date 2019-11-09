package filebarsearch;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IRegion;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.ui.javaeditor.JavaEditor;
import org.eclipse.jdt.ui.actions.FindReferencesAction;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.FileDocumentProvider;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Our sample handler extends AbstractHandler, an IHandler base class.
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
@SuppressWarnings("restriction")
public class CommandHandler extends AbstractHandler {
	public HashMap<IFile,FileElements> fileElementsMap;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Logger logger = Logger.getLogger("experiment_logger");

		fileElementsMap = new HashMap<IFile, FileElements>();

		System.out.println("start");

		try{

			// エディタ上のコンテキストメニューでjavaelementを取得して検索
			IEditorPart editor = HandlerUtil.getActiveEditor(event);
			if (editor instanceof JavaEditor) {
				// 選択部分からJavaElementを探す(左クリックでカーソルを動かしてから選択する必要がある)
				MyFindReferencesAction action = new MyFindReferencesAction((JavaEditor) editor);
				action.run((ITextSelection) ((JavaEditor) editor).getSelectionProvider().getSelection());
				IJavaElement element = action.element;

				if(element == null){
					System.out.println("error: javaElement missing");
					logger.info("CANNOT_EXPLORE");
					return null;
				}
				logger.info("START_EXPLORE, " + Activator.javaElementTypeToString(element) + ", " + element.getElementName());


				// SearchEngineを使って参照と定義を検索
				search(getProgressMonitor(), element);

				// ASTを使って検索
				//SourceReader reader = new SourceReader(getActiveICompilationUnit(), this);
				//reader.read(element);


				{
					ICompilationUnit icu = getActiveICompilationUnit();
					ASTNode unit = runConversion(AST.JLS8, icu, true, true, true);
					CompilationUnit cu = (CompilationUnit) unit;


					Job job = Job.create("ファイルバー検索", new ICoreRunnable() {

						@Override
						public void run(IProgressMonitor monitor) throws CoreException {

							// createBindingsでIJavaElement→バインディング
							IJavaElement[] elements = {element};
							ASTParser parser = ASTParser.newParser(AST.JLS8);
							parser.setSource(icu);
							parser.setResolveBindings(true);
							parser.setStatementsRecovery(true);
							parser.setBindingsRecovery(true);
							parser.setUnitName("name");
							IBinding binding = parser.createBindings(elements, null)[0];

							if(element.getElementType() == IJavaElement.LOCAL_VARIABLE || element.getElementType() == IJavaElement.FIELD){
								IVariableBinding variableBinding = (IVariableBinding)binding;
								ITypeBinding typeBinding = variableBinding.getType();
								System.out.println("この変数は"+typeBinding.getQualifiedName());

								ITypeBinding declBinding = typeBinding.getTypeDeclaration();
								addDeclElementToMap(declBinding.getJavaElement(), RelatedElement.Type.VARIABLE_TYPE_DECLARATION);

							}else if(element.getElementType() == IJavaElement.METHOD){
								IMethodBinding methodBinding = (IMethodBinding)binding;
								IMethod method = (IMethod) methodBinding.getJavaElement();
								ITypeBinding typeBinding = methodBinding.getDeclaringClass();
								IType type = (IType)typeBinding.getJavaElement();
								IMethodBinding declBinding = methodBinding.getMethodDeclaration();

								for(IMethodBinding declaringClassMethodBinding: methodBinding.getDeclaringClass().getDeclaredMethods()){
									if(methodBinding.getName().equals(declaringClassMethodBinding.getName()) && methodBinding != declaringClassMethodBinding){
										System.out.println("オーバーロード発見");
										addDeclElementToMap(declaringClassMethodBinding.getJavaElement(), RelatedElement.Type.OVERLOAD_METHOD);
									}
								}

								// ITypeHierarchyを使ってオーバーライド、オーバーロードを探索
								{
									IJavaProject javaProject = element.getJavaProject();
									IPackageFragmentRoot root = null;
									IPackageFragmentRoot[] roots = javaProject
											.getPackageFragmentRoots();
									for (IPackageFragmentRoot rootTmp : roots) {
										if (rootTmp.getKind() == IPackageFragmentRoot.K_SOURCE) { // ソースディレクトリだったら
											root = rootTmp;
										}
									}
									IRegion region = JavaCore.newRegion();
									region.add(root);

									ITypeHierarchy hierarchy = javaProject.newTypeHierarchy(type, region, null);
									IType[] subtypes= hierarchy.getAllSubtypes(type);
									IType[] supertypes = hierarchy.getAllSupertypes(type);

									for(IType subtype: subtypes){
										IMethod[] methods = subtype.findMethods(method);
										if(methods != null){
											for(IMethod m: methods){
												System.out.println("オーバーライド発見");
												addDeclElementToMap(m, RelatedElement.Type.OVERRIDE_METHOD);
											}
										}
									}


									for(IType supertype: supertypes){
										IMethod[] methods = supertype.findMethods(method);
										if(methods != null){
											for(IMethod m: methods){
												if(supertype.isInterface()){
													System.out.println("インタフェース発見");
													addDeclElementToMap(supertype, RelatedElement.Type.INTERFACE_DECLARATION);
													addDeclElementToMap(m, RelatedElement.Type.INTERFACE_METHOD);
												}else{
													System.out.println("被オーバーライドメソッド発見");
													addDeclElementToMap(supertype, RelatedElement.Type.SUPERCLASS_DECLARATION);
													addDeclElementToMap(m, RelatedElement.Type.OVERRIDED_METHOD);
												}
											}
										}
									}

								}

								// 内部の呼び出しを探索
								// 定義のNodeを探す
								// bindingのkeyで検索
								/*MethodDeclaration decl = (MethodDeclaration)cu.findDeclaringNode( declBinding.getKey() );
						if(decl != null){
							parseBlock(decl.getBody());
						}*/

								// コンストラクタならクラス自体も探索
								if(method.isConstructor()){
									parseTypeElement(type, typeBinding);
								}
							}else if(element.getElementType() == IJavaElement.TYPE){
								parseTypeElement(element, binding);
							}

							List<FileElements> list = new ArrayList<FileElements>(fileElementsMap.values());

							list.sort(new Comparator<FileElements>(){
								@Override
								public int compare(FileElements o1, FileElements o2) {
									if(o1.getScore() < o2.getScore()){
										return 1;
									}else if(o1.getScore() > o2.getScore()){
										return -1;
									}
									return 0;
								}

							});

							Display display = Display.getDefault();
							if(!display.isDisposed()){
								display.syncExec(new Runnable(){
									@Override
									public void run() {
										// 結果を表示
										ExplorerView view =   getExplorerView();
										view.setFileElementsList(list);
									}
								});
							}
						}

					});
					job.schedule();
				}

			}

		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}


	public void search(IProgressMonitor monitor, IJavaElement element) {
		// 検索条件
		SearchParticipant[] participants = { SearchEngine.getDefaultSearchParticipant() };
		IJavaSearchScope scope = SearchEngine.createWorkspaceScope();

		// 検索結果の処理
		MySearchRequestor requestor = new MySearchRequestor();

		// 検索の実行
		try {
			SearchPattern pattern;

			requestor.type = RelatedElement.Type.REFERENCE;
			pattern = createSearchPattern(element,IJavaSearchConstants.REFERENCES);
			new SearchEngine().search(pattern, participants, scope, requestor, monitor);

			requestor.type = RelatedElement.Type.DECLARATION;
			pattern = createSearchPattern(element,IJavaSearchConstants.DECLARATIONS);
			new SearchEngine().search(pattern, participants, scope, requestor, monitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}


	// elementは使われている箇所を知りたいクラス(IType)やメソッド(IMethod)
	// DECRALATIONS 宣言
	// IMPLEMENTORS クラス・インターフェースを派生・実装するクラス
	// REFERENCES 参照（実装を含む)
	// ALL_OCCURRENCES 宣言、参照、またはインタフェースの実装者
	private SearchPattern createSearchPattern(IJavaElement element, int limitTo) {
		int matchRule = SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_ERASURE_MATCH;
		return SearchPattern.createPattern(element, limitTo, matchRule);
	}

	public class MySearchRequestor extends SearchRequestor{
		public RelatedElement.Type type = null;

		@Override
		public void acceptSearchMatch(SearchMatch match) throws CoreException {
			IJavaElement element = (IJavaElement) match.getElement();
			IResource resource = match.getResource();
			if(resource instanceof IFile){
				IFile file = (IFile) resource;
				FileEditorInput input = new FileEditorInput(file);
				FileDocumentProvider provider = new FileDocumentProvider();
				provider.connect(input);
				IDocument document = provider.getDocument(input);

				int top = 0;
				int bottom = 0;
				try {
					top = document.getLineOfOffset(match.getOffset());
					bottom = document.getLineOfOffset(match.getOffset()+match.getLength());
				} catch (BadLocationException e) {
					e.printStackTrace();
				}

				FileElements fileElements = (FileElements) fileElementsMap.get(file);
				RelatedElement relatedElement = new RelatedElement(top,bottom, match.getOffset(), match.getLength(), type, element);

				// 既に関連のあるファイルだったら追加するだけ
				if(fileElements != null){
					fileElements.relatedElementList.add(relatedElement);
				}else{
					fileElements = new FileElements(file);
					fileElements.relatedElementList.add(relatedElement);
					fileElementsMap.put(file, fileElements);
				}

				System.out.printf("%s in %s[line:%d,length:%d]\n", element.getElementName(), match.getResource().getName(), top, match.getLength());
			}
		}
	}

	// FindReferenceActionの検索機能を使わずに、IJavaElement取得のために使う
	public class MyFindReferencesAction extends FindReferencesAction{
		IJavaElement element = null;

		public MyFindReferencesAction(JavaEditor editor) {
			super(editor);
		}

		@Override
		public void run(IJavaElement e) {
			element = e;
		}
	}

	public void parseBlock(Block block){
		// 各ステートメントを走査
		for(Object o : block.statements()){
			Expression exp = null;
			if(o instanceof IfStatement){
				IfStatement statement = (IfStatement)o;
				Statement thenStatement = statement.getThenStatement();
				Statement elseStatement = statement.getElseStatement();

				if(thenStatement instanceof Block){
					parseBlock((Block) thenStatement);
				}else if(thenStatement instanceof ExpressionStatement){
					parseExpression(((ExpressionStatement) thenStatement).getExpression());
				}
				if(elseStatement != null){
					if(elseStatement instanceof Block){
						parseBlock((Block) elseStatement);
					}else if(thenStatement instanceof ExpressionStatement){
						parseExpression(((ExpressionStatement) thenStatement).getExpression());
					}
				}

				exp = statement.getExpression();
			}else if(o instanceof ForStatement){
				ForStatement statement = (ForStatement)o;
				Statement body = statement.getBody();
				if(body instanceof Block){
					parseBlock((Block)body);
				}
				exp = statement.getExpression();
			}else if(o instanceof EnhancedForStatement){
				EnhancedForStatement statement = (EnhancedForStatement)o;
				Statement body = statement.getBody();
				if(body instanceof Block){
					parseBlock((Block)body);
				}
				exp = statement.getExpression();
			}else if(o instanceof VariableDeclarationStatement){
				VariableDeclarationStatement statement = (VariableDeclarationStatement)o;

				for(Object f: statement.fragments()){
					VariableDeclarationFragment vf = (VariableDeclarationFragment)f;
					parseExpression(vf.getInitializer());
				}
			}else if(o instanceof TryStatement){
				TryStatement statement = (TryStatement)o;
				parseBlock(statement.getBody());
				for(Object o1 : statement.catchClauses()){
					CatchClause catchClause = (CatchClause)o1;
					parseBlock(catchClause.getBody());
				}
			}else if(o instanceof ExpressionStatement){
				ExpressionStatement statement = (ExpressionStatement)o;
				exp = statement.getExpression();
			}
			if(exp != null ){
				parseExpression(exp);
			}
		}
	}

	public void parseExpression(Expression exp){
		if(exp instanceof MethodInvocation){
			System.out.println("内部の呼び出し発見"+exp.toString());
			MethodInvocation methodInvocation = (MethodInvocation) exp;
			IJavaElement declElement = methodInvocation.resolveMethodBinding().getJavaElement();
			addDeclElementToMap(declElement, RelatedElement.Type.INTERNAL_INVOCATION);

			Expression internalExp = methodInvocation.getExpression();
			if(internalExp != null){
				parseExpression(internalExp);
			}

			for(Object o : methodInvocation.arguments()){
				parseExpression((Expression)o);
			}
		}else if(exp instanceof Assignment){
			parseExpression(((Assignment) exp).getRightHandSide());
		}else if(exp instanceof PrefixExpression){
			parseExpression(((PrefixExpression) exp).getOperand());
		}else if(exp instanceof ParenthesizedExpression){
			parseExpression(((ParenthesizedExpression) exp).getExpression());
		}else if(exp instanceof InfixExpression){
			parseExpression(((InfixExpression) exp).getLeftOperand());
			parseExpression(((InfixExpression) exp).getRightOperand());
		}else if(exp instanceof ClassInstanceCreation){
			ClassInstanceCreation creation = (ClassInstanceCreation)exp;
			IMethodBinding methodBinding = creation.resolveConstructorBinding();
			System.out.println("内部の呼び出し(コンストラクタ)発見"+exp.toString());
			addDeclElementToMap(methodBinding.getJavaElement(), RelatedElement.Type.INTERNAL_INVOCATION);
			if(creation.getExpression() != null){
				parseExpression(creation.getExpression());
			}
		}
	}


	public void addDeclElementToMap(IJavaElement decl, RelatedElement.Type lineType){
		if(decl == null){
			return;
		}

		ISourceReference  sourceRef = (ISourceReference)decl;

		IFile file = (IFile) decl.getResource();
		if(file != null){
			IDocument document = getDocument(file);

			//ISourceRange range = getSourceRange(decl);
			ISourceRange range = null;
			try {
				range = sourceRef.getNameRange();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
			RelatedElement relatedElement = new RelatedElement(document, range, lineType, decl);

			// 既に関連のあるファイルだったら追加するだけ
			FileElements fileElements = (FileElements) fileElementsMap.get(file);
			if(fileElements != null){
				fileElements.relatedElementList.add(relatedElement);
			}else{
				fileElements = new FileElements(file);
				fileElements.relatedElementList.add(relatedElement);
				fileElementsMap.put(file, fileElements);
			}
		}
	}


	public IDocument getDocument(IFile file){
		FileEditorInput input = new FileEditorInput(file);
		FileDocumentProvider provider = new FileDocumentProvider();
		try {
			provider.connect(input);
		} catch (CoreException e1) {
			e1.printStackTrace();
		}
		return provider.getDocument(input);
	}

	// プログレスモニターを取得
	public IProgressMonitor getProgressMonitor(){
		IWorkbench workbench = PlatformUI.getWorkbench();
		WorkbenchWindow workbenchWindow = (WorkbenchWindow)workbench.getActiveWorkbenchWindow();
		IActionBars bars = workbenchWindow.getActionBars();
		IStatusLineManager lineManager = bars.getStatusLineManager();
		return lineManager.getProgressMonitor();
	}

	// ファイルバーのビューを取得
	public ExplorerView getExplorerView(){
		IWorkbench workbench = PlatformUI.getWorkbench();
		IWorkbenchPage workbenchPage = workbench.getActiveWorkbenchWindow().getActivePage();
		try {
			return  (ExplorerView) workbenchPage.showView("filebarsearch.explorerView");
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}

	public ICompilationUnit getActiveICompilationUnit(){
		// アクティブエディタ（編集中のファイル）の情報を取得する。
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor1 = window.getActivePage().getActiveEditor();
		ITextEditor textEditor = (ITextEditor)editor1;

		IFileEditorInput fileEditorInput = (IFileEditorInput)textEditor.getEditorInput();
		IFile file = fileEditorInput.getFile();
		return JavaCore.createCompilationUnitFrom(file);
	}


	public ISourceRange getSourceRange(IJavaElement javaElement){
		if(javaElement instanceof ISourceReference){
			ISourceReference sourceRef = (ISourceReference) javaElement;
			try {
				return sourceRef.getSourceRange();
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	protected ASTNode getASTNode(org.eclipse.jdt.core.dom.CompilationUnit unit, int typeIndex, int bodyIndex) {
		return (ASTNode) ((AbstractTypeDeclaration)unit.types().get(typeIndex)).bodyDeclarations().get(bodyIndex);
	}
	public ASTNode runConversion(int astLevel, ICompilationUnit unit, boolean resolveBindings) {
		return runConversion(astLevel, unit, resolveBindings, false);
	}
	public ASTNode runConversion(int astLevel, ICompilationUnit unit, boolean resolveBindings, boolean statementsRecovery) {
		return runConversion(astLevel, unit, resolveBindings, statementsRecovery, false);
	}
	public ASTNode runConversion(
			int astLevel,
			ICompilationUnit unit,
			boolean resolveBindings,
			boolean statementsRecovery,
			boolean bindingsRecovery) {
		ASTParser parser = ASTParser.newParser(astLevel);
		parser.setSource(unit);
		parser.setResolveBindings(resolveBindings);
		parser.setStatementsRecovery(statementsRecovery);
		parser.setBindingsRecovery(bindingsRecovery);
		parser.setUnitName("name");
		return parser.createAST(null);
	}

	public void parseTypeElement(IJavaElement element, IBinding binding) throws JavaModelException{
		ITypeBinding typeBinding = (ITypeBinding)binding;


		// ITypeHierarchyを使って探索
		IType type = (IType)element;

		IJavaProject javaProject = element.getJavaProject();
		IPackageFragmentRoot root = null;
		IPackageFragmentRoot[] roots = javaProject
				.getPackageFragmentRoots();
		for (IPackageFragmentRoot rootTmp : roots) {
			if (rootTmp.getKind() == IPackageFragmentRoot.K_SOURCE) { // ソースディレクトリだったら
				root = rootTmp;
			}
		}
		IRegion region = JavaCore.newRegion();
		region.add(root);

		ITypeHierarchy hierarchy = javaProject.newTypeHierarchy(type, region, null);
		IType[] subtypes= hierarchy.getAllSubtypes(type);
		IType superclass = hierarchy.getSuperclass(type);
		IType[] interfaces = hierarchy.getAllSuperInterfaces(type);
		IType[] implementors = hierarchy.getImplementingClasses(type);

		if(superclass != null && !superclass.getFullyQualifiedName().equals("java.lang.Object")){
			System.out.println("親クラスは"+superclass.getFullyQualifiedName());
			addDeclElementToMap(superclass, RelatedElement.Type.SUPERCLASS_DECLARATION);

			ITypeHierarchy superHierarchy = javaProject.newTypeHierarchy(superclass, region, null);
			for(IType brother: superHierarchy.getSubtypes(superclass)){
				if(!type.getFullyQualifiedName().equals(brother.getFullyQualifiedName())){
					System.out.println("兄弟クラスは"+brother.getFullyQualifiedName());
					addDeclElementToMap(brother, RelatedElement.Type.BROTHERCLASS_DECLARATION);
				}
			}
		}

		for(IType intf: interfaces){
			System.out.println("インタフェース"+intf.getFullyQualifiedName()+"を実装");
			addDeclElementToMap(intf, RelatedElement.Type.INTERFACE_DECLARATION);

			ITypeHierarchy superHierarchy = javaProject.newTypeHierarchy(intf, region, null);
			for(IType brother: superHierarchy.getSubtypes(intf)){
				if(!type.getFullyQualifiedName().equals(brother.getFullyQualifiedName())){
					System.out.println("兄弟クラスは"+brother.getFullyQualifiedName());
					addDeclElementToMap(brother, RelatedElement.Type.BROTHERCLASS_DECLARATION);
				}
			}
		}


		for(IType subtype: subtypes){
			System.out.println("子クラスは"+subtype.getFullyQualifiedName());
			addDeclElementToMap(subtype, RelatedElement.Type.SUBCLASS_DECLARATION);
		}

		// インタフェースだったら実装を探す
		for(IType implementor : subtypes){
			// インタフェースのメソッド一覧
			for(IMethodBinding declaringMethodBinding: typeBinding.getDeclaredMethods()){
				IMethod declMethod = (IMethod)declaringMethodBinding.getJavaElement();
				if(declMethod != null){
					// 実装クラスのメソッド一覧
					for(IMethod method : implementor.getMethods()){
						if(method.getElementName().equals(declMethod.getElementName()) && method.getSignature().equals(declMethod.getSignature())){
							if(typeBinding.isInterface()){
								System.out.println(implementor.getFullyQualifiedName()+"はインタフェース"+typeBinding.getQualifiedName() + "の" + method.getElementName()+"を実装");
								addDeclElementToMap(method, RelatedElement.Type.INTERFACE_METHOD);
							}else{
								System.out.println(implementor.getFullyQualifiedName()+"は"+typeBinding.getQualifiedName() + "の" + method.getElementName()+"をオーバーライド");
								addDeclElementToMap(method, RelatedElement.Type.OVERRIDE_METHOD);
							}
						}
					}
				}
			}
		}
	}
}
