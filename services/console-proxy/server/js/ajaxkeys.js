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
X11_KEY_CIRCUMFLEX_ACCENT = 0x5e;    // ^
X11_KEY_YEN_MARK = 0xa5;
X11_KEY_OPEN_BRACKET = 0x5b;
X11_KEY_CLOSE_BRACKET = 0x5d;
X11_KEY_COLON = 0x3a;
X11_KEY_REVERSE_SOLIUS = 0x5c;       // another back slash (back slash on JP keyboard)
X11_KEY_CAPSLOCK = 0xffe5;
X11_KEY_SEMI_COLON = 0x3b;
X11_KEY_SHIFT = 0xffe1;
X11_KEY_ADD = 0x2b;

KEY_DOWN = 5;
KEY_UP = 6;

//JP keyboard type
// 
var	keyboardTables = [
           {tindex: 0, keyboardType: "EN-Cooked", mappingTable: 
		{X11: [ {keycode: 222, entry: X11_KEY_CIRCUMFLEX_ACCENT},
                        {keycode: 220, entry: X11_KEY_YEN_MARK},
                        {keycode: 219, entry: X11_KEY_OPEN_BRACKET},
                        {keycode: 221, entry: X11_KEY_CLOSE_BRACKET},
                        {keycode: 59, entry: X11_KEY_COLON, browser: "Firefox"},
                        {keycode: 186, entry: X11_KEY_COLON, browser: "Chrome"},
                        {keycode: 9,  entry: 9, guestos: "XenServer"},
                        {keycode: 226, entry: X11_KEY_REVERSE_SOLIUS},
                        {keycode: 240, entry: [
                            {type: KEY_DOWN, code: X11_KEY_CAPSLOCK, modifiers: 0 },
                            {type: KEY_UP, code: X11_KEY_CAPSLOCK, modifiers: 0 },
                            ]
                        },
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
                      },
                           ]
		}
	   }	]

