<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>


<!-- alert detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_alerticon.gif" /></div>

    <h1>Alerts
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>        
    </div>    
    <div id="tab_content_details">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>  
        <div id="tab_container">
		    <div class="grid_container">
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("ID")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="id">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Type")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="type">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Description")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="description">
		                    </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("Sent")%>:</div>
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


<!-- advanced search template (begin) -->
<div id="advanced_search_popup" class="adv_searchpopup_bg" style="display: none;">
    	<div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text textwatermark" type="text" id="adv_search_type" value="by type" />
                </li>            
            </ol>
            </form>
         </div>
</div>
<!-- advanced search template (end) -->