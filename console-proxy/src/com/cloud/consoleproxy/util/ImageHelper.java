package com.cloud.consoleproxy.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageHelper {
	public static byte[] jpegFromImage(BufferedImage image) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream(128000);
		javax.imageio.ImageIO.write(image, "jpg", bos);
		
		byte[] jpegBits = bos.toByteArray();
		bos.close();
		return jpegBits;
	}
}
