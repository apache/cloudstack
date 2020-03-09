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
package rdpclient.ntlmssp;

import rdpclient.ntlmssp.asn1.TSCredentials;
import rdpclient.ntlmssp.asn1.TSPasswordCreds;
import rdpclient.ntlmssp.asn1.TSRequest;
import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;

public class ClientNtlmsspUserCredentials extends OneTimeSwitch implements Element {

    protected NtlmState ntlmState;

    public ClientNtlmsspUserCredentials(String id, NtlmState ntlmState) {
        super(id);
        this.ntlmState = ntlmState;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        throw new RuntimeException("Unexpected packet: " + buf + ".");
    }

    @Override
    protected void onStart() {
        super.onStart();

        ByteBuffer buf = new ByteBuffer(4096, true);

        TSRequest tsRequest = new TSRequest("TSRequest");
        tsRequest.version.value = 2L;

        ByteBuffer tsCredentialsBuf = generateTSCredentials();
        tsRequest.authInfo.value = encryptTSCredentials(tsCredentialsBuf);
        tsCredentialsBuf.unref();

        tsRequest.writeTag(buf);

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
    }

    private ByteBuffer encryptTSCredentials(ByteBuffer buf2) {
        return new ByteBuffer(ntlmState.ntlm_EncryptMessage(buf2.toByteArray()));
    }

    private ByteBuffer generateTSCredentials() {
        ByteBuffer buf = new ByteBuffer(4096);

        TSCredentials tsCredentials = new TSCredentials("authInfo");
        // 1 means that credentials field contains a TSPasswordCreds structure
        tsCredentials.credType.value = 1L;

        ByteBuffer tsPasswordCredsBuf = new ByteBuffer(4096, true);
        TSPasswordCreds tsPasswordCreds = new TSPasswordCreds("credentials");
        tsPasswordCreds.domainName.value = new ByteBuffer(ntlmState.domain.getBytes(RdpConstants.CHARSET_16));
        tsPasswordCreds.userName.value = new ByteBuffer(ntlmState.user.getBytes(RdpConstants.CHARSET_16));
        tsPasswordCreds.password.value = new ByteBuffer(ntlmState.password.getBytes(RdpConstants.CHARSET_16));
        tsPasswordCreds.writeTag(tsPasswordCredsBuf);
        tsPasswordCredsBuf.trimAtCursor();
        //* DEBUG */System.out.println("TSPasswordCreds:\n" + tsPasswordCredsBuf.dump());

        tsCredentials.credentials.value = tsPasswordCredsBuf;

        tsCredentials.writeTag(buf);
        tsPasswordCredsBuf.unref();

        // Trim buffer to actual length of data written
        buf.trimAtCursor();
        //* DEBUG */System.out.println("TSCredentials:\n" + buf.dump());

        return buf;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        /* @formatter:off */
        // TSCredentials
//  30 57 // Sequence
//  a0 03 // TAG 0
//  02 01 01 // Integer: 1 : credentials contains a TSPasswordCreds structure
//  a1 50 // TAG 1
//  04 4e // OCTETSTRING
        // TSPasswordCreds
//  30 4c // SEQUENCE
//  a0 14 // TAG 0
//  04 12 // OCTETSTRING
//  77 00 6f 00 72 00 6b 00 67 00 72 00 6f 00 75 00 70 00 // "workgroup"
//  a1 1c // TAG 1
//  04 1a // OCTETSTRING
//  41 00 64 00 6d 00 69 00 6e 00 69 00 73 00 74 00 72 00 61 00 74 00 6f 00 72 00 // "Administrator"
//  a2 16 // TAG 2
//  04 14 //
//  52 00 32 00 50 00 72 00 65 00 76 00 69 00 65 00 77 00 21 00 // "R2Preview!"
        /* @formatter:on */

    }

}
