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

import java.awt.event.KeyEvent;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;
import common.KeyOrder;

public class AwtKeyboardEventToVncAdapter extends BaseElement {

    protected boolean sh = false;
    protected boolean caps = false;
    protected boolean num = false;

    public AwtKeyboardEventToVncAdapter(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        KeyOrder order = (KeyOrder)buf.getOrder();
        buf.unref();

        ByteBuffer outBuf = new ByteBuffer(8);
        outBuf.writeByte(RfbConstants.CLIENT_KEYBOARD_EVENT);

        outBuf.writeByte((order.pressed) ? RfbConstants.KEY_DOWN : RfbConstants.KEY_UP);
        outBuf.writeShort(0); // padding
        outBuf.writeInt(map_en_us(order));

        pushDataToAllOuts(outBuf);
    }

    /**
     * Return key scan code (in lower byte) and extended flags (in second byte).
     */
    private int map_en_us(KeyOrder order) {

        switch (order.event.getKeyCode()) {
        // Functional keys
            case KeyEvent.VK_ESCAPE:
                return 0xff1b;
            case KeyEvent.VK_F1:
                return 0xffbe;
            case KeyEvent.VK_F2:
                return 0xffbf;
            case KeyEvent.VK_F3:
                return 0xffc0;
            case KeyEvent.VK_F4:
                return 0xffc1;
            case KeyEvent.VK_F5:
                return 0xffc2;
            case KeyEvent.VK_F6:
                return 0xffc3;
            case KeyEvent.VK_F7:
                return 0xffc4;
            case KeyEvent.VK_F8:
                return 0xffc5;
            case KeyEvent.VK_F9:
                return 0xffc6;
            case KeyEvent.VK_F10:
                return 0xffc7;
            case KeyEvent.VK_F11:
                return 0xffc8;
            case KeyEvent.VK_F12:
                return 0xffc9;

                // Row #1
            case KeyEvent.VK_BACK_QUOTE:
                return (sh) ? '~' : '`';
            case KeyEvent.VK_1:
                return (sh) ? '!' : '1';
            case KeyEvent.VK_2:
                return (sh) ? '@' : '2';
            case KeyEvent.VK_3:
                return (sh) ? '#' : '3';
            case KeyEvent.VK_4:
                return (sh) ? '$' : '4';
            case KeyEvent.VK_5:
                return (sh) ? '%' : '5';
            case KeyEvent.VK_6:
                return (sh) ? '^' : '6';
            case KeyEvent.VK_7:
                return (sh) ? '&' : '7';
            case KeyEvent.VK_8:
                return (sh) ? '*' : '8';
            case KeyEvent.VK_9:
                return (sh) ? '(' : '9';
            case KeyEvent.VK_0:
                return (sh) ? ')' : '0';
            case KeyEvent.VK_MINUS:
                return (sh) ? '_' : '-';
            case KeyEvent.VK_EQUALS:
                return (sh) ? '+' : '=';
            case KeyEvent.VK_BACK_SPACE:
                return 0xff08;

                // Row #2
            case KeyEvent.VK_TAB:
                return 0xff09;
            case KeyEvent.VK_Q:
                return (sh ^ caps) ? 'Q' : 'q';
            case KeyEvent.VK_W:
                return (sh ^ caps) ? 'W' : 'w';
            case KeyEvent.VK_E:
                return (sh ^ caps) ? 'E' : 'e';
            case KeyEvent.VK_R:
                return (sh ^ caps) ? 'R' : 'r';
            case KeyEvent.VK_T:
                return (sh ^ caps) ? 'T' : 't';
            case KeyEvent.VK_Y:
                return (sh ^ caps) ? 'Y' : 'y';
            case KeyEvent.VK_U:
                return (sh ^ caps) ? 'U' : 'u';
            case KeyEvent.VK_I:
                return (sh ^ caps) ? 'I' : 'i';
            case KeyEvent.VK_O:
                return (sh ^ caps) ? 'O' : 'o';
            case KeyEvent.VK_P:
                return (sh ^ caps) ? 'P' : 'p';
            case KeyEvent.VK_OPEN_BRACKET:
                return (sh) ? '{' : '[';
            case KeyEvent.VK_CLOSE_BRACKET:
                return (sh) ? '{' : ']';
            case KeyEvent.VK_ENTER:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_STANDARD:
                        return 0xff0d;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return 0xff8d;
                }

                // Row #3
            case KeyEvent.VK_CAPS_LOCK:
                if (order.pressed)
                    caps = !caps;
                return 0xFFE5;
            case KeyEvent.VK_A:
                return (sh ^ caps) ? 'A' : 'a';
            case KeyEvent.VK_S:
                return (sh ^ caps) ? 'S' : 's';
            case KeyEvent.VK_D:
                return (sh ^ caps) ? 'D' : 'd';
            case KeyEvent.VK_F:
                return (sh ^ caps) ? 'F' : 'f';
            case KeyEvent.VK_G:
                return (sh ^ caps) ? 'G' : 'g';
            case KeyEvent.VK_H:
                return (sh ^ caps) ? 'H' : 'h';
            case KeyEvent.VK_J:
                return (sh ^ caps) ? 'J' : 'j';
            case KeyEvent.VK_K:
                return (sh ^ caps) ? 'K' : 'k';
            case KeyEvent.VK_L:
                return (sh ^ caps) ? 'L' : 'l';
            case KeyEvent.VK_SEMICOLON:
                return (sh) ? ':' : ';';
            case KeyEvent.VK_QUOTE:
                return (sh) ? '"' : '\'';

                // Row #4
            case KeyEvent.VK_SHIFT:
                sh = !sh;
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xffe1;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 0xffe2;
                }
            case KeyEvent.VK_BACK_SLASH:
                return (sh) ? '|' : '\\';
            case KeyEvent.VK_Z:
                return (sh ^ caps) ? 'Z' : 'z';
            case KeyEvent.VK_X:
                return (sh ^ caps) ? 'X' : 'x';
            case KeyEvent.VK_C:
                return (sh ^ caps) ? 'C' : 'c';
            case KeyEvent.VK_V:
                return (sh ^ caps) ? 'V' : 'v';
            case KeyEvent.VK_B:
                return (sh ^ caps) ? 'B' : 'b';
            case KeyEvent.VK_N:
                return (sh ^ caps) ? 'N' : 'n';
            case KeyEvent.VK_M:
                return (sh ^ caps) ? 'M' : 'M';
            case KeyEvent.VK_COMMA:
                return (sh) ? '<' : ',';
            case KeyEvent.VK_PERIOD:
                return (sh) ? '>' : '.';
            case KeyEvent.VK_SLASH:
                return (sh) ? '?' : '/';

