<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.delete.IP.range' : '<fmt:message key="label.action.delete.IP.range"/>',
	'label.action.delete.IP.range.processing' : '<fmt:message key="label.action.delete.IP.range.processing"/>',
	'label.action.delete.firewall' : '<fmt:message key="label.action.delete.firewall"/>',
	'label.action.delete.firewall.processing' : '<fmt:message key="label.action.delete.firewall.processing"/>',
	'label.action.delete.load.balancer' : '<fmt:message key="label.action.delete.load.balancer"/>',
	'label.action.delete.load.balancer.processing' : '<fmt:message key="label.action.delete.load.balancer.processing"/>',
	'label.action.edit.network' : '<fmt:message key="label.action.edit.network"/>',
	'label.action.edit.network.processing' : '<fmt:message key="label.action.edit.network.processing"/>',
	'label.action.delete.network' : '<fmt:message key="label.action.delete.network"/>',
	'label.action.delete.network.processing' : '<fmt:message key="label.action.delete.network.processing"/>',
	'message.action.delete.network' : '<fmt:message key="message.action.delete.network"/>',
	'message.action.delete.external.firewall': '<fmt:message key="message.action.delete.external.firewall"/>',
	'message.action.delete.external.load.balancer': '<fmt:message key="message.action.delete.external.load.balancer"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_podicon.gif" /></div>
    <h1 id="page_title">
        <fmt:message key="label.network"/>
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
                <fmt:message key="label.details"/></div>
            <div class="content_tabs off" id="tab_ipallocation" style="display:none">
                <fmt:message key="label.ip.allocations"/></div>
            <div class="content_tabs off" id="tab_firewall">
                <fmt:message key="label.firewall"/></div>
            <div class="content_tabs off" id="tab_loadbalancer">
                <fmt:message key="label.load.balancer"/></div>
        </div>    
        <!-- Details tab (end)-->
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
	        	    <div class="grid_header">
	            	    <div id="grid_header_title" class="grid_header_title">Title</div>
	                       <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><fmt:message key="label.no.actions"/></li>
	                            </ul>
	                        </div>
	                    </div>
	                    <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                     display: none;">
	                        <div class="gridheader_loader" id="icon">
	                        </div>
	                        <p id="description">
	                            <fmt:message key="label.waiting"/> &hellip;</p>
	                    </div>
	                </div>
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
	                            <fmt:message key="label.state"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="state">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.traffic.type"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="traffictype">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.broadcast.domain.type"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="broadcastdomaintype">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.is.shared"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="isshared">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.is.system"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="issystem">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.network.offering.name"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingname">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.network.offering.display.text"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingdisplaytext">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.network.offering.id"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkofferingid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.related"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="related">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.zone.id"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="zoneid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.dns.1"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="dns1">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.dns.2"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="dns2">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.domain.id"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="domainid">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.account"/>:</div>
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
                       <p><fmt:message key="label.loading"/> &hellip;</p>    
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
                       <p><fmt:message key="label.loading"/> &hellip;</p>    
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
                       <p><fmt:message key="label.loading"/> &hellip;</p>    
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
                <fmt:message key="label.details"/></div>
            <div class="content_tabs off" id="tab_ipallocation">
                <fmt:message key="label.ip.allocations"/></div>
        </div>    
        <!-- Details tab (end)-->
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
	        	    <div class="grid_header">
	            	    <div id="grid_header_title" class="grid_header_title">Title</div>
	                       <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><fmt:message key="label.no.actions"/></li>
	                            </ul>
	                        </div>
	                    </div>
	                    <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                     display: none;">
	                        <div class="gridheader_loader" id="icon">
	                        </div>
	                        <p id="description">
	                            <fmt:message key="label.waiting"/> &hellip;</p>
	                    </div>
	                </div>
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
	                            <fmt:message key="label.name"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="name">
	                        </div>	                        
	                        <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                    	<div id="name_edit_errormsg" style="display:none"></div>  	                        
	                    </div>
	                </div>	                
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.display.text"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="displaytext">
	                        </div>	                        
	                        <input class="text" id="displaytext_edit" style="width: 200px; display: none;" type="text" />
	                    	<div id="displaytext_edit_errormsg" style="display:none"></div>  	                        
	                    </div>
	                </div>
					<div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.is.default"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="default">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.vlan"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="vlan">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.gateway"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="gateway">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.netmask"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="netmask">
	                        </div>
	                    </div>
	                </div>	
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.network.domain"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="networkdomain">
	                        </div>	                        
	                        <input class="text" id="networkdomain_edit" style="width: 200px; display: none;" type="text" />
	                    	<div id="networkdomain_edit_errormsg" style="display:none"></div>  	                        
	                    </div>
	                </div>	                
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.tags"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="tags">
	                        </div>	                        
	                        <input class="text" id="tags_edit" style="width: 200px; display: none;" type="text" />
	                    	<div id="tags_edit_errormsg" style="display:none"></div>  	                        
	                    </div>
	                </div>
	                <div class="grid_rows even">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.domain"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="domain">
	                        </div>
	                    </div>
	                </div>
	                <div class="grid_rows odd">
	                    <div class="grid_row_cell" style="width: 20%;">
	                        <div class="row_celltitles">
	                            <fmt:message key="label.account"/>:</div>
	                    </div>
	                    <div class="grid_row_cell" style="width: 79%;">
	                        <div class="row_celltitles" id="account">
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
        <!-- Details tab (end)-->
        
        <!-- IP Allocation tab (start)-->
        <div style="display: none;" id="tab_content_ipallocation">
    	    <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
                  <div class="rightpanel_mainloaderbox">
                       <div class="rightpanel_mainloader_animatedicon"></div>
                       <p><fmt:message key="label.loading"/> &hellip;</p>    
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
    <div class="actionpanel_button_wrapper" id="add_network_button" style="display:none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.network"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.network"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_iprange_button" style="display:none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.ip.range"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.ip.range"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_external_firewall_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.firewall"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.firewall"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_load_balancer_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.load.balancer"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.load.balancer"/>
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
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
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
                <fmt:message key="label.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ip">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.username"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="username">
            </div>
        </div>
    </div>       
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.public.interface"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publicinterface">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.private.interface"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privateinterface">
            </div>
        </div>
    </div>       
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.usage.interface"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="usageinterface">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.public.zone"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publiczone">
            </div>
        </div>
    </div>       
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.private.zone"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privatezone">
            </div>
        </div>
    </div>      
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.numretries"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="numretries">
            </div>
        </div>
    </div>       
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.timeout.in.second"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="timeout">
            </div>
        </div>
    </div>         
