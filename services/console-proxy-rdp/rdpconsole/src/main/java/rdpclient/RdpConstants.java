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

import java.nio.charset.Charset;

public interface RdpConstants {

    /**
     * Default charset to use when communicating with server using 8 bit strings.
     */
    public static final Charset CHARSET_8 = Charset.availableCharsets().get("US-ASCII");

    /**
     * Default charset to use when communicating with server using 16 bit strings.
     */
    public static final Charset CHARSET_16 = Charset.availableCharsets().get("UTF-16LE");

    /**
     * Negotiate SSL protocol to use to protect RDP connection.
     * @see http://msdn.microsoft.com/en-us/library/cc240500.aspx
     */
    public static final int RDP_NEG_REQ_PROTOCOL_SSL = 1;

    /**
     * Negotiate CredSSP protocol to use to protect RDP connection.
     * @see http://msdn.microsoft.com/en-us/library/cc240500.aspx
     * When used, client must set @see RDP_NEG_REQ_PROTOCOL_SSL too.
     */
    public static final int RDP_NEG_REQ_PROTOCOL_HYBRID = 2;

    /**
     * RDP negotiation: flags (not used, always 0).
     */
    public static final int RDP_NEG_REQ_FLAGS = 0;

    /**
     * RDP Negotiation: request.
     */
    public static final int RDP_NEG_REQ_TYPE_NEG_REQ = 1;

    /**
     * RDP Negotiation: response.
     */
    public static final int RDP_NEG_REQ_TYPE_NEG_RSP = 2;

    /**
     * RDP Negotiation: failure.
     */
    public static final int RDP_NEG_REQ_TYPE_NEG_FAILURE = 3;

    public static final int CHANNEL_IO = 1003;

    public static final int CHANNEL_RDPRDR = 1004;

}
