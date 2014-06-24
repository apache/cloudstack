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
package com.cloud.consoleproxy.rdp;

import java.awt.event.KeyEvent;

public class KeysymToKeycode {

    // some of this keymap is taken from http://openwonderland.googlecode.com/svn/trunk/modules/foundation/xremwin/src/classes/org/jdesktop/wonderland/modules/xremwin/client/KeycodeToKeysym.java
    private final static int[][] map = {
            /* XK_BackSpace         */{0xFF08, KeyEvent.VK_BACK_SPACE},
            /* XK_Tab               */{0xFF09, KeyEvent.VK_TAB},
            /* XK_Clear             */{0xFF0B, KeyEvent.VK_CLEAR},
            /* XK_Return            */{0xFF0D, KeyEvent.VK_ENTER},
            /* XK_Pause             */{0xFF13, KeyEvent.VK_PAUSE},
            /* XK_Scroll_Lock       */{0xFF14, KeyEvent.VK_SCROLL_LOCK},
            /* XK_Escape            */{0xFF1B, KeyEvent.VK_ESCAPE},
            /* XK_Delete            */{0xFFFF, KeyEvent.VK_DELETE},
            /* XK_Home              */{0xFF50, KeyEvent.VK_HOME},
            /* XK_Left              */{0xFF51, KeyEvent.VK_LEFT},
            /* XK_Up                */{0xFF52, KeyEvent.VK_UP},
            /* XK_Right             */{0xFF53, KeyEvent.VK_RIGHT},
            /* XK_Down              */{0xFF54, KeyEvent.VK_DOWN},
            /* XK_Page_Up           */{0xFF55, KeyEvent.VK_PAGE_UP},
            /* XK_Page_Down         */{0xFF56, KeyEvent.VK_PAGE_DOWN},
            /* XK_End               */{0xFF57, KeyEvent.VK_END},
            /* XK_Print             */{0xFF61, KeyEvent.VK_PRINTSCREEN},
            /* XK_Insert            */{0xFF63, KeyEvent.VK_INSERT},
            /* XK_Undo              */{0xFF65, KeyEvent.VK_UNDO},
            /* XK_Find              */{0xFF68, KeyEvent.VK_FIND},
            /* XK_Cancel            */{0xFF69, KeyEvent.VK_CANCEL},
            /* XK_Help              */{0xFF6A, KeyEvent.VK_HELP},
            /* XK_Mode_switch       */{0xFF7E, KeyEvent.VK_MODECHANGE},
            /* XK_Num_Lock          */{0xFF7F, KeyEvent.VK_NUM_LOCK},
            /* XK_F1                */{0xFFBE, KeyEvent.VK_F1},
            /* XK_F2                */{0xFFBF, KeyEvent.VK_F2},
            /* XK_F3                */{0xFFC0, KeyEvent.VK_F3},
            /* XK_F4                */{0xFFC1, KeyEvent.VK_F4},
            /* XK_F5                */{0xFFC2, KeyEvent.VK_F5},
            /* XK_F6                */{0xFFC3, KeyEvent.VK_F6},
            /* XK_F7                */{0xFFC4, KeyEvent.VK_F7},
            /* XK_F8                */{0xFFC5, KeyEvent.VK_F8},
            /* XK_F9                */{0xFFC6, KeyEvent.VK_F9},
            /* XK_F10               */{0xFFC7, KeyEvent.VK_F10},
            /* XK_F11               */{0xFFC8, KeyEvent.VK_F11},
            /* XK_F12               */{0xFFC9, KeyEvent.VK_F12},
            /* XK_F13               */{0xFFCA, KeyEvent.VK_F13},
            /* XK_F14               */{0xFFCB, KeyEvent.VK_F14},
            /* XK_F15               */{0xFFCC, KeyEvent.VK_F15},
            /* XK_F16               */{0xFFCD, KeyEvent.VK_F16},
            /* XK_F17               */{0xFFCE, KeyEvent.VK_F17},
            /* XK_F18               */{0xFFCF, KeyEvent.VK_F18},
            /* XK_F19               */{0xFFD0, KeyEvent.VK_F19},
            /* XK_F20               */{0xFFD1, KeyEvent.VK_F20},
            /* XK_F21               */{0xFFD2, KeyEvent.VK_F21},
            /* XK_F22               */{0xFFD3, KeyEvent.VK_F22},
            /* XK_F23               */{0xFFD4, KeyEvent.VK_F23},
            /* XK_F24               */{0xFFD5, KeyEvent.VK_F24},
            /* XK_Shift_L           */{0xFFE1, KeyEvent.VK_SHIFT},
            /* XK_Control_L         */{0xFFE3, KeyEvent.VK_CONTROL},
            /* XK_Caps_Lock         */{0xFFE5, KeyEvent.VK_CAPS_LOCK},
            /* XK_Meta_L            */{0xFFE7, KeyEvent.VK_META},
            /* XK_Alt_L             */{0xFFE9, KeyEvent.VK_ALT},
            /* XK_a                 */{0x0061, KeyEvent.VK_A},
            /* XK_b                 */{0x0062, KeyEvent.VK_B},
            /* XK_c                 */{0x0063, KeyEvent.VK_C},
            /* XK_d                 */{0x0064, KeyEvent.VK_D},
            /* XK_e                 */{0x0065, KeyEvent.VK_E},
            /* XK_f                 */{0x0066, KeyEvent.VK_F},
            /* XK_g                 */{0x0067, KeyEvent.VK_G},
            /* XK_h                 */{0x0068, KeyEvent.VK_H},
            /* XK_i                 */{0x0069, KeyEvent.VK_I},
            /* XK_j                 */{0x006a, KeyEvent.VK_J},
            /* XK_k                 */{0x006b, KeyEvent.VK_K},
            /* XK_l                 */{0x006c, KeyEvent.VK_L},
            /* XK_m                 */{0x006d, KeyEvent.VK_M},
            /* XK_n                 */{0x006e, KeyEvent.VK_N},
            /* XK_o                 */{0x006f, KeyEvent.VK_O},
            /* XK_p                 */{0x0070, KeyEvent.VK_P},
            /* XK_q                 */{0x0071, KeyEvent.VK_Q},
            /* XK_r                 */{0x0072, KeyEvent.VK_R},
            /* XK_s                 */{0x0073, KeyEvent.VK_S},
            /* XK_t                 */{0x0074, KeyEvent.VK_T},
            /* XK_u                 */{0x0075, KeyEvent.VK_U},
            /* XK_v                 */{0x0076, KeyEvent.VK_V},
            /* XK_w                 */{0x0077, KeyEvent.VK_W},
            /* XK_x                 */{0x0078, KeyEvent.VK_X},
            /* XK_y                 */{0x0079, KeyEvent.VK_Y},
            /* XK_z                 */{0x007a, KeyEvent.VK_Z},
            {0x0060, KeyEvent.VK_BACK_QUOTE},
            {0x007e, KeyEvent.VK_BACK_QUOTE},
            {0x0021, KeyEvent.VK_1},
            {0x0040, KeyEvent.VK_2},
            {0x0023, KeyEvent.VK_3},
            {0x0024, KeyEvent.VK_4},
            {0x0025, KeyEvent.VK_5},
            {0x005e, KeyEvent.VK_6},
            {0x0026, KeyEvent.VK_7},
            {0x002A, KeyEvent.VK_8},
            {0x0028, KeyEvent.VK_9},
            {0x0029, KeyEvent.VK_0},
            {0x005f, KeyEvent.VK_MINUS},
            {0x002b, KeyEvent.VK_EQUALS},
            {0x007b, KeyEvent.VK_OPEN_BRACKET},
            {0x007d, KeyEvent.VK_CLOSE_BRACKET},
            {0x007c, KeyEvent.VK_BACK_SLASH},
            {0x003a, KeyEvent.VK_SEMICOLON},
            {0x0027, KeyEvent.VK_QUOTE},
            {0x0022, KeyEvent.VK_QUOTE},
            {0x003c, KeyEvent.VK_COMMA},
            {0x003e, KeyEvent.VK_PERIOD},
            {0x003f, KeyEvent.VK_SLASH},
    };

    public static int getKeycode(int keysym) {
        for (int i = 0; i < (map.length); i++) {
            if (map[i][0] == keysym) {
                return map[i][1];
            }
        }
        return keysym;
    }

}
