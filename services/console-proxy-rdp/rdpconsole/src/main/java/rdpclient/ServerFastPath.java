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

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240621.aspx
 */
public class ServerFastPath extends BaseElement {

    /**
     * TPKT protocol version (first byte).
     */
    public static final int PROTOCOL_TPKT = 3;

    /**
     * Fast path protocol version (first two bits of first byte).
     */
    public static final int PROTOCOL_FASTPATH = 0;

    /**
     * TPKT packets will be pushed to that pad.
     */
    public static final String TPKT_PAD = "tpkt";

    private static final String ORDERS_PAD = "orders";
    private static final String BITMAP_PAD = "bitmap";
    private static final String PALETTE_PAD = "palette";

    /**
     * Indicates that packet contains 8 byte secure checksum at top of packet. Top
     * two bits of first byte.
     */
    public static final int FASTPATH_OUTPUT_SECURE_CHECKSUM = 1;

    /**
     * Indicates that packet contains 8 byte secure checksum at top of packet and
     * packet content is encrypted. Top two bits of first byte.
     */
    public static final int FASTPATH_OUTPUT_ENCRYPTED = 2;

    public static final int FASTPATH_UPDATETYPE_ORDERS = 0;
    public static final int FASTPATH_UPDATETYPE_BITMAP = 1;
    public static final int FASTPATH_UPDATETYPE_PALETTE = 2;
    public static final int FASTPATH_UPDATETYPE_SYNCHRONIZE = 3;
    public static final int FASTPATH_UPDATETYPE_SURFCMDS = 4;
    public static final int FASTPATH_UPDATETYPE_PTR_NULL = 5;
    public static final int FASTPATH_UPDATETYPE_PTR_DEFAULT = 6;
    public static final int FASTPATH_UPDATETYPE_PTR_POSITION = 8;
    public static final int FASTPATH_UPDATETYPE_COLOR = 9;
    public static final int FASTPATH_UPDATETYPE_CACHED = 0xa;
    public static final int FASTPATH_UPDATETYPE_POINTER = 0xb;

    public static final int FASTPATH_FRAGMENT_SINGLE = 0;
    public static final int FASTPATH_FRAGMENT_LAST = 1;
    public static final int FASTPATH_FRAGMENT_FIRST = 2;
    public static final int FASTPATH_FRAGMENT_NEXT = 3;

    public static final int FASTPATH_OUTPUT_COMPRESSION_USED = 2;

    public ServerFastPath(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        //* DEBUG */System.out.println(buf.toHexString(buf.length));

        // We need at 4 bytes to read packet type (TPKT or FastPath) and packet
        // length
        if (!cap(buf, 4, UNLIMITED, link, false))
            return;

        int typeAndFlags = buf.readUnsignedByte();

        if (typeAndFlags == PROTOCOL_TPKT) {
            //
            // TPKT
            //

            // Reserved
            buf.skipBytes(1);

            // Read TPKT length
            int length = buf.readUnsignedShort();

            if (!cap(buf, length, length, link, false))
                // Wait for full packet to arrive
                return;

            pushDataToPad(TPKT_PAD, buf);

            // TPKT is handled
            return;
        }

        //
        // FastPath
        //
        // Number of bytes in updateData field (including header (1+1 or 2
        // bytes))
        int length = buf.readVariableUnsignedShort();

        // Length is the size of payload, so we need to calculate from cursor
        if (!cap(buf, length, length, link, false))
            // Wait for full packet to arrive
            return;

        int type = typeAndFlags & 0x3;
        int securityFlags = (typeAndFlags >> 6) & 0x3;

        // Assertions
        {
            if (type != PROTOCOL_FASTPATH)
                throw new RuntimeException("Unknown protocol. Expected protocol: 0 (FastPath). Actual protocol: " + type + ", data: " + buf + ".");

            switch (securityFlags) {
                case FASTPATH_OUTPUT_SECURE_CHECKSUM:
                    // TODO
                    throw new RuntimeException("Secure checksum is not supported in FastPath packets.");
                case FASTPATH_OUTPUT_ENCRYPTED:
                    // TODO
                    throw new RuntimeException("Encryption is not supported in FastPath packets.");
            }
        }

        // TODO: optional FIPS information, when FIPS is selected
        // TODO: optional data signature (checksum), when checksum or FIPS is
        // selected

        // Array of FastPath update fields
        while (buf.cursor < buf.length) {

            int updateHeader = buf.readUnsignedByte();

            int size = buf.readUnsignedShortLE();

            int updateCode = updateHeader & 0xf;
            int fragmentation = (updateHeader >> 4) & 0x3;
            int compression = (updateHeader >> 6) & 0x3;

            if (verbose)
                System.out.println("[" + this + "] INFO: FastPath update received. UpdateCode: " + updateCode + ", fragmentation: " + fragmentation + ", compression: " +
                    compression + ", size: " + size + ".");

            ByteBuffer data = buf.readBytes(size);
            buf.putMetadata("fragmentation", fragmentation);
            buf.putMetadata("compression", compression);

            switch (updateCode) {

                case FASTPATH_UPDATETYPE_ORDERS:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_ORDERS.");
                    pushDataToPad(ORDERS_PAD, data);
                    break;

                case FASTPATH_UPDATETYPE_BITMAP:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_BITMAP.");
                    pushDataToPad(BITMAP_PAD, data);
                    break;

                case FASTPATH_UPDATETYPE_PALETTE:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_PALETTE.");
                    pushDataToPad(PALETTE_PAD, data);
                    break;

                case FASTPATH_UPDATETYPE_SYNCHRONIZE:
                    // @see http://msdn.microsoft.com/en-us/library/cc240625.aspx
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_SYNCHRONIZE.");

                    data.unref();

                    if (size != 0)
                        throw new RuntimeException("Size of FastPath synchronize packet must be 0. UpdateCode: " + updateCode + ", fragmentation: " + fragmentation +
                            ", compression: " + compression + ", size: " + size + ", data: " + data + ".");
                    break;

                case FASTPATH_UPDATETYPE_SURFCMDS:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_SURFCMDS.");

                    break;

                case FASTPATH_UPDATETYPE_PTR_NULL:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_PTR_NULL.");

                    break;

                case FASTPATH_UPDATETYPE_PTR_DEFAULT:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_PTR_DEFAULT.");

                    break;

                case FASTPATH_UPDATETYPE_PTR_POSITION:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_PTR_POSITION.");

                    break;

                case FASTPATH_UPDATETYPE_COLOR:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_COLOR.");

                    break;

                case FASTPATH_UPDATETYPE_CACHED:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_CACHED.");

                    break;

                case FASTPATH_UPDATETYPE_POINTER:
                    if (verbose)
                        System.out.println("[" + this + "] INFO: FASTPATH_UPDATETYPE_POINTER.");

                    break;

                default:
                    throw new RuntimeException("Unknown FastPath update. UpdateCode: " + updateCode + ", fragmentation: " + fragmentation + ", compression: " +
                        compression + ", size: " + size + ", data: " + data + ".");

            }

        }

        buf.unref();
    }

}