</div>
<!--  External Firewall template (end) -->

<!--  grid row template (begin) -->
<div class="grid_rows" id="grid_row_template" style="display:none">  <!-- add class "odd" or "even" here from JavaScript file -->
    <div class="grid_row_cell" style="width: 20%;">
        <div class="row_celltitles" id="label"></div>
    </div>
    <div class="grid_row_cell" style="width: 79%;">
        <div class="row_celltitles" id="value"></div>
    </div>
</div>
<!--  grid row template (end) -->

<!--  Load Balancer template (begin) -->
<div class="grid_container" id="loadbalancer_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
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
                <fmt:message key="label.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ip">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.username"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="username">
            </div>
        </div>
    </div>       
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.public.interface"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publicinterface">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.private.interface"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privateinterface">
            </div>
        </div>
    </div>      
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.numretries"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="numretries">
            </div>
        </div>
    </div>         
</div>
<!--  Load Balancer template (end) -->

<!--  Public IP Range template (begin) -->
<div class="grid_container" id="public_iprange_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
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
                <fmt:message key="label.vlan"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vlan">
            </div>
        </div>
    </div>    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.gateway"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="gateway">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.netmask"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="netmask">
            </div>
        </div>
    </div>        
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.ip.range"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="iprange">
            </div>
        </div>
    </div>     
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.domain"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>        
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.account"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>    
</div>
<!--  Public IP Range template (end) -->

