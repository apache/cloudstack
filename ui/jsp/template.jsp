<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>




<!-- template detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_templatesicon.gif" alt="Instance" /></div>
    <h1>
        Template
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
    </div>    
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
	                    <div class="gridheader_loader" id="Div1">
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
	                        <%=t.t("Zone")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zonename">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Name")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="name">
	                    </div>
	                    <input class="text" id="name_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="name_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Display.Text")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="displaytext">
	                    </div>
	                    <input class="text" id="displaytext_edit" style="width: 200px; display: none;" type="text" />
	                    <div id="displaytext_edit_errormsg" style="display:none"></div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Status")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="status">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Size")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="size">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Password.Enabled")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="passwordenabled">                        
	                    </div>
	                    <select class="select" id="passwordenabled_edit" style="width: 202px; display: none;">
	                        <option value="false">No</option>
							<option value="true">Yes</option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Public")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ispublic">                       
	                    </div>
	                    <select class="select" id="ispublic_edit" style="width: 202px; display: none;">
	                        <option value="true">Yes</option>
							<option value="false">No</option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Featured")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="isfeatured">                        
	                    </div>
	                    <select class="select" id="isfeatured_edit" style="width: 202px; display: none;">
	                        <option value="true">Yes</option>
							<option value="false">No</option>
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Cross.Zones")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="crossZones">                        
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("OS.Type")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="ostypename">
	                    </div>
	                    <select class="select" id="ostypename_edit" style="width: 202px; display: none;">                      
	                    </select>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Account")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
	                    </div>                   
	                </div>
	            </div>
				<div class="grid_rows odd">
					<div class="grid_row_cell" style="width: 20%;">
						<div class="row_celltitles">
							<%=t.t("Domain")%>:</div>
					</div>
					<div class="grid_row_cell" style="width: 79%;">
						<div class="row_celltitles" id="domain">
						</div>
					</div>
				</div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Created")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
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
</div>
<!-- template detail panel (end) -->

<!-- Copy Template Dialog (begin) -->
<div id="dialog_copy_template" title="Copy Template" style="display:none">
	<p>Copy template <b id="copy_template_name_text">XXX</b> from zone <b id="copy_template_source_zone_text">XXX</b> to</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form3">
			<ol>				
				<li>
                    <label>Zone:</label>
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
<div id="dialog_create_vm_from_template" title="Create VM from template" style="display:none">
	<p>Create VM from template <b id="p_name">xxx</b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="name"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Group:</label>
					<input class="text" type="text" id="group"/>
					<div id="group_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label>Service Offering:</label>
                    <select class="select" id="service_offering">
                    </select>
                </li>					
				<li>
                    <label>Disk Offering:</label>
                    <select class="select" id="disk_offering">
                    </select>
                </li>					
			</ol>
		</form>
	</div>
</div>
<!-- Create VM from template (end) -->

<!-- Add Template Dialog (begin) -->
<div id="dialog_add_template" title="Add Template" style="display:none">
	<p>Please enter the following data to create your new template</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="add_template_name" id="add_template_name" style="width:250px"/>
					<div id="add_template_name_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;"></div>
				</li>
				<li>
					<label for="user_name">Display Text:</label>
					<input class="text" type="text" name="add_template_display_text" id="add_template_display_text" style="width:250px"/>
					<div id="add_template_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0"></div>
				</li>
				<li>
					<label for="user_name">URL:</label>
					<input class="text" type="text" name="add_template_url" id="add_template_url" style="width:250px"/>
					<div id="add_template_url_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0"></div>
				</li>
				<li>
                    <label>Zone:</label>
                    <select class="select" id="add_template_zone">
                    </select>
                </li>					
				<li>
					<label for="add_template_os_type">OS Type:</label>
					<select class="select" name="add_template_os_type" id="add_template_os_type">
					</select>
				</li>					
				<li>
					<label for="add_template_hypervisor">Hypervisor:</label>
					<select class="select" name="add_template_hypervisor" id="add_template_hypervisor">						
						<option value='XenServer'>Citrix XenServer</option>
						<option value='VmWare'>VMware ESX</option>
						<option value='KVM'>KVM</option>
					</select>
				</li>
				<li>
					<label for="add_template_format">Format:</label>
					<select class="select" name="add_template_format" id="add_template_format">
					</select>
				</li>	
				<li>
					<label>Password Enabled?:</label>
					<select class="select" id="add_template_password">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>
				<li>
					<label>Public?:</label>
					<select class="select" id="add_template_public">
						<option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>				
				<li id="add_template_featured_container" style="display:none">
					<label>Featured?:</label>
					<select class="select" id="add_template_featured">
					    <option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add Template Dialog (end) -->

<div id="dialog_confirmation_delete_template_all_zones" title="Confirmation" style="display:none">
    <p>
        <%=t.t("the.template.is.used.by.all.zones.please.confirm.you.want.to.delete.it.from.all.zones")%>
    </p>
</div>

<div id="dialog_confirmation_delete_template" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.delete.the.template")%>
    </p>
</div>
