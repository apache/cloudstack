<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.pod' : '<fmt:message key="label.action.edit.pod"/>',
	'label.action.delete.pod' : '<fmt:message key="label.action.delete.pod"/>',
	'label.action.delete.pod.processing' : '<fmt:message key="label.action.delete.pod.processing"/>',
	'message.action.delete.pod' : '<fmt:message key="message.action.delete.pod"/>',
	'label.action.enable.pod' : '<fmt:message key="label.action.enable.pod"/>',
	'label.action.enable.pod.processing' : '<fmt:message key="label.action.enable.pod.processing"/>',
	'message.action.enable.pod' : '<fmt:message key="message.action.enable.pod"/>',
	'label.action.disable.pod' : '<fmt:message key="label.action.disable.pod"/>',
	'label.action.disable.pod.processing' : '<fmt:message key="label.action.disable.pod.processing"/>',
	'message.action.disable.pod' : '<fmt:message key="message.action.disable.pod"/>'		
};	
</script>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_podicon.gif" /></div>
    <h1>
        <fmt:message key="label.pod"/>
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
        <div class="content_tabs off" id="tab_ipallocation">
            <fmt:message key="label.ip.allocations"/></div>
       <div class="content_tabs off" id="tab_networkdevice">
            <fmt:message key="label.network.device"/></div>   
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
	                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
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
	                    <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
		                <div id="name_edit_errormsg" style="display:none"></div>
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
	                    <input class="text" id="netmask_edit" style="width: 200px; display: none;" type="text" />
		                <div id="netmask_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.private.ip.range"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ipRange">
	                    </div>                                        
	                    <input class="text" id="startIpRange_edit" style="width: 100px; display: none;" type="text" />
		                <div id="startIpRange_edit_errormsg" style="display:none"></div>  	                    
		                <input class="text" id="endIpRange_edit" style="width: 100px; display: none;" type="text" />
		                <div id="endIpRange_edit_errormsg" style="display:none"></div>  
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
	                    <input class="text" id="gateway_edit" style="width: 200px; display: none;" type="text" />
		                <div id="gateway_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>
	             <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <fmt:message key="label.state"/>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="allocationstate">
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
        
 	<!-- Network tab (start)-->
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
    <!-- Network tab (end)-->

    <!-- Network Device tab (start)-->
    <div style="display: none;" id="tab_content_networkdevice">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">           
        </div>
    </div> 
    <!-- Network Device tab (end)-->
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_cluster_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.cluster"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.cluster"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.host"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.host"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_primarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.primary.storage"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.primary.storage"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_iprange_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.ip.range"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.ip.range"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_network_device_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt='<fmt:message key="label.add.network.device"/>' /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.network.device"/>
            </div>
        </div>
    </div>    
</div>
<!--  top buttons (end) -->

<!--  Network tab template (begin) -->
<div class="grid_container" id="network_tab_template" style="display: none">
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
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.id"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.guest.ip.range"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="iprange">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.guest.netmask"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="netmask">
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
                <fmt:message key="label.pod"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="podname">
            </div>
        </div>
    </div>   
</div>
<!--  Network tab template (end) -->


<!--  Network Device tab template (begin) -->
<div class="grid_container" id="network_device_tab_template" style="display: none">
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
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.id"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.url"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="url">
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
       
    <div class="grid_rows odd" id="pingstorageserverip_container" style="display:none">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.PING.storage.IP"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="pingstorageserverip">
            </div>
        </div>
    </div>
    <div class="grid_rows even" id="pingdir_container" style="display:none">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.PING.dir"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="pingdir">
            </div>
        </div>
    </div>
    <div class="grid_rows odd" id="tftpdir_container" style="display:none">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.TFTP.dir"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="tftpdir">
            </div>
        </div>
    </div>
   
</div>
<!--  Network Device tab template (end) -->


