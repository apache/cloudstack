<!--
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
-->
<html>
<head>
<script type="text/javascript" language="javascript" src="/resource/js/jquery.js"></script>
<script type="text/javascript" language="javascript" src="/resource/js/ajaxviewer.js"></script>
<script type="text/javascript" language="javascript" src="/resource/js/ajaxkeys.js"></script>
<script type="text/javascript" language="javascript" src="/resource/js/handler.js"></script>
<link rel="stylesheet" type="text/css" href="/resource/css/ajaxviewer.css"></link>
<title>${title}</title>
</head>
<body>
<div id="toolbar">
<ul>
	<li> 
		<a href="#" onclick="javascript:sendCtrlAltDel();"> 
			<span><img align="left" src="/resource/images/cad.gif" alt="Ctrl-Alt-Del" />Ctrl-Alt-Del</span> 
		</a> 
	</li>
	<li> 
		<a href="#" onclick="javascript:sendCtrlEsc();"> 
			<span><img align="left" src="/resource/images/winlog.png" alt="Ctrl-Esc" style="width:16px;height:16px"/>Ctrl-Esc</span> 
		</a> 
	</li>
</ul>
<span id="light" class="dark"></span> 
</div>

<div id="main_panel" tabindex="1"></div>
	
<script language="javascript">

var tileMap = [ ${tileSequence} ];
var ajaxViewer = new AjaxViewer('main_panel', '${imgUrl}', '${updateUrl}', tileMap, 
	${width}, ${height}, ${tileWidth}, ${tileHeight}, ${rawKeyboard});

$(function() {
	ajaxViewer.start();
});

</script>

</body>
</html>	
