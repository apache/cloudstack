/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.consoleproxy;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

public class ConsoleProxyAjaxKeyMapper {
	
    private Map<Integer, Integer> actionKeyVkCodeMap;
    private Map<Integer, Integer> regularKeyVkCodeMap;
    private Map<Integer, Integer> js2javaCodeMap;
    
    private Map<Integer, Integer> shiftedKeyCharMap;
    
    private static ConsoleProxyAjaxKeyMapper instance = new ConsoleProxyAjaxKeyMapper();
    
    private ConsoleProxyAjaxKeyMapper() {
    	int code;
    	
    	// setup action char-code to vk-code map
    	actionKeyVkCodeMap = new HashMap<Integer, Integer>();
    	
        actionKeyVkCodeMap.put(new Integer(27), 	new Integer(KeyEvent.VK_ESCAPE)); 	// Esc
        actionKeyVkCodeMap.put(new Integer(9), 		new Integer(KeyEvent.VK_TAB)); 		// Tab

        actionKeyVkCodeMap.put(new Integer(112), 	new Integer(KeyEvent.VK_F1)); 		// F1
        actionKeyVkCodeMap.put(new Integer(113), 	new Integer(KeyEvent.VK_F2)); 		// F2
        actionKeyVkCodeMap.put(new Integer(114), 	new Integer(KeyEvent.VK_F3)); 		// F3
        actionKeyVkCodeMap.put(new Integer(115), 	new Integer(KeyEvent.VK_F4)); 		// F4
        actionKeyVkCodeMap.put(new Integer(116), 	new Integer(KeyEvent.VK_F5)); 		// F5
        actionKeyVkCodeMap.put(new Integer(117), 	new Integer(KeyEvent.VK_F6)); 		// F6
        actionKeyVkCodeMap.put(new Integer(118), 	new Integer(KeyEvent.VK_F7)); 		// F7
        actionKeyVkCodeMap.put(new Integer(119), 	new Integer(KeyEvent.VK_F8)); 		// F8
        actionKeyVkCodeMap.put(new Integer(120), 	new Integer(KeyEvent.VK_F9)); 		// F9
        actionKeyVkCodeMap.put(new Integer(121), 	new Integer(KeyEvent.VK_F10)); 		// F10
        actionKeyVkCodeMap.put(new Integer(122), 	new Integer(KeyEvent.VK_F11)); 		// F11
        actionKeyVkCodeMap.put(new Integer(123), 	new Integer(KeyEvent.VK_F12)); 		// F12

        actionKeyVkCodeMap.put(new Integer(46), 	new Integer(KeyEvent.VK_DELETE)); 	// Del
        actionKeyVkCodeMap.put(new Integer(13), 	new Integer(KeyEvent.VK_ENTER)); 	// Enter
        actionKeyVkCodeMap.put(new Integer(36), 	new Integer(KeyEvent.VK_HOME)); 	// Home
        actionKeyVkCodeMap.put(new Integer(38), 	new Integer(KeyEvent.VK_UP)); 		// Up
        actionKeyVkCodeMap.put(new Integer(33), 	new Integer(KeyEvent.VK_PAGE_UP)); 	// PgUp
        actionKeyVkCodeMap.put(new Integer(37), 	new Integer(KeyEvent.VK_LEFT)); 	// Left
        actionKeyVkCodeMap.put(new Integer(39), 	new Integer(KeyEvent.VK_RIGHT)); 	// Right
        actionKeyVkCodeMap.put(new Integer(35), 	new Integer(KeyEvent.VK_END)); 		// End
        actionKeyVkCodeMap.put(new Integer(40), 	new Integer(KeyEvent.VK_DOWN)); 	// Down
        actionKeyVkCodeMap.put(new Integer(34), 	new Integer(KeyEvent.VK_PAGE_DOWN));// PgDn
        actionKeyVkCodeMap.put(new Integer(45), 	new Integer(KeyEvent.VK_INSERT)); 	// Ins
        actionKeyVkCodeMap.put(new Integer(46), 	new Integer(KeyEvent.VK_DELETE)); 	// Del

        actionKeyVkCodeMap.put(new Integer(16), new Integer(KeyEvent.VK_SHIFT));
        actionKeyVkCodeMap.put(new Integer(18), new Integer(KeyEvent.VK_ALT));
        actionKeyVkCodeMap.put(new Integer(17), new Integer(KeyEvent.VK_CONTROL));
        actionKeyVkCodeMap.put(new Integer(20), new Integer(KeyEvent.VK_CAPS_LOCK));
        
        actionKeyVkCodeMap.put(new Integer(KeyEvent.VK_BACK_SPACE), new Integer(KeyEvent.VK_BACK_SPACE));
        
        // setup regular char-code to vk-code map
        regularKeyVkCodeMap = new HashMap<Integer, Integer>();
        code = KeyEvent.VK_A;
        for(char c='A'; c <='Z'; c++, code++)
        	regularKeyVkCodeMap.put(new Integer((int)c), new Integer(code));
        
        code = KeyEvent.VK_A;
        for(char c='a'; c <='z'; c++, code++)
        	regularKeyVkCodeMap.put(new Integer((int)c), new Integer(code));
        
        code = KeyEvent.VK_0;
        for(char c='0'; c <='9'; c++, code++)
        	regularKeyVkCodeMap.put(new Integer((int)c), new Integer(code));

        regularKeyVkCodeMap.put(new Integer('~'), new Integer(192));
        regularKeyVkCodeMap.put(new Integer('`'), new Integer(192));
        
        regularKeyVkCodeMap.put(new Integer('!'), new Integer(49));
        regularKeyVkCodeMap.put(new Integer('@'), new Integer(50));
        regularKeyVkCodeMap.put(new Integer('#'), new Integer(51));
        regularKeyVkCodeMap.put(new Integer('$'), new Integer(52));
        regularKeyVkCodeMap.put(new Integer('%'), new Integer(53));
        regularKeyVkCodeMap.put(new Integer('^'), new Integer(54));
        regularKeyVkCodeMap.put(new Integer('&'), new Integer(55));
        regularKeyVkCodeMap.put(new Integer('*'), new Integer(56));
        regularKeyVkCodeMap.put(new Integer('('), new Integer(57));
        regularKeyVkCodeMap.put(new Integer(')'), new Integer(48));
        regularKeyVkCodeMap.put(new Integer('-'), new Integer(109));
        regularKeyVkCodeMap.put(new Integer('_'), new Integer(109));
        regularKeyVkCodeMap.put(new Integer('='), new Integer(107));
        regularKeyVkCodeMap.put(new Integer('+'), new Integer(107));
        
        regularKeyVkCodeMap.put(new Integer('['), new Integer(219));
        regularKeyVkCodeMap.put(new Integer('{'), new Integer(219));
        regularKeyVkCodeMap.put(new Integer(']'), new Integer(221));
        regularKeyVkCodeMap.put(new Integer('}'), new Integer(221));
        regularKeyVkCodeMap.put(new Integer('\\'), new Integer(220));
        regularKeyVkCodeMap.put(new Integer('|'), new Integer(220));
        
        regularKeyVkCodeMap.put(new Integer(';'), new Integer(59));
        regularKeyVkCodeMap.put(new Integer(':'), new Integer(59));
        regularKeyVkCodeMap.put(new Integer('\''), new Integer(222));
        regularKeyVkCodeMap.put(new Integer('"'), new Integer(222));

        regularKeyVkCodeMap.put(new Integer(','), new Integer(188));
        regularKeyVkCodeMap.put(new Integer('<'), new Integer(188));
        regularKeyVkCodeMap.put(new Integer('.'), new Integer(190));
        regularKeyVkCodeMap.put(new Integer('>'), new Integer(190));
        regularKeyVkCodeMap.put(new Integer('/'), new Integer(191));
        regularKeyVkCodeMap.put(new Integer('?'), new Integer(191));
        
        regularKeyVkCodeMap.put(new Integer(' '), new Integer(KeyEvent.VK_SPACE));

        //
        // Java script key code to AWT key code
        //
        js2javaCodeMap = new HashMap<Integer, Integer>();
        js2javaCodeMap.put(new Integer(20), new Integer(new Integer(KeyEvent.VK_CAPS_LOCK)));
        
        js2javaCodeMap.put(new Integer(192), new Integer(new Integer('`')));
        
        // for Firefox
        js2javaCodeMap.put(new Integer(109), new Integer(new Integer('-')));
        js2javaCodeMap.put(new Integer(107), new Integer(KeyEvent.VK_EQUALS));

        // for IE/Safari/Chrome
        js2javaCodeMap.put(new Integer(189), new Integer(new Integer('-')));
        js2javaCodeMap.put(new Integer(187), new Integer(KeyEvent.VK_EQUALS));

        js2javaCodeMap.put(new Integer(219), new Integer(KeyEvent.VK_OPEN_BRACKET));
        js2javaCodeMap.put(new Integer(221), new Integer(KeyEvent.VK_CLOSE_BRACKET));
        js2javaCodeMap.put(new Integer(220), new Integer(KeyEvent.VK_BACK_SLASH));
        
        js2javaCodeMap.put(new Integer(186), new Integer(KeyEvent.VK_SEMICOLON));
        js2javaCodeMap.put(new Integer(222), new Integer(KeyEvent.VK_QUOTE));
        js2javaCodeMap.put(new Integer(13), new Integer(KeyEvent.VK_ENTER));
        
        js2javaCodeMap.put(new Integer(190), new Integer(KeyEvent.VK_PERIOD));
        js2javaCodeMap.put(new Integer(188), new Integer(KeyEvent.VK_COMMA));
        js2javaCodeMap.put(new Integer(191), new Integer(KeyEvent.VK_SLASH));
        
        js2javaCodeMap.put(new Integer(45), new Integer(KeyEvent.VK_INSERT));
        js2javaCodeMap.put(new Integer(46), new Integer(KeyEvent.VK_DELETE));

        // numpad keys
        js2javaCodeMap.put(new Integer(96), new Integer(KeyEvent.VK_0));
        js2javaCodeMap.put(new Integer(97), new Integer(KeyEvent.VK_1));
        js2javaCodeMap.put(new Integer(98), new Integer(KeyEvent.VK_2));
        js2javaCodeMap.put(new Integer(99), new Integer(KeyEvent.VK_3));
        js2javaCodeMap.put(new Integer(100), new Integer(KeyEvent.VK_4));
        js2javaCodeMap.put(new Integer(101), new Integer(KeyEvent.VK_5));
        js2javaCodeMap.put(new Integer(102), new Integer(KeyEvent.VK_6));
        js2javaCodeMap.put(new Integer(103), new Integer(KeyEvent.VK_7));
        js2javaCodeMap.put(new Integer(104), new Integer(KeyEvent.VK_8));
        js2javaCodeMap.put(new Integer(105), new Integer(KeyEvent.VK_9));

        js2javaCodeMap.put(new Integer(110), new Integer(KeyEvent.VK_DELETE));
        js2javaCodeMap.put(new Integer(111), new Integer(KeyEvent.VK_SLASH));
        
        js2javaCodeMap.put(new Integer(20), new Integer(0xffe5));
        js2javaCodeMap.put(new Integer(17), new Integer(0xffe3));
        js2javaCodeMap.put(new Integer(18), new Integer(0xffe9));
        
        // for SHIFT transaction at proxy side
        shiftedKeyCharMap = new HashMap<Integer, Integer>();
        shiftedKeyCharMap.put(new Integer('1'), new Integer('!'));
        shiftedKeyCharMap.put(new Integer('2'), new Integer('@'));
        shiftedKeyCharMap.put(new Integer('3'), new Integer('#'));
        shiftedKeyCharMap.put(new Integer('4'), new Integer('$'));
        shiftedKeyCharMap.put(new Integer('5'), new Integer('%'));
        shiftedKeyCharMap.put(new Integer('6'), new Integer('^'));
        shiftedKeyCharMap.put(new Integer('7'), new Integer('&'));
        shiftedKeyCharMap.put(new Integer('8'), new Integer('*'));
        shiftedKeyCharMap.put(new Integer('9'), new Integer('('));
        shiftedKeyCharMap.put(new Integer('0'), new Integer(')'));
        shiftedKeyCharMap.put(new Integer('-'), new Integer('_'));
        shiftedKeyCharMap.put(new Integer('='), new Integer('+'));
        shiftedKeyCharMap.put(new Integer('`'), new Integer('~'));
        shiftedKeyCharMap.put(new Integer('['), new Integer('{'));
        shiftedKeyCharMap.put(new Integer(']'), new Integer('}'));
        shiftedKeyCharMap.put(new Integer('\\'), new Integer('|'));
        shiftedKeyCharMap.put(new Integer(';'), new Integer(':'));
        shiftedKeyCharMap.put(new Integer('\''), new Integer('"'));
        shiftedKeyCharMap.put(new Integer(','), new Integer('<'));
        shiftedKeyCharMap.put(new Integer('.'), new Integer('>'));
        shiftedKeyCharMap.put(new Integer('/'), new Integer('?'));
    }
    
