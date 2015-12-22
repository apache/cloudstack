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

var g_logger;

/////////////////////////////////////////////////////////////////////////////
// class StringBuilder
//
function StringBuilder(initStr) {
    this.strings = new Array("");
    this.append(initStr);
}

StringBuilder.prototype = {
	append : function (str) {
	    if (str) {
	        this.strings.push(str);
	    }
	    return this;
	},
	
	clear : function() {
	    this.strings.length = 1;
	    return this;
	},
	
	toString: function() {
	    return this.strings.join("");
	}
};


function getCurrentLanguage() {
	if(acceptLanguages) {
		var tokens = acceptLanguages.split(',');
		if(tokens.length > 0)
			return tokens[0];
		
		return "en-us";
	} else {
		return "en-us";
	}
}

/////////////////////////////////////////////////////////////////////////////
// class KeyboardMapper
//
function KeyboardMapper() {
	this.mappedInput = [];
	this.jsX11KeysymMap = [];
	this.jsKeyPressX11KeysymMap = [];
}

//
// RAW keyboard
// 		Primarily translates KeyDown/KeyUp event, either as is (if there is no mapping entry)
// 		or through mapped result.
//
// 		For KeyPress event, it translates it only if there exist a mapping entry
// 		in jsX11KeysymMap map and the entry meets the condition
// 
// COOKED keyboard
//		Primarily translates KeyPress event, either as is or through mapped result
//		It translates KeyDown/KeyUp only there exists a mapping entry, or if there
//		is no mapping entry, translate when certain modifier key is pressed (i.e., 
//		CTRL or ALT key
//
// Mapping entry types
//		direct		:	will be directly mapped into the entry value with the same event type
//		boolean		:	only valid for jsX11KeysymMap, existence of this type, no matter true or false
//						in value, corresponding KeyDown/KeyUp event will be masked
//		array		:	contains a set of conditional mapping entry
//		
// Conditional mapping entry
//
//		{ 
//			type: <event type>, code: <mapped key code>, modifiers: <modifiers>,
//		  	shift : <shift state match condition>,		-- match on shift state
//			guestos : <guest os match condition>,		-- match on guestos type
//			browser: <browser type match condition>,	-- match on browser
//			browserVersion: <brower version match condition>	-- match on browser version
//		}
//
KeyboardMapper.KEYBOARD_TYPE_RAW = 0;
KeyboardMapper.KEYBOARD_TYPE_COOKED = 1;
KeyboardMapper.KEYBOARD_TYPE_UK = 2;
KeyboardMapper.KEYBOARD_TYPE_FR = 3;

