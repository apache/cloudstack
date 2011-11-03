<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<!-- alert detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_alerticon.gif" /></div>

    <h1><fmt:message key="label.menu.alerts"/></h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <fmt:message key="label.details"/></div>        
    </div>    
    <div id="tab_content_details">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    <fmt:message key="label.loading"/> &hellip;</p>
            </div>
        </div>  
        <div id="tab_container">
		    <div class="grid_container">
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.id"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="id">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.type"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="type">
		                </div>
		            </div>
		        </div>
		        
		         <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.type.id"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="typeid">
		                </div>
		            </div>
		        </div>
		        
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.description"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="description">
		                    </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.sent"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="sent">
		                </div>
		            </div>
		        </div>        
		    </div>
	    </div>    
    </div>    
</div>
<!-- alert detail panel (end) -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div id="advanced_search_popup" class="adv_searchpopup_bg" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text textwatermark" type="text" id="adv_search_typeid" value='<fmt:message key="label.by.type.id"/>'/>
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