<!-- ***** dialogs (begin) ***** -->
<!-- Add Host Dialog (begin) -->
<div id="dialog_add_host" title='<fmt:message key="label.add.host"/>' style="display: none">
    <p>
		<fmt:message key="message.add.host"/>
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
<!-- Add Host Dialog (end) -->

<!-- Add Hypervisor managed cluster Dialog (begin) -->
<div id="dialog_add_external_cluster" title='<fmt:message key="label.add.cluster"/>' style="display: none">
    <p>
		<fmt:message key="message.add.cluster"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="cluster_hypervisor"><fmt:message key="label.hypervisor"/>:</label>
                <select class="select" id="cluster_hypervisor">                  						
                </select>
            </li>
<!-- CloudManaged cluster for VMware is disabled for now, it may be added back when we want to manage ESXi host directly  -->            
<!--              
            <li input_group="vmware">
                <label>
                    <fmt:message key="label.cluster.type"/>:</label>
                <select class="select" id="type_dropdown">
                    <option value="CloudManaged"><fmt:message key="label.cloud.managed"/></option>		
                    <option value="ExternalManaged" SELECTED><fmt:message key="label.vsphere.managed"/></option>										
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
-->            
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_hostname">
                    <fmt:message key="label.vcenter.host"/>:</label>
                <input class="text" type="text" name="cluster_hostname" id="cluster_hostname" />
                <div id="cluster_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_username">
                    <fmt:message key="label.vcenter.username"/>:</label>
                <input class="text" type="text" name="cluster_username" id="cluster_username" />
                <div id="cluster_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_password">
                    <fmt:message key="label.vcenter.password"/>:</label>
                <input class="text" type="password" name="cluster_password" id="cluster_password" autocomplete="off" />
                <div id="cluster_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_datacenter">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="cluster_datacenter" id="cluster_datacenter" />
                <div id="cluster_datacenter_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>

            <li>
                <label for="cluster_name" id="cluster_name_label">
                    <fmt:message key="label.vcenter.cluster"/>:</label>
                <input class="text" type="text" name="cluster_name" id="cluster_name" />
                <div id="cluster_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Hypervisor managed cluster Dialog (end) -->

<!-- Add Primary Storage Dialog (begin) -->
<div id="dialog_add_pool" title='<fmt:message key="label.add.primary.storage"/>' style="display: none">
    <p>
		<fmt:message key="message.add.primary.storage"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    <fmt:message key="label.cluster"/>:</label>
                <select class="select" id="pool_cluster">
                </select>
                <div id="pool_cluster_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">
                    <fmt:message key="label.protocol"/>:</label>
                <select class="select" id="add_pool_protocol">                    
                </select>
            </li>
            <li id="add_pool_server_container">
                <label for="add_pool_nfs_server" >
                    <fmt:message key="label.server"/>:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container" input_group="nfs">
                <label for="add_pool_path">
                    <fmt:message key="label.path"/>:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none" input_group="iscsi">
                <label for="add_pool_iqn">
                    <fmt:message key="label.target.iqn"/>:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none" input_group="iscsi">
                <label for="add_pool_lun">
                    <fmt:message key="label.lun"/> #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_clvm_vg_container" style="display: none" input_group="clvm">
                <label for="add_pool_clvm_vg">
                    <fmt:message key="label.volgroup"/>:</label>
                <input class="text" type="text" name="add_pool_clvm_vg" id="add_pool_clvm_vg" />
                <div id="add_pool_clvm_vg_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_dc">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="add_pool_vmfs_dc" id="add_pool_vmfs_dc" />
                <div id="add_pool_vmfs_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_ds">
                    <fmt:message key="label.vcenter.datastore"/>:</label>
                <input class="text" type="text" name="add_pool_vmfs_ds" id="add_pool_vmfs_ds" />
                <div id="add_pool_vmfs_ds_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_tags_container">
                <label for="add_pool_tags">
                    <fmt:message key="label.storage.tags"/>:</label>
                <input class="text" type="text" id="add_pool_tags" />
                <div id="add_pool_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Primary Storage Dialog (end) -->
   