    public char shiftedKeyCharFromKeyCode(int code, boolean shiftDown) {
    	
    	if(shiftDown) {
    		if(code >='A' && code <='Z')
    			return (char)code;
    		
    		if(code >='a' && code <= 'z')
    			return (char)('A' + (code - (int)'a'));
    		
	    	if(shiftedKeyCharMap.containsKey(code))
	    		return (char)(shiftedKeyCharMap.get(code).intValue());
    	} else {
    		if(code >='A' && code <='Z')
    			return (char)('a' + (code - (int)'A'));
    	}
    	
    	return (char)code;
    }
    
    public static ConsoleProxyAjaxKeyMapper getInstance() {
    	return instance;
    }
    
    public int getActionCharVkCode(int jsCode) {
    	Integer vkCode = actionKeyVkCodeMap.get(jsCode);
    	if(vkCode == null)
    		return -1;
    	return vkCode.intValue();
    }

    public int getRegularCharVkCode(int charCode) {
    	Integer vkCode = regularKeyVkCodeMap.get(charCode);
    	if(vkCode == null)
    		return -1;
    	return vkCode.intValue();
    }
    
    public int getJvmKeyCode(int jsKeyCode) {
    	Integer code = js2javaCodeMap.get(jsKeyCode);
    	if(code != null)
    		return code.intValue();
    	return jsKeyCode;
    }
}
