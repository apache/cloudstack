<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_podicon.gif" /></div>
    <h1 id="page_title">
        Network
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>  
        
    <!-- ***** Public Network Page (begin) ***** -->
    <div id="public_network_page" style="display:none">
        <div class="tabbox" style="margin-top: 15px;">
            <div class="content_tabs on" id="tab_details">
                <%=t.t("details")%></div>
            <div class="content_tabs off" id="tab_ipallocation">
                IP Allocation</div>
            <div class="content_tabs off" id="tab_firewall">
                Firewall</div>
            <div class="content_tabs off" id="tab_loadbalancer">
                Load Balancer</div>
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
	            	    <div id="grid_header_title" class="grid_header_title">Title</div>
	                       <div class="grid_actionbox" id="action_link"><p>Actions</p>
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
	                            State:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="state">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Traffic Type:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="traffictype">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Broadcast Domain Type:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="broadcastdomaintype">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Is Shared:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="isshared">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Is System:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="issystem">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Network Offering Name:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingname">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Network Offering Display Text:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingdisplaytext">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Network Offering ID:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Related:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="related">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Zone ID:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="zoneid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            DNS1:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="dns1">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            DNS2:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="dns2">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Domain ID:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="domainid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Account:</div>
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
        
        <!-- Firewall tab (start)-->
        <div style="display: none;" id="tab_content_firewall">
    	    <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
                  <div class="rightpanel_mainloaderbox">
                       <div class="rightpanel_mainloader_animatedicon"></div>
                       <p>Loading &hellip;</p>    
                  </div>               
            </div>
            <div id="tab_container">
            </div>
        </div> 
        <!-- Firewall tab (end)-->      
        
        <!-- Load Balancer tab (start)-->
        <div style="display: none;" id="tab_content_loadbalancer">
    	    <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
                  <div class="rightpanel_mainloaderbox">
                       <div class="rightpanel_mainloader_animatedicon"></div>
                       <p>Loading &hellip;</p>    
                  </div>               
            </div>
            <div id="tab_container">
            </div>
        </div> 
        <!-- Load Balancer tab (end)-->           
    </div>
    <!-- ***** Public Network Page (end) ***** -->
    
    <!-- ***** Direct Network Page (begin) ***** -->
    <div id="direct_network_page" style="display:none">
        <div class="tabbox" style="margin-top: 15px;">
            <div class="content_tabs on" id="tab_details">
                <%=t.t("Details")%></div>
            <div class="content_tabs off" id="tab_ipallocation">
                IP Allocation</div>
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
	            	    <div id="grid_header_title" class="grid_header_title">Title</div>
	                       <div class="grid_actionbox" id="action_link"><p>Actions</p>
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
	                            <%=t.t("display.text")%>:</div>
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
	                            Netmask:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="netmask">
	                        </div>
	                    </div>
	                </div>	               
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Domain:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="domain">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            Account:</div>
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
</div>

<!--  top buttons (begin) -->
<div id="top_buttons"> 
    <div class="actionpanel_button_wrapper" id="add_network_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Network" /></div>
            <div class="actionpanel_button_links">
                Add Network
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_iprange_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add IP Range" /></div>
            <div class="actionpanel_button_links">
                Add IP Range
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_external_firewall_button"
       >
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Firewall" /></div>
            <div class="actionpanel_button_links">
                Add Firewall
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_load_balancer_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Load Balancer" /></div>
            <div class="actionpanel_button_links">
                Add Load Balancer
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!--  External Firewall template (begin) -->
<div class="grid_container" id="externalfirewall_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="firewall_action_link"><p>Actions</p>
            <div class="grid_actionsdropdown_box" id="firewall_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>        
 
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                IP:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="url">
            </div>
        </div>
    </div>  
</div>
<!--  External Firewall template (end) -->


<!--  Load Balancer template (begin) -->
<div class="grid_container" id="loadbalancer_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="loadbalancer_action_link"><p>Actions</p>
            <div class="grid_actionsdropdown_box" id="loadbalancer_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>        
 
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                IP:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="url">
            </div>
        </div>
    </div>        
</div>
<!--  Load Balancer template (end) -->

<!--  IP Range template (begin) -->
<div class="grid_container" id="iprange_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="iprange_action_link"><p>Actions</p>
            <div class="grid_actionsdropdown_box" id="iprange_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>        
 
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                VLAN:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vlan">
            </div>
        </div>
    </div>   
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                IP Range:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="iprange">
            </div>
        </div>
    </div>  
</div>
<!--  IP Range template (end) -->


