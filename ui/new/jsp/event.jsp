<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- event detail panel (begin) -->
<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_eventsicon.gif" alt="Event" /></div>
   
    <h1>Event
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display:none">
        <p id="after_action_info"></p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>        
    </div>    
    <div id="tab_content_details">
	    <div class="grid_container">
	        <div class="grid_rows odd">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("id")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="id">
	                </div>
	            </div>
	        </div>
	        <div class="grid_rows even">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("Initiated.By")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="username">
	                </div>
	            </div>
	        </div>
	        <div class="grid_rows odd">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("Owner.Account")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="account">
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
	                    <%=t.t("Level")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="level">
	                </div>
	            </div>
	        </div>
	        <div class="grid_rows even">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("Description")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="description">
	                </div>
	            </div>
	        </div>
	        <div class="grid_rows odd">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("State")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="state">
	                </div>
	            </div>
	        </div>
	        <div class="grid_rows even">
	            <div class="grid_row_cell" style="width: 20%;">
	                <div class="row_celltitles">
	                    <%=t.t("Date")%>:</div>
	            </div>
	            <div class="grid_row_cell" style="width: 79%;">
	                <div class="row_celltitles" id="created">
	                </div>
	            </div>
	        </div>       
	    </div>
    </div>     
</div>
<!-- event detail panel (end) -->