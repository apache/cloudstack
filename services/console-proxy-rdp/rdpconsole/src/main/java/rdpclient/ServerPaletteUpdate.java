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

import java.awt.image.IndexColorModel;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.ScreenDescription;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240623.aspx
 */
public class ServerPaletteUpdate extends BaseElement {

    public static final int UPDATETYPE_PALETTE = 0x0002;
    protected ScreenDescription screen;

    public ServerPaletteUpdate(String id, ScreenDescription screen) {
        super(id);
        this.screen = screen;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // (2 bytes): A 16-bit, unsigned integer. The update type. This field MUST
        // be set to UPDATETYPE_PALETTE (0x0002).
        int updateType = buf.readUnsignedShortLE();
        if (updateType != UPDATETYPE_PALETTE)
            throw new RuntimeException("Unexpected update type. Expected type: UPDATETYPE_PALETTE (0x0002), actual value: " + updateType + ", data: " + buf + ".");

        // pad2Octets (2 bytes): A 16-bit, unsigned integer. Padding. Values in this
        // field MUST be ignored.
        buf.skipBytes(2);

        // (4 bytes): A 32-bit, unsigned integer. The number of RGB triplets in the
        // paletteData field. This field MUST be set to 256 (the number of entries
        // in an 8 bpp palette).
        int numberColors = (int)buf.readUnsignedIntLE();
        if (numberColors != 256)
            throw new RuntimeException("Unexpected value for number of color field in server Palette Update packet. Expected value: 256 colors, actual value: " +
                numberColors + ", data: " + buf + ".");

        // (variable): An array of palette entries in RGB triplet format packed on
        // byte boundaries. The number of triplet entries is given by the
        // numberColors field.
        ByteBuffer paletteEntries = buf.readBytes(numberColors * 3);

        // In the case of a Palette Update, the client MUST update the global
        // palette on all drawing surfaces
        screen.colorMap = new IndexColorModel(8, numberColors, paletteEntries.data, paletteEntries.offset, false);

        /* DEBUG */buf.assertThatBufferIsFullyRead();

        buf.unref();
    }

}
