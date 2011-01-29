
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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.Date;

public class ConsoleApplet extends java.applet.Applet implements WindowListener {

	private static final long serialVersionUID = -8463170916581351766L;
	
	ConsoleViewer viewer;

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
		refApplet = this;
		viewer.applet = this;
		viewer.init();
		disableFocusTraversal(this);
	}

	public void update(Graphics g) {
	}

	public void stop() {
		Logger.log(Logger.INFO, "Stopping applet");
		viewer.stop();
	}

	//
	// This method is called before the applet is destroyed.
	//
	public void destroy() {
		Logger.log(Logger.INFO, "Destroying applet");
		viewer.destroy();
		viewer = null;
	}

	//
	// Close application properly on window close event.
	//

	public void windowClosing(WindowEvent evt) {
		Logger.log(Logger.INFO, "Closing window");
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
