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
	'message.action.delete.pod' : '<fmt:message key="message.action.delete.pod"/>'
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

</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_cluster_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Cluster" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.cluster"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Host" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.host"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_primarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Primary Storage" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.primary.storage"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_iprange_button" style="display: none">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add IP Range" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.ip.range"/>
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
        <div class="grid_actionbox" id="network_action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="network_action_menu" style="display: none;">
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


<!-- ***** dialogs (begin) ***** -->
<!-- Add Host Dialog (begin) -->
<div id="dialog_add_host" title="Add Host" style="display: none">
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
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_host">
                    <fmt:message key="label.esx.host"/>:</label>
                <input class="text" type="text" name="host_vcenter_host" id="host_vcenter_host" />
                <div id="host_vcenter_host_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<div id="dialog_add_external_cluster" title="Add Cluster" style="display: none">
    <p>
		<fmt:message key="message.add.cluster"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="cluster_hypervisor"><fmt:message key="label.hypervisor"/>:</label>
                <select class="select" id="cluster_hypervisor">
                    <!--  
                    <option value="Xen Server" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>		
                    -->								
                </select>
            </li>
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
<div id="dialog_add_pool" title="Add Primary Storage" style="display: none">
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
                    <option value="nfs">NFS</option>
                    <option value="iscsi">ISCSI</option>
                    <option value="vmfs">VMFS</option>
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
                    <fmt:message key="label.tags"/>:</label>
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
<div id="dialog_add_iprange_to_pod" title="Add IP Range to Pod" style="display:none">
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
<!-- ***** dialogs (begin) ***** -->
