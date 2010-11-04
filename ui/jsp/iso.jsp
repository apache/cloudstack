<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>



<!-- ISO detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_isoicon.gif" alt="ISO" /></div>
    <h1>
        ISO
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
	                        Detaching Disk &hellip;</p>
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
	                        <%=t.t("Bootable")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="bootable">                      
	                    </div>
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
	                        <%=t.t("crossZones")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="crossZones">
	                    </div>
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
<!-- ISO detail panel (end) -->

<!-- Add ISO Dialog (begin) -->
<div id="dialog_add_iso" title="Add ISO" style="display:none">		
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form2">
			<ol>
				<li>
					<label><%=t.t("name")%>:</label>
					<input class="text" type="text" name="add_iso_name" id="add_iso_name" style="width:250px"/>
					<div id="add_iso_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><%=t.t("display.text")%>:</label>
					<input class="text" type="text" name="add_iso_display_text" id="add_iso_display_text" style="width:250px"/>
					<div id="add_iso_display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><%=t.t("URL")%>:</label>
					<input class="text" type="text" name="add_iso_url" id="add_iso_url" style="width:250px"/>
					<div id="add_iso_url_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label><%=t.t("zone")%>:</label>
                    <select class="select" id="add_iso_zone">
                    </select>
                </li>	
				<li>
					<label for="add_iso_public"><%=t.t("bootable")%>:</label>
					<select class="select" name="add_iso_bootable" id="add_iso_bootable">
						<option value="true">Yes</option>
						<option value="false">No</option>
					</select>
				</li>				
				<li>
					<label>Public?:</label>
					<select class="select" id="add_iso_public">
						<option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>	
				<li>
					<label for="add_iso_os_type"><%=t.t("os.type")%>:</label>
					<select class="select" name="add_iso_os_type" id="add_iso_os_type">
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add ISO Dialog (end) -->

<!-- Copy ISO Dialog (begin) -->
<div id="dialog_copy_iso" title="Copy ISO" style="display:none">	
    <p>
	    <%=t.t("copy.ISO.to")%>:	    
	</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form4">
			<ol>				
				<li>
                    <label><%=t.t("zone")%>:</label>
                    <select class="select" id="copy_iso_zone">  
                        <option value=""></option>                        
                    </select>
                </li>		
				<div id="copy_iso_zone_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div> 
			</ol>
		</form>
	</div>
</div>
<!--  Copy ISO Dialog (end) -->

<!-- Create VM from ISO (begin) -->
<div id="dialog_create_vm_from_iso" title="Create VM from ISO" style="display:none">	
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>			   
				<li>
					<label><%=t.t("name")%>:</label>
					<input class="text" type="text" id="name"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><%=t.t("group")%>:</label>
					<input class="text" type="text" id="group"/>
					<div id="group_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
                    <label><%=t.t("service.offering")%>:</label>
                    <select class="select" id="service_offering">
                    </select>
                </li>					
				<li>
                    <label><%=t.t("disk.offering")%>:</label>
                    <select class="select" id="disk_offering">
                    </select>
                </li>	
                <li>
                    <label><%=t.t("hypervisor")%>:</label>
                    <select class="select" id="hypervisor">
                        <option value='XenServer'>Citrix XenServer</option>
                        <option value='VmWare'>VMware ESX</option>                            
                        <option value='KVM'>KVM</option>
                    </select>     
                </li>						
			</ol>
		</form>
	</div>
</div>
<!-- Create VM from template/ISO (end) -->

<div id="dialog_confirmation_delete_iso_all_zones" title="Confirmation" style="display:none">
    <p>
        <%=t.t("the.ISO.is.used.by.all.zones.please.confirm.you.want.to.delete.it.from.all.zones")%>
    </p>
</div>

<div id="dialog_confirmation_delete_iso" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.delete.the.ISO")%>
    </p>
</div>

