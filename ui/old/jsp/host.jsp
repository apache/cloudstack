<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.host' : '<fmt:message key="label.action.edit.host"/>',
	'label.action.enable.maintenance.mode' : '<fmt:message key="label.action.enable.maintenance.mode"/>',
	'label.action.enable.maintenance.mode.processing' : '<fmt:message key="label.action.enable.maintenance.mode.processing"/>',
	'message.action.host.enable.maintenance.mode' : '<fmt:message key="message.action.host.enable.maintenance.mode"/>',
	'label.action.cancel.maintenance.mode' : '<fmt:message key="label.action.cancel.maintenance.mode"/>',
	'label.action.cancel.maintenance.mode.processing' : '<fmt:message key="label.action.cancel.maintenance.mode.processing"/>',
	'message.action.cancel.maintenance.mode' : '<fmt:message key="message.action.cancel.maintenance.mode"/>',
	'label.action.force.reconnect' : '<fmt:message key="label.action.force.reconnect"/>',
	'label.action.force.reconnect.processing' : '<fmt:message key="label.action.force.reconnect.processing"/>',
	'message.action.force.reconnect' : '<fmt:message key="message.action.force.reconnect"/>',
	'label.action.remove.host' : '<fmt:message key="label.action.remove.host"/>',
	'label.action.remove.host.processing' : '<fmt:message key="label.action.remove.host.processing"/>',
	'message.action.remove.host' : '<fmt:message key="message.action.remove.host"/>',
	'message.action.enable.maintenance' : '<fmt:message key="message.action.enable.maintenance"/>',
	'message.action.cancel.maintenance' : '<fmt:message key="message.action.cancel.maintenance"/>',
	'message.action.force.reconnect' : '<fmt:message key="message.action.force.reconnect"/>',
	'label.action.update.OS.preference' : '<fmt:message key="label.action.update.OS.preference"/>',
	'label.action.update.OS.preference.processing' : '<fmt:message key="label.action.update.OS.preference.processing"/>'
};	
</script>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_clustericon.gif" /></div>
    <h1>
       <fmt:message key="label.host"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <fmt:message key="label.details"/></div>  
        <div class="content_tabs off" id="tab_instance">
            <fmt:message key="label.instances"/></div>
        <div class="content_tabs off" id="tab_router">
            <fmt:message key="label.virtual.appliances"/></div>
        <div class="content_tabs off" id="tab_systemvm">
            <fmt:message key="label.system.vms"/></div>
        <div class="content_tabs off" id="tab_statistics">
            <fmt:message key="label.statistics"/></div>
    </div>
    <!-- Details tab (start)-->
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
	                <div id="grid_header_title" class="grid_header_title">
	                    (title)</div>
	                <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
	                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                        <ul class="actionsdropdown_boxlist" id="action_list">
	                            <li>
	                                <fmt:message key="label.no.actions"/></li>
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
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.state"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="state">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.host.tags"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hosttags">
	                    </div>
	                    <input class="text" id="hosttags_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="hosttags_edit_errormsg" style="display:none"></div>	                    
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="type">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.zone"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zonename">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.pod"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="podname">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.cluster"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="clustername">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.ip.address"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ipaddress">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.version"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="version">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.os.preference"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="oscategoryname">
	                    </div>	                    
	                    <select class="select" id="os_dropdown" style="width: 202px; display: none;">                    
                	    </select>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.last.disconnected"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="disconnected">
	                    </div>
	                </div>
	            </div>
	            
	        </div>
	        
	        <div class="grid_botactionpanel">
	        	<div class="gridbot_buttons" id="save_button" style="display:none;"><fmt:message key="label.save"/></div>
	            <div class="gridbot_buttons" id="cancel_button" style="display:none;"><fmt:message key="label.cancel"/></div>
	        </div> 
	        
	    </div>        
    </div>
    <!-- Details tab (end)-->
      
    <!--Primary Storage tab (start)-->
    <div style="display: none;" id="tab_content_primarystorage">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Primary Storage tab (end)-->  
           
    <!--Instance tab (start)-->
    <div style="display: none;" id="tab_content_instance">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Instance tab (end)-->
    
    <!--router tab (start)-->
    <div style="display: none;" id="tab_content_router">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--router tab (end)-->
    
    <!--systemvm tab (start)-->
    <div style="display: none;" id="tab_content_systemvm">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--systemvm tab (end)-->
    
    
    <!--Statistics tab (start)-->
    <div style="display: none;" id="tab_content_statistics">
        <div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container"> 
            <div class="grid_container">
        	    <div class="grid_header">
            	    <div id="grid_header_title" class="grid_header_title"></div>
                </div>
                
                <!--  
                <div class="dbrow odd" id="cpu_barchart">
                    <div class="dbrow_cell" style="width: 40%;">
                        <div class="dbgraph_titlebox">
                            <h2>
                                <fmt:message key="label.cpu"/></h2>
                            <div class="dbgraph_title_usedbox">
                                <p>
                                    Total: <span id="capacityused">
	                                    <span id="cpunumber">M</span> 
	                                    x 
	                                    <span id="cpuspeed">N</span> 
                                    </span>
                                </p>
                            </div>
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 43%; border: none;">
                        <div class="db_barbox low" id="bar_chart">
                        </div>
                    </div>
                    <div class="dbrow_cell" style="width: 16%; border: none;">
                        <div class="db_totaltitle" id="percentused">
                        K%
                        </div>
                    </div>
                </div>
                -->
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.total.cpu"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles">
                            <span id="cpunumber">M</span> 
	                        x 
	                        <span id="cpuspeed">N</span> 
                        </div>
                    </div>
                </div>
                
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.cpu.utilized"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="percentused">
                        </div>
                    </div>
                </div>
                
                
                
                
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.cpu.allocated.for.VMs"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="cpuallocated">
                        </div>
                    </div>
                </div>
                
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.memory.total"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="memorytotal">
                        </div>
                    </div>
                </div>
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.memory.allocated"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="memoryallocated">
                        </div>
                    </div>
                </div>
                
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.memory.used"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="memoryused">
                        </div>
                    </div>
                </div>                
                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.network.read"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbsread">
                        </div>
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.network.write"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networkkbswrite">
                        </div>
                    </div>
                </div>  
            </div>
        </div>   
    </div>
    <!--Statistics tab (end)--> 
