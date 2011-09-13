<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 	
	'label.action.edit.template' : '<fmt:message key="label.action.edit.template"/>',
	'label.action.delete.template' : '<fmt:message key="label.action.delete.template"/>',
	'label.action.delete.template.processing' : '<fmt:message key="label.action.delete.template.processing"/>',
	'message.action.delete.template' : '<fmt:message key="message.action.delete.template"/>',
	'message.action.delete.template.for.all.zones' : '<fmt:message key="message.action.delete.template.for.all.zones"/>',
	'label.action.copy.template' : '<fmt:message key="label.action.copy.template"/>',
	'label.action.copy.template.processing' : '<fmt:message key="label.action.copy.template.processing"/>',
	'label.action.create.vm' : '<fmt:message key="label.action.create.vm"/>',
	'label.action.create.vm.processing' : '<fmt:message key="label.action.create.vm.processing"/>',
	'label.action.download.template' : '<fmt:message key="label.action.download.template"/>',
	'message.download.template': '<fmt:message key="message.download.template"/>'	
};	
</script>

<!-- template detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_templatesicon.gif"/></div>
    <h1>
        <fmt:message key="label.template"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <fmt:message key="label.details"/></div>
    </div>    
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
	                        <fmt:message key="label.hypervisor"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="hypervisor">
	                    </div>                   
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="templatetype">
	                    </div>                   
	                </div>
	            </div>	         
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.size"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="size">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="extractable"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="isextractable">                        
	                    </div>	                    
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.password.enabled"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="passwordenabled">                        
	                    </div>
	                    <select class="select" id="passwordenabled_edit" style="width: 202px; display: none;">
	                        <option value="false"><fmt:message key="label.no"/></option>
							<option value="true"><fmt:message key="label.yes"/></option>
	                    </select>
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
	                        <fmt:message key="label.featured"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="isfeatured">                        
	                    </div>
	                    <select class="select" id="isfeatured_edit" style="width: 202px; display: none;">
	                        <option value="true"><fmt:message key="label.yes"/></option>
							<option value="false"><fmt:message key="label.no"/></option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.cross.zones"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="crossZones">                        
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.os.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ostypename">
	                    </div>
	                    <select class="select" id="ostypename_edit" style="width: 202px; display: none;">                      
	                    </select>
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
	                        <fmt:message key="label.status"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="status">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd" id="progressbar_container">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.download.progress"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div id="progressbar"></div>
	                </div>
	            </div>        
	        </div>
	        <div class="grid_botactionpanel">
	        	<div class="gridbot_buttons" id="save_button" style="display:none;"><fmt:message key="label.save"/></div>
	            <div class="gridbot_buttons" id="cancel_button" style="display:none;"><fmt:message key="label.cancel"/></div>
	        </div>
        </div>   
    </div>
</div>
<!-- template detail panel (end) -->

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_template_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Template" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.template"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Copy Template Dialog (begin) -->
<div id="dialog_copy_template" title='<fmt:message key="label.action.copy.template"/>' style="display:none">
	<p><fmt:message key="message.copy.template"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form3">
			<ol>				
				<li>
                    <label><fmt:message key="label.zone"/>:</label>
                    <select class="select" id="copy_template_zone">  
                        <option value=""></option>                      
                    </select>
                </li>		
			</ol>
			<div id="copy_template_zone_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
		</form>
	</div>
</div>
<!--  Copy Template Dialog (end) -->

<!-- Create VM from template (begin) -->
<div id="dialog_create_vm_from_template" title='<fmt:message key="label.action.create.vm"/>' style="display:none">
	<p><fmt:message key="message.create.template"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label><fmt:message key="label.name"/>:</label>
					<input class="text" type="text" id="name"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><fmt:message key="label.group"/>:</label>
					<input class="text" type="text" id="group"/>
					<div id="group_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label><fmt:message key="label.service.offering"/>:</label>
                    <select class="select" id="service_offering">
                    </select>
                </li>					
				<li>
                    <label><fmt:message key="label.disk.offering"/>:</label>
                    <select class="select" id="disk_offering">
                    </select>
                </li>	  
                <li id="size_container">
                    <label>
                        <fmt:message key="label.size"/>:</label>
                    <input class="text" type="text" id="size" />
                    <div id="size_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>
                </li>    		
			</ol>
		</form>
	</div>
</div>
<!-- Create VM from template (end) -->

<!-- Add Template Dialog (begin) -->
<div id="dialog_add_template" title='<fmt:message key="label.add.template"/>' style="display:none">
	<p><fmt:message key="message.add.template"/></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name"><fmt:message key="label.name"/>:</label>
					<input class="text" type="text" name="add_template_name" id="add_template_name" style="width:250px"/>
					<div id="add_template_name_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;"></div>
				</li>
				<li>
					<label for="user_name"><fmt:message key="label.display.text"/>:</label>
					<input class="text" type="text" name="add_template_display_text" id="add_template_display_text" style="width:250px"/>
					<div id="add_template_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0"></div>
				</li>
				<li>
					<label for="user_name"><fmt:message key="label.url"/>:</label>
					<input class="text" type="text" name="add_template_url" id="add_template_url" style="width:250px"/>
					<div id="add_template_url_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0"></div>
				</li>
				<li>
                    <label><fmt:message key="label.zone"/>:</label>
                    <select class="select" id="add_template_zone">
                    </select>
                </li>					
				<li>
					<label for="add_template_os_type"><fmt:message key="label.os.type"/>:</label>
					<select class="select" name="add_template_os_type" id="add_template_os_type">
					</select>
				</li>					
				<li>
					<label for="add_template_hypervisor"><fmt:message key="label.hypervisor"/>:</label>
					<select class="select" name="add_template_hypervisor" id="add_template_hypervisor">		
					    <!--  			
						<option value='XenServer'>Citrix XenServer</option>
						<option value='VmWare'>VMware ESX</option>
						<option value='KVM'>KVM</option>
						-->	
					</select>
				</li>
				<li>
					<label for="add_template_format"><fmt:message key="label.format"/>:</label>
					<select class="select" name="add_template_format" id="add_template_format">
					</select>
				</li>
				<li>
					<label><fmt:message key="extractable"/>:</label>
					<select class="select" id="isextractable">						
						<option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>
					</select>
				</li>
				<li>
					<label><fmt:message key="label.password.enabled"/>:</label>
					<select class="select" id="add_template_password">						
						<option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>
					</select>
				</li>
				<li id="add_template_public_container" style="display:none">
					<label><fmt:message key="label.public"/>:</label>
					<select class="select" id="add_template_public">
						<option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>						
					</select>
				</li>				
				<li id="add_template_featured_container" style="display:none">
					<label><fmt:message key="label.featured"/>:</label>
					<select class="select" id="add_template_featured">
					    <option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>						
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>

<!-- Download Template Dialog (begin) -->
<div id="dialog_download_template" title='<fmt:message key="label.action.download.template"/>' style="display: none">    
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox">
        <div class="ui_dialog_loader">
        </div>
        <p>
            <fmt:message key="label.generating.url"/>....</p>
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
<!-- Download Template Dialog (end) -->
<!-- Add Template Dialog (end) -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
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
