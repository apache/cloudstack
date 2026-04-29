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

import java.net.InetAddress;
import java.net.UnknownHostException;

import rdpclient.adapter.AwtRdpKeyboardAdapter;
import rdpclient.adapter.AwtRdpMouseAdapter;
import rdpclient.hyperv.ClientPreConnectionBlob;
import rdpclient.ntlmssp.ClientNtlmsspNegotiate;
import rdpclient.ntlmssp.ClientNtlmsspPubKeyAuth;
import rdpclient.ntlmssp.ClientNtlmsspUserCredentials;
import rdpclient.ntlmssp.NtlmState;
import rdpclient.ntlmssp.ServerNtlmsspChallenge;
import rdpclient.ntlmssp.ServerNtlmsspPubKeyPlus1;
import rdpclient.rdp.ClientConfirmActivePDU;
import rdpclient.rdp.ClientFastPathPDU;
import rdpclient.rdp.ClientInfoPDU;
import rdpclient.rdp.ClientMCSAttachUserRequest;
import rdpclient.rdp.ClientMCSChannelJoinRequestServerMCSChannelConfirmPDUs;
import rdpclient.rdp.ClientMCSConnectInitial;
import rdpclient.rdp.ClientMCSErectDomainRequest;
import rdpclient.rdp.ClientTpkt;
import rdpclient.rdp.ClientX224ConnectionRequestPDU;
import rdpclient.rdp.ClientX224DataPDU;
import rdpclient.rdp.RdpConstants;
import rdpclient.rdp.RdpState;
import rdpclient.rdp.ServerBitmapUpdate;
import rdpclient.rdp.ServerDemandActivePDU;
import rdpclient.rdp.ServerFastPath;
import rdpclient.rdp.ServerIOChannelRouter;
import rdpclient.rdp.ServerLicenseErrorPDUValidClient;
import rdpclient.rdp.ServerMCSAttachUserConfirmPDU;
import rdpclient.rdp.ServerMCSConnectResponse;
import rdpclient.rdp.ServerMCSPDU;
import rdpclient.rdp.ServerPaletteUpdate;
import rdpclient.rdp.ServerX224ConnectionConfirmPDU;
import rdpclient.rdp.ServerX224DataPdu;
import streamer.PipelineImpl;
import streamer.Queue;
import streamer.ssl.SSLState;
import streamer.ssl.UpgradeSocketToSSL;
import common.AwtKeyEventSource;
import common.AwtMouseEventSource;
import common.BufferedImageCanvas;
import common.ScreenDescription;
import common.adapter.AwtCanvasAdapter;

public class RdpClient extends PipelineImpl {

    AwtMouseEventSource mouseEventSource = null;
    AwtKeyEventSource keyEventSource = null;

    /**
     * Name of last OneTimePacket in handshake sequence.
     */
    private static final String HANDSHAKE_END = "server_valid_client";

    /**
     * Create new RDP or HyperV cli
     *
     * @param id
     *          id of this element
     * @param userName
     *          user name
     * @param password
     *          password
     * @param pcb
     *          pre-connection blob for HyperV server or null/empty string to
     *          disable. Usually, HyperV VM ID, e.g.
     *          "39418F90-6D03-468E-B796-91C60DD6653A".
     * @param screen
     *          screen description to fill
     * @param canvas
     *          canvas to draw on
     * @param sslState
     */
    public RdpClient(String id, String serverHostName, String domain, String userName, String password, String pcb, ScreenDescription screen,
            BufferedImageCanvas canvas, SSLState sslState) {
        super(id);
        assembleRDPPipeline(serverHostName, domain, userName, password, pcb, screen, canvas, sslState);
    }

    // /* DEBUG */
//  @Override
//  protected HashMap<String, streamer.Element> initElementMap(String id) {
//    HashMap<String, streamer.Element> map = new HashMap<String, streamer.Element>();
//    map.put("IN", new ServerPacketSniffer("server <"));
//    map.put("OUT", new ClientPacketSniffer("> client"));
//    return map;
//  }

