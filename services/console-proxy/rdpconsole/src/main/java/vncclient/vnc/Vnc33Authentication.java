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
package vncclient.vnc;

import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.FakeSink;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

public class Vnc33Authentication extends OneTimeSwitch {

    /**
     * Password to use when authentication is required.
     */
    protected String password = null;

    /**
     * Authentication stage:
     * <ul>
     * <li>0 - challenge received, response must be sent
     * <li>1 - authentication result received.
     * </ul>
     */
    protected int stage = 0;

    public Vnc33Authentication(String id) {
        super(id);
    }

    public Vnc33Authentication(String id, String password) {
        super(id);
        this.password = password;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        switch (stage) {
        case 0: // Read security with optional challenge and response
            stage0(buf, link);

            break;
        case 1: // Read authentication response
            stage1(buf, link);
            break;
        }

    }

    /**
     * Read security type. If connection type is @see
     * RfbConstants.CONNECTION_FAILED, then throw exception. If connection type is @see
     * RfbConstants.NO_AUTH, then switch off this element. If connection type is @see
     * RfbConstants.VNC_AUTH, then read challenge, send encoded password, and read
     * authentication response.
     */
    private void stage0(ByteBuffer buf, Link link) {
        // At least 4 bytes are necessary
        if (!cap(buf, 4, UNLIMITED, link, true))
            return;

        // Read security type
        int authType = buf.readSignedInt();

        switch (authType) {
        case RfbConstants.CONNECTION_FAILED: {
            // Server forbids to connect. Read reason and throw exception

            int length = buf.readSignedInt();
            String reason = new String(buf.data, buf.offset, length, RfbConstants.US_ASCII_CHARSET);

            throw new RuntimeException("Authentication to VNC server is failed. Reason: " + reason);
        }

        case RfbConstants.NO_AUTH: {
            // Client can connect without authorization. Nothing to do.
            // Switch off this element from circuit
            switchOff();
            break;
        }

        case RfbConstants.VNC_AUTH: {
            // Read challenge and generate response
            responseToChallenge(buf, link);
            break;
        }

        default:
            throw new RuntimeException("Unsupported VNC protocol authorization scheme, scheme code: " + authType + ".");
        }

    }

    private void responseToChallenge(ByteBuffer buf, Link link) {
        // Challenge is exactly 16 bytes long
        if (!cap(buf, 16, 16, link, true))
            return;

        ByteBuffer challenge = buf.slice(buf.cursor, 16, true);
        buf.unref();

        // Encode challenge with password
        ByteBuffer response;
        try {
            response = encodePassword(challenge, password);
            challenge.unref();
        } catch (Exception e) {
            throw new RuntimeException("Cannot encrypt client password to send to server: " + e.getMessage());
        }

        if (verbose) {
            response.putMetadata("sender", this);
        }

        // Send encoded challenge
        nextStage();
        pushDataToOTOut(response);

    }

    private void nextStage() {
        stage++;

        if (verbose)
            System.out.println("[" + this + "] INFO: Next stage: " + stage + ".");
    }

    /**
     * Encode password using DES encryption with given challenge.
     *
     * @param challenge
     *          a random set of bytes.
     * @param password
     *          a password
     * @return DES hash of password and challenge
     */
    public ByteBuffer encodePassword(ByteBuffer challenge, String password) {
        if (challenge.length != 16)
            throw new RuntimeException("Challenge must be exactly 16 bytes long.");

        // VNC password consist of up to eight ASCII characters.
        byte[] key = {0, 0, 0, 0, 0, 0, 0, 0}; // Padding
        byte[] passwordAsciiBytes = password.getBytes(RfbConstants.US_ASCII_CHARSET);
        System.arraycopy(passwordAsciiBytes, 0, key, 0, Math.min(password.length(), 8));

        // Flip bytes (reverse bits) in key
        for (int i = 0; i < key.length; i++) {
            key[i] = flipByte(key[i]);
        }

        try {
            KeySpec desKeySpec = new DESKeySpec(key);
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey secretKey = secretKeyFactory.generateSecret(desKeySpec);
            Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            ByteBuffer buf = new ByteBuffer(cipher.doFinal(challenge.data, challenge.offset, challenge.length));

            return buf;
        } catch (Exception e) {
            throw new RuntimeException("Cannot encode password.", e);
        }
    }

    /**
     * Reverse bits in byte, so least significant bit will be most significant
     * bit. E.g. 01001100 will become 00110010.
     *
     * See also: http://www.vidarholen.net/contents/junk/vnc.html ,
     * http://bytecrafter .blogspot.com/2010/09/des-encryption-as-used-in-vnc.html
     *
     * @param b
     *          a byte
     * @return byte in reverse order
     */
    private static byte flipByte(byte b) {
        int b1_8 = (b & 0x1) << 7;
        int b2_7 = (b & 0x2) << 5;
        int b3_6 = (b & 0x4) << 3;
        int b4_5 = (b & 0x8) << 1;
        int b5_4 = (b & 0x10) >>> 1;
        int b6_3 = (b & 0x20) >>> 3;
        int b7_2 = (b & 0x40) >>> 5;
        int b8_1 = (b & 0x80) >>> 7;
        byte c = (byte)(b1_8 | b2_7 | b3_6 | b4_5 | b5_4 | b6_3 | b7_2 | b8_1);
        return c;
    }

    /**
     * Read authentication result, send nothing.
     */
    private void stage1(ByteBuffer buf, Link link) {
        // Read authentication response
        if (!cap(buf, 4, 4, link, false))
            return;

        int authResult = buf.readSignedInt();

        switch (authResult) {
        case RfbConstants.VNC_AUTH_OK: {
            // Nothing to do
            if (verbose)
                System.out.println("[" + this + "] INFO: Authentication successful.");
            break;
        }

        case RfbConstants.VNC_AUTH_TOO_MANY:
            throw new RuntimeException("Connection to VNC server failed: too many wrong attempts.");

        case RfbConstants.VNC_AUTH_FAILED:
            throw new RuntimeException("Connection to VNC server failed: wrong password.");

        default:
            throw new RuntimeException("Connection to VNC server failed, reason code: " + authResult);
        }

        switchOff();

    }

    @Override
    public String toString() {
        return "VNC3.3 Authentication(" + id + ")";
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        final String password = "test";

        Element source = new MockSource("source") {
            {
                bufs = ByteBuffer.convertByteArraysToByteBuffers(
                        // Request authentication and send 16 byte challenge
                        new byte[] {0, 0, 0, RfbConstants.VNC_AUTH, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16},
                        // Respond to challenge with AUTH_OK
                        new byte[] {0, 0, 0, RfbConstants.VNC_AUTH_OK});
            }
        };

        Element mainSink = new FakeSink("mainSink");
        final Vnc33Authentication auth = new Vnc33Authentication("auth", password);
        Element initSink = new MockSink("initSink") {
            {
                // Expect encoded password
                bufs = new ByteBuffer[] {auth.encodePassword(new ByteBuffer(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}), password)};
            }
        };

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.addAndLink(source, auth, mainSink);
        pipeline.add(initSink);
        pipeline.link("auth >otout", "initSink");

        pipeline.runMainLoop("source", STDOUT, false, false);

    }
}
