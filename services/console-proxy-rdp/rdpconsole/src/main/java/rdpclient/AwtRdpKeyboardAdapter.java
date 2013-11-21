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

import java.awt.event.KeyEvent;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.KeyOrder;

public class AwtRdpKeyboardAdapter extends BaseElement {

    /**
     * Absence of this flag indicates a key-down event, while its presence
     * indicates a key-release event.
     */
    public static final int FASTPATH_INPUT_KBDFLAGS_RELEASE = 0x01;

    /**
     * Keystroke message contains an extended scancode. For enhanced 101-key and
     * 102-key keyboards, extended keys include the right ALT and right CTRL keys
     * on the main section of the keyboard; the INS, DEL, HOME, END, PAGE UP, PAGE
     * DOWN and ARROW keys in the clusters to the left of the numeric keypad; and
     * the Divide ("/") and ENTER keys in the numeric keypad.
     */
    public static final int FASTPATH_INPUT_KBDFLAGS_EXTENDED = 0x02;

    public static final int FASTPATH_INPUT_EVENT_SCANCODE = 0;

    public AwtRdpKeyboardAdapter(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        KeyOrder order = (KeyOrder)buf.getOrder();
        buf.unref();

        ByteBuffer outBuf = new ByteBuffer(2, true);

        int scanCode = map_en_us(order.event);

        // eventHeader (1 byte): An 8-bit, unsigned integer. The format of this
        // field is the same as the eventHeader byte field described in section
        // 2.2.8.1.2.2. The eventCode bitfield (3 bits in size) MUST be set to
        // FASTPATH_INPUT_EVENT_SCANCODE (0). The eventFlags bitfield (5 bits in
        // size) contains flags describing the keyboard event.
        outBuf.writeByte((scanCode >> 8) | (FASTPATH_INPUT_EVENT_SCANCODE << 5) | ((order.pressed) ? 0 : FASTPATH_INPUT_KBDFLAGS_RELEASE));

        // keyCode (1 byte): An 8-bit, unsigned integer. The scancode of the key
        // which triggered the event.
        outBuf.writeByte(scanCode);

        // Push buffer to one pad only, so it can be modified without copying of
        // data
        pushDataToPad(STDOUT, outBuf);
    }

