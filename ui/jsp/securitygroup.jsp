<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>



<div class="main_title" id="right_panel_header">
    
    <div class="main_titleicon">
        <img src="images/title_secgroupicons.gif" /></div>
    
    <h1>
        Security Group
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("details")%></div>
        <div class="content_tabs off" id="tab_ingressrule">
            Ingress Rule</div>
    </div>
    <!--Details tab (start)-->
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
	            	<div id="grid_header_title" class="grid_header_title">(title)</div>
	                <div id="action_link" class="grid_actionbox">
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
	                        <%=t.t("name")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="name">
	                    </div>                        
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
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("account")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
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
    <!--Details tab (end)-->
    
    <!--Ingress Rule tab (start)-->
    <div style="display: none;" id="tab_content_ingressrule">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Ingress Rule tab (end)-->    
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_securitygroup_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Security Group" /></div>
            <div class="actionpanel_button_links">
                Add Security Group
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!--  Ingress Rule tab template (begin) -->
<div class="grid_container" id="ingressrule_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="ingressrule_action_link">
            <div class="grid_actionsdropdown_box" id="ingressrule_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
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
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Protocol:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="protocol">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Endpoint or Operation:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="endpoint">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                CIDR or Account/Network Group:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="cidr">
            </div>
        </div>
    </div>   
</div>
<!--  Ingress Rule tab template (end) -->

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
					<label>Custom disk size?:</label>
					<select class="select" id="customized">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>				
				<li id="add_disk_disksize_container">
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
                <li>
				    <label>
				        Public?:</label>
				    <select class="select" id="public_dropdown">
				        <option value="true">Yes</option>
				        <option value="false">No</option>
				    </select>
				</li>
				<li id="domain_dropdown_container" style="display: none">
				    <label>
				        Domain:</label>
				    <select class="select" id="domain_dropdown">
				    </select>
				</li>    	
			</ol>
		</form>
	</div>
</div>

<!-- Add Security Group Dialog (begin) -->
<div id="dialog_add_security_group" title="Add Security Group" style="display: none">
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    Name:</label>
                <input class="text" type="text" id="name" />
                <div id="name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label>
                    Description:</label>
                <input class="text" type="text" id="description" />
                <div id="description_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Security Group Dialog (end) -->

<!-- advanced search template (begin) -->
<div class="adv_searchpopup_bg" id="advanced_search_popup" style="display:none;">
            <div class="adv_searchformbox">
                <form action="#" method="post">
                <ol>
                    <li>
                        <input class="text textwatermark" type="text" id="adv_search_name" value="by name" />
                    </li>           
                    <li id="adv_search_domain_li" style="display: none;">
                        <select class="select" id="adv_search_domain">
                        </select>
                    </li>           
                </ol>
                </form>
 
         </div>
</div>
<!-- advanced search template (end) -->
