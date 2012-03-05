package com.cloud.consoleproxy.vnc;

public interface VncClientListener {
	void onFramebufferSizeChange(int w, int h);
	void onFramebufferUpdate(int x, int y, int w, int h);
}
