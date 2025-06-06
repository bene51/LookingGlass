package lookingglass;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import animation3d.renderer3d.ExtendedRenderingState;
import animation3d.renderer3d.OpenCLRaycaster;
import animation3d.renderer3d.Renderer3D;
import animation3d.textanim.Animator;
import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.measure.Calibration;
import ij.plugin.filter.PlugInFilter;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij.process.StackProcessor;


public class Create_Quilt implements PlugInFilter {

	public static final double DEFAULT_VIEW_CONE_ANGLE = 35.0;

	public static final String DEFAULT_DISPLAY = Quilt.PORTRAIT.name;

	public static final boolean DEFAULT_RESIZE_IMAGE = true;

	public static final boolean DEFAULT_FIT_VIEW_TO_CONTENT = true;

	public static final double DEFAULT_FOCUS = -0.01;

	public static final boolean DEFAULT_HIDE_SCALEBAR = true;

	public static final boolean DEFAULT_HIDE_BORDER = true;

	public enum Quilt {

		GO(                          "Looking Glass Go",                           11, 6, 4092, 4092, "0.75"),

		PORTRAIT(                    "Looking Glass Portrait",                      8, 6, 3360, 3360, "0.75"),

		LIGHTFIELD_16_INCH_LANDSCAPE("Looking Glass 16\" Light Field (Landscape)",  7, 7, 5999, 5999, "0.75"),

		LIGHTFIELD_16_INCH_PORTRAIT( "Looking Glass 16\" Light Field (Portrait)",  11, 6, 5995, 6000, "0.75"),

		LIGHTFIELD_32_INCH_LANDSCAPE("Looking Glass 32\" Light Field (Landscape)",  7, 7, 8190, 8190, "0.75"),

		LIGHTFIELD_32_INCH_PORTRAIT( "Looking Glass 32\" Light Field (Portrait)",  11, 6, 8184, 8184, "0.75"),

		LIGHTFIELD_65_INCH_PORTRAIT( "Looking Glass 65\" Light Field",              8, 9, 8192, 8192, "0.75");

		Quilt(String name, int columns, int rows, int pixelsX, int pixelsY, String aspect) {
			this.name = name;
			this.columns = columns;
			this.rows = rows;
			this.pixelsX = pixelsX;
			this.pixelsY = pixelsY;
			this.tileWidth  = pixelsX / columns;
			this.tileHeight = pixelsY / rows;
			this.aspect = aspect;
		}

		public final String name;
		public final int columns;
		public final int rows;

		public final int pixelsX;
		public final int pixelsY;

		public final int tileWidth;
		public final int tileHeight;

		public final String aspect;

		public int calculatePixelOffsetX(int tileIdx) {
			int column = tileIdx % columns;
			return column * tileWidth;
		}

		public int calculatePixelOffsetY(int tileIdx) {
			int row = tileIdx / columns;
			row = rows - row - 1;
			return row * tileHeight;
		}
	}

	private ImagePlus image;

	public Create_Quilt() {
	}

	@Override
	public int setup(String arg, ImagePlus imp) {
		this.image = imp;
		return DOES_8G | DOES_16;
	}

	private static String loadText(String file) throws IOException {
		try (BufferedReader buf = new BufferedReader(new FileReader(file))) {
			String line;
			StringBuilder res = new StringBuilder();
			while ((line = buf.readLine()) != null) {
				res.append(line).append("\n");
			}
			buf.close();
			return res.toString();
		}
	}

