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
package rdpclient;

import streamer.PipelineImpl;
import streamer.Queue;
import common.AwtCanvasAdapter;
import common.AwtKeyEventSource;
import common.AwtMouseEventSource;
import common.BufferedImageCanvas;
import common.ScreenDescription;

public class RdpClient extends PipelineImpl {

    /**
     * Name of last OneTimePacket in handshake sequence.
     */
    private static final String HANDSHAKE_END = "server_valid_client";

    public RdpClient(String id, String userName, ScreenDescription screen, BufferedImageCanvas canvas) {
        super(id);
        assembleRDPPipeline(userName, screen, canvas);
    }

//  /* DEBUG */
//  @Override
//  protected HashMap<String, streamer.Element> initElementMap(String id) {
//    HashMap<String, streamer.Element> map = new HashMap<String, streamer.Element>();
//    map.put("IN", new ServerPacketSniffer("server <"));
//    map.put("OUT", new ClientPacketSniffer("> client"));
//    return map;
//  }

    private void assembleRDPPipeline(String userName, ScreenDescription screen, BufferedImageCanvas canvas) {
        //
        // Handshake chain
        //

        RdpState state = new RdpState();
        int[] channelsToJoin = new int[] {RdpConstants.CHANNEL_RDPRDR, RdpConstants.CHANNEL_IO};

        // Add elements

        add(

        new ClientX224ConnectionRequestPDU("client_connection_req", userName), new ServerX224ConnectionConfirmPDU("server_connection_conf"),

        new UpgradeSocketToSSL("upgrade_to_ssl"),

        new ClientMCSConnectInitial("client_initial_conference_create"), new ServerMCSConnectResponse("server_initial_conference_create"),

        new ClientMCSErectDomainRequest("client_erect_domain"),

        new ClientMCSAttachUserRequest("client_atach_user"), new ServerMCSAttachUserConfirmPDU("server_atach_user_confirm", state),

        new ClientMCSChannelJoinRequest_ServerMCSChannelConfirmPDUs("client_channel_join_rdprdr", channelsToJoin, state),

        new ClientInfoPDU("client_info_req", userName),

        new ServerLicenseErrorPDUValidClient("server_valid_client"),

        new ServerFastPath("server_fastpath"),

        new ServerTpkt("server_tpkt"),

        new ServerX224DataPdu("server_x224_data"),

        // These TPKT and X224 wrappers are connected directly to OUT for handshake
        // sequence
            new ClientTpkt("client_tpkt_ot"),

            new ClientX224DataPdu("client_x224_data_ot")

        );

        // Handshake sequence (via SlowPath)
        link("IN",

        "server_fastpath >tpkt", "server_tpkt",

        "client_connection_req", "server_connection_conf",

        "upgrade_to_ssl",

        "client_initial_conference_create", "server_initial_conference_create",

        "client_erect_domain",

        "server_x224_data",

        "client_atach_user", "server_atach_user_confirm",

        "client_channel_join_rdprdr",

        "client_info_req",

        "server_valid_client"

        );

        // Chain for direct handshake responses (without involving of queue)
        link("client_x224_data_ot", "client_tpkt_ot", "client_tpkt_ot< OUT");

        // Connect one time outputs to client TPKT input
        String tpkt_peers[] = new String[] {"client_connection_req", "server_connection_conf", "upgrade_to_ssl", "client_x224_data_ot"};
        for (String element : tpkt_peers) {
            link(element + " >otout", element + "< client_tpkt_ot");
        }

        // Connect one time outputs to client X224 input
        String x224_peers[] =
            new String[] {"client_initial_conference_create", "server_initial_conference_create", "client_erect_domain", "client_atach_user",
                "server_atach_user_confirm", "client_channel_join_rdprdr", "client_info_req", "server_valid_client"};
        for (String element : x224_peers) {
            link(element + " >otout", element + "< client_x224_data_ot");
        }

        //
        // Transition
        //

        add(
        // To transfer packets between input threads and output thread.
        new Queue("queue"),

        // Slow path: MultiChannel Support
            new ServerMCSPDU("server_mcs")

        );

        // Last element of handshake sequence will wake up queue and and socket
        // output pull loop, which will switch links, between socket output and
        // queue, from push mode to pull mode.
        link(HANDSHAKE_END + " >queue", "queue", "OUT");

        // Transition from handshake sequence for slow path packets
        link(HANDSHAKE_END, "server_mcs");

        //
        // Main network
        //

        AwtMouseEventSource mouseEventSource = new AwtMouseEventSource("mouse");
        AwtKeyEventSource keyEventSource = new AwtKeyEventSource("keyboard");

        // Subscribe packet sender to various events
        canvas.addMouseListener(mouseEventSource);
        canvas.addMouseMotionListener(mouseEventSource);
        canvas.addKeyListener(keyEventSource);

        // Add elements
        add(

        new ServerChannel1003Router("server_channel_1003", state),

        new ServerDemandActivePDU("server_demand_active", screen, state),

        new ClientConfirmActivePDU("client_confirm_active", screen, state),

        new ServerBitmapUpdate("server_bitmap_update"),

        new AwtCanvasAdapter("canvas_adapter", canvas, screen),

        new ServerPaletteUpdate("server_palette", screen),

        keyEventSource, new AwtRdpKeyboardAdapter("keyboard_adapter"),

        mouseEventSource, new AwtRdpMouseAdapter("mouse_adapter"),

        // These FastPath, TPKT, and X224 wrappers are connected to queue
            new ClientTpkt("client_tpkt_queue"),

            new ClientX224DataPdu("client_x224_data_queue"),

            new ClientFastPathPDU("client_fastpath_queue"));

        // Server packet handlers
        link("server_mcs >channel_1003", "server_channel_1003");
        link("server_fastpath >bitmap", "fastpath< server_bitmap_update", "server_bitmap_update< canvas_adapter");
        link("server_channel_1003 >bitmap", "slowpath< server_bitmap_update");

        link("server_fastpath >palette", "fastpath< server_palette");
        link("server_channel_1003 >palette", "slowpath< server_palette");

        link("server_channel_1003 >demand_active", "slowpath< server_demand_active");
        // link("server_demand_active >confirm_active", "client_confirm_active",
        // "confirm_active< client_channel_1003");
        link("server_demand_active >confirm_active", "client_confirm_active", "confirm_active< client_x224_data_queue");

        // Link mouse and keyboard to socket via adapters and send them using
        // FastPath protocol
        link(mouseEventSource.getId(), "mouse_adapter", "mouse_adapter< client_fastpath_queue");
        link(keyEventSource.getId(), "keyboard_adapter", "keyboard_adapter< client_fastpath_queue");

        // Link packet wrappers to outgoing queue
        link("client_fastpath_queue", "client_fastpath_queue< queue");
        link("client_x224_data_queue", "client_tpkt_queue", "client_tpkt_queue< queue");

    }
}