                //
                // Bottom row
            case KeyEvent.VK_CONTROL:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xFFE3;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 0xFFE4;
                }
            case KeyEvent.VK_WINDOWS:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xFFED; // HyperL
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 0xFFEE; // HyperR
                }
            case KeyEvent.VK_META:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xFFE7; // MetaL
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 0xFFE8; // MetaR
                }

            case KeyEvent.VK_ALT:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xFFE9;
                    case KeyEvent.KEY_LOCATION_RIGHT:
                        return 0xFFEA;
                }
            case KeyEvent.VK_ALT_GRAPH:
                return 0xfe03;

            case KeyEvent.VK_SPACE:
                return ' ';

            case KeyEvent.VK_CONTEXT_MENU:
                return 0xff67;

                //
                // Special keys
            case KeyEvent.VK_PRINTSCREEN:
                return (sh) ? 0xFF15/* SysRq */: 0xFF61 /* Print */;
            case KeyEvent.VK_SCROLL_LOCK:
                return 0xFF14;
            case KeyEvent.VK_PAUSE:
                return (sh) ? 0xFF6B/* Break */: 0xFF13/* Pause */;

                // Text navigation keys
            case KeyEvent.VK_INSERT:
                return 0xff63;
            case KeyEvent.VK_DELETE:
                return 0xffff;
            case KeyEvent.VK_HOME:
                return 0xff50;
            case KeyEvent.VK_END:
                return 0xff57;
            case KeyEvent.VK_PAGE_UP:
                return 0xff55;
            case KeyEvent.VK_PAGE_DOWN:
                return 0xff56;

                // Cursor keys
            case KeyEvent.VK_LEFT:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xff51;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return 0xFF96;
                }
            case KeyEvent.VK_UP:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xff52;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return 0xFF97;
                }
            case KeyEvent.VK_RIGHT:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xff53;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return 0xFF98;
                }
            case KeyEvent.VK_DOWN:
                switch (order.event.getKeyLocation()) {
                    default:
                    case KeyEvent.KEY_LOCATION_LEFT:
                        return 0xff54;
                    case KeyEvent.KEY_LOCATION_NUMPAD:
                        return 0xFF99;
                }

                // Keypad
            case KeyEvent.VK_NUM_LOCK:
                if (order.pressed)
                    num = !num;
                return 0xFF6F;
            case KeyEvent.VK_DIVIDE:
                return 0xFFAF;
            case KeyEvent.VK_MULTIPLY:
                return 0xFFAA;
            case KeyEvent.VK_SUBTRACT:
                return 0xFFAD;
            case KeyEvent.VK_ADD:
                return 0xFFAB;

            case KeyEvent.VK_KP_LEFT:
                return 0xFF96;
            case KeyEvent.VK_KP_UP:
                return 0xFF97;
            case KeyEvent.VK_KP_RIGHT:
                return 0xFF98;
            case KeyEvent.VK_KP_DOWN:
                return 0xFF99;

            case KeyEvent.VK_NUMPAD0:
                return 0xFFB0;
            case KeyEvent.VK_NUMPAD1:
                return 0xFFB1;
            case KeyEvent.VK_NUMPAD2:
                return 0xFFB2;
            case KeyEvent.VK_NUMPAD3:
                return 0xFFB3;
            case KeyEvent.VK_NUMPAD4:
                return 0xFFB4;
            case KeyEvent.VK_NUMPAD5:
                return 0xFFB5;
            case KeyEvent.VK_NUMPAD6:
                return 0xFFB6;
            case KeyEvent.VK_NUMPAD7:
                return 0xFFB7;
            case KeyEvent.VK_NUMPAD8:
                return 0xFFB8;
            case KeyEvent.VK_NUMPAD9:
                return 0xFFB9;
            case KeyEvent.VK_DECIMAL:
                return 0xFFAE;

            default:
                System.err.println("Key is not mapped: " + order + ".");
                return ' '; // Space
        }
    }

}
