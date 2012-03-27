package com.cloud.consoleproxy.vnc;

import java.awt.Image;
import java.util.List;

import com.cloud.consoleproxy.util.TileInfo;

public interface FrameBufferCanvas {
	Image getFrameBufferScaledImage(int width, int height);
	public byte[] getFrameBufferJpeg();
	public byte[] getTilesMergedJpeg(List<TileInfo> tileList, int tileWidth, int tileHeight);
}
