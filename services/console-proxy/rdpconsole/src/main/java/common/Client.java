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
import java.util.Arrays;

import rdpclient.RdpClient;
import streamer.Element;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.SocketWrapper;
import streamer.SocketWrapperImpl;
import streamer.apr.AprSocketWrapperImpl;
import streamer.bco.BcoSocketWrapperImpl;
import streamer.ssl.SSLState;
import vncclient.VncClient;

import common.opt.IntOption;
import common.opt.Option;
import common.opt.OptionParser;
import common.opt.StringEnumerationOption;
import common.opt.StringOption;

public class Client {

    enum Protocol {
        NONE, VNC, RDP, HYPERV
    }

    // Common options
    private final Option help = new Option() {
        {
            name = "--help";
            alias = "-h";
            description = "Show this help text.";
        }
    };
    private final Option debugLink = new Option() {
        {
            name = "--debug-link";
            alias = "-DL";
            description = "Print debugging messages when packets are trasnferred via links.";
        }
    };
    private final Option debugElement = new Option() {
        {
            name = "--debug-element";
            alias = "-DE";
            description = "Print debugging messages when packets are received or sent by elements.";
        }
    };
    private final Option debugPipeline = new Option() {
        {
            name = "--debug-pipeline";
            alias = "-DP";
            description = "Print debugging messages in pipelines.";
        }
    };

    private final StringOption hostName = new StringOption() {
        {
            name = "--host";
            alias = "-n";
            aliases = new String[] {"--host-name"};
            required = true;
            description = "Name or IP address of host to connect to.";
        }
    };
    private final IntOption canvasWidth = new IntOption() {
        {
            name = "--width";
            alias = "-W";
            value = 1024;
            description = "Width of canvas.";
        }
    };

    private final IntOption canvasHeight = new IntOption() {
        {
            name = "--height";
            alias = "-H";
            value = 768;
            description = "Height of canvas.";
        }
    };

    // Protocol specific options

    private final IntOption vncPort = new IntOption() {
        {
            name = "--port";
            alias = "-p";
            value = 5901;
            description = "Port of VNC display server to connect to. Calculate as 5900 + display number, e.g. 5900 for display #0, 5901 for display #1, and so on.";
        }
    };

    private final IntOption rdpPort = new IntOption() {
        {
            name = "--port";
            alias = "-p";
            value = 3389;
            description = "Port of RDP server to connect to.";
        }
    };

    private final IntOption hyperVPort = new IntOption() {
        {
            name = "--port";
            alias = "-p";
            value = 2179;
            description = "Port of HyperV server to connect to.";
        }
    };

    private final StringOption password = new StringOption() {
        {
            name = "--password";
            alias = "-P";
            required = true;
            description = "Password to use.";
        }
    };

    private final StringOption rdpPassword = new StringOption() {
        {
            name = "--password";
            alias = "-P";
            required = false;
            description = "Password to use. If omitted, then login screen will be shown.";
        }
    };

    private final StringOption userName = new StringOption() {
        {
            name = "--user";
            alias = "-U";
            value = "Administrator";
            description = "User name to use.";
        }
    };

    private final StringOption domain = new StringOption() {
        {
            name = "--domain";
            alias = "-D";
            value = "Workgroup";
            description = "NTLM domain to login into.";
        }
    };

    private final StringOption hyperVInstanceId = new StringOption() {
        {
            name = "--instance";
            alias = "-i";
            required = true;
            description = "HyperV instance ID to use.";
        }
    };
    private final StringEnumerationOption sslImplementation = new StringEnumerationOption() {
        {
            name = "--ssl-implementation";
            alias = "-j";
            value = "apr";
            choices = new String[] {"jre", "apr", "bco"};
            description = "Select SSL engine to use: JRE standard implementation, Apache Portable Runtime native library, BonuncyCastle.org implementation.";
        }
    };

    private final Option[] commonOptions = new Option[] {help, debugLink, debugElement, debugPipeline, hostName, canvasWidth, canvasHeight};
    private final Option[] vncOptions = new Option[] {vncPort, password};
    private final Option[] rdpOptions = new Option[] {sslImplementation, rdpPort, domain, userName, rdpPassword};
    private final Option[] hyperVOptions = new Option[] {sslImplementation, hyperVPort, hyperVInstanceId, domain, userName, password};

    private static Frame frame;
    private static SocketWrapper socket;
    private static ScrollPane scroller;
    private static ScreenDescription screen;
    private static BufferedImageCanvas canvas;
    private InetSocketAddress address;

