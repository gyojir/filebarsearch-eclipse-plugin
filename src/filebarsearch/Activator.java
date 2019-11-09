package filebarsearch;
import java.util.logging.Logger;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin {
	static final Logger logger = Logger.getLogger("experiment_logger");


	// The plug-in ID
	public static final String PLUGIN_ID = "filebarsearch"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;


	private ColorManager colorManager;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@SuppressWarnings("restriction")
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		colorManager = new ColorManager();

		Display.getDefault().addFilter(SWT.KeyDown, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				//System.out.println(event.keyCode + "key pressed");
				logger.info("KEY_PRESSED, " + event.keyCode);
			}
		});

		Display.getDefault().addFilter(SWT.MouseDown, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				Display display = Display.getDefault();
				if(display != null && !display.isDisposed()){
					Shell shell = display.getActiveShell();
					if(shell != null){
						Point p = shell.toDisplay(event.x, event.y);
						//System.out.println("button:" + event.button + " widget:" + event.widget + " pos:" + p.x+","+p.y);
						logger.info("MOUSE_PRESSED, " + event.button + ", "+ p.x+", "+p.y +", " + event.widget);
					}
				}

			}
		});


		Display.getDefault().addFilter(SWT.MouseMove, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				Display display = Display.getDefault();
				if(display != null && !display.isDisposed()){
					Shell shell = display.getActiveShell();
					if(shell != null){
						Point p = shell.toDisplay(event.x, event.y);
						logger.info("MOUSE_MOVE, " + p.x+", "+p.y);
					}
				}
			}
		});


		Display.getDefault().addFilter(SWT.MouseWheel, new Listener()
		{
			@Override
			public void handleEvent(Event e)
			{
				if(e.widget instanceof StyledText){
					logger.info("MOUSE_WHEEL, " + e.count);
				}
			}
		});

		//Workbench.getInstance()
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(new IPartListener2(){

			@Override
			public void partActivated(IWorkbenchPartReference partRef) {
				if (partRef instanceof IEditorReference) {
					IEditorReference currentEditor = (IEditorReference)partRef;
					logger.info("ACTIVATE_EDITOR, " + currentEditor.getName());
				}
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
			}

		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		colorManager.dispose();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public ColorManager getColorManager(){
		return colorManager;
	}

	public static String javaElementTypeToString(IJavaElement element){
		switch(element.getElementType()){
		case IJavaElement.JAVA_MODEL: return "JAVA_MODEL";
		case IJavaElement.JAVA_PROJECT: return "JAVA_PROJECT";
		case IJavaElement.PACKAGE_FRAGMENT_ROOT: return "PACKAGE_FRAGMENT_ROOT";
		case IJavaElement.PACKAGE_FRAGMENT: return "PACKAGE_FRAGMENT";
		case IJavaElement.COMPILATION_UNIT: return "COMPILATION_UNIT";
		case IJavaElement.CLASS_FILE: return "CLASS_FILE";
		case IJavaElement.TYPE: return "TYPE";
		case IJavaElement.FIELD: return "FIELD";
		case IJavaElement.METHOD: return "METHOD";
		case IJavaElement.INITIALIZER: return "INITIALIZER";
		case IJavaElement.PACKAGE_DECLARATION: return "PACKAGE_DECLARATION";
		case IJavaElement.IMPORT_CONTAINER: return "IMPORT_CONTAINER";
		case IJavaElement.IMPORT_DECLARATION: return "IMPORT_DECLARATION";
		case IJavaElement.LOCAL_VARIABLE: return "LOCAL_VARIABLE";
		case IJavaElement.TYPE_PARAMETER: return "TYPE_PARAMETER";
		case IJavaElement.ANNOTATION: return "ANNOTATION";
		}
		return null;
	}
}
