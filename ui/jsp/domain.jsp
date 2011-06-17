<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.domain' : '<fmt:message key="label.action.edit.domain"/>',
	'label.action.update.resource.count' : '<fmt:message key="label.action.update.resource.count"/>',
	'label.action.update.resource.count.processing' : '<fmt:message key="label.action.update.resource.count.processing"/>',	
	'label.action.delete.domain' : '<fmt:message key="label.action.delete.domain"/>',
	'label.action.delete.domain.processing' : '<fmt:message key="label.action.delete.domain.processing"/>',
	'message.action.delete.domain' : '<fmt:message key="message.action.delete.domain"/>',
	'label.action.edit.resource.limits' : '<fmt:message key="label.action.edit.resource.limits"/>'
};	
</script>

<!-- domain detail panel (begin) -->
<div class="main_title" id="right_panel_header">
  
    <div class="main_titleicon">
        <img src="images/title_domainicon.gif" /></div>
   
    <h1>
        <fmt:message key="label.menu.domains"/>
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
        <div class="content_tabs off" id="tab_admin_account">
            <fmt:message key="label.admin.accounts"/></div>
        <div class="content_tabs off" id="tab_resource_limits">
            <fmt:message key="label.resource.limits"/></div>
    </div>  
    <!-- Details tab (start)-->
    <div id="tab_content_details">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">    
        	<div class="grid_header">
            	<div id="grid_header_title" class="grid_header_title">(title)</div>
                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                        	<li><fmt:message key="label.no.actions"/></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999; display: none;">
                    <div class="gridheader_loader" id="Div1">
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
                        <fmt:message key="label.accounts"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="redirect_to_account_page">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.instances"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="redirect_to_instance_page">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.volumes"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="redirect_to_volume_page">
                    </div>
                </div>
            </div>
        </div>        
        <div class="grid_botactionpanel">
	        <div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
	        <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
	    </div>         
    </div>    
    <!-- Details tab (end)-->
    
    <!-- Admin Account tab (start)-->
    <div style="display: none;" id="tab_content_admin_account">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div>     
    <!-- Admin Account tab (end)-->
    
    <!-- Resource Limits tab (start)-->
    <div id="tab_content_resource_limits" style="display:none">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">          
            <div class="grid_header">
        	    <div id="grid_header_title" class="grid_header_title"></div>
                <div id="action_link" class="grid_actionbox"><p><fmt:message key="label.actions"/></p>
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                    	    <li><fmt:message key="label.no.actions"/></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999; display: none;">
                    <div class="gridheader_loader" id="Div1">
                    </div>
                    <p id="description">
                        <fmt:message key="label.waiting"/> &hellip;</p>
                </div>
            </div>        
                
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.instance.limits"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_vm">
                    </div>
                    <input class="text" id="limits_vm_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_vm_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.ip.limits"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_ip">
                    </div>
                    <input class="text" id="limits_ip_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_ip_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.volume.limits"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_volume">
                    </div>
                    <input class="text" id="limits_volume_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_volume_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.snapshot.limits"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_snapshot">
                    </div>
                    <input class="text" id="limits_snapshot_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_snapshot_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <fmt:message key="label.template.limits"/>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_template">
                    </div>
                    <input class="text" id="limits_template_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_template_edit_errormsg" style="display:none"></div>
                </div>
            </div>
        </div>
        <div class="grid_botactionpanel">
        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
        </div>
    </div>
    <!-- Resource Limits tab (start)-->    

<!-- domain detail panel (end) -->

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_domain_button" style="display:none;">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Domain" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.domain"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- admin account tab template (begin) -->
<div class="grid_container" id="admin_account_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
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
                <fmt:message key="label.role"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="role">
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
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.vms"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vm_total">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.ips"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ip_total">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.bytes.received"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="bytes_received">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.bytes.sent"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="bytes_sent">
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
</div>
<!-- admin account tab template (end) -->

<!-- Add Domain Dialog (begin) -->
<div id="dialog_add_domain" title='<fmt:message key="label.add.domain"/>' style="display:none">
	<p></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form1">
			<ol>		
				<li>
					<label><fmt:message key="label.name"/>:</label>
					<input class="text" type="text" id="add_domain_name"/>
					<div id="add_domain_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
				<li>
					<label><fmt:message key="label.parent.domain"/>:</label>					
					<input class="text" type="text" id="parent_domain" />
                	<div id="parent_domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
					<!--  
					<select class="select" id="domain_dropdown">						
					</select>
					-->
				</li>		
			</ol>
		</form>
	</div>
</div>
<!-- Add Domain Dialog (end) -->

<!-- Confirm to delete domain (begin) -->
<div id="dialog_confirmation_delete_domain" title='<fmt:message key="label.confirmation"/>' style="display: none">
 	<p> 
		<fmt:message key="message.action.delete.domain" />
	</p> 		
    <div class="dialog_formcontent" id="force_delete_domain_container" style="display:none">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li style="padding-top:10px">
                <input type="checkbox" class="checkbox" id="force_delete_domain" /> 
                <p style="color:red"><fmt:message key="force.delete" /></p>		
            </li>
            <li>
                <p style="color:red"><fmt:message key="force.delete.domain.warning" /></p>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Confirm to delete domain (end) -->