KeyboardMapper.prototype = {

	setKeyboardType : function(keyboardType) {
		this.keyboardType = keyboardType;
		if(keyboardType == KeyboardMapper.KEYBOARD_TYPE_COOKED || keyboardType == KeyboardMapper.KEYBOARD_TYPE_UK || keyboardType == KeyboardMapper.KEYBOARD_TYPE_FR) {
			// initialize mapping for COOKED keyboard
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_CAPSLOCK] 		= AjaxViewer.X11_KEY_CAPSLOCK;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_BACKSPACE] 		= AjaxViewer.X11_KEY_BACKSPACE;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_TAB] 				= AjaxViewer.X11_KEY_TAB;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_ENTER] 			= AjaxViewer.X11_KEY_ENTER;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_ESCAPE] 			= AjaxViewer.X11_KEY_ESCAPE;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_INSERT] 			= AjaxViewer.X11_KEY_INSERT;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_DELETE] 			= AjaxViewer.X11_KEY_DELETE;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_HOME] 			= AjaxViewer.X11_KEY_HOME;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_END] 				= AjaxViewer.X11_KEY_END;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_PAGEUP] 			= AjaxViewer.X11_KEY_PAGEUP;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_PAGEDOWN] 		= AjaxViewer.X11_KEY_PAGEDOWN;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_LEFT] 			= AjaxViewer.X11_KEY_LEFT;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_UP] 				= AjaxViewer.X11_KEY_UP;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_RIGHT] 			= AjaxViewer.X11_KEY_RIGHT;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_DOWN] 			= AjaxViewer.X11_KEY_DOWN;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F1] 				= AjaxViewer.X11_KEY_F1;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F2] 				= AjaxViewer.X11_KEY_F2;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F3] 				= AjaxViewer.X11_KEY_F3;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F4] 				= AjaxViewer.X11_KEY_F4;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F5] 				= AjaxViewer.X11_KEY_F5;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F6] 				= AjaxViewer.X11_KEY_F6;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F7] 				= AjaxViewer.X11_KEY_F7;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F8] 				= AjaxViewer.X11_KEY_F8;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F9] 				= AjaxViewer.X11_KEY_F9;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F10] 				= AjaxViewer.X11_KEY_F10;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F11] 				= AjaxViewer.X11_KEY_F11;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_F12] 				= AjaxViewer.X11_KEY_F12;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_SHIFT] 			= AjaxViewer.X11_KEY_SHIFT;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_CTRL] 			= AjaxViewer.X11_KEY_CTRL;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_ALT] 				= AjaxViewer.X11_KEY_ALT;
			this.jsX11KeysymMap[AjaxViewer.JS_KEY_SELECT_KEY] 		= AjaxViewer.X11_KEY_SELECT_KEY;
			//this.jsX11KeysymMap[AjaxViewer.JS_KEY_DECIMAL_POINT] 	= AjaxViewer.X11_KEY_DECIMAL_POINT;
			this.jsKeyPressX11KeysymMap[45] 				= [{type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SUBSTRACT, modifiers: 0, shift: true },
															   {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SUBSTRACT, modifiers: 0, shift: true },
															   {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SUBSTRACT, modifiers: 0, shift: false },
															   {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SUBSTRACT, modifiers: 0, shift: false }];
			this.jsKeyPressX11KeysymMap[47] 				= [{type: AjaxViewer.KEY_DOWN, code: 0x2f, modifiers: 0, shift: true },
															   {type: AjaxViewer.KEY_UP, code: 0x2f, modifiers: 0, shift: true },
															   {type: AjaxViewer.KEY_DOWN, code: 0x2f, modifiers: 0, shift: false },
															   {type: AjaxViewer.KEY_UP, code: 0x2f, modifiers: 0, shift: false }];
		}
	},
	RawkeyboardInputHandler : function(eventType, code, modifiers, guestos, browser, browserVersion) {
		if(eventType == AjaxViewer.KEY_DOWN || eventType == AjaxViewer.KEY_UP) {
			
			// special handling for Alt + Ctrl + Ins, convert it into Alt-Ctrl-Del
			if(code == AjaxViewer.JS_KEY_INSERT) {
				if((modifiers & AjaxViewer.ALT_KEY_MASK) != 0 && (modifiers & AjaxViewer.CTRL_KEY_MASK) != 0) {
					this.mappedInput.push({type : eventType, code: 0xffff, modifiers: modifiers});
					return;
				}
			}
			
			var X11Keysym = code;
			if(this.jsX11KeysymMap[code] != undefined && (guestos == 'windows' || modifiers != AjaxViewer.SHIFT_KEY_MASK || code == AjaxViewer.JS_KEY_CAPSLOCK)) {
				X11Keysym = this.jsX11KeysymMap[code];
				if(typeof this.jsX11KeysymMap[code] == "boolean") {
					return;
				} else if($.isArray(X11Keysym)) {
					for(var i = 0; i < X11Keysym.length; i++) {
					// How to set the guestos, browser, version value??? add later
						if(this.isConditionalEntryMatched(eventType, code, modifiers, X11Keysym[i], guestos, browser, browserVersion)) {
							this.mappedInput.push(X11Keysym[i]);
						}
					}
				} else {
					this.mappedInput.push({type : eventType, code: X11Keysym, modifiers: modifiers});
				}
			} else if(guestos == 'windows' || ((modifiers & (AjaxViewer.CTRL_KEY_MASK | AjaxViewer.ALT_KEY_MASK)) != 0)){
				this.mappedInput.push({type : eventType, code: X11Keysym, modifiers: modifiers});
			}

			// special handling for ALT/CTRL key
			if(eventType == AjaxViewer.KEY_UP && (code == AjaxViewer.JS_KEY_ALT || code == AjaxViewer.JS_KEY_CTRL))
				this.mappedInput.push({type : eventType, code: this.jsX11KeysymMap[code], modifiers: modifiers});
			
		} else if(eventType == AjaxViewer.KEY_PRESS && guestos == 'null') {
			// ENTER/BACKSPACE key should already have been sent through KEY DOWN/KEY UP event
			if(code == AjaxViewer.JS_KEY_ENTER || code == AjaxViewer.JS_KEY_BACKSPACE)
				return;

			var X11Keysym = code;
			X11Keysym = this.jsKeyPressX11KeysymMap[code];
			if(X11Keysym) {
				if($.isArray(X11Keysym)) {
					for(var i = 0; i < X11Keysym.length; i++) {
						if(this.isConditionalEntryMatched(eventType, code, modifiers, X11Keysym[i], guestos, browser))
							this.mappedInput.push(X11Keysym[i]);
					}
				} else {
					this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: X11Keysym, modifiers: modifiers});
					this.mappedInput.push({type : AjaxViewer.KEY_UP, code: X11Keysym, modifiers: modifiers});
				}
			} else if(!(code == 48 && modifiers == AjaxViewer.SHIFT_KEY_MASK) && !(code == 95 && modifiers == 0)){
				this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: code, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_UP, code: code, modifiers: modifiers});
			}
		}
	},
	
	CookedKeyboardInputHandler : function(eventType, code, modifiers, guestos, browser, browserVersion) {
		if(eventType == AjaxViewer.KEY_DOWN || eventType == AjaxViewer.KEY_UP) {
			
			// special handling for Alt + Ctrl + Ins, convert it into Alt-Ctrl-Del
			if(code == AjaxViewer.JS_KEY_INSERT) {
				if((modifiers & AjaxViewer.ALT_KEY_MASK) != 0 && (modifiers & AjaxViewer.CTRL_KEY_MASK) != 0) {
					this.mappedInput.push({type : eventType, code: 0xffff, modifiers: modifiers});
					return;
				}
			}
			
			var X11Keysym = code;
			if(this.jsX11KeysymMap[code] != undefined) {
				X11Keysym = this.jsX11KeysymMap[code];
				if(typeof this.jsX11KeysymMap[code] == "boolean") {
					return;
				} else if($.isArray(X11Keysym)) {
					for(var i = 0; i < X11Keysym.length; i++) {
						if(this.isConditionalEntryMatched(eventType, code, modifiers, X11Keysym[i], guestos, browser, browserVersion)) {
							this.mappedInput.push(X11Keysym[i]);
						}
					}
				} else {
					this.mappedInput.push({type : eventType, code: X11Keysym, modifiers: modifiers});
				}
			} else {
				if((modifiers & (AjaxViewer.CTRL_KEY_MASK | AjaxViewer.ALT_KEY_MASK)) != 0) {
					this.mappedInput.push({type : eventType, code: X11Keysym, modifiers: modifiers});
				}
			}

			// special handling for ALT/CTRL key
			if(eventType == AjaxViewer.KEY_UP && (code == AjaxViewer.JS_KEY_ALT || code == code == AjaxViewer.JS_KEY_CTRL)) {
				this.mappedInput.push({type : eventType, code: this.jsX11KeysymMap[code], modifiers: modifiers});
			}
		} else if(eventType == AjaxViewer.KEY_PRESS) {
			// special handling for * and + key on number pad
			if(code == AjaxViewer.JS_NUMPAD_MULTIPLY) {
				this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SHIFT, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: 42, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_UP, code: 42, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SHIFT, modifiers: modifiers});
				return;
			}
			
			if(code == AjaxViewer.JS_NUMPAD_PLUS) {
				this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SHIFT, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: 43, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_UP, code: 43, modifiers: modifiers});
				this.mappedInput.push({type : AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SHIFT, modifiers: modifiers});
				return;
			}
			
			// ENTER/BACKSPACE key should already have been sent through KEY DOWN/KEY UP event
			if(code == AjaxViewer.JS_KEY_ENTER || code == AjaxViewer.JS_KEY_BACKSPACE)
				return;
			if(code > 0) {
				var X11Keysym = code;
				X11Keysym = this.jsKeyPressX11KeysymMap[code];
				if(X11Keysym) {
					if($.isArray(X11Keysym)) {
						for(var i = 0; i < X11Keysym.length; i++) {
							if(this.isConditionalEntryMatched(eventType, code, modifiers, X11Keysym[i], guestos, browser))
								this.mappedInput.push(X11Keysym[i]);
						}
					} else {
						this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: X11Keysym, modifiers: modifiers});
						this.mappedInput.push({type : AjaxViewer.KEY_UP, code: X11Keysym, modifiers: modifiers});
					}
				} else {
					// if there is no mappting entry, use the JS keypress code directly
					this.mappedInput.push({type : AjaxViewer.KEY_DOWN, code: code, modifiers: modifiers});
					this.mappedInput.push({type : AjaxViewer.KEY_UP, code: code, modifiers: modifiers});
				}
			}
		}
	},
	
	inputFeed : function(eventType, code, modifiers, guestos, browser, browserVersion) {
		if(this.keyboardType == KeyboardMapper.KEYBOARD_TYPE_RAW)
			this.RawkeyboardInputHandler(eventType, code, modifiers, guestos, browser, browserVersion);
		else
			this.CookedKeyboardInputHandler(eventType, code, modifiers, guestos, browser, browserVersion);
	},
	
	getMappedInput : function() {
		var mappedInput = this.mappedInput;
		this.mappedInput = [];
		return mappedInput;
	},
	
	isConditionalEntryMatched : function(eventType, code, modifiers, entry, guestos, browser, browserVersion) {
		if(eventType == AjaxViewer.KEY_DOWN || eventType == AjaxViewer.KEY_UP) {
			// for KeyDown/KeyUp events, we require that the type in entry should match with
			// the real input
			if(entry.type != eventType)
				return false;
		}
		
		// check conditional match
		if(entry.shift != undefined) {
			var shift = ((modifiers & AjaxViewer.SHIFT_KEY_MASK) != 0 ? true : false); 
			if(entry.shift ^ shift)
				return false;
		}

                // Consider Alt+Ctrl as AltGr
                if(entry.altgr != undefined){
                        var altgr = ((modifiers & AjaxViewer.ALT_KEY_MASK) && (modifiers & AjaxViewer.CTRL_KEY_MASK)) != 0 ? true : false;
                        if(entry.altgr ^ altgr)
                                return false;
                }

		if(entry.guestos != undefined) {
			if(entry.guestos != guestos)
				return false;
		}
		
		if(entry.browser != undefined) {
			if(entry.browser != browser)
				return false;
		}
		
		if(entry.browserVersion != undefined) {
			if(entry.browserVersion != browserVersion)
				return false;
		}
		
		return true;
	},
	
	isModifierInput : function(code) {
		return $.inArray(code, [AjaxViewer.ALT_KEY_MASK, AjaxViewer.SHIFT_KEY_MASK, AjaxViewer.CTRL_KEY_MASK, AjaxViewer.META_KEY_MASK]) >= 0;
	}
};

