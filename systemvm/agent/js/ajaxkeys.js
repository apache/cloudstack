/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

/*
 * This var contains the limited keyboard translation tables.
 * This is the table that users can modify to make special keyboard to work properly.
 * They are used by the ajaxviewer.js
 */

//client event type. corresponds to events in ajaxviewer.


//use java AWT key modifier masks
JS_KEY_BACKSPACE = 8;
JS_KEY_TAB = 9;
JS_KEY_ENTER = 13;
JS_KEY_SHIFT = 16;
JS_KEY_CTRL = 17;
JS_KEY_ALT = 18;
JS_KEY_CAPSLOCK = 20;
JS_KEY_ESCAPE = 27;
JS_KEY_PAGEUP = 33;
JS_KEY_PAGEDOWN = 34;
JS_KEY_END = 35;
JS_KEY_HOME = 36;
JS_KEY_LEFT = 37;
JS_KEY_UP = 38;
JS_KEY_RIGHT = 39;
JS_KEY_DOWN = 40;
JS_KEY_INSERT = 45;
JS_KEY_DELETE = 46;
JS_KEY_SELECT_KEY = 93;
JS_KEY_NUMPAD0 = 96;
JS_KEY_NUMPAD1 = 97;
JS_KEY_NUMPAD2 = 98;
JS_KEY_NUMPAD3 = 99;
JS_KEY_NUMPAD4 = 100;
JS_KEY_NUMPAD5 = 101;
JS_KEY_NUMPAD6 = 102;
JS_KEY_NUMPAD7 = 103;
JS_KEY_NUMPAD8 = 104;
JS_KEY_NUMPAD9 = 105;
JS_KEY_MULTIPLY = 106;
JS_KEY_ADD = 107;
JS_KEY_SUBSTRACT = 109;
JS_KEY_DECIMAL_POINT = 110;
JS_KEY_DIVIDE = 111;
JS_KEY_F1 = 112;
JS_KEY_F2 = 113;
JS_KEY_F3 = 114;
JS_KEY_F4 = 115;
JS_KEY_F5 = 116;
JS_KEY_F6 = 117;
JS_KEY_F7 = 118;
JS_KEY_F8 = 119;
JS_KEY_F9 = 120;
JS_KEY_F10 = 121;
JS_KEY_F11 = 122;
JS_KEY_F12 = 123;
JS_KEY_SEMI_COLON = 186;			// ;
JS_KEY_COMMA = 188;				// ,
JS_KEY_DASH = 189;				// -
JS_KEY_PERIOD = 190;				// .
JS_KEY_FORWARD_SLASH = 191;		// /
JS_KEY_GRAVE_ACCENT = 192;		// `
JS_KEY_OPEN_BRACKET = 219;		// [
JS_KEY_BACK_SLASH = 220;			// \
JS_KEY_CLOSE_BRACKET = 221;		// ]
JS_KEY_SINGLE_QUOTE = 222;		// '