    private void help() {
        System.out.println("Usage: \n  java common.Client vnc|rdp|hyperv OPTIONS\n");
        System.out.println(Option.toHelp("Common options", commonOptions));
        System.out.println(Option.toHelp("VNC options", vncOptions));
        System.out.println(Option.toHelp("RDP options", rdpOptions));
        System.out.println(Option.toHelp("HyperV options", hyperVOptions));
    }

    public void runClient(String[] args) {

        try {

            Protocol protocol = parseOptions(args);
            if (protocol == Protocol.NONE)
                return;

            System.setProperty("streamer.Link.debug", "" + debugLink.used);
            System.setProperty("streamer.Element.debug", "" + debugElement.used);
            System.setProperty("streamer.Pipeline.debug", "" + debugPipeline.used);

            SSLState sslState = new SSLState();

            // Create socket wrapper
            if ("jre".equals(sslImplementation.value)) {
                socket = new SocketWrapperImpl("socket", sslState);
            } else if ("apr".equals(sslImplementation.value)) {
                socket = new AprSocketWrapperImpl("socket", sslState);
            } else if ("bco".equals(sslImplementation.value)) {
                socket = new BcoSocketWrapperImpl("socket", sslState);
            } else {
                throw new RuntimeException("Unexpected option value: \"" + sslImplementation.value + "\". " + sslImplementation.help());
            }

            screen = new ScreenDescription();
            canvas = new BufferedImageCanvas(canvasWidth.value, canvasHeight.value);
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

            assemblePipeline(setMainElementAndAddressBasedOnProtocol(protocol, sslState));

            frame = createVncClientMainWindow(canvas, protocol.toString() + " " + hostName.value, new WindowAdapter() {
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

    protected static void assemblePipeline(Element main) {
        Pipeline pipeline = new PipelineImpl("Client");
        pipeline.add(socket, main);
        pipeline.link("socket", main.getId(), "socket");

        pipeline.validate();
    }

    private Element setMainElementAndAddressBasedOnProtocol(Protocol protocol, SSLState sslState) {
        Element main;
        switch (protocol) {
        case VNC:
            address = new InetSocketAddress(hostName.value, vncPort.value);
            main = new VncClient("client", password.value, screen, canvas);
            break;
        case RDP:
            address = new InetSocketAddress(hostName.value, rdpPort.value);
            main = new RdpClient("client", hostName.value, domain.value, userName.value, rdpPassword.value, null, screen, canvas, sslState);
            break;
        case HYPERV:
            address = new InetSocketAddress(hostName.value, hyperVPort.value);
            main = new RdpClient("client", hostName.value, domain.value, userName.value, password.value, hyperVInstanceId.value, screen, canvas, sslState);
            break;
        default:
            address = null;
            main = null;
        }

        return main;
    }

    private Protocol parseOptions(String[] args) {
        String protocolName = (args.length > 0) ? args[0] : "";
        Protocol protocol = Protocol.NONE;

        Option[] options;
        if (protocolName.equals("vnc")) {
            protocol = Protocol.VNC;
            options = join(commonOptions, vncOptions);
        } else if (protocolName.equals("rdp")) {
            protocol = Protocol.RDP;
            options = join(commonOptions, rdpOptions);
        } else if (protocolName.equals("hyperv")) {
            protocol = Protocol.HYPERV;
            options = join(commonOptions, hyperVOptions);
        } else {
            help();
            return Protocol.NONE;
        }

        // Parse all options for given protocol
        String[] arguments = OptionParser.parseOptions(args, 1, options);

        if (arguments.length > 0) {
            System.err.println("[Client] ERROR: Arguments are not allowed here. Check command syntax. Extra arguments: \"" + Arrays.toString(arguments) + "\".");
            help();
            return Protocol.NONE;
        }

        if (help.used) {
            help();
            return Protocol.NONE;
        }
        return protocol;
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

    /**
     * Join two arrays with options and return new array.
     */
    private Option[] join(Option[] a1, Option[] a2) {
        // Extend first array
        Option[] result = Arrays.copyOf(a1, a1.length + a2.length);

        // Append second array to first
        for (int i = 0, p = a1.length; i < a2.length; i++, p++)
            result[p] = a2[i];

        return result;
    }

    public static void main(String args[]) {
        // *DEBUG*/System.setProperty("javax.net.debug", "ssl");
        // * DEBUG */System.setProperty("javax.net.debug", "ssl:record:packet");

        new Client().runClient(args);
    }

}