<!--  Direct IP Range template (begin) -->
<div class="grid_container" id="direct_iprange_template" style="display: none">    
	<div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
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
                <fmt:message key="label.vlan"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vlan">
            </div>
        </div>
    </div>   
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.ip.range"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="iprange">
            </div>
        </div>
    </div>  
</div>
<!--  Direct IP Range template (end) -->


<!-- Add IP Range for public netework dialog (begin) -->
<div id="dialog_add_iprange_to_publicnetwork" title='<fmt:message key="label.add.ip.range"/>' style="display: none">
    <p>
		<fmt:message key="message.add.ip.range"/>: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li id="add_publicip_vlan_container">
                <label for="add_publicip_vlan_tagged">
                    <fmt:message key="label.vlan"/>:</label>
                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                    <option value="untagged"><fmt:message key="label.untagged"/></option>
                    <option value="tagged"><fmt:message key="label.tagged"/></option>
                </select>
            </li>
            <li style="display: none" id="add_publicip_vlan_vlan_container">
                <label>
                    <fmt:message key="label.vlan.id"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_scope_container">
                <label for="add_publicip_vlan_scope">
                    <fmt:message key="label.scope"/>:</label>
                <select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
                    <!--  
                    <option value="zone-wide">zone-wide</option>
                    <option value="account-specific">account-specific</option>
                    -->
                </select>
            </li>
            <li style="display: none" id="domain_container">
                <label>
                    <fmt:message key="label.domain"/>:</label>
                
                <input class="text" type="text" id="domain" />
                <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                <!--  
                <select class="select" id="add_publicip_vlan_domain">
                </select>
                -->
            </li>
            <li style="display: none" id="add_publicip_vlan_account_container">
                <label>
                    <fmt:message key="label.account"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.gateway"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.netmask"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.ip.range"/>:</label>
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_external_firewall" title='<fmt:message key="label.add.firewall"/>' style="display: none">   
    <p>
		<fmt:message key="message.add.firewall"/>: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>   
            <li>
                <label>
                    <fmt:message key="label.ip"/>:</label>
                <input class="text" type="text" id="ip" />
                <div id="ip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" id="username" />
                <div id="username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" id="password" autocomplete="off" />
                <div id="password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>  
            
            <li>
                <label>
                    <fmt:message key="label.public.interface"/>:</label>
                <input class="text" type="text" id="public_interface" />
                <div id="public_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>         
            <li>
                <label>
                    <fmt:message key="label.private.interface"/>:</label>
                <input class="text" type="text" id="private_interface" />
                <div id="private_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>             
            <li>
                <label>
                    <fmt:message key="label.usage.interface"/>:</label>
                <input class="text" type="text" id="usage_interface" />
                <div id="usage_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>                  
            <li>
                <label>
                    <fmt:message key="label.public.zone"/>:</label>
                <input class="text" type="text" id="public_zone" />
                <div id="public_zone_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>       
            <li>
                <label>
                    <fmt:message key="label.private.zone"/>:</label>
                <input class="text" type="text" id="private_zone" />
                <div id="private_zone_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>    
            <li>
                <label>
                    <fmt:message key="label.numretries"/>:</label>
                <input class="text" type="text" id="numretries" />
                <div id="numretries_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>               
            <li>
                <label>
                    <fmt:message key="label.timeout.in.second"/>:</label>
                <input class="text" type="text" id="timeout" />
                <div id="timeout_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_load_balancer" title='<fmt:message key="label.add.load.balancer"/>' style="display: none">   
    <p>
		<fmt:message key="message.add.load.balancer"/>: <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>   
            <li>
                <label>
                    <fmt:message key="label.ip"/>:</label>
                <input class="text" type="text" id="ip" />
                <div id="ip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" id="username" />
                <div id="username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" id="password" autocomplete="off" />
                <div id="password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>  
             
            <li>
                <label>
                    <fmt:message key="label.public.interface"/>:</label>
                <input class="text" type="text" id="public_interface" />
                <div id="public_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>         
            <li>
                <label>
                    <fmt:message key="label.private.interface"/>:</label>
                <input class="text" type="text" id="private_interface" />
                <div id="private_interface_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>       
            <li>
                <label>
                    <fmt:message key="label.numretries"/>:</label>
                <input class="text" type="text" id="numretries" />
                <div id="numretries_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>  
            <li>
				<label>
				    <fmt:message key="mode"/>:</label>
				<select class="select" id="mode">						
					<option value="false"><fmt:message key="side.by.side"/></option>
					<option value="true"><fmt:message key="inline"/></option>
				</select>
			</li>       
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_iprange_to_directnetwork" title='<fmt:message key="label.add.ip.range"/>' style="display: none">
    <p>
		<fmt:message key="message.add.ip.range.direct.network"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>         
            <li style="display: none" id="vlan_id_container">
                <label>
                    <fmt:message key="label.vlan.id"/>:</label>
                <input class="text" type="text" id="vlan_id" />
                <div id="vlan_id_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>        
            <li  style="display: none" id="gateway_container">
                <label>
                    <fmt:message key="label.gateway"/>:</label>
                <input class="text" type="text" id="gateway" />
                <div id="gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li  style="display: none" id="netmask_container">
                <label>
                    <fmt:message key="label.netmask"/>:</label>
                <input class="text" type="text"id="netmask" />
                <div id="netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>                       
            <li>
                <label>
                    <fmt:message key="label.ip.range"/>:</label>
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_network_for_zone" title='<fmt:message key="label.add.network"/>' style="display: none">
    <p>
		<fmt:message key="message.add.network"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
			<li>
                <label>
                    <fmt:message key="label.network.name"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_name" id="add_publicip_vlan_network_name" />
                <div id="add_publicip_vlan_network_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li>
                <label>
                    <fmt:message key="label.network.desc"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_network_desc" id="add_publicip_vlan_network_desc" />
                <div id="add_publicip_vlan_network_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li>
                <label>
                    <fmt:message key="label.is.default"/>:</label>
                <select class="select" name="add_publicip_vlan_default" id="add_publicip_vlan_default">
                    <option value="false">No</option>
					<option value="true">Yes</option>
                </select>
            </li>
            <li id="add_publicip_vlan_container">
                <label for="add_publicip_vlan_tagged">
                    <fmt:message key="label.vlan"/>:</label>
                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                    <option value="tagged">tagged</option>
                </select>
            </li>
            <li id="add_publicip_vlan_vlan_container">
                <label>
                    <fmt:message key="label.vlan.id"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_publicip_vlan_scope_container">
                <label for="add_publicip_vlan_scope">
                    <fmt:message key="label.scope"/>:</label>
                <select class="select" id="add_publicip_vlan_scope">     
                    <!--            
                    <option value="zone-wide"><fmt:message key="label.zone.wide"/></option>
                    <option value="account-specific"><fmt:message key="label.account.specific"/></option>  
                    -->                      
                </select>
            </li>           
            <li style="display: none" id="domain_container">
                <label>
                    <fmt:message key="label.domain"/>:</label>
                    
                <input class="text" type="text" id="domain" />
                <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
                <!--  
                <select class="select" id="add_publicip_vlan_domain">
                </select>
                -->    
            </li>
            <li style="display: none" id="add_publicip_vlan_account_container">
                <label>
                    <fmt:message key="label.account"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.gateway"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.netmask"/>:</label>
                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                    style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.ip.range"/>:</label>
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
            <li>
                <label>
                    <fmt:message key="label.network.domain"/>:</label>
                <input class="text" type="text" id="networkdomain" />
                <div id="networkdomain_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    <fmt:message key="label.tags"/>:</label>
                <input class="text" type="text" id="tags" />
                <div id="tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
            <fmt:message key="label.adding"/>....</p>
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
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value='<fmt:message key="label.by.account" />' />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