//X11 keysym definitions
X11_KEY_CAPSLOCK = 0xffe5;
X11_KEY_BACKSPACE = 0xff08;
X11_KEY_TAB = 0xff09;
X11_KEY_ENTER = 0xff0d;
X11_KEY_ESCAPE = 0xff1b;
X11_KEY_INSERT = 0xff63;
X11_KEY_DELETE = 0xffff;
X11_KEY_HOME = 0xff50;
X11_KEY_END = 0xff57;
X11_KEY_PAGEUP = 0xff55;
X11_KEY_PAGEDOWN = 0xff56;
X11_KEY_LEFT = 0xff51;
X11_KEY_UP = 0xff52;
X11_KEY_RIGHT = 0xff53;
X11_KEY_DOWN = 0xff54;
X11_KEY_F1 = 0xffbe;
X11_KEY_F2 = 0xffbf;
X11_KEY_F3 = 0xffc0;
X11_KEY_F4 = 0xffc1;
X11_KEY_F5 = 0xffc2;
X11_KEY_F6 = 0xffc3;
X11_KEY_F7 = 0xffc4;
X11_KEY_F8 = 0xffc5;
X11_KEY_F9 = 0xffc6;
X11_KEY_F10 = 0xffc7;
X11_KEY_F11 = 0xffc8;
X11_KEY_F12 = 0xffc9;
X11_KEY_SHIFT = 0xffe1;
X11_KEY_CTRL = 0xffe3;
X11_KEY_ALT = 0xffe9;
X11_KEY_GRAVE_ACCENT = 0x60;
X11_KEY_SUBSTRACT = 0x2d;
X11_KEY_ADD = 0x2b;
X11_KEY_OPEN_BRACKET = 0x5b;
X11_KEY_CLOSE_BRACKET = 0x5d;
X11_KEY_BACK_SLASH = 0x7c;
X11_KEY_REVERSE_SOLIUS = 0x5c;			// another back slash (back slash on JP keyboard)
X11_KEY_SINGLE_QUOTE = 0x22;
X11_KEY_COMMA = 0x3c;
X11_KEY_PERIOD = 0x3e;
X11_KEY_FORWARD_SLASH = 0x3f;
X11_KEY_DASH = 0x2d;
X11_KEY_COLON = 0x3a;
X11_KEY_SEMI_COLON = 0x3b;
X11_KEY_NUMPAD0 = 0x30;
X11_KEY_NUMPAD1 = 0x31;
X11_KEY_NUMPAD2 = 0x32;
X11_KEY_NUMPAD3 = 0x33;
X11_KEY_NUMPAD4 = 0x34;
X11_KEY_NUMPAD5 = 0x35;
X11_KEY_NUMPAD6 = 0x36;
X11_KEY_NUMPAD7 = 0x37;
X11_KEY_NUMPAD8 = 0x38;
X11_KEY_NUMPAD9 = 0x39;
X11_KEY_DECIMAL_POINT = 0x2e;
X11_KEY_DIVIDE = 0x3f;
X11_KEY_TILDE = 0x7e;				// ~
X11_KEY_CIRCUMFLEX_ACCENT = 0x5e;	// ^
X11_KEY_YEN_MARK = 0xa5;				// Japanese YEN mark
X11_KEY_ASTERISK = 0x2a;
X11_KEY_KP_0 = 0xFFB0;
X11_KEY_KP_1 = 0xFFB1;
X11_KEY_KP_2 = 0xFFB2;
X11_KEY_KP_3 = 0xFFB3;
X11_KEY_KP_4 = 0xFFB4;
X11_KEY_KP_5 = 0xFFB5;
X11_KEY_KP_6 = 0xFFB6;
X11_KEY_KP_7 = 0xFFB7;
X11_KEY_KP_8 = 0xFFB8;
X11_KEY_KP_9 = 0xFFB9;
X11_KEY_KP_Decimal = 0xFFAE;

KEY_DOWN = 5;
KEY_UP = 6;

KEYBOARD_TYPE_COOKED = "us";
KEYBOARD_TYPE_JP = "jp";
KEYBOARD_TYPE_UK = "uk";
KEYBOARD_TYPE_FR = "fr";

//JP keyboard type