</div>

<!--  top buttons (begin) -->
<div id="top_buttons"> 
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.host"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.host"/>
            </div>
        </div>
    </div>    
</div>
<!--  top buttons (end) -->

<!--  instance tab template (begin) -->
<div class="grid_container" id="instance_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="snapshot_action_link" style="display: none;"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="snapshot_action_menu" style="display: none;">
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
                <fmt:message key="label.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.service.offering"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="serviceOfferingName">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.created"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.account"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.domain"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>  
</div>
<!--  instance tab template (end) -->

<!--  router tab template (begin) -->
<div class="grid_container" id="router_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="snapshot_action_link" style="display: none;"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="snapshot_action_menu" style="display: none;">
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
                <fmt:message key="label.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.public.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publicip">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.private.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privateip">
            </div>
        </div>
    </div>   
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
               <fmt:message key="label.guest.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="guestipaddress">
            </div>
        </div>
    </div>   
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.created"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
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
</div>
<!--  router tab template (end) -->

<!--  systemvm tab template (begin) -->
<div class="grid_container" id="systemvm_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="snapshot_action_link" style="display: none;"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="snapshot_action_menu" style="display: none;">
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
                <fmt:message key="label.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.system.vm.type"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="systemvmtype">
            </div>
        </div>
    </div>
    
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.public.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="publicip">
            </div>
        </div>
    </div>   
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.private.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="privateip">
            </div>
        </div>
    </div>
	<div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.linklocal.ip"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="linklocalip">
            </div>
        </div>
    </div> 
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.created"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
            </div>
        </div>
    </div>   
</div>
<!--  systemvm tab template (end) -->

