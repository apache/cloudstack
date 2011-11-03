<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.zone' : '<fmt:message key="label.action.edit.zone"/>',
	'label.action.delete.zone' : '<fmt:message key="label.action.delete.zone"/>',
	'label.action.delete.zone.processing' : '<fmt:message key="label.action.delete.zone.processing"/>',
	'message.action.delete.zone' : '<fmt:message key="message.action.delete.zone"/>',
	'label.action.enable.zone' : '<fmt:message key="label.action.enable.zone"/>',
	'label.action.enable.zone.processing' : '<fmt:message key="label.action.enable.zone.processing"/>',
	'message.action.enable.zone' : '<fmt:message key="message.action.enable.zone"/>',
	'label.action.disable.zone' : '<fmt:message key="label.action.disable.zone"/>',
	'label.action.disable.zone.processing' : '<fmt:message key="label.action.disable.zone.processing"/>',
	'message.action.disable.zone' : '<fmt:message key="message.action.disable.zone"/>'	
};	
</script>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_zoneicon.gif" /></div>
   <h1>
        <fmt:message key="label.zone"/>
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
                        Title</div>
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
                        <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="name_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.dns.1"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="dns1">
                        </div>
                        <input class="text" id="dns1_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="dns1_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.dns.2"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="dns2">
                        </div>
                        <input class="text" id="dns2_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="dns2_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.internal.dns.1"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="internaldns1">
                        </div>
                        <input class="text" id="internaldns1_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="internaldns1_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.internal.dns.2"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="internaldns2">
                        </div>
                        <input class="text" id="internaldns2_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="internaldns2_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.network.type"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="networktype">
                        </div>                             
                    </div>
                </div>    
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.security.groups.enabled"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="securitygroupsenabled">
                        </div>                             
                    </div>
                </div>    
                <div class="grid_rows odd" id="vlan_container">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.vlan"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="vlan">
                        </div>
                        <input class="text" id="startvlan_edit" style="width: 100px; display: none;" type="text" />
	                    <div id="startvlan_edit_errormsg" style="display:none"></div>  	                    
	                    <input class="text" id="endvlan_edit" style="width: 100px; display: none;" type="text" />
	                    <div id="endvlan_edit_errormsg" style="display:none"></div>            
                    </div>
                </div>
                <div class="grid_rows even" id="guestcidraddress_container">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.guest.cidr"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="guestcidraddress">
                        </div>
                        <input class="text" id="guestcidraddress_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="guestcidraddress_edit_errormsg" style="display:none"></div>       
                    </div>
                </div>                
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <fmt:message key="label.public"/>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="ispublic">
                        </div>                          
                        <select class="select" id="ispublic_edit" style="width: 202px; display: none;">
	                        <option value="true"><fmt:message key="label.yes"/></option>
							<option value="false"><fmt:message key="label.no"/></option>
	                    </select>             
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
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
	<div class="actionpanel_button_wrapper" id="add_pod_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.pod"/>
            </div>
        </div>
    </div>  
    <div class="actionpanel_button_wrapper" id="add_cluster_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png"/></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.cluster"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.host"/>
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_primarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.primary.storage"/>
            </div>
        </div>
    </div>    
</div>
<!--  top buttons (end) -->


<!-- ***** dialogs ***** (begin)-->
<!-- Add Pod Dialog (begin) -->
<div id="dialog_add_pod" title='<fmt:message key="label.add.pod"/>' style="display: none">
    <p>
        <fmt:message key="message.add.pod"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pod_gateway" style="width: 115px;">
                    <fmt:message key="label.gateway"/>:</label>
                <input class="text" type="text" id="add_pod_gateway" />
                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.netmask"/>:</label>
                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.private.ip.range"/>:</label>
                <input class="text" style="width: 67px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_pod_endip" id="add_pod_endip" />
                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
            <li id="guestip_container" style="display: none">
                <label style="width: 115px;">
                    <fmt:message key="label.guest.ip.range"/>:</label>
                <input class="text" style="width: 92px" type="text" id="startguestip" /><span>-</span>
                <input class="text" style="width: 92px" type="text" id="endguestip" />
                <div id="startguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 133px;">
                </div>
                <div id="endguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="guestnetmask_container" style="display: none">
                <label style="width: 115px;">
                    <fmt:message key="label.guest.netmask"/>:</label>
                <input class="text" type="text" id="guestnetmask" />
                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
                </div>
            </li>
            <li id="guestgateway_container" style="display: none">
                <label style="width: 115px;">
                    <fmt:message key="label.guest.gateway"/>:</label>
                <input class="text" type="text" id="guestgateway" />
                <div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
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
<!-- Add Pod Dialog (end) -->

<!-- Add Host Dialog in zone page (begin) -->
<div id="dialog_add_host_in_zone_page" title='<fmt:message key="label.add.host"/>' style="display: none">   
    <p>
        <fmt:message key="message.add.host"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>    
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>              
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
                <label for="host_password">
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
<!-- Add Host Dialog in zone page (end) -->

<!-- Add Hypervisor managed cluster Dialog (begin) -->
<div id="dialog_add_external_cluster_in_zone_page" title='<fmt:message key="label.add.cluster"/>' style="display: none">
    <p>
		<fmt:message key="message.add.cluster.zone"/>
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
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
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


<!-- Add Primary Storage Dialog  in zone page (begin) -->
<div id="dialog_add_pool_in_zone_page" title='<fmt:message key="label.add.primary.storage"/>' style="display: none"> 
    <p>
		<fmt:message key="message.add.primary.storage"/>
    </p>   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
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
                <label for="add_pool_nfs_server">
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
<!-- Add Primary Storage Dialog  in zone page (end) -->

<!-- ***** dialogs ***** (end)-->

