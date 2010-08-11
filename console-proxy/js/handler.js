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

//
// Callback handlers for AJAX viewer
// Author
//		Kelven Yang
//		11/18/2009
//
function onKickoff() {
	ajaxViewer.stop();
	$('#toolbar').remove();
	$('#main_panel').html('<p>This session is terminated because a session for the same VM has been created elsewhere.</p>');
}

function onDisconnect() {
	ajaxViewer.stop();
	$('#toolbar').remove();
	$('#main_panel').html('<p>This session is terminated as the machine you are accessing has terminated the connection.</p>');
}

function onClientError() {
	ajaxViewer.stop();
	$('#toolbar').remove();
	$('#main_panel').html('<p>Client communication error, please retry later.</p>');
}

function onCanvasSizeChange(width, height) {
	$('#toolbar').width(width);
}

function onStatusNotify(status) {
	if(status == ajaxViewer.STATUS_SENDING || status == ajaxViewer.STATUS_RECEIVING)
		$('#light').removeClass('dark').addClass('bright');
	else
		$('#light').removeClass('bright').addClass('dark');
}

function sendCtrlAltDel() {
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_DOWN, 45, ajaxViewer.CTRL_KEY | ajaxViewer.ALT_KEY);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_UP, 45, ajaxViewer.CTRL_KEY | ajaxViewer.ALT_KEY);
}

function sendCtrlEsc() {
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_DOWN, 17, 0);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_DOWN, 27, ajaxViewer.CTRL_KEY);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_UP, 27, ajaxViewer.CTRL_KEY);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_UP, 17, 0);
}

function sendAltTab() {
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_DOWN, 18, 0);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_DOWN, 9, ajaxViewer.ALT_KEY);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_UP, 9, ajaxViewer.ALT_KEY);
	ajaxViewer.sendKeyboardEvent(ajaxViewer.KEY_UP, 18, 0);
}