<!-- Add Host Dialog -->
<div id="dialog_add_host" title='<fmt:message key="label.add.host"/>' style="display: none">
    <p><fmt:message key="message.add.host"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>    
            <li>
                <label>
                    <fmt:message key="label.cluster"/>:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>     
            <li input_group="general">
                <label for="host_hostname">
                    <fmt:message key="label.host.name"/>:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
<!--              
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_address">
                    <fmt:message key="label.vcenter.host"/>:</label>
                <input class="text" type="text" name="host_vcenter_address" id="host_vcenter_address" />
                <div id="host_vcenter_address_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_username">
                    <fmt:message key="label.vcenter.username"/>:</label>
                <input class="text" type="text" name="host_vcenter_username" id="host_vcenter_username" />
                <div id="host_vcenter_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_password">
                    <fmt:message key="label.vcenter.password"/>:</label>
                <input class="text" type="password" name="host_vcenter_password" id="host_vcenter_password" autocomplete="off" />
                <div id="host_vcenter_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_dc">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="host_vcenter_dc" id="host_vcenter_dc" />
                <div id="host_vcenter_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
-->            
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_host">
                    <fmt:message key="label.esx.host"/>:</label>
                <input class="text" type="text" name="host_vcenter_host" id="host_vcenter_host" />
                <div id="host_vcenter_host_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_cpucores">
                    # of CPU Cores:</label>
                <input class="text" type="text" name="host_baremetal_cpucores" id="host_baremetal_cpucores" />
                <div id="host_baremetal_cpucores_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_cpu">
                    CPU (in MHz):</label>
                <input class="text" type="text" name="host_baremetal_cpu" id="host_baremetal_cpu" />
                <div id="host_baremetal_cpu_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_memory">
                    Memory (in MB):</label>
                <input class="text" type="text" name="host_baremetal_memory" id="host_baremetal_memory" />
                <div id="host_baremetal_memory_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_mac">
                    Host MAC:</label>
                <input class="text" type="text" name="host_baremetal_mac" id="host_baremetal_mac" />
                <div id="host_baremetal_mac_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="Ovm" style="display: none;">
                <label>
                    Agent Username:</label>
                <input class="text" type="text" id="agent_username" value="oracle" />
                <div id="agent_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="Ovm" style="display: none;">
                <label>
                    Agent Password:</label>
                <input class="text" type="password" id="agent_password" />
                <div id="agent_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>          
			<li>
                <label input_group="general">                    
					<fmt:message key="label.host.tags"/>:</label>
                <input class="text" type="text" name="host_tags" id="host_tags" />
                <div id="host_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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

<!-- Update OS Preference Dialog -->
<div id="dialog_update_os" title='<fmt:message key="label.action.update.OS.preference"/>' style="display: none">
    <p>
		<fmt:message key="message.update.os.preference"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.os.preference"/>:</label>
                <select class="select" name="host_os" id="host_os">                    
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Confirm to remove host (begin) -->
<div id="dialog_confirmation_remove_host" title='<fmt:message key="label.confirmation"/>' style="display: none">
 	<p> 
		<fmt:message key="message.action.remove.host" />
	</p> 		
    <div class="dialog_formcontent" id="force_remove_host_container" style="display:none">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li style="padding-top:10px">
                <input type="checkbox" class="checkbox" id="force_remove_host" /> 
                <p style="color:red"><fmt:message key="force.remove" /></p>		
            </li>
            <li>
                <p style="color:red"><fmt:message key="force.remove.host.warning" /></p>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Confirm to remove host (end) -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_state">
                        <option value=""><fmt:message key="label.by.state"/></option>
                        <option value="Up">Up</option>
                        <option value="Down">Down</option>
                        <option value="Disconnected">Disconnected</option>
                        <option value="Updating">Updating</option>
                        <option value="Alert">Alert</option>
                        <option value="PrepareForMaintenance">PrepareForMaintenance</option>
                        <option value="Maintenance">Maintenance</option>
                        <option value="ErrorInMaintenance">ErrorInMaintenance</option>
                    </select>
                </li>
                <li>
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
                <li id="adv_search_pod_li" style="display: none;">
                    <select class="select" id="adv_search_pod">
                    </select>
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
