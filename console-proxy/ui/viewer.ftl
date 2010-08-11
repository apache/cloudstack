<html>
<head>
<script type="text/javascript" language="javascript" src="/resource/js/jquery.js"></script>
<script type="text/javascript" language="javascript" src="/resource/js/ajaxviewer.js"></script>
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
