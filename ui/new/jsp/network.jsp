<!--
<script type="text/javascript" src="scripts/cloud.core.event.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- event detail panel (begin) -->
<div class="main_title" id="right_panel_header">
     
    <div class="main_titleicon">
        <img src="images/iptitle_icons.gif" alt="Network" /></div>
    
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
        <div class="content_tabs off">
            <%=t.t("Port Forwarding")%></div>    
        <div class="content_tabs off">
            <%=t.t("Load Balancer")%></div>       
    </div>
    <div class="grid_actionpanel">
    	<div class="grid_actionbox"></div>   
        <div class="grid_editbox"></div>    
    </div>
   <!-- Network Details start here-->
    <div class="grid_container">
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    IP:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    Zone:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                    </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    VLAN:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Level")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="level">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                   Source NAT:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    Network Type:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    Domain:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    Account:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                   Allocated:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                </div>
            </div>
        </div>    
    </div>
   <!-- Network Details ends here-->
   
   <!-- Port Forwarding start here-->
   <div class="grid_container">
    	<div class="grid_header">
        	<div class="grid_header_cell" style="width:20%">
            	<div class="grid_header_title">Public Port</div>
            </div>
            <div class="grid_header_cell" style="width:20%">
            	<div class="grid_header_title">Private Port</div>
            </div>
            <div class="grid_header_cell" style="width:29%">
            	<div class="grid_header_title">Protocol</div>
            </div>
            <div class="grid_header_cell" style="width:30%; border:none;">
            	<div class="grid_header_title">Instance</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">8080</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">80</div>
            </div>
            <div class="grid_row_cell" style="width: 29%;">
                <div class="row_celltitles">8 GB</div>
            </div>
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">Instance Name</div>
            </div>
            
        </div>
        
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">8080</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">80</div>
            </div>
            <div class="grid_row_cell" style="width: 29%;">
                <div class="row_celltitles">8 GB</div>
            </div>
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">Instance Name</div>
            </div>
            
        </div>
	 </div>
    <!-- Port Forwarding ends here-->
    
    
    <!-- Load Balancer start here-->
   <div class="grid_container">
    	<div class="grid_header">
        	<div class="grid_header_cell" style="width:29%">
            	<div class="grid_header_title">Name</div>
            </div>
            <div class="grid_header_cell" style="width:20%">
            	<div class="grid_header_title">Public Port</div>
            </div>
            <div class="grid_header_cell" style="width:20%">
            	<div class="grid_header_title">Private Port</div>
            </div>
            <div class="grid_header_cell" style="width:30%; border:none;">
            	<div class="grid_header_title">Algorithm</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 29%;">
                <div class="row_celltitles">LB#1</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">8080</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">80</div>
            </div>
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">Source</div>
            </div>
          	<div class="grid_detailspanel" style="display:block;">
            	<div class="grid_details_pointer"></div>
                <div class="grid_detailsbox">
                	<div class="grid_details_row">
                    	      <div class="grid_row_cell" style="width: 9%;">
                                    <div class="row_celltitles"><img src="images/network_managevmicon.gif" /></div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">1-2-2-TEST</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">10.23.231.230</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles"><a href="#">Remove</a></div>
                              </div>
                    </div>
                    
                    <div class="grid_details_row odd">
                    	      <div class="grid_row_cell" style="width: 9%;">
                                    <div class="row_celltitles"><img src="images/network_managevmicon.gif" /></div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">1-2-2-TEST</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">10.23.231.230</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles"><a href="#">Remove</a></div>
                              </div>
                    </div>
                </div>
            </div>  
        </div>
        
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 29%;">
                <div class="row_celltitles">LB#1</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">8080</div>
            </div>
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">80</div>
            </div>
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">Source</div>
            </div>
            
            <div class="grid_detailspanel" style="display:none;">
            	<div class="grid_details_pointer"></div>
                <div class="grid_detailsbox">
                	<div class="grid_details_row">
                    	      <div class="grid_row_cell" style="width: 9%;">
                                    <div class="row_celltitles"><img src="images/network_managevmicon.gif" /></div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">1-2-2-TEST</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">10.23.231.230</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles"><a href="#">Remove</a></div>
                              </div>
                    </div>
                    
                    <div class="grid_details_row odd">
                    	      <div class="grid_row_cell" style="width: 9%;">
                                    <div class="row_celltitles"><img src="images/network_managevmicon.gif" /></div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">1-2-2-TEST</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles">10.23.231.230</div>
                              </div>
                              <div class="grid_row_cell" style="width: 30%;">
                                    <div class="row_celltitles"><a href="#">Remove</a></div>
                              </div>
                    </div>
                </div>
            </div>
            
        </div>
           
    </div>
    <!-- Load Balancer ends here-->
</div>
<!-- event detail panel (end) -->