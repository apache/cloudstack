tileMap = [ ${tileSequence} ];
<#if resized == true>
	ajaxViewer.resize('main_panel', ${width}, ${height}, ${tileWidth}, ${tileHeight}); 
</#if>
ajaxViewer.refresh('${imgUrl}', tileMap, false);
 