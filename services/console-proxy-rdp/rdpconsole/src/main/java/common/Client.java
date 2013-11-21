// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package common;

import java.awt.Frame;
import java.awt.ScrollPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.InetSocketAddress;

import rdpclient.RdpClient;
import streamer.Element;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.SocketWrapper;
import vncclient.VncClient;

public class Client {

    private static Frame frame;
    private static SocketWrapper socket;
    private static ScrollPane scroller;
    private static ScreenDescription screen;
    private static BufferedImageCanvas canvas;

    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        // System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        try {
            if (args.length < 4) {
                System.out.println("Usage: \n  java common.Client vnc IP PORT PASSWORD\n  java common.Client rdp IP PORT username\n");
                System.exit(0);
            }

            String connectionType = args[0];
            String hostname = args[1];
            int port = Integer.parseInt(args[2]);
            String userNameOrPassword = args[3];

            // Create address from arguments
            InetSocketAddress address = new InetSocketAddress(hostname, port);

            // Create socket wrapper
            socket = new SocketWrapper("socket");

            screen = new ScreenDescription();
            canvas = new BufferedImageCanvas(1024, 768);
            screen.addSizeChangeListener(new SizeChangeListener() {
                @Override
                public void sizeChanged(int width, int height) {
                    if (canvas != null) {
                        canvas.setCanvasSize(width, height);
                        if (scroller != null)
                            scroller.setSize(canvas.getWidth(), canvas.getHeight());
                    }
                }
            });

            // Assemble pipeline
            Element main;
            if ("vnc".equals(connectionType)) {
                main = new VncClient("client", userNameOrPassword, screen, canvas);
            } else if ("rdp".equals(connectionType)) {
                main = new RdpClient("client", userNameOrPassword, screen, canvas);
            } else {
                throw new RuntimeException("Unknown connection type. Expected value: \"vnc\" or \"rdp\", actual value: \"" + connectionType + "\".");
            }

            Pipeline pipeline = new PipelineImpl("Client");
            pipeline.add(socket, main);
            pipeline.link("socket", main.getId(), "socket");

            pipeline.validate();

            frame = createVncClientMainWindow(canvas, "VNC", new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent evt) {
                    shutdown();
                }
            });

            try {
                // Connect socket to remote server and run main loop(s)
                socket.connect(address);
            } finally {
                shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    protected static void shutdown() {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
        if (socket != null)
            socket.shutdown();
    }

    protected static Frame createVncClientMainWindow(BufferedImageCanvas canvas, String title, WindowListener windowListener) {
        // Create AWT windows
        Frame frame = new Frame(title + " - RDP");

        // Use scrolling pane to support screens, which are larger than ours
        scroller = new ScrollPane(ScrollPane.SCROLLBARS_AS_NEEDED);
        scroller.add(canvas);
        scroller.setSize(canvas.getWidth(), canvas.getHeight());

        frame.add(scroller);
        frame.pack();
        frame.setVisible(true);

        frame.addWindowListener(windowListener);

        return frame;
    }

}
