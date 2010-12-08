<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_podicon.gif" alt="Network" /></div>
    <h1 id="page_title">
        Network
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>    
    <!-- ***** Direct Network (begin) ***** -->
    <div id="direct_network_page" style="display:none">
        <div class="tabbox" style="margin-top: 15px;">
            <div class="content_tabs on" id="tab_details">
                <%=t.t("Details")%></div>
            <div class="content_tabs off" id="tab_ipallocation">
                <%=t.t("IP Allocation")%></div>
        </div>    
        <!-- Details tab (end)-->
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
	        	    <div class="grid_header">
	            	    <div class="grid_header_title">Title</div>
	                       <div class="grid_actionbox" id="action_link">
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><%=t.t("no.available.actions")%></li>
	                            </ul>
	                        </div>
	                    </div>
	                    <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                     display: none;">
	                        <div class="gridheader_loader" id="icon">
	                        </div>
	                        <p id="description">
	                            Waiting &hellip;</p>
	                    </div>
	                </div>
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
	                            <%=t.t("Name")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="name">
	                        </div>
	                    </div>
	                </div>	                
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("displaytext")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="displaytext">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("vlan")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="vlan">
	                        </div>
	                    </div>
	                </div>
	                                    <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("gateway")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="gateway">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("netmask")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="netmask">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("iprange")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="iprange">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("domain")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="domain">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <%=t.t("account")%>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="account">
	                        </div>
	                    </div>
	                </div>	               
	            </div>
	        </div>    
        </div>   
        <!-- Details tab (end)-->
        
        <!-- IP Allocation tab (start)-->
        <div style="display: none;" id="tab_content_ipallocation">
    	    <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
                  <div class="rightpanel_mainloaderbox">
                       <div class="rightpanel_mainloader_animatedicon"></div>
                       <p>Loading &hellip;</p>    
                  </div>               
            </div>
            <div id="tab_container">
            </div>
        </div> 
        <!-- IP Allocation tab (end)-->        
             
    </div>
    <!-- ***** Direct Network (end) ***** -->
</div>

<!--  Direct Network - IP Allocation tab template (begin) -->
<div class="grid_container" id="directnetwork_iprange_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>        
    </div>    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                VLAN:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vlan">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Start IP:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="startip">
            </div>
        </div>
    </div>    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                End IP:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="endip">
            </div>
        </div>
    </div> 
</div>
<!--  Direct Network - IP Allocation tab template (end) -->