    /**
     * Assemble connection sequence and main pipeline.
     *
     * Connection sequence for RDP w/o NLA: cookie(TPKT) SSL x224(TPKT)
     * main(FastPath).
     *
     * Connection sequence for RDP w NLA: cookie(TPKT) SSL credssp x224(TPKT)
     * main(FastPath).
     *
     * Connection sequence for HyperV w NLA: pcb SSL credssp cookie(TPKT)
     * x224(TPKT) main(FastPath).
     */
    protected void assembleRDPPipeline(String serverHostName, String domain, String userName, String password, String pcb, ScreenDescription screen,
            BufferedImageCanvas canvas, SSLState sslState) {
        // If preconnection blob with VM ID is specified, then we are connecting to
        // HyperV server
        boolean hyperv = (pcb != null && !pcb.isEmpty());
        // HyperV server requires NLA (CredSSP/SPNEGO/NTLMSSP) to connect, because
        // it cannot display login screen
        boolean credssp = hyperv || (password != null && !password.isEmpty());

        String workstation;
        try {
            workstation = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            workstation = "workstation";
        }

        //
        // Handshake chain
        //

        RdpState state = new RdpState();
        NtlmState ntlmState = new NtlmState();

        int[] channelsToJoin = new int[] {RdpConstants.CHANNEL_IO,
                // RdpConstants.CHANNEL_RDPRDR, // RDPRDR channel is not used in current
                // version

                // RdpConstants .CHANNEL_CLIPRDR // Clipboard channel is refused to join :-/
        };

        // Add elements

        // If pre-connection blob is specified, then add element to send it as
        // first packet
        if (hyperv) {
            add(new ClientPreConnectionBlob("pcb", pcb));
        }

        // If password is specified, then use CredSSP/NTLM (NTLMSSP)
        int protocol = RdpConstants.RDP_NEG_REQ_PROTOCOL_SSL;
        if (credssp) {
            protocol = RdpConstants.RDP_NEG_REQ_PROTOCOL_HYBRID;

            add(
                    new ClientNtlmsspNegotiate("client_ntlmssp_nego", ntlmState),

                    new ServerNtlmsspChallenge("server_ntlmssp_challenge", ntlmState),

                    new ClientNtlmsspPubKeyAuth("client_ntlmssp_auth", ntlmState, sslState, serverHostName, domain, workstation, userName, password),

                    new ServerNtlmsspPubKeyPlus1("server_ntlmssp_confirm", ntlmState),

                    new ClientNtlmsspUserCredentials("client_ntlmssp_finish", ntlmState)

                    );
        }

        add(new ClientX224ConnectionRequestPDU("client_connection_req", userName, protocol), new ServerX224ConnectionConfirmPDU("server_connection_conf"),
                new UpgradeSocketToSSL("upgrade_to_ssl"),

                new ClientMCSConnectInitial("client_initial_conference_create"), new ServerMCSConnectResponse("server_initial_conference_create"),

                new ClientMCSErectDomainRequest("client_erect_domain"),

                new ClientMCSAttachUserRequest("client_atach_user"), new ServerMCSAttachUserConfirmPDU("server_atach_user_confirm", state),

                new ClientMCSChannelJoinRequestServerMCSChannelConfirmPDUs("client_channel_join_rdprdr", channelsToJoin, state),

                new ClientInfoPDU("client_info_req", userName),

                new ServerLicenseErrorPDUValidClient("server_valid_client"),

                new ServerFastPath("server_fastpath"),

                // new ServerTpkt("server_tpkt"),

                new ServerX224DataPdu("server_x224_data"),

                // These TPKT and X224 wrappers are connected directly to OUT for
                // handshake sequence
                new ClientTpkt("client_tpkt_ot"),

                new ClientX224DataPDU("client_x224_data_ot")

                );

        // If HyperV VM ID is set, then insert element which will send VM ID as
        // first packet of connection, before other packets
        if (hyperv) {

            // HyperV: pcb SSL credssp cookie x224 main.

            link("IN",

                    // Pre Connection Blob
                    "pcb",

                    // Main (will be used after connection seq) or tpkt (to X224)
                    "server_fastpath >tpkt",

                    // SSL
                    "upgrade_to_ssl",

                    // CredSSP
                    "client_ntlmssp_nego", "server_ntlmssp_challenge", "client_ntlmssp_auth", "server_ntlmssp_confirm", "client_ntlmssp_finish",

                    // Cookie
                    "client_connection_req", "server_connection_conf",

                    // X224
                    "client_initial_conference_create");

            for (String element : new String[] {"pcb", "client_ntlmssp_nego", "server_ntlmssp_challenge", "client_ntlmssp_auth", "server_ntlmssp_confirm",
            "client_ntlmssp_finish"}) {
                link(element + " >otout", element + "< OUT");

            }

        } else {

            // RDP: cookie SSL (credssp) x224 main.

            link("IN",

                    // Main or tpkt
                    "server_fastpath >tpkt",

                    // Cookie
                    "client_connection_req", "server_connection_conf",

                    // SSL
                    "upgrade_to_ssl");

            if (credssp) {
                // SSL
                link("upgrade_to_ssl",

                        // CredSSP
                        "client_ntlmssp_nego", "server_ntlmssp_challenge", "client_ntlmssp_auth", "server_ntlmssp_confirm", "client_ntlmssp_finish",

                        // X224
                        "client_initial_conference_create");

                for (String element : new String[] {"client_ntlmssp_nego", "server_ntlmssp_challenge", "client_ntlmssp_auth", "server_ntlmssp_confirm",
                "client_ntlmssp_finish"}) {
                    link(element + " >otout", element + "< OUT");

                }

            } else {

                link(
                        // SSL
                        "upgrade_to_ssl",

                        // X224
                        "client_initial_conference_create");
            }
        }

        link(
                // X224
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
        String x224_peers[] = new String[] {"client_initial_conference_create", "server_initial_conference_create", "client_erect_domain", "client_atach_user",
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

        // Last element of handshake sequence will wake up queue and socket
        // output pull loop, which will switch links, between socket output and
        // queue, from push mode to pull mode.
        link(HANDSHAKE_END + " >queue", "queue", "OUT");

        // Transition from handshake sequence for slow path packets
        link(HANDSHAKE_END, "server_mcs");

        //
        // Main network
        //

        mouseEventSource = new AwtMouseEventSource("mouse");
        keyEventSource = new AwtKeyEventSource("keyboard");

        // Subscribe packet sender to various events
        canvas.addMouseListener(mouseEventSource);
        canvas.addMouseMotionListener(mouseEventSource);
        canvas.addKeyListener(keyEventSource);

        // Add elements
        add(

                new ServerIOChannelRouter("server_io_channel", state),

                new ServerDemandActivePDU("server_demand_active", screen, state),

                new ClientConfirmActivePDU("client_confirm_active", screen, state),

                new ServerBitmapUpdate("server_bitmap_update"),

                new AwtCanvasAdapter("canvas_adapter", canvas, screen),

                new ServerPaletteUpdate("server_palette", screen),

                keyEventSource, new AwtRdpKeyboardAdapter("keyboard_adapter"),

                mouseEventSource, new AwtRdpMouseAdapter("mouse_adapter"),

                // These FastPath, TPKT, and X224 wrappers are connected to queue
                new ClientTpkt("client_tpkt_queue"),

                new ClientX224DataPDU("client_x224_data_queue"),

                new ClientFastPathPDU("client_fastpath_queue"));

        // Server packet handlers
        link("server_mcs >channel_1003", "server_io_channel");
        link("server_fastpath >bitmap", "fastpath< server_bitmap_update", "server_bitmap_update< canvas_adapter");
        link("server_io_channel >bitmap", "slowpath< server_bitmap_update");

        link("server_fastpath >palette", "fastpath< server_palette");
        link("server_io_channel >palette", "slowpath< server_palette");

        link("server_io_channel >demand_active", "slowpath< server_demand_active");
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

    public AwtMouseEventSource getMouseEventSource() {
        return mouseEventSource;
    }

    public AwtKeyEventSource getKeyEventSource() {
        return keyEventSource;
    }
}
