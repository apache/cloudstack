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

import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.MouseOrder;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240594.aspx
 */
public class AwtRdpMouseAdapter extends BaseElement {
    public static int FASTPATH_INPUT_EVENT_MOUSE = 0x01;

    /**
     * Event is a mouse wheel rotation. The only valid flags in a wheel rotation
     * event are PTRFLAGS_WHEEL_NEGATIVE and the WheelRotationMask; all other
     * pointer flags are ignored.
     */
    public static int PTRFLAGS_WHEEL = 0x0200;

    /**
     * Wheel rotation value (contained in the WheelRotationMask bit field) is
     * negative and MUST be sign-extended before injection at the server.
     */
    public static int PTRFLAGS_WHEEL_NEGATIVE = 0x0100;

    /**
     * Bit field describing the number of rotation units the mouse wheel was
     * rotated. The value is negative if the PTRFLAGS_WHEEL_NEGATIVE flag is set.
     */
    public static int WHEEL_ROTATION_MASK = 0x01FF;

    /**
     * Indicates that the mouse position MUST be updated to the location specified
     * by the xPos and yPos fields.
     */
    public static int PTRFLAGS_MOVE = 0x0800;

    /**
     * Indicates that a click event has occurred at the position specified by the
     * xPos and yPos fields. The button flags indicate which button has been
     * clicked and at least one of these flags MUST be set.
     */
    public static int PTRFLAGS_DOWN = 0x8000;

    /**
     * Mouse button 1 (left button) was clicked or released. If the PTRFLAGS_DOWN
     * flag is set, then the button was clicked, otherwise it was released.
     */
    public static int PTRFLAGS_BUTTON1 = 0x1000;

    /**
     * Mouse button 2 (right button) was clicked or released. If the PTRFLAGS_DOWN
     * flag is set, then the button was clicked, otherwise it was released.
     */
    public static int PTRFLAGS_BUTTON2 = 0x2000;

    /**
     * Mouse button 3 (middle button or wheel) was clicked or released. If the
     * PTRFLAGS_DOWN flag is set, then the button was clicked, otherwise it was
     * released.
     */
    public static int PTRFLAGS_BUTTON3 = 0x4000;

    public AwtRdpMouseAdapter(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Get mouse event
        MouseOrder order = (MouseOrder)buf.getOrder();

        ByteBuffer outBuf = new ByteBuffer(7, true);

        // eventHeader (1 byte): An 8-bit, unsigned integer. EventCode bitfield (top
        // 3 bits) MUST be set to FASTPATH_INPUT_EVENT_MOUSE (1). The
        // eventFlags bitfield (low 5 bits) MUST be zeroed out.
        outBuf.writeByte(FASTPATH_INPUT_EVENT_MOUSE << 5);

        // pointerFlags (2 bytes): A 16-bit, unsigned integer.
        outBuf.writeShortLE(getPointerFlags(order));

        // xPos (2 bytes): A 16-bit, unsigned integer. The x-coordinate of the
        // pointer.
        outBuf.writeShortLE(order.event.getX());

        // yPos (2 bytes): A 16-bit, unsigned integer. The y-coordinate of the
        // pointer.
        outBuf.writeShortLE(order.event.getY());

        // Push buffer to one pad only, so it can be modified without copying of
        // data
        pushDataToPad(STDOUT, outBuf);
    }

    // Remember mouse buttons
    protected boolean button1, button2, button3;

    protected int getPointerFlags(MouseOrder order) {
        int flags = 0;

        int modifiers = order.event.getModifiersEx();

        if (order.pressed) {
            // Mouse pressed
            flags |= PTRFLAGS_DOWN;

            // Check, which one of buttons is released
            boolean b1 = ((modifiers & InputEvent.BUTTON1_DOWN_MASK) > 0) && !button1;
            boolean b2 = ((modifiers & InputEvent.BUTTON2_DOWN_MASK) > 0) && !button2;
            boolean b3 = ((modifiers & InputEvent.BUTTON3_DOWN_MASK) > 0) && !button3;

            if (b1) {
                flags |= PTRFLAGS_BUTTON1;
                button1 = true;
            }

            if (b2) {
                flags |= PTRFLAGS_BUTTON3;
                button2 = true;
            }

            if (b3) {
                flags |= PTRFLAGS_BUTTON2;
                button3 = true;
            }
        } else if (order.released) {
            // Mouse released

            // Check, which one of buttons is released
            boolean b1 = !((modifiers & InputEvent.BUTTON1_DOWN_MASK) > 0) && button1;
            boolean b2 = !((modifiers & InputEvent.BUTTON2_DOWN_MASK) > 0) && button2;
            boolean b3 = !((modifiers & InputEvent.BUTTON3_DOWN_MASK) > 0) && button3;

            if (b1) {
                flags |= PTRFLAGS_BUTTON1;
                button1 = false;
            }

            if (b2) {
                flags |= PTRFLAGS_BUTTON3;
                button2 = false;
            }

            if (b3) {
                flags |= PTRFLAGS_BUTTON2;
                button3 = false;
            }
        } else {
            // Mouse moved
            flags |= PTRFLAGS_MOVE;
        }

        return flags;
    }

}
