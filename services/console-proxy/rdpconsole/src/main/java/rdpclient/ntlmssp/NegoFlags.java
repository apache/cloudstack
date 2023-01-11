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

/**
 * During NTLM authentication, each of the following flags is a possible value
 * of the NegotiateFlags field of the NEGOTIATE_MESSAGE, CHALLENGE_MESSAGE, and
 * AUTHENTICATE_MESSAGE, unless otherwise noted. These flags define client or
 * server NTLM capabilities supported by the sender.
 *
 * @see http://msdn.microsoft.com/en-us/library/cc236650.aspx
 */
public class NegoFlags {

    /**
     * 56-bit encryption. If the client sends NTLMSSP_NEGOTIATE_SEAL or
     * NTLMSSP_NEGOTIATE_SIGN with NTLMSSP_NEGOTIATE_56 to the server in the
     * NEGOTIATE_MESSAGE, the server MUST return NTLMSSP_NEGOTIATE_56 to the
     * client in the CHALLENGE_MESSAGE. Otherwise it is ignored. If both
     * NTLMSSP_NEGOTIATE_56 and NTLMSSP_NEGOTIATE_128 are requested and supported
     * by the client and server, NTLMSSP_NEGOTIATE_56 and NTLMSSP_NEGOTIATE_128
     * will both be returned to the client. Clients and servers that set
     * NTLMSSP_NEGOTIATE_SEAL SHOULD set NTLMSSP_NEGOTIATE_56 if it is supported.
     * An alternate name for this field is
     */
    public static final int NTLMSSP_NEGOTIATE_56 = 0x80000000;

    /**
     * Explicit key exchange. This capability SHOULD be used because it improves
     * security for message integrity or confidentiality. See sections 3.2.5.1.2,
     * 3.2.5.2.1, and 3.2.5.2.2 for details.
     */
    public static final int NTLMSSP_NEGOTIATE_KEY_EXCH = 0x40000000;

    /**
     * 128-bit session key negotiation. An alternate name for this field is
     * NTLMSSP_NEGOTIATE_128. If the client sends NTLMSSP_NEGOTIATE_128 to the
     * server in the NEGOTIATE_MESSAGE, the server MUST return
     * NTLMSSP_NEGOTIATE_128 to the client in the CHALLENGE_MESSAGE only if the
     * client sets NTLMSSP_NEGOTIATE_SEAL or NTLMSSP_NEGOTIATE_SIGN. Otherwise it
     * is ignored. If both NTLMSSP_NEGOTIATE_56 and NTLMSSP_NEGOTIATE_128 are
     * requested and supported by the client and server, NTLMSSP_NEGOTIATE_56 and
     * NTLMSSP_NEGOTIATE_128 will both be returned to the client. Clients and
     * servers that set NTLMSSP_NEGOTIATE_SEAL SHOULD set NTLMSSP_NEGOTIATE_128 if
     * it is supported.
     */
    public static final int NTLMSSP_NEGOTIATE_128 = 0x20000000;

    /**
     * Protocol version number. The data corresponding to this flag is provided in
     * the Version field of the NEGOTIATE_MESSAGE, the CHALLENGE_MESSAGE, and the
     * AUTHENTICATE_MESSAGE.
     */
    public static final int NTLMSSP_NEGOTIATE_VERSION = 0x02000000;

    /**
     * TargetInfo fields in the CHALLENGE_MESSAGE (section 2.2.1.2) are populated.
     */
    public static final int NTLMSSP_NEGOTIATE_TARGET_INFO = 0x00800000;

    /** LMOWF (section 3.3). */
    public static final int NTLMSSP_REQUEST_NON_NT_SESSION_KEY = 0x00400000;

    /** An identify level token. */
    public static final int NTLMSSP_NEGOTIATE_IDENTIFY = 0x00100000;

