<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <!--  
    <div class="main_titleicon">
        <img src="images/title_isoicon.gif" alt="ISO" /></div>
    -->
    <h1>
        Service Offering
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
                        <%=t.t("storage.type")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="storagetype">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("CPU")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="cpu">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("memory")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="memory">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("offer.HA")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="offerha">
                    </div>
                    <select class="select" id="offerha_edit" style="width: 202px; display: none;">
                        <option value="false">No</option>
						<option value="true">Yes</option>
                    </select>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("network.type")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="networktype">
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
                        <%=t.t("created")%>:</div>
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

<!-- Add Service Offering Dialog -->
<div id="dialog_add_service" title="Add Service Offering" style="display:none">
	<p>Please fill in the following data to add a new Service Offering.</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="add_service_name" id="add_service_name"/>
					<div id="add_service_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Display text:</label>
					<input class="text" type="text" name="add_service_display" id="add_service_display"/>
					<div id="add_service_display_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="add_service_storagetype">Storage type:</label>
					<select class="select" name="add_service_storagetype" id="add_service_storagetype">
					    <option value="shared">shared</option>
						<option value="local">local</option>						
					</select>
				</li>		
				<li>
					<label for="user_name"># of CPU cores:</label>
					<input class="text" type="text" name="add_service_cpucore" id="add_service_cpucore"/>
					<div id="add_service_cpucore_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">CPU (in MHz):</label>
					<input class="text" type="text" name="add_service_cpu" id="add_service_cpu"/>
					<div id="add_service_cpu_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Memory (in MB):</label>
					<input class="text" type="text" name="add_service_memory" id="add_service_memory"/>
					<div id="add_service_memory_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
				<li id="add_service_offerha_container">
					<label>Offer HA?</label>
					<select class="select" id="add_service_offerha">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>	
				<li>
					<label>Network Type</label>
					<select class="select" id="add_service_networktype">						
						<option value="direct">Direct</option>
						<option value="public">Public</option>
					</select>
				</li>	
				<li id="add_service_tags_container">
                    <label for="add_service_tags">
                        Tags:</label>
                    <input class="text" type="text" id="add_service_tags" />
                    <div id="add_service_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                    </div>
                </li>						
			</ol>
		</form>
	</div>
</div>