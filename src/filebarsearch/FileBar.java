package filebarsearch;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class FileBar extends Canvas {
	String text;
	List<RelatedElement> relatedElementList = null;
	IDocument document = null;
	Font font = new Font(getDisplay(), "Courier New", 9, SWT.NORMAL);
	private static Shell popup;
	private static Label popupLabel;

	public FileBar(Composite parent, int style) {
		super(parent, style);


		setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));

		text = "text";

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(DisposeEvent e) {
			}
		});

		addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				FileBar.this.paintControl(e);
			}
		});


		Runnable runnable = new Runnable(){
			public void run() {
				// timerExecで繰り返すバージョン
				if(this != null && !isDisposed()){
					redraw();

					Display display = getDisplay();
					if (!display.isDisposed())
						display.timerExec(100, this);
				}
			}
		};

		final Cursor handCursor = new Cursor(getDisplay(), SWT.CURSOR_HAND);
		final Cursor normalCursor = new Cursor(getDisplay(), SWT.CURSOR_ARROW);
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e){
				ArrayList<RelatedElement> elements = getRelatedElementsOfPixel(e.y);
				if(elements.size() != 0){
					setCursor(handCursor);
					callPopUp(toDisplay(e.x, e.y), getShell(), elements);
				}else{
					setCursor(normalCursor);
					hidePopUp();
				}
			}
		});
		addMouseTrackListener(new MouseTrackAdapter(){
			public void mouseExit(MouseEvent e){
				setCursor(normalCursor);
				hidePopUp();
			}
		});

		Display display = getDisplay();
		if (!display.isDisposed())
			display.timerExec(100, runnable);
	}

	void paintControl(PaintEvent e) {
		Rectangle bounds = this.getBounds();

		e.gc.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_BLACK));

		e.gc.drawRectangle(0,0, bounds.width-2, bounds.height-1);

		if(relatedElementList != null && document != null){
			double heightRate = (double)bounds.height / (double)document.getNumberOfLines(); // ソースコードの長さとビューの長さの比
			for(RelatedElement range: relatedElementList){
				// 行をピクセルに変換
				int width = (int) ((range.bottomLine - range.topLine+1) * heightRate) + 3; // +1は一行のときwidht=0にならないように
				int y = (int)(range.topLine * heightRate) + width / 2;

				Color c = getElementColor(range.type);
				e.gc.setForeground(c);
				e.gc.setLineWidth(width);
				e.gc.drawLine(1, y, bounds.width-3, y);
			}
		}


		e.gc.setForeground(this.getDisplay().getSystemColor(SWT.COLOR_BLACK));

		if(text != null){
			Transform tr = null;
			tr = new Transform(e.display);

			e.gc.setFont(font);
			e.gc.setAntialias(SWT.ON);
			Point p = e.gc.stringExtent(text);
			int w = e.width;
			tr.rotate(90);
			e.gc.setTransform(tr); // 左上を中心に座標軸を90度回転
			e.gc.drawString(text, 10, -w/2 - p.y/2); //
		}
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
		redraw();
	}

	public void setRelatedElementList(List<RelatedElement> relatedElementList){
		this.relatedElementList = relatedElementList;
	}

	public void setDocument(IDocument document){
		this.document = document;
	}

	public IDocument getDocument(){
		return document;
	}

	public int getLineOfPixel(int p){
		Rectangle bounds = this.getBounds();

		if(document != null){
			return (int) ((double)p * (double)document.getNumberOfLines() / (double)bounds.height); // ソースコードの長さとビューの長さの比
		}
		return 0;
	}

	public int getPixelOfLine(int line){
		Rectangle bounds = this.getBounds();

		if(document != null){
			return (int) ((double)line * (double)bounds.height / (double)document.getNumberOfLines()); // ソースコードの長さとビューの長さの比
		}
		return 0;

	}

	public RelatedElement getRelatedElementOfPixel(int p){
		if(relatedElementList != null && document != null){
			for(RelatedElement range: relatedElementList){
				int top = (int) getPixelOfLine(range.topLine);
				int width = (int)getPixelOfLine(range.bottomLine - range.topLine+1) + 3; // +1は一行のときwidht=0にならないように

				// もし範囲内なら
				if(top <= p && p <= top + width){
					return range;
				}
			}
		}
		return null;
	}

	public ArrayList<RelatedElement> getRelatedElementsOfPixel(int p){
		ArrayList<RelatedElement> list = new ArrayList<>();
		if(relatedElementList != null && document != null){
			for(RelatedElement range: relatedElementList){
				int top = (int) getPixelOfLine(range.topLine);
				int width = (int)getPixelOfLine(range.bottomLine - range.topLine+1) + 3; // +1は一行のときwidht=0にならないように

				// もし範囲内なら
				if(top <= p && p <= top + width){
					list.add(range);
				}
			}
		}
		return list;
	}

	public Point computeSize(int wHint, int hHint, boolean changed) {
		Point initialSize = super.computeSize (wHint, hHint, changed);
		initialSize.x = 30; // 横幅はここで決める
		initialSize.y = 100;
		return initialSize;
	}


	// ポップアップを表示
	private static void callPopUp(Point p, Shell shell, ArrayList<RelatedElement> elements){
		if (popup == null)
		{
			popup = new Shell(shell.getDisplay(), SWT.NO_TRIM | SWT.ON_TOP | SWT.MODELESS | SWT.TOOL | SWT.NO_FOCUS);
			popup.setLayout(new FillLayout());
			popup.setLocation(p.x + 5, p.y + 5);

			if(popupLabel != null){
				popupLabel.dispose();
			}
			popupLabel = new Label(popup, SWT.NONE);
			popupLabel.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

			popup.pack();
			popup.setVisible(true); // open()だとフォーカスが移ってしまうので
			shell.forceFocus();
		}

		String text = "";
		for(RelatedElement element : elements){
			text += element.type.toString() + "\n";
		}

		popupLabel.setText(text);

		popup.setLocation(p.x + 15, p.y + 15);

		popup.pack();
	}
	// ポップアップを消す
	private static void hidePopUp(){
		if (popup != null && !popup.isDisposed())
		{
			popup.close();
			popup = null;
		}
	}

	public Color getElementColor(RelatedElement.Type type){
		ColorManager manager = Activator.getDefault().getColorManager();
		switch(type){
		case DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_DECLARATION);
		case REFERENCE:
			return manager.getColor(PreferenceConstants.COLOR_REFERENCE);
		case VARIABLE_TYPE_DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_VARIABLE_TYPE_DECLARATION);
		case SUPERCLASS_DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_SUPERCLASS_DECLARATION);
		case INTERFACE_DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_INTERFACE_DECLARATION);
		case SUBCLASS_DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_SUBCLASS_DECLARATION);
		case BROTHERCLASS_DECLARATION:
			return manager.getColor(PreferenceConstants.COLOR_BROTHERCLASS_DECLARATION);
		case INTERFACE_METHOD:
			return manager.getColor(PreferenceConstants.COLOR_INTERFACE_METHOD);
		case OVERLOAD_METHOD:
			return manager.getColor(PreferenceConstants.COLOR_OVERLOAD_METHOD);
		case OVERRIDE_METHOD:
			return manager.getColor(PreferenceConstants.COLOR_OVERRIDE_METHOD);
		case OVERRIDED_METHOD:
			return manager.getColor(PreferenceConstants.COLOR_OVERRIDED_METHOD);
		default:
			break;
		}
		return getDisplay().getSystemColor(SWT.COLOR_BLACK);
	}

}