    /**
     * NTLM v2 session security. NTLM v2 session security is a misnomer because it
     * is not NTLM v2. It is NTLM v1 using the extended session security that is
     * also in NTLM v2. NTLMSSP_NEGOTIATE_LM_KEY and
     * NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY are mutually exclusive. If both
     * NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY and NTLMSSP_NEGOTIATE_LM_KEY are
     * requested, NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY alone MUST be
     * returned to the client. NTLM v2 authentication session key generation MUST
     * be supported by both the client and the DC in order to be used, and
     * extended session security signing and sealing requires support from the
     * client and the server in order to be used.
     */
    public static final int NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY = 0x00080000;

    /**
     * TargetName MUST be a server name. The data corresponding to this flag is
     * provided by the server in the TargetName field of the CHALLENGE_MESSAGE. If
     * this bit is set, then NTLMSSP_TARGET_TYPE_DOMAIN MUST NOT be set. This flag
     * MUST be ignored in the NEGOTIATE_MESSAGE and the AUTHENTICATE_MESSAGE.
     */
    public static final int NTLMSSP_TARGET_TYPE_SERVER = 0x00020000;

    /**
     * TargetName MUST be a domain name. The data corresponding to this flag is
     * provided by the server in the TargetName field of the CHALLENGE_MESSAGE. If
     * set, then NTLMSSP_TARGET_TYPE_SERVER MUST NOT be set. This flag MUST be
     * ignored in the NEGOTIATE_MESSAGE and the AUTHENTICATE_MESSAGE.
     */
    public static final int NTLMSSP_TARGET_TYPE_DOMAIN = 0x00010000;

    /**
     * Signature block on all messages. NTLMSSP_NEGOTIATE_ALWAYS_SIGN MUST be set
     * in the NEGOTIATE_MESSAGE to the server and the CHALLENGE_MESSAGE to the
     * client. NTLMSSP_NEGOTIATE_ALWAYS_SIGN is overridden by
     * NTLMSSP_NEGOTIATE_SIGN and NTLMSSP_NEGOTIATE_SEAL, if they are supported.
     */
    public static final int NTLMSSP_NEGOTIATE_ALWAYS_SIGN = 0x00008000;

    /**
     * Workstation field is present. If this flag is not set, the Workstation
     * field MUST be ignored. If this flag is set, the length field of the
     * Workstation field specifies whether the workstation name is nonempty or
     * not.
     */
    public static final int NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED = 0x00002000;

    /**
     * Domain name is provided.
     *
     * Sent by the client in the Type 1 message to indicate that the name of the
     * domain in which the client workstation has membership is included in the
     * message. This is used by the server to determine whether the client is
     * eligible for local authentication.
     */
    public static final int NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED = 0x00001000;

    /**
     * Connection SHOULD be anonymous.
     *
     * Sent by the client in the Type 3 message to indicate that an anonymous
     * context has been established. This also affects the response fields (as
     * detailed in the "Anonymous Response" section).
     */
    public static final int NTLMSSP_NEGOTIATE_ANONYMOUS = 0x00000800;

    /**
     * Usage of the NTLM v1 session security protocol. NTLMSSP_NEGOTIATE_NTLM MUST
     * be set in the NEGOTIATE_MESSAGE to the server and the CHALLENGE_MESSAGE to
     * the client.
     */
    public static final int NTLMSSP_NEGOTIATE_NTLM = 0x00000200;

    /**
     * LAN Manager (LM) session key computation. NTLMSSP_NEGOTIATE_LM_KEY and
     * NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY are mutually exclusive. If both
     * NTLMSSP_NEGOTIATE_LM_KEY and NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY are
     * requested, NTLMSSP_NEGOTIATE_EXTENDED_SESSIONSECURITY alone MUST be
     * returned to the client. NTLM v2 authentication session key generation MUST
     * be supported by both the client and the DC in order to be used, and
     * extended session security signing and sealing requires support from the
     * client and the server to be used.
     */
    public static final int NTLMSSP_NEGOTIATE_LM_KEY = 0x00000080;