<!-- Add IP Range to pod (begin) -->
<div id="dialog_add_iprange_to_pod" title='<fmt:message key="label.add.ip.range"/>' style="display:none">
	<fmt:message key="message.add.ip.range.to.pod"/>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>				    		
				<li>
					<label><fmt:message key="label.guest.ip.range"/>:</label>
					<input class="text" style="width:67px" type="text" id="startip"/><span>-</span>
                    <input class="text" style="width:67px" type="text" id="endip"/>
					<div id="startip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="endip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="netmask"><fmt:message key="label.guest.netmask"/>:</label>
					<input class="text" type="text" id="netmask"/>
					<div id="netmask_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				
				<li>
					<label><fmt:message key="label.guest.gateway"/>:</label>
					<input class="text" type="text" id="guestgateway"/>
					<div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
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
<!-- Add IP Range to pod (end) -->

<!-- Add Network Device (begin) -->
<div id="dialog_add_network_device" title='<fmt:message key="label.add.network.device"/>' style="display:none">	
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>				    		
				<li input_group="general">
					<label>
                    <fmt:message key="label.type"/>:</label>
                    <select class="select" id="network_device_type">                       
                        <option value="ExternalDhcp">ExternalDhcp</option>
                        <option value="PxeServer">PxeServer</option>
                    </select>
                    <div id="network_device_type_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>                
				</li>
				<li input_group="general">
					<label><fmt:message key="label.url"/>:</label>
					<input class="text" type="text" id="url"/>
					<div id="url_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li input_group="general">
					<label><fmt:message key="label.username"/>:</label>
					<input class="text" type="text" id="username"/>
					<div id="username_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
				<li input_group="general">
					<label><fmt:message key="label.password"/>:</label>
					<input class="text" type="password" id="password"/>
					<div id="password_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li input_group="ExternalDhcp" style="display:none;" id="DHCP_server_type_container">
					<label>
                    <fmt:message key="label.DHCP.server.type"/>:</label>
                    <select class="select" id="DHCP_server_type">                       
                        <option value="Dhcpd">Dhcpd</option>
                        <option value="Dnsmasq">Dnsmasq</option>
                    </select>
                    <div id="DHCP_server_type_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>                
				</li>	
				<li input_group="PxeServer" style="display:none;" id="Pxe_server_type_container">
					<label>
                    <fmt:message key="label.Pxe.server.type"/>:</label>
                    <select class="select" id="Pxe_server_type">                       
                        <option value="PING">PING</option>
                        <option value="DMCD">DMCD</option>
                    </select>
                    <div id="Pxe_server_type_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>                
				</li>
				<li input_group="PxeServer" style="display:none;" id="PING_storage_IP_container">
					<label><fmt:message key="label.PING.storage.IP"/>:</label>
					<input class="text" type="text" id="PING_storage_IP"/>
					<div id="PING_storage_IP_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li input_group="PxeServer" style="display:none;" id="PING_dir_container">
					<label><fmt:message key="label.PING.dir"/>:</label>
					<input class="text" type="text" id="PING_dir"/>
					<div id="PING_dir_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>		
				<li input_group="PxeServer" style="display:none;" id="TFTP_dir_container">
					<label><fmt:message key="label.TFTP.dir"/>:</label>
					<input class="text" type="text" id="TFTP_dir"/>
					<div id="TFTP_dir_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>		
				<li input_group="PxeServer" style="display:none;" id="PING_CIFS_username_container">
					<label><fmt:message key="label.PING.CIFS.username"/>:</label>
					<input class="text" type="text" id="PING_CIFS_username"/>
					<div id="PING_CIFS_username_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>		
				<li input_group="PxeServer" style="display:none;" id="PING_CIFS_password_container">
					<label><fmt:message key="label.PING.CIFS.password"/>:</label>
					<input class="text" type="password" id="PING_CIFS_password"/>
					<div id="PING_CIFS_password_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
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
<!-- Add Network Device (end) -->

<!-- ***** dialogs (begin) ***** -->
