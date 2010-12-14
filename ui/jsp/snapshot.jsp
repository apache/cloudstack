<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- snapshot detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_snapshoticon.gif" /></div>
    <h1>
        Snapshot
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
	                    <div class="gridheader_loader" id="icon">
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

<!-- snapshot confirmation dialog (begin) -->
<div id="dialog_confirmation_delete_snapshot" title="Confirmation" style="display:none">
    <p>Please confirm you want to delete the snapshot.</p>   
</div>
<!-- snapshot confirmation dialog (end) -->

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
				
				<li id="isfeatured_container" style="display:none">
					<label>Featured?:</label>
					<select class="select" id="isfeatured">
					    <option value="false">No</option>
						<option value="true">Yes</option>						
					</select>
				</li>
				
			</ol>
		</form>
	</div>
</div>
<!-- Create template from snapshot (end) -->

<!-- advanced search template (begin) -->
<div id="advanced_search_template" class="adv_searchpopup" style="display: none;">
    <div class="adv_searchformbox">
        <h3>
            Advance Search</h3>
        <a id="advanced_search_close" href="#">Close </a>
        <form action="#" method="post">
        <ol>
            <li>
                <label>
                    Name:</label>
                <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
            </li>
            <li id="adv_search_domain_li" style="display: none;">
                <label for="filter">
                    Domain:</label>
                <select class="select" id="adv_search_domain">
                </select>
            </li>
            <li id="adv_search_account_li" style="display: none;">
                <label for="filter">
                    Account:</label>
                <input class="text" type="text" id="adv_search_account" />
            </li>
        </ol>
        </form>
        <div class="adv_search_actionbox">
            <div class="adv_searchpopup_button" id="adv_search_button">
            </div>
        </div>
    </div>
</div>
<!-- advanced search template (end) -->



