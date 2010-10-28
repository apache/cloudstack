<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>



<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_globalsettingsicon.gif" alt=" Global_Settings" /></div>
    
    <h1>
        Global Settings
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("details")%></div>
    </div>
    <div id="tab_content_details">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">
        	<div class="grid_header">
            	<div class="grid_header_cell" style="width:25%;">
            		<div class="grid_header_title">Name</div>
                </div>
                <div class="grid_header_cell" style="width:20%;">
            		<div class="grid_header_title">Value</div>
                </div>
                <div class="grid_header_cell" style="width:24%;">
            		<div class="grid_header_title">Description</div>
                </div>
                <div class="grid_header_cell" style="width:20%;">
            		<div class="grid_header_title">Category</div>
                </div>
                <div class="grid_header_cell" style="width:10%;">
            		<div class="grid_header_title"></div>
                </div>
                     
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 25%;">
                    <div class="row_celltitles" id="name"></div>
                </div>
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles" id="value">
                    </div>                    
                    <input class="text" id="value_edit" style="width: 200px; display: none;" type="text" />
                    <div id="value_edit_errormsg" style="display:none"></div>                    
                </div>
                <div class="grid_row_cell" style="width: 24%;">
                    <div class="row_celltitles" id="description">
                    </div>
                </div>
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles" id="category">
                    </div>
                </div>
                <div class="grid_row_cell" style="width: 10%;">
                    <div class="row_celltitles">
                    	<a id="edit_button" href="#">Edit</a>
                    </div>
                </div>
               
            </div>
            
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 25%;">
                    <div class="row_celltitles" id="name"></div>
                </div>
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles" id="value">
                    </div>                    
                    <input class="text" id="value_edit" style="width: 200px; display: none;" type="text" />
                    <div id="value_edit_errormsg" style="display:none"></div>                    
                </div>
                <div class="grid_row_cell" style="width: 24%;">
                    <div class="row_celltitles" id="description">
                    </div>
                </div>
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles" id="category">
                    </div>
                </div>
                <div class="grid_row_cell" style="width: 10%;">
                    <div class="row_celltitles">
                    	<a href="#">Edit</a>
                    </div>
                </div>
               
            </div>
                        
                           
        </div>        
        <div class="grid_botactionpanel">
        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
        </div>  
    </div>
</div>

<div id="dialog_alert_restart_management_server" title="Alert" style="display:none">
    <p>
        <%=t.t("please.restart.your.management.server.for.your.new.settings.to.take.effect")%>        
    </p>
</div>