    /**
     * Return key scan code (in lower byte) and extended flags (in second byte).
     */
    private int map_en_us(KeyEvent event) {
        // Also set extended key flag when necessary.
        // For enhanced 101-key and 102-key keyboards, extended keys include the
        // right ALT and right CTRL keys on the main section of the keyboard; the
        // INS, DEL, HOME, END, PAGE UP, PAGE DOWN and ARROW keys in the clusters to
        // the left of the numeric keypad; and the Divide ("/") and ENTER keys in
        // the numeric keypad.

        switch (event.getKeyCode()) {
        // Functional keys
            case KeyEvent.VK_ESCAPE:
                return 1;
            case KeyEvent.VK_F1:
                return 59;
            case KeyEvent.VK_F2:
                return 60;
            case KeyEvent.VK_F3:
                return 61;
            case KeyEvent.VK_F4:
                return 62;
            case KeyEvent.VK_F5:
                return 63;
            case KeyEvent.VK_F6:
                return 64;
            case KeyEvent.VK_F7:
                return 65;
            case KeyEvent.VK_F8:
                return 66;
            case KeyEvent.VK_F9:
                return 67;
            case KeyEvent.VK_F10:
                return 68;
            case KeyEvent.VK_F11:
                return 87;
            case KeyEvent.VK_F12:
                return 88;

                // Row #1
            case KeyEvent.VK_BACK_QUOTE:
                return 41;
            case KeyEvent.VK_1:
                return 2;
            case KeyEvent.VK_2:
                return 3;
            case KeyEvent.VK_3:
                return 4;
            case KeyEvent.VK_4:
                return 5;
            case KeyEvent.VK_5:
                return 6;
            case KeyEvent.VK_6:
                return 7;
            case KeyEvent.VK_7:
                return 8;
            case KeyEvent.VK_8:
                return 9;
            case KeyEvent.VK_9:
                return 10;
            case KeyEvent.VK_0:
                return 11;
            case KeyEvent.VK_MINUS:
                return 12;
            case KeyEvent.VK_EQUALS:
                return 13;
            case KeyEvent.VK_BACK_SPACE:
                return 14;

                // Row #2
            case KeyEvent.VK_TAB:
                return 15;
            case KeyEvent.VK_Q:
                return 16;
            case KeyEvent.VK_W:
                return 17;
            case KeyEvent.VK_E:
                return 18;
            case KeyEvent.VK_R:
                return 19;
            case KeyEvent.VK_T:
                return 20;
            case KeyEvent.VK_Y:
                return 21;
            case KeyEvent.VK_U:
                return 22;
            case KeyEvent.VK_I:
                return 23;
            case KeyEvent.VK_O:
                return 24;
            case KeyEvent.VK_P:
                return 25;
            case KeyEvent.VK_OPEN_BRACKET:
                return 26;
            case KeyEvent.VK_CLOSE_BRACKET:
                return 27;
            case KeyEvent.VK_ENTER:
                switch (event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_STANDARD:
                        return 28;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 28;
                }

                // Row #3
            case KeyEvent.VK_CAPS_LOCK:
                return 58;
            case KeyEvent.VK_A:
                return 30;
            case KeyEvent.VK_S:
                return 31;
            case KeyEvent.VK_D:
                return 32;
            case KeyEvent.VK_F:
                return 33;
            case KeyEvent.VK_G:
                return 34;
            case KeyEvent.VK_H:
                return 35;
            case KeyEvent.VK_J:
                return 36;
            case KeyEvent.VK_K:
                return 37;
            case KeyEvent.VK_L:
                return 38;
            case KeyEvent.VK_SEMICOLON:
                return 39;
            case KeyEvent.VK_QUOTE:
                return 40;

                // Row #4
            case KeyEvent.VK_SHIFT:
                switch (event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 42;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 54;
                }
            case KeyEvent.VK_BACK_SLASH:
                return 43;
            case KeyEvent.VK_Z:
                return 44;
            case KeyEvent.VK_X:
                return 45;
            case KeyEvent.VK_C:
                return 46;
            case KeyEvent.VK_V:
                return 47;
            case KeyEvent.VK_B:
                return 48;
            case KeyEvent.VK_N:
                return 49;
            case KeyEvent.VK_M:
                return 50;
            case KeyEvent.VK_COMMA:
                return 51;
            case KeyEvent.VK_PERIOD:
                return 52;
            case KeyEvent.VK_SLASH:
                return 53;

                //
                // Bottom row
            case KeyEvent.VK_CONTROL:
                switch (event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 29;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 29;
                }
            case KeyEvent.VK_WINDOWS:
                switch (event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 91;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 92;
                }
            case KeyEvent.VK_ALT:
                switch (event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 56;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 56;
                }
            case KeyEvent.VK_ALT_GRAPH:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 56;

            case KeyEvent.VK_SPACE:
                return 57;

            case KeyEvent.VK_CONTEXT_MENU:
                return 93;

                //
                // Special keys
            case KeyEvent.VK_PRINTSCREEN:
                return 55;
            case KeyEvent.VK_SCROLL_LOCK:
                return 70;
            case KeyEvent.VK_PAUSE:
                return 29;

                // Text navigation keys
            case KeyEvent.VK_INSERT:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 82;
            case KeyEvent.VK_HOME:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 71;
            case KeyEvent.VK_PAGE_UP:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 73;
            case KeyEvent.VK_DELETE:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 83;
            case KeyEvent.VK_END:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 79;
            case KeyEvent.VK_PAGE_DOWN:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 81;

                // Cursor keys
            case KeyEvent.VK_UP:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 72;
            case KeyEvent.VK_LEFT:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 75;
            case KeyEvent.VK_DOWN:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 80;
            case KeyEvent.VK_RIGHT:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 77;

                // Keypad
            case KeyEvent.VK_NUM_LOCK:
                return 69;
            case KeyEvent.VK_DIVIDE:
                return (FASTPATH_INPUT_KBDFLAGS_EXTENDED << 8) | 53;
            case KeyEvent.VK_MULTIPLY:
                return 55;
            case KeyEvent.VK_SUBTRACT:
                return 74;
            case KeyEvent.VK_ADD:
                return 78;

            case KeyEvent.VK_NUMPAD7:
                return 71;
            case KeyEvent.VK_NUMPAD8:
                return 72;
            case KeyEvent.VK_NUMPAD9:
                return 73;
            case KeyEvent.VK_NUMPAD4:
                return 75;
            case KeyEvent.VK_NUMPAD5:
                return 76;
            case KeyEvent.VK_NUMPAD6:
                return 77;
            case KeyEvent.VK_NUMPAD1:
                return 79;
            case KeyEvent.VK_NUMPAD2:
                return 80;
            case KeyEvent.VK_NUMPAD3:
                return 81;
            case KeyEvent.VK_NUMPAD0:
                return 82;
            case KeyEvent.VK_DECIMAL:
                return 83;

            default:
                System.err.println("Key is not mapped: " + event + ".");
                return 57; // Space
        }
    }

}
