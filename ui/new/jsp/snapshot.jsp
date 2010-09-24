<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<!-- snapshot detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_snapshoticon.gif" alt="Instance" /></div>
    <h1>
        Snapshot
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
    </div>    
    <div id="tab_content_details">
        <div class="grid_actionpanel">
            <div class="grid_actionbox" id="action_link">
                <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                    <ul class="actionsdropdown_boxlist" id="action_list">
                        <!--  
                    	<li> <a href="#"> Delete </a> </li>
                        <li> <a href="#"> Attach Disk </a> </li>
                        -->
                    </ul>
                </div>
            </div>
            <div class="grid_editbox">
            </div>
            <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                <div class="gridheader_loader" id="icon">
                </div>
                <p id="description">
                    Detaching Disk &hellip;</p>
            </div>
            <div class="gridheader_message" id="action_message_box" style="border: 1px solid #999; display: none;">
                <p id="description"></p>
                <div class="close_button" id="close_button">
                </div>
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
                        <%=t.t("Name")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="name">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Volume")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="volume_name">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Interval.Type")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="interval_type">
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
        </div>
    </div>        
</div>
<!-- snapshot detail panel (end) -->

<!-- Add Volume Dialog from Snapshot (begin) -->
<div id="dialog_add_volume_from_snapshot" title="Add Volume from Snapshot" style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form5">
        <ol>
            <li>
                <label>Name:</label>
                <input class="text" type="text" id="name" />
                <div id="name_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
            </li>           
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog from Snapshot (end) -->

<!-- Create template from snapshot (begin) -->
<div id="dialog_create_template_from_snapshot" title="Create Template from Snapshot" style="display:none">	
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form6">
			<ol>
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="name" style="width:250px"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Display Text:</label>
					<input class="text" type="text" id="display_text" style="width:250px"/>
					<div id="display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
					<label>OS Type:</label>
					<select class="select" id="os_type">
					</select>
				</li>		
				<li>
					<label>Password Enabled?:</label>
					<select class="select" id="password">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Create template from snapshot (end) -->
