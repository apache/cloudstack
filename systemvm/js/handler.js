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