var	keyboardTables = [
           {tindex: 0, keyboardType: KEYBOARD_TYPE_COOKED, mappingTable:
               {X11: [  {keycode: 226, entry: X11_KEY_REVERSE_SOLIUS},

                        {keycode: 240, entry: [
                            {type: KEY_DOWN, code: X11_KEY_CAPSLOCK, modifiers: 0 },
                            {type: KEY_UP, code: X11_KEY_CAPSLOCK, modifiers: 0 },
                            ]
                        }
                     ],
                keyPress: [
                        {keycode: 59, entry: [
                                              {type: KEY_DOWN, code: X11_KEY_SEMI_COLON, modifiers: 0 },
                                              {type: KEY_UP, code: X11_KEY_SEMI_COLON, modifiers: 0 },
                                              ]
                        },
                        {keycode: 43, entry: [
                                              {type: KEY_DOWN, code: X11_KEY_SHIFT, modifiers: 0, shift: false },
                                              {type: KEY_DOWN, code: X11_KEY_ADD, modifiers: 0, shift: false },
                                              {type: KEY_UP, code: X11_KEY_ADD, modifiers: 0, shift: false },
                                              {type: KEY_UP, code: X11_KEY_SHIFT, modifiers: 0, shift: false },
                                              {type: KEY_DOWN, code: X11_KEY_ADD, modifiers: 0, shift: true },
                                              {type: KEY_UP, code: X11_KEY_ADD, modifiers: 0, shift: true },
                                              ]
                        }
                        ]
               }
           }, {tindex: 1, keyboardType: KEYBOARD_TYPE_JP, mappingTable:
           // intialize keyboard mapping for RAW keyboard
           {X11: [
                  {keycode: JS_KEY_CAPSLOCK,			entry : X11_KEY_CAPSLOCK},
                  {keycode: JS_KEY_BACKSPACE,			entry : X11_KEY_BACKSPACE},
                  {keycode: JS_KEY_TAB,					entry : X11_KEY_TAB},
                  {keycode: JS_KEY_ENTER,				entry : X11_KEY_ENTER},
                  {keycode: JS_KEY_ESCAPE,				entry : X11_KEY_ESCAPE},
                  {keycode: JS_KEY_INSERT,				entry : X11_KEY_INSERT},
                  {keycode: JS_KEY_DELETE,				entry : X11_KEY_DELETE},
                  {keycode: JS_KEY_HOME,				entry : X11_KEY_HOME},
                  {keycode: JS_KEY_END,					entry : X11_KEY_END},
                  {keycode: JS_KEY_PAGEUP,				entry : X11_KEY_PAGEUP},
                  {keycode: JS_KEY_PAGEDOWN,			entry : X11_KEY_PAGEDOWN},
                  {keycode: JS_KEY_LEFT,				entry : X11_KEY_LEFT},
                  {keycode: JS_KEY_UP,					entry : X11_KEY_UP},
                  {keycode: JS_KEY_RIGHT,				entry : X11_KEY_RIGHT},
                  {keycode: JS_KEY_DOWN,				entry : X11_KEY_DOWN},
                  {keycode: JS_KEY_F1,					entry : X11_KEY_F1},
                  {keycode: JS_KEY_F2,					entry : X11_KEY_F2},
                  {keycode: JS_KEY_F3,					entry : X11_KEY_F3},
                  {keycode: JS_KEY_F4,					entry : X11_KEY_F4},
                  {keycode: JS_KEY_F5,					entry : X11_KEY_F5},
                  {keycode: JS_KEY_F6,					entry : X11_KEY_F6},
                  {keycode: JS_KEY_F7,					entry : X11_KEY_F7},
                  {keycode: JS_KEY_F8,					entry : X11_KEY_F8},
                  {keycode: JS_KEY_F9,					entry : X11_KEY_F9},
                  {keycode: JS_KEY_F10,					entry : X11_KEY_F10},
                  {keycode: JS_KEY_F11,					entry : X11_KEY_F11},
                  {keycode: JS_KEY_F12,					entry : X11_KEY_F12},
                  {keycode: JS_KEY_SHIFT,				entry : X11_KEY_SHIFT},
                  {keycode: JS_KEY_CTRL,				entry : X11_KEY_CTRL},
                  {keycode: JS_KEY_ALT,					entry : X11_KEY_ALT},
                  //{keycode: JS_KEY_GRAVE_ACCENT,		entry : X11_KEY_GRAVE_ACCENT},
                  //[192 / 64 = "' @"]
                  {keycode: 192,	entry : 0x40,	guestos: "windows",	browser: "IE"},
                  {keycode: 64,		entry : 0x40,	guestos: "windows",	browser: "Firefox"},
                  //{keycode: JS_KEY_ADD,					entry : X11_KEY_ADD},
                  //[187 / 59 = "; +"]
                  {keycode: 187,	entry : 0x3b,	guestos: "windows",	browser: "IE"},
                  {keycode: 59,		entry : 0x3b,	guestos: "windows",	browser: "Firefox"},
                  //{keycode: JS_KEY_OPEN_BRACKET,		entry : X11_KEY_OPEN_BRACKET},
                  //[219 = "[{"]
                  {keycode: 219,	entry : 0x5b,	guestos: "windows",	browser: "IE"},
                  {keycode: 219,	entry : 0x5b,	guestos: "windows",	browser: "Firefox"},
                  //{keycode: JS_KEY_CLOSE_BRACKET,		entry : X11_KEY_CLOSE_BRACKET},
                  //[221 = "]}"]
                  {keycode: 221,	entry : 0x5d,	guestos: "windows",	browser: "IE"},
                  {keycode: 221,	entry : 0x5d,	guestos: "windows",	browser: "Firefox"},
                  {keycode: JS_KEY_BACK_SLASH,		entry : X11_KEY_BACK_SLASH,	guestos: "windows"},
                  //{keycode: JS_KEY_SINGLE_QUOTE,		entry : X11_KEY_SINGLE_QUOTE},
                  //[222 / 160 = "~^"]
                  {keycode: 222,	entry : 0x5e,	guestos: "windows",	browser: "IE"},
                  {keycode: 160,	entry : 0x5e,	guestos: "windows",	browser: "Firefox"},
                  //[173 = "-=" ] specific to Firefox browser
                  {keycode: 173,	entry : 0x2d,	guestos: "windows",	browser: "Firefox"},
                  {keycode: JS_KEY_COMMA,				entry : X11_KEY_COMMA, guestos: "windows"},
                  {keycode: JS_KEY_PERIOD, 				entry : X11_KEY_PERIOD, guestos: "windows"},
                  {keycode: JS_KEY_FORWARD_SLASH,		entry : X11_KEY_FORWARD_SLASH, guestos: "windows"},
                  {keycode: JS_KEY_DASH,				entry : X11_KEY_DASH, guestos: "windows"},
                  {keycode: JS_KEY_SEMI_COLON,			entry : 0x3a,	guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD0,				entry : X11_KEY_NUMPAD0, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD1,				entry : X11_KEY_NUMPAD1, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD2,				entry : X11_KEY_NUMPAD2, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD3,				entry : X11_KEY_NUMPAD3, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD4,				entry : X11_KEY_NUMPAD4, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD5,				entry : X11_KEY_NUMPAD5, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD6,				entry : X11_KEY_NUMPAD6, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD7,				entry : X11_KEY_NUMPAD7, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD8,				entry : X11_KEY_NUMPAD8, guestos: "windows"},
                  {keycode: JS_KEY_NUMPAD9,				entry : X11_KEY_NUMPAD9, guestos: "windows"},
                  {keycode: JS_KEY_DECIMAL_POINT,		entry : X11_KEY_PERIOD, guestos: "windows"},
                  {keycode: JS_KEY_DIVIDE,				entry : 0xffaf, guestos: "windows"},
                  {keycode: JS_KEY_MULTIPLY,			entry : 0xffaa, guestos: "windows"},
                  {keycode: JS_KEY_ADD,					entry : 0xffab, guestos: "windows"},
                  {keycode: JS_KEY_SUBSTRACT,			entry : 0xffad, guestos: "windows"},
                  //Kanji Key = 243 / 244
                  {keycode: 243,	entry : 0x7e,	browser: "IE"},
                  {keycode: 244,	entry : 0x7e,	browser: "IE"},
                  //Caps Lock = 240
                  {keycode: 240,			entry : 0xffe5},
                  //[186 / 58 = "~^"]
                  {keycode: 186,	entry : 0x3a, guestos: "windows", browser: "IE"},
                  {keycode: 58,		entry : 0x3a, guestos: "windows",	browser: "Firefox"},
                  //[226 = "_"]
                  {keycode: 226,	entry : 0x5f, guestos: "windows"},
                  ],
                  keyPress: [
                             // These mappings are for japanese guestOS. it is recommended that admin should deploy
                             // the VM with "keyboard=jp" paramenter or change the VM properties in hypervisor to use jp mapping.
                             {keycode: 42,       entry:  0xffaa}, // *
                             {keycode: 43,       entry:  0xffab}, // +
                            ]
           }
           }, {tindex: 2, keyboardType: KEYBOARD_TYPE_UK, mappingTable:
                 {X11: [],
                  keyPress: [
                          //[34 = "]
                          {keycode: 34,		entry: 0x40,	guestos: "windows"},
                          //[35 = #]
                          {keycode: 35,		entry: 0x5c,	guestos: "windows"},
                          // [64 = @]
                          {keycode: 64,		entry: 0x22,	guestos: "windows"},
                          // [92 = \]
                          {keycode: 92,		entry: 0xa6,	guestos: "windows"},
                          // [124 = |]
                          {keycode: 124,	entry: 0xa6,	guestos: "windows"},
                          // [126 = ~]
                          {keycode: 126,	entry: 0x7c,	guestos: "windows"},
                          // [163 = £]
                          {keycode: 163,	entry: 0x23,	guestos: "windows"},
                          // [172 = ¬]
                          {keycode: 172,	entry: 0x7e,	guestos: "windows"},
                          // [166 = ¦]
                          {keycode: 166,	entry: [{type : KEY_DOWN, code : 0x60, modifiers : 896, shift : false}],	guestos: "windows"}
                          ]
           }
           },
        {tindex: 3, keyboardType: KEYBOARD_TYPE_FR, mappingTable:{
                X11: [
                        // '*' doesn't work
                        {keycode: 220, browser: "Chrome", entry: 0x5c},
                        {keycode: 170, browser: "Firefox", entry: 0x5c},
//ROW 1 AltGr
                        //[50 = ~]
                        {keycode: 50, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x32, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x32, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[51 = #]
                        {keycode: 51, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x33, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x33, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[52 = {]
                        {keycode: 52, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x34, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x34, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[53 = []
                        {keycode: 53, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x35, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x35, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[54 = |]
                        {keycode: 54, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x36, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x36, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[55 = `]
                        {keycode: 55, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x37, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x37, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[56 = \]
                        {keycode: 56, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x38, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x38, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[57 = ^]
                        {keycode: 57, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x39, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x39, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[48 = @]
                        {keycode: 48, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x30, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x30, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[219 = ]]
                        {keycode: 219, browser: "Chrome", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x2d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x2d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[169 = ]]
                        {keycode: 169, browser: "Firefox", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x2d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x2d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[187 = }]
                        {keycode: 187, browser: "Chrome", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x3d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x3d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[61 = }]
                        {keycode: 61, browser: "Firefox", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x3d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x3d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
//ROW 2 AltGr
                        //[69 = €]
                        {keycode: 69, entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x65, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x65, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[186 = ¤]
                        {keycode: 186, browser: "Chrome", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x5d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x5d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]},
                        //[164 = ¤]
                        {keycode: 164, browser: "Firefox", entry: [
                                {type: KEY_DOWN, code: 0xffea, modifiers: 0, altgr: true},
                                {type: KEY_DOWN, code: 0x5d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0x5d, modifiers: 0, altgr: true},
                                {type: KEY_UP, code: 0xffea, modifiers: 0, altgr: true}
                        ]}
                ],
                keyPress: [
//ROW 1
                        //[178 = ²]
                        {keycode: 178, entry: [
                                {type: KEY_DOWN, code: 0x60, modifiers: 0},
                                {type: KEY_UP, code: 0x60, modifiers: 0}
                        ]},
                        //[38 = &]
                        {keycode: 38, entry: [
                                {type: KEY_DOWN, code: 0x31, modifiers: 0},
                                {type: KEY_UP, code: 0x31, modifiers: 0}
                        ]},
                        //[233 = é]
                        {keycode: 233, entry: [
                                {type: KEY_DOWN, code: 0x32, modifiers: 0},
                                {type: KEY_UP, code: 0x32, modifiers: 0}
                        ]},
                        //[34 = "]
                        {keycode: 34, entry: [
                                {type: KEY_DOWN, code: 0x33, modifiers: 0},
                                {type: KEY_UP, code: 0x33, modifiers: 0}
                        ]},
                        //[39 = ']
                        {keycode: 39, entry: [
                                {type: KEY_DOWN, code: 0x34, modifiers: 0},
                                {type: KEY_UP, code: 0x34, modifiers: 0}
                        ]},
                        //[40 = (]
                        {keycode: 40, entry: [
                                {type: KEY_DOWN, code: 0x35, modifiers: 0},
                                {type: KEY_UP, code: 0x35, modifiers: 0}
                        ]},
                        //[45 = -]
                        {keycode: 45, entry: [
                                {type: KEY_DOWN, code: 0x36, modifiers: 0},
                                {type: KEY_UP, code: 0x36, modifiers: 0}
                        ]},
                        //[232 = è]
                        {keycode: 232, entry: [
                                {type: KEY_DOWN, code: 0x37, modifiers: 0},
                                {type: KEY_UP, code: 0x37, modifiers: 0}
                        ]},
                        //[95 = _]
                        {keycode: 95, entry: [
                                {type: KEY_DOWN, code: 0x38, modifiers: 0},
                                {type: KEY_UP, code: 0x38, modifiers: 0}
                        ]},
                        //[231 = ç]
                        {keycode: 231, entry: [
                                {type: KEY_DOWN, code: 0x39, modifiers: 0},
                                {type: KEY_UP, code: 0x39, modifiers: 0}
                        ]},
                        //[224 = à]
                        {keycode: 224, entry: [
                                {type: KEY_DOWN, code: 0x30, modifiers: 0},
                                {type: KEY_UP, code: 0x30, modifiers: 0}
                        ]},
                        //[41 = )]
                        {keycode: 41, entry: [
                                {type: KEY_DOWN, code: 0x2d, modifiers: 0},
                                {type: KEY_UP, code: 0x2d, modifiers: 0}
                        ]},
                        //[176 = =]
                        {keycode: 176, entry: [
                                {type: KEY_DOWN, code: 0x2d, modifiers: 64},
                                {type: KEY_UP, code: 0x2d, modifiers: 64}
                        ]},
                        //[181 = µ]
                        {keycode: 181, entry: [
                                {type: KEY_DOWN, code: 0x5c, modifiers: 64},
                                {type: KEY_UP, code: 0x5c, modifiers: 64}
                        ]},

//ROW 2
                        //[97 = a]
                        {keycode: 97, entry: [
                                {type: KEY_DOWN, code: 0x71, modifiers: 0},
                                {type: KEY_UP, code: 0x71, modifiers: 0}
                        ]},
                        //[65 = A]
                        {keycode: 65, entry: [
                                {type: KEY_DOWN, code: 0x51, modifiers: 64},
                                {type: KEY_UP, code: 0x51, modifiers: 64}
                        ]},
                        //[122 = z]
                        {keycode: 122, entry: [
                                {type: KEY_DOWN, code: 0x77, modifiers: 0},
                                {type: KEY_UP, code: 0x77, modifiers: 0}
                        ]},
                        //[90 = Z]
                        {keycode: 90, entry: [
                                {type: KEY_DOWN, code: 0x57, modifiers: 64},
                                {type: KEY_UP, code: 0x57, modifiers: 64}
                        ]},
                        //[94 = ^]
                        {keycode: 94, entry: [
                                {type: KEY_DOWN, code: 0x5b, modifiers: 0},
                                {type: KEY_UP, code: 0x5b, modifiers: 0}
                        ]},
                        //[168 = ¨]
                        {keycode: 168, entry: [
                                {type: KEY_DOWN, code: 0x5b, modifiers: 64},
                                {type: KEY_UP, code: 0x5b, modifiers: 64}
                        ]},
                        //[36 = $]
                        {keycode: 36, entry: [
                                {type: KEY_DOWN, code: 0x5d, modifiers: 0},
                                {type: KEY_UP, code: 0x5d, modifiers: 0}
                        ]},
                        //[163 = £]
                        {keycode: 163, entry: [
                                {type: KEY_DOWN, code: 0x5d, modifiers: 64},
                                {type: KEY_UP, code: 0x5d, modifiers: 64}
                        ]},

//ROW 3
                        //[113 = q]
                        {keycode: 113, entry: [
                                {type: KEY_DOWN, code: 0x61, modifiers: 0},
                                {type: KEY_UP, code: 0x61, modifiers: 0}
                        ]},
                        //[81 = Q]
                        {keycode: 81, entry: [
                                {type: KEY_DOWN, code: 0x41, modifiers: 64},
                                {type: KEY_UP, code: 0x41, modifiers: 64}
                        ]},
                        //[109 = m]
                        {keycode: 109, entry: [
                                {type: KEY_DOWN, code: 0x3b, modifiers: 0},
                                {type: KEY_UP, code: 0x3b, modifiers: 0}
                        ]},
                        //[77 = M]
                        {keycode: 77, entry: [
                                {type: KEY_DOWN, code: 0x3b, modifiers: 64},
                                {type: KEY_UP, code: 0x3b, modifiers: 64}
                        ]},
                        //[249 = ù]
                        {keycode: 249, entry: [
                                {type: KEY_DOWN, code: 0x27, modifiers: 0},
                                {type: KEY_UP, code: 0x27, modifiers: 0}
                        ]},
                        //[37 = %]
                        {keycode: 37, entry: [
                                {type: KEY_DOWN, code: 0x27, modifiers: 64},
                                {type: KEY_UP, code: 0x27, modifiers: 64}
                        ]},

//ROW 4
                        //[60 = <]
                        {keycode: 60, entry: [
                                {type: KEY_DOWN, code: 0xa6, modifiers: 0},
                                {type: KEY_UP, code: 0xa6, modifiers: 0}
                        ]},
                        //[62 = >]
                        {keycode: 62, entry: [
                                {type: KEY_DOWN, code: 0xa6, modifiers: 64},
                                {type: KEY_UP, code: 0xa6, modifiers: 64}
                        ]},
                        //[119 = w]
                        {keycode: 119, entry: [
                                {type: KEY_DOWN, code: 0x7a, modifiers: 0},
                                {type: KEY_UP, code: 0x7a, modifiers: 0}
                        ]},
                        //[87 = W]
                        {keycode: 87, entry: [
                                {type: KEY_DOWN, code: 0x5a, modifiers: 64},
                                {type: KEY_UP, code: 0x5a, modifiers: 64}
                        ]},
                        //[44 = ,]
                        {keycode: 44, entry: [
                                {type: KEY_DOWN, code: 0x6d, modifiers: 0},
                                {type: KEY_UP, code: 0x6d, modifiers: 0}
                        ]},
                        //[63 = ?]
                        {keycode: 63, entry: [
                                {type: KEY_DOWN, code: 0x4d, modifiers: 64},
                                {type: KEY_UP, code: 0x4d, modifiers: 64}
                        ]},
                        //[59 = ;]
                        {keycode: 59, entry: [
                                {type: KEY_DOWN, code: 0x2c, modifiers: 0},
                                {type: KEY_UP, code: 0x2c, modifiers: 0}
                        ]},
                        //[46 = .]
                        {keycode: 46, entry: [
                                {type: KEY_DOWN, code: 0x2c, modifiers: 64},
                                {type: KEY_UP, code: 0x2c, modifiers: 64}
                        ]},
                        //[58 = :]
                        {keycode: 58, entry: [
                                {type: KEY_DOWN, code: 0x2e, modifiers: 0},
                                {type: KEY_UP, code: 0x2e, modifiers: 0}
                        ]},
                        //[47 = /]
                        {keycode: 47, entry: [
                                {type: KEY_DOWN, code: 0x2e, modifiers: 64},
                                {type: KEY_UP, code: 0x2e, modifiers: 64}
                        ]},
                        //[33 = !]
                        {keycode: 33, entry: [
                                {type: KEY_DOWN, code: 0x2f, modifiers: 0},
                                {type: KEY_UP, code: 0x2f, modifiers: 0}
                        ]},
                        //[167 = §]
                        {keycode: 167, entry: [
                                {type: KEY_DOWN, code: 0x2f, modifiers: 64},
                                {type: KEY_UP, code: 0x2f, modifiers: 64}
                        ]},
                ]
        }}
]