<!-- Add IP Range for public netework dialog (begin) -->
<div id="dialog_add_iprange_to_publicnetwork" title="Add IP Range to Public Network" style="display: none">
    <p>
        Add an IP range to public network in zone: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li id="add_publicip_vlan_container">
                <label for="add_publicip_vlan_tagged">
                    VLAN:</label>
                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                    <option value="untagged">untagged</option>
                    <option value="tagged">tagged</option>
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_vlan_container">
                <label for="user_name">
                    VLAN ID:</label>
                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_scope_container">
                <label for="add_publicip_vlan_scope">
                    Scope:</label>
                <select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
                    <!--  
                    <option value="zone-wide">zone-wide</option>
                    <option value="account-specific">account-specific</option>
                    -->
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_pod_container">
                <label for="user_name">
                    Pod:</label>
                <select class="select" name="add_publicip_vlan_pod" id="add_publicip_vlan_pod">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_domain_container">
                <label for="user_name">
                    Domain:</label>
                <select class="select" name="add_publicip_vlan_domain" id="add_publicip_vlan_domain">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_account_container">
                <label for="user_name">
                    Account:</label>
                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Gateway:</label>
                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Netmask:</label>
                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_startip"
                    id="add_publicip_vlan_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_endip"
                    id="add_publicip_vlan_endip" />
                <div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
                <div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add IP Range for public netework dialog (end) -->


<!-- Add External Firewall dialog (begin) -->
<div id="dialog_add_external_firewall" title="Add Firewall" style="display: none">   
    <p>
        Add firewall for zone: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>   
            <li>
                <label>
                    IP:</label>
                <input class="text" type="text" id="ip" />
                <div id="ip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    User name:</label>
                <input class="text" type="text" id="username" />
                <div id="username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    Password:</label>
                <input class="text" type="password" id="password" autocomplete="off" />
                <div id="password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>  
            
            <li>
                <label>
                    Public Interface:</label>
                <input class="text" type="text" id="public_interface" />
                <div id="public_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>         
            <li>
                <label>
                    Private Interface:</label>
                <input class="text" type="text" id="private_interface" />
                <div id="private_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>       
            <li>
                <label>
                    Public Zone:</label>
                <input class="text" type="text" id="public_zone" />
                <div id="public_zone_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>       
            <li>
                <label>
                    Private Zone:</label>
                <input class="text" type="text" id="private_zone" />
                <div id="private_zone_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>        
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add External Firewall dialog (end) -->


<!-- Add Load Balancer dialog (begin) -->
<div id="dialog_add_load_balancer" title="Add Load Balancer" style="display: none">   
    <p>
        Add load balancer for zone: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>   
            <li>
                <label>
                    IP:</label>
                <input class="text" type="text" id="ip" />
                <div id="ip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    User name:</label>
                <input class="text" type="text" id="username" />
                <div id="username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    Password:</label>
                <input class="text" type="password" id="password" autocomplete="off" />
                <div id="password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>  
            
            <li>
                <label>
                    Public Interface:</label>
                <input class="text" type="text" id="public_interface" />
                <div id="public_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>         
            <li>
                <label>
                    Private Interface:</label>
                <input class="text" type="text" id="private_interface" />
                <div id="private_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>       
            
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add Load Balancer dialog (end) -->


<!-- Add IP Range for direct netework dialog (begin) -->
<div id="dialog_add_iprange_to_directnetwork" title="Add IP Range to Direct Network" style="display: none">
    <p>
        Add an IP range to direct network <b><span id="directnetwork_name"></span></b> in zone <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>                
            <li>
                <label for="user_name">
                    IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_startip"
                    id="add_publicip_vlan_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_endip"
                    id="add_publicip_vlan_endip" />
                <div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
                <div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add IP Range for direct netework dialog (end) -->

<!-- Add network dialog for zone (begin) -->
<div id="dialog_add_network_for_zone" title="Add Network" style="display: none">
    <p>
        Add a new network for zone: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
			<li>
                <label for="user_name">
                    Network Name:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_name" id="add_publicip_vlan_network_name" />
                <div id="add_publicip_vlan_network_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li>
                <label for="user_name">
                    Network Desc:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_desc" id="add_publicip_vlan_network_desc" />
                <div id="add_publicip_vlan_network_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_container">
                <label for="add_publicip_vlan_tagged">
                    VLAN:</label>
                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                    <option value="tagged">tagged</option>
                </select>
            </li>
            <li id="add_publicip_vlan_vlan_container">
                <label for="user_name">
                    VLAN ID:</label>
                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_scope_container">
                <label for="add_publicip_vlan_scope">
                    Scope:</label>
                <select class="select" id="add_publicip_vlan_scope">                    
                    <option value="zone-wide">zone-wide</option>
                    <option value="account-specific">account-specific</option>                   
                </select>
            </li>           
            <li style="display: none" id="add_publicip_vlan_domain_container">
                <label for="user_name">
                    Domain:</label>
                <select class="select" name="add_publicip_vlan_domain" id="add_publicip_vlan_domain">
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_account_container">
                <label for="user_name">
                    Account:</label>
                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Gateway:</label>
                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Netmask:</label>
                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_startip"
                    id="add_publicip_vlan_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_endip"
                    id="add_publicip_vlan_endip" />
                <div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
                <div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add network dialog for zone (end) -->


<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>
                <li id="adv_search_domain_li" style="display: none;">
                    <select class="select" id="adv_search_domain">
                    </select>
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value="by account" />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