    /**
     * Connectionless authentication. If NTLMSSP_NEGOTIATE_DATAGRAM is set, then
     * NTLMSSP_NEGOTIATE_KEY_EXCH MUST always be set in the AUTHENTICATE_MESSAGE
     * to the server and the CHALLENGE_MESSAGE to the client.
     */
    public static final int NTLMSSP_NEGOTIATE_DATAGRAM = 0x00000040;

    /**
     * Session key negotiation for message confidentiality. If the client sends
     * NTLMSSP_NEGOTIATE_SEAL to the server in the NEGOTIATE_MESSAGE, the server
     * MUST return NTLMSSP_NEGOTIATE_SEAL to the client in the CHALLENGE_MESSAGE.
     * Clients and servers that set NTLMSSP_NEGOTIATE_SEAL SHOULD always set
     * NTLMSSP_NEGOTIATE_56 and NTLMSSP_NEGOTIATE_128, if they are supported.
     */
    public static final int NTLMSSP_NEGOTIATE_SEAL = 0x00000020;

    /**
     * Session key negotiation for message signatures. If the client sends
     * NTLMSSP_NEGOTIATE_SIGN to the server in the NEGOTIATE_MESSAGE, the server
     * MUST return NTLMSSP_NEGOTIATE_SIGN to the client in the CHALLENGE_MESSAGE.
     */
    public static final int NTLMSSP_NEGOTIATE_SIGN = 0x00000010;

    /**
     * TargetName field of the CHALLENGE_MESSAGE (section 2.2.1.2) MUST be
     * supplied.
     */
    public static final int NTLMSSP_REQUEST_TARGET = 0x00000004;

    /**
     * OEM character set encoding.
     *
     * @see NTLMSSP_NEGOTIATE_UNICODE
     */
    public static final int NTLMSSP_NEGOTIATE_OEM = 0x00000002;

    /**
     * Unicode character set encoding.
     *
     * The NTLMSSP_NEGOTIATE_UNICODE(A) and NTLM_NEGOTIATE_OEM(B) bits are
     * evaluated together as follows:
     * <ul>
     * <li>A==1: The choice of character set encoding MUST be Unicode.
     *
     * <li>A==0 and B==1: The choice of character set encoding MUST be OEM.
     *
     * <li>A==0 and B==0: The protocol MUST return SEC_E_INVALID_TOKEN.
     * <ul>
     * */
    public static final int NTLMSSP_NEGOTIATE_UNICODE = 0x00000001;

    public int value;

    public NegoFlags(int value) {
        this.value = value;
    }

    public NegoFlags() {
        value = 0;
    }

    @Override
    public String toString() {
        return String.format("NegoFlags [value=0x%04x (%s)]", value, flagsToString());
    }

    public String flagsToString() {

        String str = "";

        if (NEGOTIATE_56())
            str += "NEGOTIATE_56 ";
        if (NEGOTIATE_KEY_EXCH())
            str += "NEGOTIATE_KEY_EXCH ";
        if (NEGOTIATE_128())
            str += "NEGOTIATE_128 ";
        if (NEGOTIATE_VERSION())
            str += "NEGOTIATE_VERSION ";
        if (NEGOTIATE_TARGET_INFO())
            str += "NEGOTIATE_TARGET_INFO ";
        if (REQUEST_NON_NT_SESSION_KEY())
            str += "REQUEST_NON_NT_SESSION_KEY ";
        if (NEGOTIATE_IDENTIFY())
            str += "NEGOTIATE_IDENTIFY ";
        if (NEGOTIATE_EXTENDED_SESSION_SECURITY())
            str += "NEGOTIATE_EXTENDED_SESSION_SECURITY ";
        if (TARGET_TYPE_SERVER())
            str += "TARGET_TYPE_SERVER ";
        if (TARGET_TYPE_DOMAIN())
            str += "TARGET_TYPE_DOMAIN ";
        if (NEGOTIATE_ALWAYS_SIGN())
            str += "NEGOTIATE_ALWAYS_SIGN ";
        if (NEGOTIATE_OEM_WORKSTATION_SUPPLIED())
            str += "NEGOTIATE_OEM_WORKSTATION_SUPPLIED ";
        if (NEGOTIATE_OEM_DOMAIN_SUPPLIED())
            str += "NEGOTIATE_OEM_DOMAIN_SUPPLIED ";
        if (NEGOTIATE_ANONYMOUS())
            str += "NEGOTIATE_ANONYMOUS ";
        if (NEGOTIATE_NTLM())
            str += "NEGOTIATE_NTLM ";
        if (NEGOTIATE_LM_KEY())
            str += "NEGOTIATE_LM_KEY ";
        if (NEGOTIATE_DATAGRAM())
            str += "NEGOTIATE_DATAGRAM ";
        if (NEGOTIATE_SEAL())
            str += "NEGOTIATE_SEAL ";
        if (NEGOTIATE_SIGN())
            str += "NEGOTIATE_SIGN ";
        if (REQUEST_TARGET())
            str += "REQUEST_TARGET ";
        if (NEGOTIATE_OEM())
            str += "NEGOTIATE_OEM ";
        if (NEGOTIATE_UNICODE())
            str += "NEGOTIATE_UNICODE ";

        return str;
    }