/////////////////////////////////////////////////////////////////////////////
// class AjaxViewer
//
function AjaxViewer(panelId, imageUrl, updateUrl, locale, guestos, tileMap, width, height, tileWidth, tileHeight) {
	// logging is disabled by default so that it won't have negative impact on performance
	// however, a back door key-sequence can trigger to open the logger window, it is designed to help
	// trouble-shooting
	g_logger = new Logger();
	
	// g_logger.enable(true);
	// g_logger.open();
	
	var ajaxViewer = this;
	this.imageLoaded = false;
	this.fullImage = true;
	this.imgUrl = imageUrl;
	this.img = new Image();
	$(this.img).attr('src', imageUrl).load(function() {
		ajaxViewer.imageLoaded = true;
	});
	
	this.updateUrl = updateUrl;
	this.tileMap = tileMap;
	this.guestos = guestos;
	this.dirty = true;
	this.width = width;
	this.height = height;
	this.tileWidth = tileWidth;
	this.tileHeight = tileHeight;
	this.maxTileZIndex = 1;

	if (locale == AjaxViewer.KEYBOARD_TYPE_UK_ENGLISH || locale == AjaxViewer.KEYBOARD_TYPE_JAPANESE || locale == AjaxViewer.KEYBOARD_TYPE_FRENCH)
		this.currentKeyboard = locale;
	else
		this.currentKeyboard = AjaxViewer.KEYBOARD_TYPE_ENGLISH;

	this.keyboardMappers = [];
	
	this.timer = 0;
	this.eventQueue = [];
	this.sendingEventInProgress = false;
	
	this.lastClickEvent = { x: 0, y: 0, button: 0, modifiers: 0, time: new Date().getTime() };
	
	if(window.onStatusNotify == undefined)
		window.onStatusNotify = function(status) {};
	
	this.panel = this.generateCanvas(panelId, width, height, tileWidth, tileHeight);
//	this.setupKeyboardTranslationle();
	this.setupKeyboardTranslationTable(this.keyboardMappers);
	this.setupUIController();
}

// client event types
AjaxViewer.MOUSE_MOVE = 1;
AjaxViewer.MOUSE_DOWN = 2;
AjaxViewer.MOUSE_UP = 3;
AjaxViewer.KEY_PRESS = 4;
AjaxViewer.KEY_DOWN = 5;
AjaxViewer.KEY_UP = 6;
AjaxViewer.EVENT_BAG = 7;
AjaxViewer.MOUSE_DBLCLK = 8;

// use java AWT key modifier masks 
AjaxViewer.SHIFT_KEY_MASK = 64;
AjaxViewer.CTRL_KEY_MASK = 128;
AjaxViewer.META_KEY_MASK = 256;
AjaxViewer.ALT_KEY_MASK = 512;
AjaxViewer.LEFT_SHIFT_MASK = 1024;
AjaxViewer.LEFT_CTRL_MASK = 2048;
AjaxViewer.LEFT_ALT_MASK = 4096;

AjaxViewer.EVENT_QUEUE_MOUSE_EVENT = 1;
AjaxViewer.EVENT_QUEUE_KEYBOARD_EVENT = 2;

AjaxViewer.STATUS_RECEIVING = 1;
AjaxViewer.STATUS_RECEIVED = 2;
AjaxViewer.STATUS_SENDING = 3;
AjaxViewer.STATUS_SENT = 4;

AjaxViewer.KEYBOARD_TYPE_ENGLISH = "us";
AjaxViewer.KEYBOARD_TYPE_UK_ENGLISH = "uk";
AjaxViewer.KEYBOARD_TYPE_JAPANESE = "jp";
AjaxViewer.KEYBOARD_TYPE_FRENCH = "fr";

AjaxViewer.JS_KEY_BACKSPACE = 8;
AjaxViewer.JS_KEY_TAB = 9;
AjaxViewer.JS_KEY_ENTER = 13;
AjaxViewer.JS_KEY_SHIFT = 16;
AjaxViewer.JS_KEY_CTRL = 17;
AjaxViewer.JS_KEY_ALT = 18;
AjaxViewer.JS_KEY_PAUSE = 19;
AjaxViewer.JS_KEY_CAPSLOCK = 20;
AjaxViewer.JS_KEY_ESCAPE = 27;
AjaxViewer.JS_KEY_PAGEUP = 33;
AjaxViewer.JS_KEY_PAGEDOWN = 34;
AjaxViewer.JS_KEY_END = 35;
AjaxViewer.JS_KEY_HOME = 36;
AjaxViewer.JS_KEY_LEFT = 37;
AjaxViewer.JS_KEY_UP = 38;
AjaxViewer.JS_KEY_RIGHT = 39;
AjaxViewer.JS_KEY_DOWN = 40;
AjaxViewer.JS_KEY_INSERT = 45;
AjaxViewer.JS_KEY_DELETE = 46;
AjaxViewer.JS_KEY_LEFT_WINDOW_KEY = 91;
AjaxViewer.JS_KEY_RIGHT_WINDOW_KEY = 92;
AjaxViewer.JS_KEY_SELECT_KEY = 93;
AjaxViewer.JS_KEY_NUMPAD0 = 96;
AjaxViewer.JS_KEY_NUMPAD1 = 97;
AjaxViewer.JS_KEY_NUMPAD2 = 98;
AjaxViewer.JS_KEY_NUMPAD3 = 99;
AjaxViewer.JS_KEY_NUMPAD4 = 100;
AjaxViewer.JS_KEY_NUMPAD5 = 101;
AjaxViewer.JS_KEY_NUMPAD6 = 102;
AjaxViewer.JS_KEY_NUMPAD7 = 103;
AjaxViewer.JS_KEY_NUMPAD8 = 104;
AjaxViewer.JS_KEY_NUMPAD9 = 105;
AjaxViewer.JS_KEY_MULTIPLY = 106;
AjaxViewer.JS_KEY_ADD = 107;
AjaxViewer.JS_KEY_SUBSTRACT = 109;
AjaxViewer.JS_KEY_DECIMAL_POINT = 110;
AjaxViewer.JS_KEY_DIVIDE = 111;
AjaxViewer.JS_KEY_F1 = 112;
AjaxViewer.JS_KEY_F2 = 113;
AjaxViewer.JS_KEY_F3 = 114;
AjaxViewer.JS_KEY_F4 = 115;
AjaxViewer.JS_KEY_F5 = 116;
AjaxViewer.JS_KEY_F6 = 117;
AjaxViewer.JS_KEY_F7 = 118;
AjaxViewer.JS_KEY_F8 = 119;
AjaxViewer.JS_KEY_F9 = 120;
AjaxViewer.JS_KEY_F10 = 121;
AjaxViewer.JS_KEY_F11 = 122;
AjaxViewer.JS_KEY_F12 = 123;
AjaxViewer.JS_KEY_NUMLOCK = 144;
AjaxViewer.JS_KEY_SCROLLLOCK = 145;
AjaxViewer.JS_KEY_SEMI_COLON = 186;			// ;
AjaxViewer.JS_KEY_EQUAL_SIGN = 187;			// =
AjaxViewer.JS_KEY_COMMA = 188;				// ,
AjaxViewer.JS_KEY_DASH = 189;				// -
AjaxViewer.JS_KEY_PERIOD = 190;				// .
AjaxViewer.JS_KEY_FORWARD_SLASH = 191;		// /
AjaxViewer.JS_KEY_GRAVE_ACCENT = 192;		// `				
AjaxViewer.JS_KEY_OPEN_BRACKET = 219;		// [
AjaxViewer.JS_KEY_BACK_SLASH = 220;			// \
AjaxViewer.JS_KEY_CLOSE_BRACKET = 221;		// ]
AjaxViewer.JS_KEY_SINGLE_QUOTE = 222;		// '
AjaxViewer.JS_NUMPAD_PLUS = 43;
AjaxViewer.JS_NUMPAD_MULTIPLY = 42;
AjaxViewer.JS_KEY_NUM8 = 56;

