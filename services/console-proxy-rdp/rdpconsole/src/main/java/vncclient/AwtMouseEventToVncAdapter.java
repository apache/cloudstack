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
package vncclient;

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.MouseOrder;

public class AwtMouseEventToVncAdapter extends BaseElement {

    public AwtMouseEventToVncAdapter(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Get mouse event
        MouseOrder order = (MouseOrder)buf.getOrder();

        ByteBuffer outBuf = new ByteBuffer(6);

        outBuf.writeByte(RfbConstants.CLIENT_POINTER_EVENT);

        int buttonMask = mapAwtModifiersToVncButtonMask(order.event.getModifiersEx());
        outBuf.writeByte(buttonMask);
        outBuf.writeShort(order.event.getX());
        outBuf.writeShort(order.event.getY());

        pushDataToAllOuts(outBuf);
    }

    /**
     * Current state of buttons 1 to 8 are represented by bits 0 to 7 of
     * button-mask respectively, 0 meaning up, 1 meaning down (pressed). On a
     * conventional mouse, buttons 1, 2 and 3 correspond to the left, middle and
     * right buttons on the mouse. On a wheel mouse, each step of the wheel
     * upwards is represented by a press and release of button 4, and each step
     * downwards is represented by a press and release of button 5.
     *
     * @param modifiers
     *          extended modifiers from AWT mouse event
     * @return VNC mouse button mask
     */
    public static int mapAwtModifiersToVncButtonMask(int modifiers) {
        int mask =
            (((modifiers & InputEvent.BUTTON1_DOWN_MASK) != 0) ? 0x1 : 0) | (((modifiers & InputEvent.BUTTON2_DOWN_MASK) != 0) ? 0x2 : 0) |
                (((modifiers & InputEvent.BUTTON3_DOWN_MASK) != 0) ? 0x4 : 0);
        return mask;
    }

}
