package com.cloud.consoleproxy;

public interface ConsoleProxyClientListener {
	void onFramebufferSizeChange(int w, int h);
	void onFramebufferUpdate(int x, int y, int w, int h);

	void onClientConnected();
	void onClientClose();
}
