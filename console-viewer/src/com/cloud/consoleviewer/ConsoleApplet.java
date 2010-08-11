
//
//  Copyright (C) 2001-2004 HorizonLive.com, Inc.  All Rights Reserved.
//  Copyright (C) 2002 Constantin Kaplinsky.  All Rights Reserved.
//  Copyright (C) 1999 AT&T Laboratories Cambridge.  All Rights Reserved.
//
//  This is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This software is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this software; if not, write to the Free Software
//  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
//  USA.
//

//
// VncViewer.java - the VNC viewer applet.  This class mainly just sets up the
// user interface, leaving it to the VncCanvas to do the actual rendering of
// a VNC desktop.
//

package com.cloud.consoleviewer;

import java.awt.*;
import java.awt.event.*;

import com.cloud.console.Logger;

public class ConsoleApplet extends java.applet.Applet implements WindowListener {
	private static final Logger s_logger = Logger.getLogger(ConsoleApplet.class);

	private static final long serialVersionUID = -8463170916581351766L;
	
	ConsoleViewer viewer;
	String errorMsg;

	public ConsoleApplet() {
		viewer = new ConsoleViewer();
	}

	public static void main(String[] argv) {
		ConsoleApplet applet = new ConsoleApplet();
		applet.viewer.mainArgs = argv;
		applet.viewer.inAnApplet = false;
		applet.viewer.inSeparateFrame = true;
		applet.viewer.inProxyMode = false;
		applet.init();
		applet.start();
	}

	// Reference to this applet for inter-applet communication.
	public static java.applet.Applet refApplet;

	//
	// init()
	//

	public void init() {
		s_logger.info("Initializing applet");
		refApplet = this;
		viewer.applet = this;
		viewer.init();
		disableFocusTraversal(this);
		errorMsg = "Connecting...";
		invalidate();
	}

	public void paint(Graphics g) {
		if(viewer != null && viewer.vc != null && viewer.vc.memGraphics != null)
			g.setColor(Color.WHITE);
		else
			g.setColor(Color.BLACK);
		g.fillRect(0, 0, 800, 600);
		
		if(errorMsg != null && errorMsg.length() > 0) {
			g.setFont(new Font(null, Font.PLAIN, 20));
			g.setColor(Color.WHITE);
	  		FontMetrics fm = g.getFontMetrics();
	  		int width = fm.stringWidth(errorMsg);
	  		int startx = (800 - width) / 2;
	  		if (startx < 0) startx = 0;
	  			g.drawString(errorMsg, startx, 600 / 2); 
		}
	}
	
	public void paintErrorString(String msg) {
		s_logger.info("paintErrorString");
		
		errorMsg = msg;
		invalidate();
		repaint();
	}

	public void stop() {
		s_logger.info("Stopping applet");
		viewer.stop();
	}

	//
	// This method is called before the applet is destroyed.
	//
	public void destroy() {
		s_logger.info("Destroying applet");
		viewer.destroy();
		viewer = null;
	}
	
  	public void sendCtrlAltDel() {
  		if(viewer != null && viewer.vc != null)
  			viewer.vc.sendCtrlAltDel();
  	}
  	
  	public int getFrameBufferWidth() {
  		if(viewer != null && viewer.vc != null)
  			return viewer.vc.getWidth();
  		
  		return 800;
  	}
  	
  	public int getFrameBufferHeight() {
  		if(viewer != null && viewer.vc != null)
  			return viewer.vc.getHeight();
  		return 600;
  	}

	//
	// Close application properly on window close event.
	//

	public void windowClosing(WindowEvent evt) {
		s_logger.info("Closing window");
		viewer.windowClosing(evt);
	}

	//
	// Ignore window events we're not interested in.
	//
	public void windowActivated(WindowEvent evt) {
	}

	public void windowDeactivated(WindowEvent evt) {
	}

	public void windowOpened(WindowEvent evt) {
	}

	public void windowClosed(WindowEvent evt) {
	}

	public void windowIconified(WindowEvent evt) {
	}

	public void windowDeiconified(WindowEvent evt) {
	}
	
	static public void disableFocusTraversal(Container con) {
	    con.setFocusTraversalKeysEnabled(false);
	    con.setFocusCycleRoot(true);
	}
}
