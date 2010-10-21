<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_diskofferingicon.gif" alt="Disk Offering" /></div>
    
    <h1>
        Disk Offering
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("details")%></div>
    </div>
    <div id="tab_content_details">
        <div class="grid_actionpanel">
            <div class="grid_actionbox" id="action_link">
                <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                    <ul class="actionsdropdown_boxlist" id="action_list">
                        <li><%=t.t("no.available.actions")%></li>
                    </ul>
                </div>
            </div>
            <div class="grid_editbox" id="edit_button">
            </div>
            <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                <div class="gridheader_loader" id="Div1">
                </div>
                <p id="description">
                    Waiting &hellip;</p>
            </div>                 
        </div>
        <div class="grid_container">
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
                        <%=t.t("name")%>:</div>
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
                        <%=t.t("display.text")%>:</div>
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
                        <%=t.t("disk.size")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="disksize">
                    </div>
                </div>
            </div>  
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("tags")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="tags">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("domain")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="domain">
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

<!-- Add Disk Offering Dialog -->
<div id="dialog_add_disk" title="Add Disk Offering" style="display:none">
	<p>Please fill in the following data to add a new disk Offering.</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form1">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="add_disk_name" id="add_disk_name"/>
					<div id="add_disk_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Description:</label>
					<input class="text" type="text" name="add_disk_description" id="add_disk_description"/>
					<div id="add_disk_description_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
				<li>
					<label for="user_name">Disk size (in GB):</label>
					<input class="text" type="text" name="add_disk_disksize" id="add_disk_disksize"/>
					<div id="add_disk_disksize_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
				<li id="add_disk_tags_container">
                    <label for="add_disk_tags">
                        Tags:</label>
                    <input class="text" type="text" id="add_disk_tags" />
                    <div id="add_disk_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>
                </li>	
			</ol>
		</form>
	</div>
</div>