    public boolean NEGOTIATE_56() {
        return ((value & NTLMSSP_NEGOTIATE_56) != 0);
    }

    public boolean NEGOTIATE_KEY_EXCH() {
        return ((value & NTLMSSP_NEGOTIATE_KEY_EXCH) != 0);
    }

    public boolean NEGOTIATE_128() {
        return ((value & NTLMSSP_NEGOTIATE_128) != 0);
    }

    public boolean NEGOTIATE_VERSION() {
        return ((value & NTLMSSP_NEGOTIATE_VERSION) != 0);
    }

    public boolean NEGOTIATE_TARGET_INFO() {
        return ((value & NTLMSSP_NEGOTIATE_TARGET_INFO) != 0);
    }

    public boolean REQUEST_NON_NT_SESSION_KEY() {
        return ((value & NTLMSSP_REQUEST_NON_NT_SESSION_KEY) != 0);
    }

    public boolean NEGOTIATE_IDENTIFY() {
        return ((value & NTLMSSP_NEGOTIATE_IDENTIFY) != 0);
    }

    public boolean NEGOTIATE_EXTENDED_SESSION_SECURITY() {
        return ((value & NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY) != 0);
    }

    public boolean TARGET_TYPE_SERVER() {
        return ((value & NTLMSSP_TARGET_TYPE_SERVER) != 0);
    }

    public boolean TARGET_TYPE_DOMAIN() {
        return ((value & NTLMSSP_TARGET_TYPE_DOMAIN) != 0);
    }

    public boolean NEGOTIATE_ALWAYS_SIGN() {
        return ((value & NTLMSSP_NEGOTIATE_ALWAYS_SIGN) != 0);
    }

    public boolean NEGOTIATE_OEM_WORKSTATION_SUPPLIED() {
        return ((value & NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED) != 0);
    }

    public boolean NEGOTIATE_OEM_DOMAIN_SUPPLIED() {
        return ((value & NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED) != 0);
    }

    public boolean NEGOTIATE_ANONYMOUS() {
        return ((value & NTLMSSP_NEGOTIATE_ANONYMOUS) != 0);
    }

    public boolean NEGOTIATE_NTLM() {
        return ((value & NTLMSSP_NEGOTIATE_NTLM) != 0);
    }

    public boolean NEGOTIATE_LM_KEY() {
        return ((value & NTLMSSP_NEGOTIATE_LM_KEY) != 0);
    }

    public boolean NEGOTIATE_DATAGRAM() {
        return ((value & NTLMSSP_NEGOTIATE_DATAGRAM) != 0);
    }

    public boolean NEGOTIATE_SEAL() {
        return ((value & NTLMSSP_NEGOTIATE_SEAL) != 0);
    }

    public boolean NEGOTIATE_SIGN() {
        return ((value & NTLMSSP_NEGOTIATE_SIGN) != 0);
    }

    public boolean REQUEST_TARGET() {
        return ((value & NTLMSSP_REQUEST_TARGET) != 0);
    }