// keycode from Japanese keyboard
AjaxViewer.JS_KEY_JP_COLON = 222;			// :* on JP keyboard
AjaxViewer.JS_KEY_JP_CLOSE_BRACKET = 220;	// [{ on JP keyboard
AjaxViewer.JS_KEY_JP_AT_SIGN = 219;			// @` on JP keyboard
AjaxViewer.JS_KEY_JP_OPEN_BRACKET = 221;	// [{ on JP keyboard
AjaxViewer.JS_KEY_JP_BACK_SLASH = 193;		// \| on JP keyboard
AjaxViewer.JS_KEY_JP_YEN_MARK = 255;

AjaxViewer.JS_KEY_JP_EQUAL = 109;			// -= ON JP keyboard
AjaxViewer.JS_KEY_JP_ACUTE = 107;			// ^~ on JP keyboard

// X11 keysym definitions
AjaxViewer.X11_KEY_CAPSLOCK = 0xffe5;
AjaxViewer.X11_KEY_BACKSPACE = 0xff08;
AjaxViewer.X11_KEY_TAB = 0xff09;
AjaxViewer.X11_KEY_ENTER = 0xff0d;
AjaxViewer.X11_KEY_ESCAPE = 0xff1b;
AjaxViewer.X11_KEY_INSERT = 0xff63;
AjaxViewer.X11_KEY_DELETE = 0xffff;
AjaxViewer.X11_KEY_HOME = 0xff50;
AjaxViewer.X11_KEY_END = 0xff57;
AjaxViewer.X11_KEY_PAGEUP = 0xff55;
AjaxViewer.X11_KEY_PAGEDOWN = 0xff56;
AjaxViewer.X11_KEY_LEFT = 0xff51;
AjaxViewer.X11_KEY_UP = 0xff52;
AjaxViewer.X11_KEY_RIGHT = 0xff53;
AjaxViewer.X11_KEY_DOWN = 0xff54;
AjaxViewer.X11_KEY_F1 = 0xffbe;
AjaxViewer.X11_KEY_F2 = 0xffbf;
AjaxViewer.X11_KEY_F3 = 0xffc0;
AjaxViewer.X11_KEY_F4 = 0xffc1;
AjaxViewer.X11_KEY_F5 = 0xffc2;
AjaxViewer.X11_KEY_F6 = 0xffc3;
AjaxViewer.X11_KEY_F7 = 0xffc4;
AjaxViewer.X11_KEY_F8 = 0xffc5;
AjaxViewer.X11_KEY_F9 = 0xffc6;
AjaxViewer.X11_KEY_F10 = 0xffc7;
AjaxViewer.X11_KEY_F11 = 0xffc8;
AjaxViewer.X11_KEY_F12 = 0xffc9;
AjaxViewer.X11_KEY_SHIFT = 0xffe1;
AjaxViewer.X11_KEY_CTRL = 0xffe3;
AjaxViewer.X11_KEY_ALT = 0xffe9;
AjaxViewer.X11_KEY_SELECT_KEY = 0xff67;
AjaxViewer.X11_KEY_GRAVE_ACCENT = 0x60;
AjaxViewer.X11_KEY_SUBSTRACT = 0x2d;
AjaxViewer.X11_KEY_ADD = 0x2b;
AjaxViewer.X11_KEY_OPEN_BRACKET = 0x5b;
AjaxViewer.X11_KEY_CLOSE_BRACKET = 0x5d;
AjaxViewer.X11_KEY_BACK_SLASH = 0x7c;
AjaxViewer.X11_KEY_REVERSE_SOLIUS = 0x5c;			// another back slash (back slash on JP keyboard)
AjaxViewer.X11_KEY_SINGLE_QUOTE = 0x22;
AjaxViewer.X11_KEY_COMMA = 0x3c;
AjaxViewer.X11_KEY_PERIOD = 0x3e;
AjaxViewer.X11_KEY_FORWARD_SLASH = 0x3f;
AjaxViewer.X11_KEY_DASH = 0x2d;
AjaxViewer.X11_KEY_COLON = 0x3a;
AjaxViewer.X11_KEY_SEMI_COLON = 0x3b;
AjaxViewer.X11_KEY_NUMPAD0 = 0x30;
AjaxViewer.X11_KEY_NUMPAD1 = 0x31;
AjaxViewer.X11_KEY_NUMPAD2 = 0x32;
AjaxViewer.X11_KEY_NUMPAD3 = 0x33;
AjaxViewer.X11_KEY_NUMPAD4 = 0x34;
AjaxViewer.X11_KEY_NUMPAD5 = 0x35;
AjaxViewer.X11_KEY_NUMPAD6 = 0x36;
AjaxViewer.X11_KEY_NUMPAD7 = 0x37;
AjaxViewer.X11_KEY_NUMPAD8 = 0x38;
AjaxViewer.X11_KEY_NUMPAD9 = 0x39;
AjaxViewer.X11_KEY_DECIMAL_POINT = 0x2e;
AjaxViewer.X11_KEY_DIVIDE = 0x3f;
AjaxViewer.X11_KEY_TILDE = 0x7e;				// ~
AjaxViewer.X11_KEY_CIRCUMFLEX_ACCENT = 0x5e;	// ^
AjaxViewer.X11_KEY_YEN_MARK = 0xa5;				// Japanese YEN mark
AjaxViewer.X11_KEY_ASTERISK = 0x2a;

AjaxViewer.getEventName = function(type) {
	switch(type) {
	case AjaxViewer.MOUSE_MOVE :
		return "MOUSE_MOVE";
		
	case AjaxViewer.MOUSE_DOWN :
		return "MOUSE_DOWN";
		
	case AjaxViewer.MOUSE_UP :
		return "MOUSE_UP";
		
	case AjaxViewer.KEY_PRESS :
		return "KEY_PRESS";
		
	case AjaxViewer.KEY_DOWN :
		return "KEY_DOWN";
		
	case AjaxViewer.KEY_UP :
		return "KEY_UP";
		
	case AjaxViewer.EVENT_BAG :
		return "EVENT_BAG";
		
	case AjaxViewer.MOUSE_DBLCLK :
		return "MOUSE_DBLCLK";
	}
	
	return "N/A";
};

