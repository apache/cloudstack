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
package com.cloud.consoleproxy.vnc;

import java.nio.charset.Charset;

public interface RfbConstants {

    public static final String RFB_PROTOCOL_VERSION_MAJOR = "RFB 003.";
    // public static final String VNC_PROTOCOL_VERSION_MINOR = "003";
    public static final String VNC_PROTOCOL_VERSION_MINOR = "003";
    public static final String RFB_PROTOCOL_VERSION = RFB_PROTOCOL_VERSION_MAJOR + VNC_PROTOCOL_VERSION_MINOR;

    /**
     * Server message types.
     */
    final static int SERVER_FRAMEBUFFER_UPDATE = 0, SERVER_SET_COLOURMAP_ENTRIES = 1, SERVER_BELL = 2, SERVER_CUT_TEXT = 3;

    /**
     * Client message types.
     */
    public static final int CLIENT_SET_PIXEL_FORMAT = 0, CLIENT_FIX_COLOURMAP_ENTRIES = 1, CLIENT_SET_ENCODINGS = 2, CLIENT_FRAMEBUFFER_UPDATE_REQUEST = 3,
            CLIENT_KEYBOARD_EVENT = 4, CLIENT_POINTER_EVENT = 5, CLIENT_CUT_TEXT = 6;

    /**
     * Server authorization type
     */
    public final static int CONNECTION_FAILED = 0, NO_AUTH = 1, VNC_AUTH = 2;

    /**
     * Server authorization reply.
     */
    public final static int VNC_AUTH_OK = 0, VNC_AUTH_FAILED = 1, VNC_AUTH_TOO_MANY = 2;

    /**
     * Encodings.
     */
    public final static int ENCODING_RAW = 0, ENCODING_COPY_RECT = 1, ENCODING_RRE = 2, ENCODING_CO_RRE = 4, ENCODING_HEXTILE = 5, ENCODING_ZRLE = 16;

    /**
     * Pseudo-encodings.
     */
    public final static int ENCODING_CURSOR = -239 /* 0xFFFFFF11 */, ENCODING_DESKTOP_SIZE = -223 /* 0xFFFFFF21 */;

    /**
     * Encodings, which we support.
     */
    public final static int[] SUPPORTED_ENCODINGS_ARRAY = {ENCODING_RAW, ENCODING_COPY_RECT, ENCODING_DESKTOP_SIZE};

    /**
     * Frame buffer update request type: update of whole screen or partial
     * update.
     */
    public static final int FRAMEBUFFER_FULL_UPDATE_REQUEST = 0, FRAMEBUFFER_INCREMENTAL_UPDATE_REQUEST = 1;

    public static final int KEY_UP = 0, KEY_DOWN = 1;

    public static final int LITTLE_ENDIAN = 0, BIG_ENDIAN = 1;

    public static final int EXCLUSIVE_ACCESS = 0, SHARED_ACCESS = 1;

    public static final int PALETTE = 0, TRUE_COLOR = 1;

    /**
     * Default charset to use when communicating with server.
     */
    public static final Charset CHARSET = Charset.availableCharsets().get("US-ASCII");
}
