package filebarsearch;
import java.util.logging.Logger;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;

/**
 * Javaソースを解析するリーダーです。
 *
 * @author Masatomi KINO
 * @version $Revision$
 */
public class SourceReader {

	private static final Logger logger = Logger.getLogger(SourceReader.class.getName());

	private final ICompilationUnit element;
	public CommandHandler handler;

	public SourceReader(ICompilationUnit element, CommandHandler hander) {
		this.element = element;
		this.handler = hander;
	}

	/**
	 * 渡されたソースコードの解析を行います。
	 * @throws JavaModelException
	 */
	public String read(IJavaElement javaElement) throws JavaModelException {
		String str = "";
		System.out.println("read() - start");

		ASTNode unit = runConversion(AST.JLS8, element, true, true, true);
		unit.accept(new ASTVisitorImpl(javaElement));

		System.out.println("\nread() - end");
		return str;
	}

	/**
	 * ソースを走査するVisitorの実装クラスです。
	 *
	 * @author Masatomi KINO
	 * @version $Revision$
	 */
	class ASTVisitorImpl extends ASTVisitor {
		IJavaElement searchElement = null;
		boolean isFinished = false;

		ASTVisitorImpl(IJavaElement javaElement){
			super();
			searchElement = javaElement;
		}

		public void preVisit(ASTNode node){
			System.out.print("node type: "+node.getClass().getName() + "\t");
			System.out.println("node : "+node.toString());
		}

		public boolean preVisit2(ASTNode node){
			return !isFinished;
		}

		/*public boolean visit(MethodInvocation node){
			System.out.println("visit: メソッド呼び出し");
			System.out.println("MethodName:" + node.getName().getFullyQualifiedName());

			IMethodBinding binding = node.resolveMethodBinding();
			{
				IJavaElement javaElement = binding.getJavaElement();
				if(javaElement != null){
					if(searchElement.equals(javaElement)){
						System.out.println("対応する（同じ）関数呼び出しを発見");
						return false;
					}
				}
			}

			// どのクラスのメソッドかを調べる
			{
				Expression exp =  node.getExpression();
				if(exp != null){
					ITypeBinding typeBinding = exp.resolveTypeBinding();
					System.out.println("type:"+typeBinding.getQualifiedName());

					IJavaElement javaElement = typeBinding.getJavaElement();
					if(javaElement != null){
						if(searchElement.equals(javaElement)){
							System.out.println("ahohohooooooooooooooooooooooooooooooooooooooooooooooooooo");
						}
					}
				}
			}

			return super.visit(node);
		}*/

		// SimpleNameがあるのでいらない？？
		/*public boolean visit(SimpleType node){
			System.out.println("visit: クラス");
			System.out.println("TypeName:" + node.getName().getFullyQualifiedName());

			ITypeBinding binding = node.resolveBinding();
			if(binding != null){
				IJavaElement javaElement = binding.getJavaElement();
				if(javaElement != null){
					if(searchElement.equals(javaElement)){
						System.out.println("同じクラスを発見");
						//binding.getSuperclass()
					}
				}
			}
			return super.visit(node);

		}*/

		// ローカル変数などの識別子
		public boolean visit(SimpleName node){
			//System.out.println("visit: SimpleName");
			//System.out.println("Name:" + node.getFullyQualifiedName());

			IBinding binding = node.resolveBinding();
			if(binding != null){
				IJavaElement javaElement = binding.getJavaElement();
				if(javaElement != null){
					if(searchElement.equals(javaElement)){
						if(searchElement.getElementType() == IJavaElement.LOCAL_VARIABLE){
							System.out.println("対応する識別子を発見");
							ITypeBinding typeBinding = node.resolveTypeBinding();
							System.out.println("この変数は"+typeBinding.getQualifiedName());

							ITypeBinding declBinding = typeBinding.getTypeDeclaration();
							handler.addDeclElementToMap(declBinding.getJavaElement(), RelatedElement.Type.VARIABLE_TYPE_DECLARATION);

							isFinished = true;
							return false;

						}else if(searchElement.getElementType() == IJavaElement.METHOD){
							System.out.println("対応する関数を発見");
							//IMethod method = (IMethod)javaElement;
							IMethodBinding methodBinding = (IMethodBinding)binding;
							IMethodBinding declBinding = methodBinding.getMethodDeclaration();

							for(IMethodBinding declaringClassMethodBinding: methodBinding.getDeclaringClass().getDeclaredMethods()){
								if(methodBinding.getName().equals(declaringClassMethodBinding.getName()) && methodBinding != declaringClassMethodBinding){
									System.out.println("オーバーロード発見");
									handler.addDeclElementToMap(declaringClassMethodBinding.getJavaElement(), RelatedElement.Type.OVERLOAD_METHOD);
								}
							}

							// 内部の呼び出しを探索
							// 定義のNodeを探す
							// バインディングからファイル(ICompilationUnit)を取得
							ICompilationUnit unit = (ICompilationUnit) javaElement.getAncestor( IJavaElement.COMPILATION_UNIT );
							if ( unit != null ) {
								// ASTNodeを生成、bindingのkeyで検索
								CompilationUnit cu = (CompilationUnit) runConversion( AST.JLS8, unit, true );
								MethodDeclaration decl = (MethodDeclaration)cu.findDeclaringNode( declBinding.getKey() );
								// 各ステートメントを走査
								for(Object o : decl.getBody().statements()){
									if(o instanceof ExpressionStatement){
										// 関数呼び出しから関数定義を取得
										ExpressionStatement statement = (ExpressionStatement)o;
										Expression exp = statement.getExpression();
										if(exp instanceof MethodInvocation){
											System.out.println("内部の呼び出し発見");
											MethodInvocation methodInvocation = (MethodInvocation) exp;
											IJavaElement declElement = methodInvocation.resolveMethodBinding().getJavaElement();
											handler.addDeclElementToMap(declElement, RelatedElement.Type.INTERNAL_INVOCATION);
										}
									}
								}
							}

							isFinished = true;
							return false;
						}else if(searchElement.getElementType() == IJavaElement.TYPE){
							System.out.println("同じクラスを発見");
							ITypeBinding typeBinding = node.resolveTypeBinding();
							/*
							if(typeBinding.isInterface()){
								for(IMethodBinding declaringMethodBinding: typeBinding.getDeclaredMethods()){

								}
							}
							ITypeBinding superBinding = typeBinding.getSuperclass();
							if(superBinding != null){
								System.out.println("親クラスは"+superBinding.getQualifiedName());

								ITypeBinding declBinding = superBinding.getTypeDeclaration();
								hander.addDeclElementToMap(declBinding.getJavaElement(), LineRange.Type.SUPERCLASS_DECLARATION);
							}


							for(ITypeBinding interfaceBinding : typeBinding.getInterfaces()){
								System.out.println("インタフェース"+interfaceBinding.getQualifiedName()+"を実装");

								ITypeBinding declBinding = interfaceBinding.getTypeDeclaration();
								hander.addDeclElementToMap(declBinding.getJavaElement(), LineRange.Type.INTERFACE_DECLARATION);
							}*/

							isFinished = true;
							return false;
						}
					}
				}
			}
			return super.visit(node);

		}
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

}