AjaxViewer.prototype = {
	setDirty: function(value) {
		this.dirty = value;
	},
	
	isDirty: function() {
		return this.dirty;
	},
	
	isImageLoaded: function() {
		return this.imageLoaded;
	},
	
	refresh: function(imageUrl, tileMap, fullImage) {
		var ajaxViewer = this;
		var img = $(this.img); 
		this.fullImage = fullImage;
		this.imgUrl=imageUrl;

		img.attr('src', imageUrl).load(function() {
			ajaxViewer.imageLoaded = true;
		});
		this.tileMap = tileMap;
	},
	
	resize: function(panelId, width, height, tileWidth, tileHeight) {
		$(".canvas_tile", document.body).each(function() {
			$(this).remove();
		});
		$("table", $("#" + panelId)).remove();
		
		this.width = width;
		this.height = height;
		this.tileWidth = tileWidth;
		this.tileHeight = tileHeight;
		this.panel = this.generateCanvas(panelId, width, height, tileWidth, tileHeight);
	},
	
	start: function() {
		var ajaxViewer = this;
		this.timer = setInterval(function() { ajaxViewer.heartbeat(); }, 50);
		
		$(document).bind("ajaxError", function(event, XMLHttpRequest, ajaxOptions, thrownError) {
			ajaxViewer.onAjaxError(event, XMLHttpRequest, ajaxOptions, thrownError);
		});
		
		this.eventQueue = [];	// reset event queue
		this.sendingEventInProgress = false;
		ajaxViewer.installMouseHook();
		ajaxViewer.installKeyboardHook();
		ajaxViewer.panel.parent().focus();

		$(window).bind("resize", function() {
			ajaxViewer.onWindowResize();
		});
	},
	
	stop: function() {
		clearInterval(this.timer);
		this.deleteCanvas();

		this.uninstallMouseHook();
		this.uninstallKeyboardHook();	
		this.eventQueue = [];
		this.sendingEventInProgress = false;

		$(document).unbind("ajaxError");
		$(window).unbind("resize");
	},
	
	sendMouseEvent: function(event, x, y, whichButton, modifiers) {
		this.eventQueue.push({
			type: AjaxViewer.EVENT_QUEUE_MOUSE_EVENT,
			event: event,
			x: x,
			y: y,
			code: whichButton,
			modifiers: modifiers
		});
		this.checkEventQueue();
	},
//NEW  insert the keyboard tables file here
// ajaxKeys.js

	setupKeyboardTranslationTable : function() {
		this.keyboardMappers = [];
		
		var mapper = new KeyboardMapper();
		this.keyboardMappers[AjaxViewer.KEYBOARD_TYPE_ENGLISH] = mapper;
		mapper.setKeyboardType(KeyboardMapper.KEYBOARD_TYPE_COOKED);

		var mapper = new KeyboardMapper();
		this.keyboardMappers[AjaxViewer.KEYBOARD_TYPE_UK_ENGLISH] = mapper;
		mapper.setKeyboardType(KeyboardMapper.KEYBOARD_TYPE_UK);

		mapper = new KeyboardMapper();
		this.keyboardMappers[AjaxViewer.KEYBOARD_TYPE_JAPANESE] = mapper;
		mapper.setKeyboardType(KeyboardMapper.KEYBOARD_TYPE_RAW);

                var mapper = new KeyboardMapper();
                this.keyboardMappers[AjaxViewer.KEYBOARD_TYPE_FRENCH] = mapper;
                mapper.setKeyboardType(KeyboardMapper.KEYBOARD_TYPE_FR);

		// JP keyboard plugged in a English host OS
/*		
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_COLON] = AjaxViewer.X11_KEY_COLON;
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_CLOSE_BRACKET] = AjaxViewer.X11_KEY_CLOSE_BRACKET;
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_AT_SIGN] = AjaxViewer.X11_KEY_GRAVE_ACCENT;
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_OPEN_BRACKET] = AjaxViewer.X11_KEY_OPEN_BRACKET;
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_BACK_SLASH] = AjaxViewer.X11_KEY_REVERSE_SOLIUS;		// X11 REVERSE SOLIDUS
		mapper.jsX11KeysymMap[AjaxViewer.JS_KEY_JP_YEN_MARK] = AjaxViewer.X11_KEY_YEN_MARK;				// X11 YEN SIGN
		mapper.jsKeyPressX11KeysymMap[61] = [
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_CIRCUMFLEX_ACCENT, modifiers: 0 },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_CIRCUMFLEX_ACCENT, modifiers: 0 },
    	];
		
		mapper.jsKeyPressX11KeysymMap[43] = [
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SHIFT, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SHIFT, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_TILDE, modifiers: 0, shift: true },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_TILDE, modifiers: 0, shift: true }
        ];
*/

/* Old
		// JP keyboard plugged in a Japanese host OS
		mapper.jsX11KeysymMap[222] = AjaxViewer.X11_KEY_CIRCUMFLEX_ACCENT;
		mapper.jsX11KeysymMap[220] = AjaxViewer.X11_KEY_YEN_MARK;
		mapper.jsX11KeysymMap[219] = AjaxViewer.X11_KEY_OPEN_BRACKET;
		mapper.jsX11KeysymMap[221] = AjaxViewer.X11_KEY_CLOSE_BRACKET;
		mapper.jsX11KeysymMap[59] = AjaxViewer.X11_KEY_COLON;					// Firefox
		mapper.jsX11KeysymMap[186] = AjaxViewer.X11_KEY_COLON;					// Chrome
		mapper.jsX11KeysymMap[226] = AjaxViewer.X11_KEY_REVERSE_SOLIUS;			// \| key left to right SHIFT on JP keyboard
		mapper.jsX11KeysymMap[240] = [
      	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_CAPSLOCK, modifiers: 0 },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_CAPSLOCK, modifiers: 0 },
    	];
			
		// for keycode 107, keypress 59
		mapper.jsKeyPressX11KeysymMap[59] = [
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SEMI_COLON, modifiers: 0 },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SEMI_COLON, modifiers: 0 },
    	];
		
		// for keycode 107, keypress 43
		mapper.jsKeyPressX11KeysymMap[43] = [
     	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_SHIFT, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: false },
    	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_SHIFT, modifiers: 0, shift: false },
       	    {type: AjaxViewer.KEY_DOWN, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: true },
       	    {type: AjaxViewer.KEY_UP, code: AjaxViewer.X11_KEY_ADD, modifiers: 0, shift: true },
        ];

*/
	// create the mapping table based on the tables input
	if (keyboardTables != undefined ) {

		for(var i = 0; i < keyboardTables.length; i++) {
			var mappingTbl = keyboardTables[i];
			var keyboardType = mappingTbl.keyboardType;
			var mappings = mappingTbl.mappingTable;
			var x11Maps = mappings.X11;
			for (var j = 0; j < x11Maps.length; j++) {
				var code = x11Maps[j].keycode;
				var mappedEntry = x11Maps[j].entry;
				if(x11Maps[j].guestos == undefined || x11Maps[j].guestos == this.guestos) {
					this.keyboardMappers[keyboardType].jsX11KeysymMap[code] = mappedEntry;
				}
			}
			var keyPressMaps = mappings.keyPress;
			for (var j = 0; j < keyPressMaps.length; j++) {
				var code = keyPressMaps[j].keycode;
				var mappedEntry = keyPressMaps[j].entry;
				if(keyPressMaps[j].guestos == undefined || keyPressMaps[j].guestos == this.guestos) {
					this.keyboardMappers[keyboardType].jsKeyPressX11KeysymMap[code] = mappedEntry;
				}
			}
		}
	}

	}, // end of the setupKeyboardTranslationTable function
	
	getCurrentKeyboardMapper : function() {
		return this.keyboardMappers[this.currentKeyboard];
	},
	
	setupUIController : function() {
		var ajaxViewer = this;
		var pullDownElement = $("#toolbar").find(".pulldown");
		pullDownElement.hover(
			function(e) {
				var subMenu = pullDownElement.find("ul");
				var offset = subMenu.parent().offset();
				subMenu.css("left", offset.left);
			
				$("li.current").removeClass("current");
				$("li:has(a[cmd$=" + ajaxViewer.currentKeyboard + "])", subMenu).addClass("current");
				subMenu.css("z-index", "" + ajaxViewer.maxTileZIndex + 1).show();
				return false;
			},
			
			function(e) {
				pullDownElement.find("ul").hide();
				return false;
			}
		);

		$("[cmd]", "#toolbar").each(function(i, val) {
			$(val).click(function(e) {
				var cmd = $(e.target).attr("cmd");
				if(cmd)
					ajaxViewer.onCommand(cmd); 
				else {
					var cmdLink = $(e.target).closest("a");
					
					if(cmdLink.attr("cmd")) {
						var cmd = cmdLink.attr("cmd");
						ajaxViewer.onCommand(cmd);
					}
				}
			});
		});
	},
	
	onCommand : function(cmd) {
		if(cmd == "keyboard_jp") {
			$("#toolbar").find(".pulldown").find("ul").hide();
			this.currentKeyboard = AjaxViewer.KEYBOARD_TYPE_JAPANESE;
		} else if(cmd == "keyboard_us") {
			$("#toolbar").find(".pulldown").find("ul").hide();
			this.currentKeyboard = AjaxViewer.KEYBOARD_TYPE_ENGLISH;
		} else if(cmd == "keyboard_uk") {
			$("#toolbar").find(".pulldown").find("ul").hide();
			this.currentKeyboard = AjaxViewer.KEYBOARD_TYPE_UK_ENGLISH;
                } else if(cmd == "keyboard_fr") {
                        $("#toolbar").find(".pulldown").find("ul").hide();
                        this.currentKeyboard = AjaxViewer.KEYBOARD_TYPE_FRENCH;
		} else if(cmd == "sendCtrlAltDel") {
			this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, 0xffe9, 0);		// X11 Alt
			this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, 0xffe3, 0);		// X11 Ctrl
			this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, 0xffff, 0);		// X11 Del
			this.sendKeyboardEvent(AjaxViewer.KEY_UP, 0xffff, 0);
			this.sendKeyboardEvent(AjaxViewer.KEY_UP, 0xffe3, 0);
			this.sendKeyboardEvent(AjaxViewer.KEY_UP, 0xffe9, 0);
		} else if(cmd == "sendCtrlEsc") {
			this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, 0xffe3, 0);		// X11 Ctrl
			this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, 0xff1b, 0);		// X11 ESC
			this.sendKeyboardEvent(AjaxViewer.KEY_UP, 0xff1b, 0);
			this.sendKeyboardEvent(AjaxViewer.KEY_UP, 0xffe3, 0);
		} else if(cmd == "toggle_logwin") {
			if(!g_logger.isOpen()) {
				g_logger.enable(true);
				g_logger.open();
				g_logger.log(Logger.LEVEL_SYS, "Accept languages: " + acceptLanguages + ", current language: " + getCurrentLanguage());
			} else {
				g_logger.close();
			}
		}
	},
	
	sendKeyboardEvent: function(event, code, modifiers) {
		// back door to open logger window - CTRL-ATL-SHIFT+SPACE
		if(code == 32 && 
			(modifiers & AjaxViewer.SHIFT_KEY_MASK | AjaxViewer.CTRL_KEY_MASK | AjaxViewer.ALT_KEY_MASK) == (AjaxViewer.SHIFT_KEY_MASK | AjaxViewer.CTRL_KEY_MASK | AjaxViewer.ALT_KEY_MASK)) {
			
			if(!g_logger.isOpen()) {
				g_logger.enable(true);
				g_logger.open();
				g_logger.log(Logger.LEVEL_SYS, "Accept languages: " + acceptLanguages + ", current language: " + getCurrentLanguage());
			} else {
				g_logger.close();
			}
		}
			
		var len;
		g_logger.log(Logger.LEVEL_INFO, "Keyboard event: " + AjaxViewer.getEventName(event) + ", code: " + code + ", modifiers: " + modifiers + ', char: ' + String.fromCharCode(code));
		this.eventQueue.push({
			type: AjaxViewer.EVENT_QUEUE_KEYBOARD_EVENT,
			event: event,
			code: code,
			modifiers: modifiers
		});

		if(event != AjaxViewer.KEY_DOWN)
			this.checkEventQueue();
	},
	
	aggregateEvents: function() {
		var ajaxViewer = this;
		var aggratedQueue = [];
		
		var aggregating = false;
		var mouseX;
		var mouseY;
		$.each(ajaxViewer.eventQueue, function(index, item) {
			if(item.type != AjaxViewer.EVENT_QUEUE_MOUSE_EVENT) {
				aggratedQueue.push(item);
			} else {
				if(!aggregating) {
					if(item.event == AjaxViewer.MOUSE_MOVE) {
						aggregating = true;
						mouseX = item.x;
						mouseY = item.y;
					} else {
						aggratedQueue.push(item);
					}
				} else {
					if(item.event == AjaxViewer.MOUSE_MOVE) {
						// continue to aggregate mouse move event
						mouseX = item.x;
						mouseY = item.y;
					} else {
						aggratedQueue.push({
							type: AjaxViewer.EVENT_QUEUE_MOUSE_EVENT,
							event: AjaxViewer.MOUSE_MOVE,
							x: mouseX,
							y: mouseY,
							code: 0,
							modifiers: 0
						});
						aggregating = false;
						
						aggratedQueue.push(item);
					}
				}
			}
		});
		
		if(aggregating) {
			aggratedQueue.push({
				type: AjaxViewer.EVENT_QUEUE_MOUSE_EVENT,
				event: AjaxViewer.MOUSE_MOVE,
				x: mouseX,
				y: mouseY,
				code: 0,
				modifiers: 0
			});
		}
		
		this.eventQueue = aggratedQueue; 
	},
	
	checkEventQueue: function() {
		var ajaxViewer = this;
		
		if(!this.sendingEventInProgress && this.eventQueue.length > 0) {
			var sb = new StringBuilder();
			sb.append(""+this.eventQueue.length).append("|");
			$.each(this.eventQueue, function() {
				var item = this;
				if(item.type == AjaxViewer.EVENT_QUEUE_MOUSE_EVENT) {
					sb.append(""+item.type).append("|");
					sb.append(""+item.event).append("|");
					sb.append(""+item.x).append("|");
					sb.append(""+item.y).append("|");
					sb.append(""+item.code).append("|");
					sb.append(""+item.modifiers).append("|");
				} else {
					sb.append(""+item.type).append("|");
					sb.append(""+item.event).append("|");
					sb.append(""+item.code).append("|");
					sb.append(""+item.modifiers).append("|");
				}
			});
			this.eventQueue.length = 0;
			
			var url = ajaxViewer.updateUrl + "&event=" + AjaxViewer.EVENT_BAG;
			
			g_logger.log(Logger.LEVEL_TRACE, "Posting client event " + sb.toString() + "...");
			
			ajaxViewer.sendingEventInProgress = true;
			window.onStatusNotify(AjaxViewer.STATUS_SENDING);
			$.post(url, {data: sb.toString()}, function(data, textStatus) {
				g_logger.log(Logger.LEVEL_TRACE, "Client event " + sb.toString() + " is posted");
				
				ajaxViewer.sendingEventInProgress = false;
				window.onStatusNotify(AjaxViewer.STATUS_SENT);
				
				ajaxViewer.checkUpdate();
			}, 'html');
		}
	},
	
	onAjaxError: function(event, XMLHttpRequest, ajaxOptions, thrownError) {
		if(window.onClientError != undefined && jQuery.isFunction(window.onClientError)) {
			window.onClientError();
		}
	},
	
	onWindowResize: function() {
		var offset = this.panel.offset();
		
		var row = $('tr:first', this.panel);
		var cell = $('td:first', row);
		var tile = this.getTile(cell, 'tile');
		
		var tileOffset = tile.offset();
		var deltaX = offset.left - tileOffset.left;
		var deltaY = offset.top - tileOffset.top;
		
		if(deltaX != 0 || deltaY != 0) {
			$(".canvas_tile").each(function() {
				var offsetFrom = $(this).offset();
				$(this).css('left', offsetFrom.left + deltaX).css('top', offsetFrom.top + deltaY);
			});
		}
	},
	
	deleteCanvas: function() {
		$('.canvas_tile', $(document.body)).each(function() {
			$(this).remove();
		});
	},
	
	generateCanvas: function(wrapperDivId, width, height, tileWidth, tileHeight) {
		var canvasParent = $('#' + wrapperDivId);
		canvasParent.width(width);
		canvasParent.height(height);
		
		if(window.onCanvasSizeChange != undefined && jQuery.isFunction(window.onCanvasSizeChange))
			window.onCanvasSizeChange(width, height);
		
		var tableDef = '<table cellpadding="0px" cellspacing="0px">\r\n';
		var i = 0;
		var j = 0;
		for(i = 0; i < Math.ceil((height + tileHeight - 1) / tileHeight); i++) {
			var rowHeight = Math.min(height - i*tileHeight, tileHeight);
			tableDef += '<tr style="height:' + rowHeight + 'px">\r\n';
			
			for(j = 0; j < Math.ceil((width + tileWidth - 1) / tileWidth); j++) {
				var colWidth = Math.min(width - j*tileWidth, tileWidth);
				tableDef += '<td width="' + colWidth + 'px"></td>\r\n';
			}
			tableDef += '</tr>\r\n';
		}
		tableDef += '</table>\r\n';
		
		return $(tableDef).appendTo(canvasParent);
	},
	
	getTile: function(cell, name) {
		var clonedDiv = cell.data(name);
		if(!clonedDiv) {
			var offset = cell.offset();
			var divDef = "<div class=\"canvas_tile\" style=\"z-index:1;position:absolute;overflow:hidden;width:" + cell.width() + "px;height:" 
				+ cell.height() + "px;left:" + offset.left + "px;top:" + offset.top+"px\"></div>";
			
			clonedDiv = $(divDef).appendTo($(document.body));
			cell.data(name, clonedDiv);
		}
		
		return clonedDiv;
	},
	
	initCell: function(cell) {
		if(!cell.data("init")) {
			cell.data("init", true);
			
			cell.data("current", 0);
			this.getTile(cell, "tile2");
			this.getTile(cell, "tile");
		}
	},
	
	displayCell: function(cell, bg) {
		var div;
		var divPrev;
		if(!cell.data("current")) {
			cell.data("current", 1);
			
			divPrev = this.getTile(cell, "tile");
			div = this.getTile(cell, "tile2");
		} else {
			cell.data("current", 0);
			divPrev = this.getTile(cell, "tile2");
			div = this.getTile(cell, "tile");
		}
		
		var zIndex = parseInt(divPrev.css("z-index")) + 1;
		this.maxTileZIndex = Math.max(this.maxTileZIndex, zIndex);
		div.css("z-index", zIndex);
		div.css("background", bg);
	},
	
	updateTile: function() {
		if(this.dirty) {
			var ajaxViewer = this;
			var tileWidth = this.tileWidth;
			var tileHeight = this.tileHeight;
			var imgUrl = this.imgUrl;
			var panel = this.panel;
			
			if(this.fullImage) {
				$.each(this.tileMap, function() {
					var i = $(this)[0];
					var j = $(this)[1];
					var row = $("TR:eq("+i+")", panel);
					var cell = $("TD:eq("+j+")", row);
					var attr = "url(" + imgUrl + ") -"+j*tileWidth+"px -"+i*tileHeight + "px";
					
					ajaxViewer.initCell(cell);
					ajaxViewer.displayCell(cell, attr);
				});
			} else {
				$.each(this.tileMap, function(index) {
					var i = $(this)[0];
					var j = $(this)[1];
					var offset = index*tileWidth;
					var attr = "url(" + imgUrl + ") no-repeat -"+offset+"px 0px";
					var row = $("TR:eq("+i+")", panel);
					var cell = $("TD:eq("+j+")", row);
					
					ajaxViewer.initCell(cell);
					ajaxViewer.displayCell(cell, attr);
				});
			}
			
			this.dirty = false;
		}
	},
	
	heartbeat: function() {
		this.checkEventQueue();
		this.checkUpdate();
	},
	
	checkUpdate: function() {
		if(!this.isDirty())
			return;
		
		if(this.isImageLoaded()) {
			this.updateTile();
			var url = this.updateUrl;
			var ajaxViewer = this;

			window.onStatusNotify(AjaxViewer.STATUS_RECEIVING);
			$.getScript(url, function(data, textStatus) {
				if(/^<html>/.test(data)) {
					ajaxViewer.stop();
					$(document.body).html(data);
				} else {
					eval(data);
					ajaxViewer.setDirty(true);
					window.onStatusNotify(AjaxViewer.STATUS_RECEIVED);
					
					ajaxViewer.checkUpdate();
				}
			});
		} 
	},
	
	ptInPanel: function(pageX, pageY) {
		var mainPanel = this.panel;
		
		var offset = mainPanel.offset();
		var x = pageX - offset.left;
		var y = pageY - offset.top;
		
		if(x < 0 || y < 0 || x > mainPanel.width() - 1 || y > mainPanel.height() - 1)
			return false;
		return true;
	},
	
	pageToPanel: function(pageX, pageY) {
		var mainPanel = this.panel;
		
		var offset = mainPanel.offset();
		var x = pageX - offset.left;
		var y = pageY - offset.top;
		
		if(x < 0)
			x = 0;
		if(x > mainPanel.width() - 1)
			x = mainPanel.width() - 1;
		
		if(y < 0)
			y = 0;
		if(y > mainPanel.height() - 1)
			y = mainPanel.height() - 1;
		
		return { x: Math.ceil(x), y: Math.ceil(y) };
	},
	
	installMouseHook: function() {
		var ajaxViewer = this;
		var target = $(document.body);
		
		target.mousemove(function(e) {
			if(!ajaxViewer.ptInPanel(e.pageX, e.pageY))
				return true;
			
			var pt = ajaxViewer.pageToPanel(e.pageX, e.pageY);  
			ajaxViewer.onMouseMove(pt.x, pt.y);
			
			e.stopPropagation();
			return false;
		});
		
		target.mousedown(function(e) {
			ajaxViewer.panel.parent().focus();
			
			if(!ajaxViewer.ptInPanel(e.pageX, e.pageY))
				return true;
			
			var modifiers = ajaxViewer.getKeyModifiers(e);
			var whichButton = e.button;
			
			var pt = ajaxViewer.pageToPanel(e.pageX, e.pageY);  
			ajaxViewer.onMouseDown(pt.x, pt.y, whichButton, modifiers);
			
			e.stopPropagation();
			return false;
		});
		
		target.mouseup(function(e) {
			if(!ajaxViewer.ptInPanel(e.pageX, e.pageY))
				return true;
			
			var modifiers = ajaxViewer.getKeyModifiers(e);
			var whichButton = e.button;
			
			var pt = ajaxViewer.pageToPanel(e.pageX, e.pageY);  

			ajaxViewer.onMouseUp(pt.x, pt.y, whichButton, modifiers);
			e.stopPropagation();
			return false;
		});
		
		// disable browser right-click context menu
		target.bind("contextmenu", function() { return false; });
	},
	
	uninstallMouseHook : function() {
		var target = $(document);
		target.unbind("mousemove");
		target.unbind("mousedown");
		target.unbind("mouseup");
		target.unbind("contextmenu");
	},
	
	requiresDefaultKeyProcess : function(e) {
		switch(e.which) {
		case 8 :		// backspace
		case 9 :		// TAB
		case 19 :		// PAUSE/BREAK
		case 20 :		// CAPSLOCK
		case 27 :		// ESCAPE
		case 16 :		// SHIFT key
		case 17 :		// CTRL key
		case 18 :		// ALT key
		case 33 :		// PGUP
		case 34 :		// PGDN
		case 35 :		// END
		case 36 :		// HOME
		case 37 :		// LEFT
		case 38 :		// UP
		case 39 :		// RIGHT
		case 40 :		// DOWN
		case 47 :		// FORWARD SLASH // Added to stop Firefox's quick search from opening
			return false;
		}
		
		if(this.getKeyModifiers(e) == AjaxViewer.SHIFT_KEY_MASK)
			return true;
		
		if(this.getKeyModifiers(e) != 0)
			return false;
		
		return true;
	},
	
	installKeyboardHook: function() {
		var ajaxViewer = this;
		var target = $(document);

		target.keypress(function(e) {
			ajaxViewer.onKeyPress(e.which, ajaxViewer.getKeyModifiers(e));

			e.stopPropagation();
			if(ajaxViewer.requiresDefaultKeyProcess(e))
				return true;
			
			e.preventDefault();
			return false;
		});
		
		target.keydown(function(e) {
			ajaxViewer.onKeyDown(e.which, ajaxViewer.getKeyModifiers(e));
			
			e.stopPropagation();
			if(ajaxViewer.requiresDefaultKeyProcess(e))
				return true;
			
			e.preventDefault();
			return false;
		});
		
		target.keyup(function(e) {
			ajaxViewer.onKeyUp(e.which, ajaxViewer.getKeyModifiers(e));

			e.stopPropagation();
			if(ajaxViewer.requiresDefaultKeyProcess(e))
				return true;
			
			e.preventDefault();
			return false;
		});
	},
	
	uninstallKeyboardHook : function() {
		var target = $(document);
		target.unbind("keypress");
		target.unbind("keydown");
		target.unbind("keyup");
	},
	
	onMouseMove: function(x, y) {
		this.sendMouseEvent(AjaxViewer.MOUSE_MOVE, x, y, 0, 0);
	},
	
	onMouseDown: function(x, y, whichButton, modifiers) {
		this.sendMouseEvent(AjaxViewer.MOUSE_DOWN, x, y, whichButton, modifiers);
	},
	
	onMouseUp: function(x, y, whichButton, modifiers) {
		this.sendMouseEvent(AjaxViewer.MOUSE_UP, x, y, whichButton, modifiers);
		
		var curTick = new Date().getTime();
		if(this.lastClickEvent.time && (curTick - this.lastClickEvent.time < 300)) {
			this.onMouseDblClick(this.lastClickEvent.x, this.lastClickEvent.y, 
				this.lastClickEvent.button, this.lastClickEvent.modifiers);
		}
		
		this.lastClickEvent.x = x;
		this.lastClickEvent.y = y;
		this.lastClickEvent.button = whichButton;
		this.lastClickEvent.modifiers = modifiers;
		this.lastClickEvent.time = curTick;
	},
	
	onMouseDblClick: function(x, y, whichButton, modifiers) {
		this.sendMouseEvent(AjaxViewer.MOUSE_DBLCLK, x, y, whichButton, modifiers);
	},
	
	onKeyPress: function(code, modifiers) {
		g_logger.log(Logger.LEVEL_WARN, "RAW KEYBOARD EVENT. KEY-PRESS: " + code + ", modifers: " + modifiers);
		
		this.dispatchKeyboardInput(AjaxViewer.KEY_PRESS, code, modifiers);
	},
	
	onKeyDown: function(code, modifiers) {
		g_logger.log(Logger.LEVEL_WARN, "RAW KEYBOARD EVENT. KEY-DOWN: " + code + ", modifers: " + modifiers);
		
		this.dispatchKeyboardInput(AjaxViewer.KEY_DOWN, code, modifiers);
	},
	
	onKeyUp: function(code, modifiers) {
		g_logger.log(Logger.LEVEL_WARN, "RAW KEYBOARD EVENT. KEY-UP: " + code + ", modifers: " + modifiers);
		
		this.dispatchKeyboardInput(AjaxViewer.KEY_UP, code, modifiers);
	},
	
	dispatchKeyboardInput : function(event, code, modifiers) {
		var keyboardMapper = ajaxViewer.getCurrentKeyboardMapper();
		keyboardMapper.inputFeed(event, code, modifiers, this.guestos, $.browser, $.browser.version);
		this.dispatchMappedKeyboardInput(keyboardMapper.getMappedInput());
	},
	
	dispatchMappedKeyboardInput : function(mappedInput) {
		for(var i = 0; i < mappedInput.length; i++) {
			switch(mappedInput[i].type) {
			case AjaxViewer.KEY_DOWN :
				this.sendKeyboardEvent(AjaxViewer.KEY_DOWN, mappedInput[i].code, mappedInput[i].modifiers);
				break;
				
			case AjaxViewer.KEY_UP :
				this.sendKeyboardEvent(AjaxViewer.KEY_UP, mappedInput[i].code, mappedInput[i].modifiers);
				break;
				
			case AjaxViewer.KEY_PRESS :
				this.sendKeyboardEvent(AjaxViewer.KEY_PRESS, mappedInput[i].code, mappedInput[i].modifiers);
				break;
			}
		}
	},
	
	getKeyModifiers: function(e) {
		var modifiers = 0;
		if(e.altKey)
			modifiers |= AjaxViewer.ALT_KEY_MASK;
		
		if(e.altLeft)
			modifiers |= AjaxViewer.LEFT_ALT_MASK;
		
		if(e.ctrlKey)
			modifiers |= AjaxViewer.CTRL_KEY_MASK;
		
		if(e.ctrlLeft)
			modifiers |=  AjaxViewer.LEFT_CTRL_MASK;
		
		if(e.shiftKey)
			modifiers |=  AjaxViewer.SHIFT_KEY_MASK;
		
		if(e.shiftLeft)
			modifiers |= AjaxViewer.LEFT_SHIFT_MASK;
		
		if(e.metaKey)
			modifiers |= AjaxViewer.META_KEY_MASK;
		
		return modifiers;
	}
};

