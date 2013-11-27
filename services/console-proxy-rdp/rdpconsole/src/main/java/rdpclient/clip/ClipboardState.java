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
package rdpclient.clip;

import java.util.HashMap;
import java.util.Map;

public class ClipboardState {

    /**
     * The Long Format Name variant of the Format List PDU is supported for
     * exchanging updated format names. If this flag is not set, the Short Format
     * Name variant MUST be used. If this flag is set by both protocol endpoints,
     * then the Long Format Name variant MUST be used.
     */
    public boolean serverUseLongFormatNames = false;
    public final boolean clientUseLongFormatNames = false;

    /**
     * File copy and paste using stream-based operations are supported using the
     * File Contents Request PDU and File Contents Response PDU.
     */
    public boolean serverStreamFileClipEnabled = false;
    public final boolean clientStreamFileClipEnabled = false;

    /**
     * Indicates that any description of files to copy and paste MUST NOT include
     * the source path of the files.
     */
    public boolean serverFileClipNoFilePaths = false;
    public final boolean clientFileClipNoFilePaths = false;

    /**
     * Locking and unlocking of File Stream data on the clipboard is supported
     * using the Lock Clipboard Data PDU and Unlock Clipboard Data PDU.
     */
    public boolean serverCanLockClipdata = false;
    public final boolean clientCanLockClipdata = false;

    /**
     * The Monitor Ready PDU is sent from the server to the client to indicate
     * that the server is initialized and ready.
     */
    public boolean serverReady = false;

    /**
     * Set of data formats, which are supported by server for paste operation.
     */
    public Map<Object, ClipboardDataFormat> serverClipboardDataFormats = new HashMap<Object, ClipboardDataFormat>(0);

    /**
     * Server sends clipboard data in requested format.
     */
    public ClipboardDataFormat serverRequestedFormat;

}