	@Override
	public void run(ImageProcessor ip) {
		String animationFile     = Prefs.get("LookingGlass.animationFile", "");
		double viewConeAngle     = Prefs.get("LookingGlass.viewConeAngle", DEFAULT_VIEW_CONE_ANGLE);
		double focus             = Prefs.get("LookingGlass.focus", DEFAULT_FOCUS);
		String display           = Prefs.get("LookingGlass.display", DEFAULT_DISPLAY);
		boolean resizeInputImage = Prefs.get("LookingGlass.resizeInputImage", DEFAULT_RESIZE_IMAGE);
		boolean fitViewToContent = Prefs.get("LookingGlass.fitViewToContent", DEFAULT_FIT_VIEW_TO_CONTENT);
		boolean hideScalebar     = Prefs.get("LookingGlass.hideScalebar", DEFAULT_HIDE_SCALEBAR);
		boolean hideBorder       = Prefs.get("LookingGlass.hideBorder", DEFAULT_HIDE_BORDER);

		GenericDialogPlus gd = new GenericDialogPlus("");
		gd.addFileField("Animation file (optional)", animationFile);
		String[] quiltOptions = Arrays.stream(Quilt.values()).map(q -> q.name).toArray(String[]::new);
		gd.addChoice("Display", quiltOptions, display);
		gd.addNumericField("View_cone_angle", viewConeAngle, 3, 5, "degrees");
		gd.addNumericField("Focus", focus);
		gd.addCheckbox("Resize input image", resizeInputImage);
		gd.addCheckbox("Fit view to content", fitViewToContent);
		gd.addCheckbox("Hide_scalebar", hideScalebar);
		gd.addCheckbox("Hide_border", hideBorder);

		gd.showDialog();
		if(gd.wasCanceled())
			return;

		animationFile = gd.getNextString();
		Quilt quilt = Quilt.values()[gd.getNextChoiceIndex()];
		viewConeAngle = gd.getNextNumber();
		focus = gd.getNextNumber();
		resizeInputImage = gd.getNextBoolean();
		fitViewToContent = gd.getNextBoolean();
		hideScalebar = gd.getNextBoolean();
		hideBorder = gd.getNextBoolean();

		Prefs.set("LookingGlass.animationFile", animationFile);
		Prefs.set("LookingGlass.viewConeAngle", viewConeAngle);
		Prefs.set("LookingGlass.focus", focus);
		Prefs.set("LookingGlass.display", quilt.name);
		Prefs.set("LookingGlass.resizeInputImage", resizeInputImage);
		Prefs.set("LookingGlass.fitViewToContent", fitViewToContent);
		Prefs.set("LookingGlass.hideScalebar", hideScalebar);
		Prefs.set("LookingGlass.hideBorder", hideBorder);
		Prefs.savePreferences();

		ImagePlus hologram = render(image, animationFile, quilt, viewConeAngle, resizeInputImage, fitViewToContent, hideScalebar, hideBorder, true);

		try {
			File tmp = File.createTempFile("lookingglass", "_quilt_qs" + quilt.columns + "x" + quilt.rows + "a" + quilt.aspect + ".jpg");
			IJ.save(hologram, tmp.getAbsolutePath());
			LookingGlass lg = new LookingGlass();
			String orchestration = LookingGlass.enterOrchestration();
			String path = tmp.getAbsolutePath().replace('\\', '/');
			LookingGlass.Hologram holo = new LookingGlass.Hologram(path, quilt.rows, quilt.columns, Double.parseDouble(quilt.aspect), focus, quilt.rows * quilt.columns);
			lg.cast(orchestration, holo);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Rectangle findContent(ImageProcessor ip) {
		int[] result = (int[]) ip.getPixels();
		int w = ip.getWidth();
		int h = ip.getHeight();

		final int alphaThreshold = 30;

		int x, y, i;
		a: for(y = 0, i = 0; y < h; y++) {
			for(x = 0; x < w; x++, i++) {
				int a = (int) ((result[i] & 0xff000000L) >> 24);
				if(a > alphaThreshold)
					break a;
			}
		}
		int firstLine = y;
		if(firstLine == h)
			return null;

		a: for(y = h - 1, i = w * h - 1; y >= 0; y--) {
			for(x = w - 1; x >= 0; x--, i--) {
				int a = (int) ((result[i] & 0xff000000L) >> 24);
				if(a > alphaThreshold)
					break a;
			}
		}
		int lastLine = y;
		if(lastLine == -1)
			return null;

		a: for(x = 0; x < w; x++) {
			for(y = 0, i = x; y < h; y++, i += w) {
				int a = (int) ((result[i] & 0xff000000L) >> 24);
				if(a > alphaThreshold)
					break a;
			}
		}
		int firstColumn = x;
		if(x == w)
			return null;

		a: for(x = w - 1; x >= 0; x--) {
			for(y = 0, i = x; y < h; y++, i += w) {
				int a = (int) ((result[i] & 0xff000000L) >> 24);
				if(a > alphaThreshold)
					break a;
			}
		}
		int lastColumn = x;
		if(lastColumn == -1)
			return null;

		return new Rectangle(firstColumn, firstLine, lastColumn - firstColumn + 1, lastLine - firstLine + 1);
	}

	public static String makeFitToContentCode(double pw, double ph, int outputW, int outputH, Rectangle rect) {
		double imageCenterX = outputW * pw / 2;
		double imageCenterY = outputH * ph / 2;

		double roiCenterX = (rect.x + rect.width / 2.0) * pw;
		double roiCenterY = (rect.y + rect.height / 2.0) * ph;

		double dx = imageCenterX - roiCenterX;
		double dy = imageCenterY - roiCenterY;

		double factorX = (double) outputW / rect.width;
		double factorY = (double) outputH / rect.height;
		double factor = Math.min(factorX, factorY);

		return "At frame 0:\n" +
				"- translate by (" + (float) dx + ", " + (float) dy + ", 0)\n" +
				"- zoom by a factor of " + factor + "\n\n";
	}

	public static ImagePlus resizeForQuilt(ImagePlus imp, Quilt quilt) {
		int tw = quilt.tileWidth;
		int th = quilt.tileHeight;

		double factorX = 1.5 * tw / imp.getWidth();
		double factorY = 1.5 * th / imp.getHeight();

		double factor = Math.min(factorX, factorY);

		int tgtWidth = (int) Math.round(factor * imp.getWidth());
		int tgtHeight = (int) Math.round(factor * imp.getHeight());

		StackProcessor sp = new StackProcessor(imp.getStack());

		ImageStack resizedStack = sp.resize(tgtWidth, tgtHeight, true);

		ImagePlus ret = new ImagePlus(imp.getTitle(), resizedStack);
		Calibration cal = imp.getCalibration().copy();
		cal.pixelWidth /= factor;
		cal.pixelHeight /= factor;
		ret.setCalibration(cal);
		return ret;
	}

	public static ImagePlus render(
			ImagePlus input,
			String pathToAnimationScript,
			Quilt quilt,
			double viewConeAngle,
			boolean resizeInputImage,
			boolean fitViewToContent,
			boolean hideScalebar,
			boolean hideBorder,
			boolean autoShow) {

		if(resizeInputImage)
			input = resizeForQuilt(input, quilt);

		String animation = "";
		if(pathToAnimationScript != null && !pathToAnimationScript.trim().isEmpty()) {
			try {
				animation = loadText(pathToAnimationScript);
			} catch (Exception e) {
				throw new RuntimeException("Error loading " + pathToAnimationScript, e);
			}
		}

		final int outputWidth = quilt.tileWidth;
		final int outputHeight = quilt.tileHeight;

		Renderer3D renderer;
		try {
			renderer = new Renderer3D(input, input.getWidth(), input.getHeight());
			renderer.setTargetSize(outputWidth, outputHeight);
		} catch(UnsatisfiedLinkError e) {
			throw new RuntimeException("Either your graphics card doesn't support OpenCL "
					+ "or your drivers are not uptodate. Please install "
					+ "the newest drivers for your card and try again.", e);
		}

		Rectangle content = null;

		if(fitViewToContent) {
			// find content
			double showBoundingBox = renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_BOUNDINGBOX];
			double showScalebar = renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_SCALEBAR];
			renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_BOUNDINGBOX] = 0;
			renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_SCALEBAR] = 0;
			ImageProcessor rendered = renderer.render(renderer.getRenderingState());
			renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_BOUNDINGBOX] = showBoundingBox;
			renderer.getRenderingState().getNonChannelProperties()[ExtendedRenderingState.SHOW_SCALEBAR] = showScalebar;
			content = findContent(rendered);

			ImagePlus rend = new ImagePlus("rendered", rendered);
			if (content != null)
				rend.setRoi(content);
			rend.show();
		}

		Animator animator = new Animator(renderer, false);
		ImagePlus ret = null;
		ImageProcessor[] ips = null;
		double pw = input.getCalibration().pixelWidth;
		double ph = input.getCalibration().pixelHeight;
		try {
			int nTiles = quilt.columns * quilt.rows;
			for(int t = 0; t < nTiles; t++) {
				double angle = - viewConeAngle / 2.0 + t * viewConeAngle / (nTiles - 1);
				String extendedAnimation = animation;

				if(fitViewToContent && content != null)
					extendedAnimation += "\n\n" + makeFitToContentCode(pw, ph, outputWidth, outputHeight, content);

				if(hideScalebar)
					extendedAnimation += "\n\nAt frame 0 change scalebar visibility to off\n";

				if(hideBorder)
					extendedAnimation += "\n\nAt frame 0 change bounding box visibility to off\n";

				extendedAnimation += "\n\n" + "At frame 0 rotate by " + (float) angle + " degrees horizontally\n";
				animator.render(extendedAnimation);
				ImagePlus renderedTile = animator.waitForRendering(10, TimeUnit.MINUTES);
				if(t == 0) {
					ImageStack stack = new ImageStack(quilt.pixelsX, quilt.pixelsY);
					ips = new ImageProcessor[renderedTile.getStackSize()];
					for(int z = 0; z < renderedTile.getStackSize(); z++) {
						ips[z] = new ColorProcessor(quilt.pixelsX, quilt.pixelsY);
						stack.addSlice(ips[z]);
					}
					ret = new ImagePlus("quilt", stack);
					ret.setCalibration(renderedTile.getCalibration().copy());
					if(autoShow)
						ret.show();
				}
				for(int z = 0; z < renderedTile.getStackSize(); z++)
					ips[z].insert(renderedTile.getStack().getProcessor(z + 1), quilt.calculatePixelOffsetX(t), quilt.calculatePixelOffsetY(t));
				if(autoShow)
					ret.updateAndDraw();
			}
		} catch(Exception e) {
			throw new RuntimeException("Exception during rendering", e);
		}

		OpenCLRaycaster.close();

		return ret;
	}

	public static void main(String... args) throws IOException {
		new ij.ImageJ();
		// ImagePlus imp = IJ.openImage("D:\\jhuisken\\4Benjamin\\bone.tif");
		ImagePlus imp = IJ.openImage("D:\\jhuisken\\4Benjamin\\intestine-1.tif");
		imp.show();

		// render(imp, "d:/tmp.animation.txt", imp.getWidth(), imp.getHeight(), false).show();
		Create_Quilt cr = new Create_Quilt();
		cr.setup("", imp);
		cr.run(imp.getProcessor());
	}
}
