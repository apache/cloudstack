package com.cloud.consoleproxy.vnc;

import java.util.List;

import com.cloud.console.TileInfo;

public interface FrameBufferCanvas {
	public byte[] getFrameBufferJpeg();
	public byte[] getTilesMergedJpeg(List<TileInfo> tileList, int tileWidth, int tileHeight);
}