    public boolean NEGOTIATE_OEM() {
        return ((value & NTLMSSP_NEGOTIATE_OEM) != 0);
    }

    public boolean NEGOTIATE_UNICODE() {
        return ((value & NTLMSSP_NEGOTIATE_UNICODE) != 0);
    }

    public NegoFlags set_NEGOTIATE_56() {
        value |= NTLMSSP_NEGOTIATE_56;
        return this;
    }

    public NegoFlags set_NEGOTIATE_KEY_EXCH() {
        value |= NTLMSSP_NEGOTIATE_KEY_EXCH;
        return this;
    }

    public NegoFlags set_NEGOTIATE_128() {
        value |= NTLMSSP_NEGOTIATE_128;
        return this;
    }

    public NegoFlags set_NEGOTIATE_VERSION() {
        value |= NTLMSSP_NEGOTIATE_VERSION;
        return this;
    }

    public NegoFlags set_NEGOTIATE_TARGET_INFO() {
        value |= NTLMSSP_NEGOTIATE_TARGET_INFO;
        return this;
    }

    public NegoFlags set_REQUEST_NON_NT_SESSION_KEY() {
        value |= NTLMSSP_REQUEST_NON_NT_SESSION_KEY;
        return this;
    }

    public NegoFlags set_NEGOTIATE_IDENTIFY() {
        value |= NTLMSSP_NEGOTIATE_IDENTIFY;
        return this;
    }

    public NegoFlags set_NEGOTIATE_EXTENDED_SESSION_SECURITY() {
        value |= NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY;
        return this;
    }

    public NegoFlags set_TARGET_TYPE_SERVER() {
        value |= NTLMSSP_TARGET_TYPE_SERVER;
        return this;
    }

    public NegoFlags set_TARGET_TYPE_DOMAIN() {
        value |= NTLMSSP_TARGET_TYPE_DOMAIN;
        return this;
    }

    public NegoFlags set_NEGOTIATE_ALWAYS_SIGN() {
        value |= NTLMSSP_NEGOTIATE_ALWAYS_SIGN;
        return this;
    }

    public NegoFlags set_NEGOTIATE_OEM_WORKSTATION_SUPPLIED() {
        value |= NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED;
        return this;
    }

    public NegoFlags set_NEGOTIATE_OEM_DOMAIN_SUPPLIED() {
        value |= NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED;
        return this;
    }

    public NegoFlags set_NEGOTIATE_ANONYMOUS() {
        value |= NTLMSSP_NEGOTIATE_ANONYMOUS;
        return this;
    }

    public NegoFlags set_NEGOTIATE_NTLM() {
        value |= NTLMSSP_NEGOTIATE_NTLM;
        return this;
    }

    public NegoFlags set_NEGOTIATE_LM_KEY() {
        value |= NTLMSSP_NEGOTIATE_LM_KEY;
        return this;
    }

    public NegoFlags set_NEGOTIATE_DATAGRAM() {
        value |= NTLMSSP_NEGOTIATE_DATAGRAM;
        return this;
    }

    public NegoFlags set_NEGOTIATE_SEAL() {
        value |= NTLMSSP_NEGOTIATE_SEAL;
        return this;
    }

    public NegoFlags set_NEGOTIATE_SIGN() {
        value |= NTLMSSP_NEGOTIATE_SIGN;
        return this;
    }

    public NegoFlags set_REQUEST_TARGET() {
        value |= NTLMSSP_REQUEST_TARGET;
        return this;
    }

    public NegoFlags set_NEGOTIATE_OEM() {
        value |= NTLMSSP_NEGOTIATE_OEM;
        return this;
    }

    public NegoFlags set_NEGOTIATE_UNICODE() {
        value |= NTLMSSP_NEGOTIATE_UNICODE;
        return this;
    }

    /**
     * Example.
     */

    public static void main(String args[]) {

        NegoFlags flags = new NegoFlags(0xe20882b7);
        System.out.println("Negotiation flags: " + flags);

    }

}
