// Part of SourceAFIS Visualization: https://sourceafis.machinezoo.com/transparency/
package com.machinezoo.sourceafis.visualization;

import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import com.machinezoo.noexception.*;
import com.machinezoo.pushmode.dom.*;
import com.machinezoo.sourceafis.transparency.*;

public class TransparencyPixmap {
	public final int width;
	public final int height;
	private final int[] pixels;
	public TransparencyPixmap(int width, int height) {
		this.width = width;
		this.height = height;
		pixels = new int[width * height];
	}
	public TransparencyPixmap(IntPoint size) {
		this(size.x, size.y);
	}
	public IntPoint size() {
		return new IntPoint(width, height);
	}
	public int get(int x, int y) {
		return pixels[width * y + x];
	}
	public TransparencyPixmap set(int x, int y, int color) {
		pixels[width * y + x] = color;
		return this;
	}
	public TransparencyPixmap set(IntPoint at, int color) {
		return set(at.x, at.y, color);
	}
	public byte[] png() {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		image.setRGB(0, 0, width, height, pixels, 0, width);
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Exceptions.sneak().run(() -> ImageIO.write(image, "PNG", stream));
		return stream.toByteArray();
	}
	public EmbeddedImage embedded() {
		return new EmbeddedImage()
			.width(width)
			.height(height)
			.image(png());
	}
	public DomElement svg() {
		return embedded().svg();
	}
	public DomElement html() {
		return embedded().html();
	}
	public TransparencyPixmap fill(int color) {
		for (int i = 0; i < pixels.length; ++i)
			pixels[i] = color;
		return this;
	}
	public static int gray(int brightness) {
		return 0xff_00_00_00 | (brightness << 16) | (brightness << 8) | brightness;
	}
}
