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

public interface NtlmConstants {

    /**
     * Attribute type: Indicates that this is the last AV_PAIR in the list. AvLen
     * MUST be 0. This type of information MUST be present in the AV pair list.
     */
    public final static int MSV_AV_EOL = 0x0000;

    /**
     * Attribute type: The server's NetBIOS computer name. The name MUST be in
     * Unicode, and is not null-terminated. This type of information MUST be
     * present in the AV_pair list.
     */
    public final static int MSV_AV_NETBIOS_COMPUTER_NAME = 0x0001;

    /**
     * Attribute type: The server's NetBIOS domain name. The name MUST be in
     * Unicode, and is not null-terminated. This type of information MUST be
     * present in the AV_pair list.
     */
    public final static int MSV_AV_NETBIOS_DOMAIN_NAME = 0x0002;

    /**
     * Attribute type: The fully qualified domain name (FQDN (1)) of the computer.
     * The name MUST be in Unicode, and is not null-terminated.
     */
    public final static int MSV_AV_DNS_COMPUTER_NAME = 0x0003;

    /**
     * Attribute type: The FQDN of the domain. The name MUST be in Unicode, and is
     * not null-terminated.
     */
    public final static int MSV_AV_DNS_DOMAIN_NAME = 0x0004;

    /**
     * Attribute type: The FQDN of the forest. The name MUST be in Unicode, and is
     * not null-terminated.
     */
    public final static int MSV_AV_DNS_TREE_NAME = 0x0005;

    /**
     * Attribute type: A 32-bit value indicating server or client configuration.
     *
     * <li>0x00000001: indicates to the client that the account authentication is
     * constrained.
     *
     * <li>0x00000002: indicates that the client is providing message integrity in
     * the MIC field (section 2.2.1.3) in the AUTHENTICATE_MESSAGE.
     *
     * <li>0x00000004: indicates that the client is providing a target SPN
     * generated from an untrusted source.
     **/
    public final static int MSV_AV_FLAGS = 0x0006;

    public static final int MSV_AV_FLAGS_MESSAGE_INTEGRITY_CHECK = 0x00000002;

    /**
     * Attribute type: A FILETIME structure ([MS-DTYP] section 2.3.3) in
     * little-endian byte order that contains the server local time.
     */
    public final static int MSV_AV_TIMESTAMP = 0x0007;

    /**
     * Attribute type: A Single_Host_Data (section 2.2.2.2) structure. The Value
     * field contains a platform-specific blob, as well as a MachineID created at
     * computer startup to identify the calling machine.<15>
     */
    public final static int MSV_AV_SINGLE_HOST = 0x0008;

    /**
     * Attribute type: The SPN of the target server. The name MUST be in Unicode
     * and is not null-terminated.<16>
     */
    public final static int MSV_AV_TARGET_NAME = 0x0009;

    /**
     * Attribute type: A channel bindings hash. The Value field contains an MD5
     * hash ([RFC4121] section 4.1.1.2) of a gss_channel_bindings_struct
     * ([RFC2744] section 3.11). An all-zero value of the hash is used to indicate
     * absence of channel bindings.
     */
    public final static int MSV_AV_CHANNEL_BINDINGS = 0x000A;

    /**
     * Signature of NTLMSSP blob.
     */
    public static final String NTLMSSP = "NTLMSSP";

    public static final String GSS_RDP_SERVICE_NAME = "TERMSRV";

    /**
     * NTLM message type: NEGOTIATE.
     */
    public static final int NEGOTIATE = 0x00000001;

    /**
     * NTLM message type: CHALLENGE.
     */
    public static final int CHALLENGE = 0x00000002;

    /**
     * NTLM message type: NTLMSSP_AUTH.
     */
    public static final int NTLMSSP_AUTH = 0x00000003;

    public static final String OID_SPNEGO = "1.3.6.1.5.5.2";

    public static final String OID_KERBEROS5 = "1.2.840.113554.1.2.2";
    public static final String OID_MSKERBEROS5 = "1.2.840.48018.1.2.2";

    public static final String OID_KRB5USERTOUSER = "1.2.840.113554.1.2.2.3";

    public static final String OID_NTLMSSP = "1.3.6.1.4.1.311.2.2.10";

    /**
     * Magic constant used in calculation of Lan Manager response.
     */
    public static final String LM_MAGIC = "KGS!@#$%";

    /**
     * Magic constant used in generation of client signing key.
     */
    public static final String CLIENT_SIGN_MAGIC = "session key to client-to-server signing key magic constant";

    /**
     * Magic constant used in generation of client sealing key.
     */
    public static final String CLIENT_SEAL_MAGIC = "session key to client-to-server sealing key magic constant";

    public static final String SERVER_SIGN_MAGIC = "session key to server-to-client signing key magic constant";
    public static final String SERVER_SEAL_MAGIC = "session key to server-to-client sealing key magic constant";

    /**
     * In Windows XP, Windows Server 2003, Windows Vista, Windows Server 2008,
     * Windows 7, Windows Server 2008 R2, Windows 8, Windows Server 2012, Windows
     * 8.1, and Windows Server 2012 R2, the maximum lifetime of challenge is 36 hours.
     */
    public static final int CHALLENGE_MAX_LIFETIME = 36 * 60 * 60;
}
