<%@ page import="java.util.Date" %>
<%
long milliseconds = new Date().getTime();
%>
<script type="text/javascript" src="scripts/cloud.core.configuration.js?t=<%=milliseconds%>"></script>
	
<!-- Content Panel -->
<!-- Submenu -->
<div class="submenu_links">
	<div class="submenu_links_on" id="submenu_global">Global Settings</div>
	<div class="submenu_links_off" id="submenu_zones">Zones</div>
	<div class="submenu_links_off" id="submenu_service">Service Offerings</div>
	<div class="submenu_links_off" id="submenu_disk">Disk Offerings</div>
</div>

<!--Globals -->
<div class="maincontent" style="display:block;" id="submenu_content_global">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/gsettingstitle_icons.gif" title="global settings" /> </div>
		<h1>Global Settings</h1>
		<div class="search_formarea">
			<form action="#" method="post">
				<ol>
					<li><input class="text" type="text" name="search_input" id="search_input" /></li>
				</ol>
			</form>
			<a class="search_button" id="search_button" href="#"></a>
			
			<div id="advanced_search_link" class="advsearch_link" style="display: none;">Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>Advance Search</h3>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                        
                    </ol>
                    </form>
                    <div id="advanced_search_close" class="adv_search_actionbox">
                        <a href="#">Close </a>
                    </div>
                </div>
            </div>
			
		</div>
    </div>
    <div class="filter_actionbox">
    	<div class="selection_formarea" style="display:none;">
        	<form action="#" method="post">
            	<label for="filter">Filters:</label>
				<select class="select" id="template_type">
                  <option value="true">Public</option>
                  <option value="false">Private</option>
         		</select>
			</form>
         </div>
    </div>
	<div class="grid_container">
    	<div id="loading_gridtable" class="loading_gridtable">
                  <div class="loading_gridanimation"></div>
                   <p>Loading...</p>
             </div>
		<div class="grid_header">
	        <div class="grid_genheader_cell" style="width:30%;">
            	<div class="grid_headertitles">Name</div>
            </div>
            <div class="grid_genheader_cell" style="width:15%;">
            	<div class="grid_headertitles">Value</div>
            </div>
             <div class="grid_genheader_cell" style="width:50%;">
            	<div class="grid_headertitles">Description</div>
            </div>
            <div class="grid_genheader_cell" style="width:4%;">
            	<div class="grid_headertitles">Actions</div>
            </div>	   
         </div>
		<div id="grid_content">
        	 
        </div>
	</div>
    <div id="pagination_panel" class="pagination_panel" style="display:none;">
    	<p id="grid_rows_total" />
    	<div class="pagination_actionbox">
        	<div class="pagination_actions">
            	<div class="pagination_actionicon"><img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#"> Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#"> Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#"> Next</a>
            </div>
        </div>
    </div>
</div>
<!-- END Content Panel -->

<!-- Global Template -->
<div id="global_template" style="display:none">
	<div class="grid_smallgenrow_cell" style="width:30%;">
		<div class="netgrid_celltitles" id="global_name"></div>
	</div>
	<div class="grid_smallgenrow_cell" style="width:15%;">
		<div class="netgrid_celltitles" id="global_value"></div>
	</div>
	<div class="grid_smallgenrow_cell" style="width:50%;">
		<div class="netgrid_celltitles" id="global_desc"></div>
	</div>
	<div class="grid_smallgenrow_cell" style="width:4%;">
		<div class="netgrid_celltitles"><a id="global_action_edit" href="#">Edit</a></div>
	</div>
</div>
<!-- END Global Template -->

<!-- Edit Global Setting Dialog -->
<div id="dialog_edit_global" title="Edit Global Setting" style="display:none">
	<p>Please confirm your changes for the global setting and remember that you must restart your mgmt server for the setting to take effect.</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name" >Name: </label> <span id="edit_global_name"></span>
				</li>
				<li>
					<label for="user_name">Value:</label>
					<input class="text" type="text" name="edit_global_value" id="edit_global_value"/>
					<div id="edit_global_value_errormsg" class="dialog_formcontent_errormsg" style="display:none;" />
				</li>
			</ol>
		</form>
	</div>
</div>


<!--Zone -->
<div class="maincontent" style="display:none;" id="submenu_content_zones">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/zonestitle_icons.gif" title="zones" /> </div>
		<h1>Zones</h1>	
    </div>
	<div class="filter_actionbox">
	</div>
	<div class="net_gridwrapper">
    	<div class="zonetree_box">
        	<div class="zonetree_boxleft"></div>
            <div class="zonetree_boxmid">
            	<div class="zonetree_contentbox">
                    <div class="zonetree_addbox">
                    	<div class="zonetree_addicon"></div>
                        <div class="zonetree_links" id="action_add_zone">Add a Zone</div>
                    </div>
					<div id="zones_container">
					</div>
                </div>
            </div>
            <div class="zonetree_boxright"></div>
        </div>
        <div class="zone_detailsbox">
        	<div class="zone_detailsbox_left"></div>
            <div class="zone_detailsbox_mid">
            	<div class="zone_detailsbox_contentpanel">
                	<h2 id="right_panel_detail_title" ></h2>
                    <div class="zone_detailsbox_content" id="right_panel_detail_content">
                    </div>
                    <div class="zone_detailsbox_actionpanel">
                    	<div class="zone_detailsbox_actions">
                    	    <div class="zonedetails_editzonebutton" id="action_edit_zone" style="display:none"></div>
                        	<div class="zonedetails_addpodbutton" id="action_add_pod" style="display:none"></div>
                        	<div class="zonedetails_editpodbutton" id="action_edit_pod" style="display:none"></div>
                            <div class="zonedetails_addvlanipbutton" id="action_add_publicip_vlan" style="display:none"></div>
                            <div class="zonedetails_directipbutton" id="action_add_directip_vlan" style="display:none"></div>
                            <div class="zonedetails_adddeletebutton" id="action_delete" style="display:none"></div>
                        </div>
                    </div>
                </div>
                
            </div>
            <div class="zone_detailsbox_right"></div>
        </div>
    
    </div>
</div>

<!-- Zone Template -->
<div id="zone_template" style="display:none">
    <div class="adding_loading" style="height:25px;display:none" id="loading_container">
	    <div class="adding_animation"></div>
	    <div class="adding_text">Adding a zone &hellip; </div>
    </div>
    <div id="row_container">
	    <div class="zonetree_firstlevel">
		    <div class="zonetree_closedarrows" id="zone_expand"></div>
		    <div class="zonetree_zoneicon"></div>
		    <p>Zone:<div class="zonetree_links" id="zone_name">Zone 1</div></p>
	    </div>
	    <div id="zone_content" style="display:none">
		    <div id="pods_container">
		    </div>
		    <div id="publicip_ranges_container">
		    </div>
	    </div>
	</div>
</div>
<!-- END Zone Template -->

<!-- Pod Template -->
<div id="pod_template" style="display:none">
	<div class="adding_loading" style="height:25px;display:none" id="loading_container">
	    <div class="adding_animation"></div>
	    <div class="adding_text">Adding a pod &hellip; </div>
    </div>
    <div id="row_container">  
	    <div class="zonetree_secondlevel">
		    <div class="zonetree_podicon"></div>
		    <p>Pod:<div class="zonetree_links" id="pod_name">Name of the Pod</div></p>
	    </div>
	    <div id="pod_content">
	        <div id="directip_ranges_container">
		    </div>
	    </div>
	</div>
</div>
<!-- END Pod Template -->

<!-- VLAN Ip Range Template -->
<div id="vlan_ip_range_template" style="display:none">
    <div class="adding_loading" style="height:25px;display:none" id="loading_container">
	    <div class="adding_animation"></div>
	    <div class="adding_text">Adding an VLAN IP range &hellip; </div>
    </div>
    <div id="row_container">  
	    <div class="zonetree_secondlevel">
		    <div class="zonetree_ipicon"></div>
		    <p><span id="vlan_ip_range_type">IP Range:</span><div class="zonetree_links" id="vlan_ip_range_name">100.123.345.123 - 100.123.345.123</div></p>
	    </div>
	</div>
</div>
<!-- END Public Ip Range Template -->

<!-- Add Zone Dialog -->
<div id="dialog_add_zone" title="Add Zone" style="display:none">
	<p>Please enter the following info to add a new zone:</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label>Name:</label>
					<input class="text" type="text" name="add_zone_name" id="add_zone_name"/>
					<div id="add_zone_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label>DNS 1:</label>
					<input class="text" type="text" name="add_zone_dns1" id="add_zone_dns1"/>
					<div id="add_zone_dns1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label>DNS 2:</label>
					<input class="text" type="text" name="add_zone_dns2" id="add_zone_dns2"/>
					<div id="add_zone_dns2_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label>Internal DNS 1:</label>
					<input class="text" type="text" id="add_zone_internaldns1"/>
					<div id="add_zone_internaldns1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label>Internal DNS 2:</label>
					<input class="text" type="text" id="add_zone_internaldns2"/>
					<div id="add_zone_internaldns2_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li id="add_zone_container">
					<label>Zone VLAN Range:</label>
					<input class="text" style="width:67px" type="text" name="add_zone_startvlan" id="add_zone_startvlan"/><span>-</span>
                    <input class="text" style="width:67px" type="text" name="add_zone_endvlan" id="add_zone_endvlan"/>
					<div id="add_zone_startvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="add_zone_endvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="add_zone_guestcidraddress">Guest CIDR:</label>
					<input class="text" type="text" id="add_zone_guestcidraddress" value="10.1.1.0/24"/>
					<div id="add_zone_guestcidraddress_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- END Add Zone Dialog -->

<!-- Edit Zone Dialog (begin) -->
<div id="dialog_edit_zone" title="Edit Zone" style="display:none">
	<p>Please review your changes before clicking 'Change'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form3">
			<ol>
				<li>
					<label for="edit_zone_name">Name:</label>
					<input class="text" type="text" name="edit_zone_name" id="edit_zone_name"/>
					<div id="edit_zone_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_zone_dns1">DNS 1:</label>
					<input class="text" type="text" name="edit_zone_dns1" id="edit_zone_dns1"/>
					<div id="edit_zone_dns1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_zone_dns2">DNS 2:</label>
					<input class="text" type="text" name="edit_zone_dns2" id="edit_zone_dns2"/>
					<div id="edit_zone_dns2_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_zone_internaldns1">Internal DNS 1:</label>
					<input class="text" type="text" id="edit_zone_internaldns1"/>
					<div id="edit_zone_internaldns1_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_zone_internaldns2">Internal DNS 2:</label>
					<input class="text" type="text" id="edit_zone_internaldns2"/>
					<div id="edit_zone_internaldns2_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li id="edit_zone_container">
					<label>Zone VLAN Range:</label>
					<input class="text" style="width:67px" type="text" name="edit_zone_startvlan" id="edit_zone_startvlan"/><span>-</span>
                    <input class="text" style="width:67px" type="text" name="edit_zone_endvlan" id="edit_zone_endvlan"/>
					<div id="edit_zone_startvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="edit_zone_endvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>			
				<li>
					<label for="edit_zone_guestcidraddress">Guest CIDR:</label>
					<input class="text" type="text" id="edit_zone_guestcidraddress" value="10.1.1.0/24"/>
					<div id="edit_zone_guestcidraddress_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>	
			</ol>
		</form>
	</div>
</div>
<!-- Edit Zone Dialog (end) -->

<!-- Add Pod Dialog -->
<div id="dialog_add_pod" title="Add Pod" style="display:none">
	<p>Please enter the following info to add a new pod for zone: <b><span id="add_pod_zone_name"></span></b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name" style="width:115px;">Name:</label>
					<input class="text" type="text" name="add_pod_name" id="add_pod_name"/>
					<div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="add_pod_gateway" style="width:115px;">Gateway:</label>
					<input class="text" type="text" id="add_pod_gateway"/>
					<div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name" style="width:115px;">CIDR:</label>
					<input class="text" type="text" name="add_pod_cidr" id="add_pod_cidr"/>
					<div id="add_pod_cidr_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name" style="width:115px;">Private IP Range:</label>
					<input class="text" style="width:67px" type="text" name="add_pod_startip" id="add_pod_startip"/><span>-</span>
                    <input class="text" style="width:67px" type="text" name="add_pod_endip" id="add_pod_endip"/>
					<div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
			</ol>
		</form>
	</div>
</div>
<!-- END Add Pod Dialog -->

<!-- Edit Pod Dialog (begin) -->
<div id="dialog_edit_pod" title="Edit Pod" style="display:none">
	<p>Please review your changes before clicking 'Change'</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form4">
			<ol>
				<li>
					<label for="user_name" style="width:115px;">Name:</label>
					<input class="text" type="text" name="edit_pod_name" id="edit_pod_name"/>
					<div id="edit_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name" style="width:115px;">CIDR:</label>
					<input class="text" type="text" name="edit_pod_cidr" id="edit_pod_cidr"/>
					<div id="edit_pod_cidr_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name" style="width:115px;">Private IP Range:</label>
					<input class="text" style="width:67px" type="text" name="edit_pod_startip" id="edit_pod_startip"/><span>-</span>
                    <input class="text" style="width:67px" type="text" name="edit_pod_endip" id="edit_pod_endip"/>
					<div id="edit_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="edit_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="edit_pod_gateway" style="width:115px;">Gateway:</label>
					<input class="text" type="text" id="edit_pod_gateway"/>
					<div id="edit_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Edit Pod Dialog (end) -->

<!-- Add VLAN IP Range Dialog for zone (begin) -->
<div id="dialog_add_vlan_for_zone" title="Add VLAN IP Range" style="display:none">
	<p>Please enter the following info to add a new IP range for zone: <b><span id="add_publicip_vlan_zone_name"></span></b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li style="display:none" id="add_publicip_vlan_type_container">
					<label for="add_publicip_vlan_type">Type:</label>
					<select class="select" name="add_publicip_vlan_type" id="add_publicip_vlan_type">
					    <option value="false">direct</option>
						<option value="true">public</option>						
					</select>
				</li>
				<li id="add_publicip_vlan_container">
					<label for="add_publicip_vlan_tagged">VLAN:</label>
					<select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
					</select>
				</li>
				<li style="display:none" id="add_publicip_vlan_vlan_container">
					<label for="user_name">VLAN ID:</label>
					<input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan"/>
					<div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li id="add_publicip_vlan_scope_container">
					<label for="add_publicip_vlan_scope">Scope:</label>
					<select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
					    <option value="zone-wide">zone-wide</option>
						<option value="account-specific">account-specific</option>						
					</select>
				</li>
				<li style="display:none" id="add_publicip_vlan_pod_container">
					<label for="user_name">Pod:</label>
					<select class="select" name="add_publicip_vlan_pod" id="add_publicip_vlan_pod">					
					</select>
				</li>
				<li style="display:none" id="add_publicip_vlan_domain_container">
					<label for="user_name">Domain:</label>
					<select class="select" name="add_publicip_vlan_domain" id="add_publicip_vlan_domain">					
					</select>
				</li>
				<li style="display:none" id="add_publicip_vlan_account_container">
					<label for="user_name">Account:</label>
					<input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account"/>
					<div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Gateway:</label>
					<input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway"/>
					<div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Netmask:</label>
					<input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask"/>
					<div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">IP Range:</label>
					<input class="text" style="width:67px" type="text" name="add_publicip_vlan_startip" id="add_publicip_vlan_startip"/><span>-</span>
                    <input class="text" style="width:67px" type="text" name="add_publicip_vlan_endip" id="add_publicip_vlan_endip"/>
					<div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add VLAN IP Range Dialog for zone (end) -->
    
<!-- Add VLAN IP Range Dialog for pod (begin) -->
<div id="dialog_add_vlan_for_pod" title="Add Direct IP Range" style="display:none">
	<p>Please enter the following info to add a new direct IP range on untagged VLAN to pod: <b><span id="pod_name_label"></span></b></p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form5">
			<ol>					
				<li>
					<label for="gateway">Gateway:</label>
					<input class="text" type="text" id="gateway"/>
					<div id="gateway_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="netmask">Netmask:</label>
					<input class="text" type="text" id="netmask"/>
					<div id="netmask_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label>IP Range:</label>
					<input class="text" style="width:67px" type="text" id="startip"/><span>-</span>
                    <input class="text" style="width:67px" type="text" id="endip"/>
					<div id="startip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
					<div id="endip_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Add VLAN IP Range Dialog for pod (end) -->

<!--Service Offerings -->
<div class="maincontent" style="display:none;" id="submenu_content_service">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/serviceofftitle_icons.gif" title="service offerings" /> </div>
		<h1>Service Offerings</h1>
		<a class="add_serviceoffbutton" href="#" id="service_add_service"></a>
		<div class="search_formarea">
			<form action="#" method="post">
				<ol>
					<li><input class="text" type="text" name="search_input" id="search_input" /></li>
				</ol>
			</form>
			<a class="search_button" id="search_button" href="#"></a>
			
			<div id="advanced_search_link" class="advsearch_link" style="display: none;">Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>Advance Search</h3>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                 
                    </ol>
                    </form>
                    <div id="advanced_search_close" class="adv_search_actionbox">
                        <a href="#">Close </a>
                    </div>
                </div>
            </div>
			
		</div>
    </div>
    
    <div class="filter_actionbox">
    	<div class="selection_formarea" style="display:none;">
        	<form action="#" method="post">
            	<label for="filter">Filters:</label>
				<select class="select" id="template_type">
                  <option value="true">Public</option>
                  <option value="false">Private</option>
         		</select>
			</form>
         </div>
    </div>
	<div class="grid_container">
		<div class="grid_header">
		    <div class="grid_genheader_cell" style="width:4%;">
            	<div class="grid_headertitles">ID</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Name</div>
            </div>
            <div class="grid_genheader_cell" style="width:16%;">
            	<div class="grid_headertitles">Display text</div>
            </div>
            <div class="grid_genheader_cell" style="width:7%;">
            	<div class="grid_headertitles">Storage type</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">CPU</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Memory</div>
            </div>
            <div class="grid_genheader_cell" style="width:7%;">
            	<div class="grid_headertitles">Offer HA</div>
            </div>
            <div class="grid_genheader_cell" style="width:7%;">
            	<div class="grid_headertitles">Network Type</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Tags</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Created</div>
            </div>
            <div class="grid_genheader_cell" style="width:8%;">
            	<div class="grid_headertitles">Actions</div>
            </div>
		</div>
		<div id="grid_content">
        	<div id="loading_gridtable" class="loading_gridtable">
                  <div class="loading_gridanimation"></div>
                   <p>Loading...</p>
             </div>
        </div>
	</div>
    <div id="pagination_panel" class="pagination_panel" style="display:none;">
    	<p id="grid_rows_total" />
    	<div class="pagination_actionbox">
        	<div class="pagination_actions">
            	<div class="pagination_actionicon"><img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#"> Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#"> Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#"> Next</a>
            </div>
        </div>
    </div>
</div>

<!-- Service offerings Template -->
<div id="service_template" style="display:none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">  
	    <div class="grid_smallgenrow_cell" style="width:4%;">
		    <div class="netgrid_celltitles" id="service_id"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="service_name"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:16%;">
		    <div class="netgrid_celltitles" id="service_display"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:7%;">
		    <div class="netgrid_celltitles" id="service_storagetype"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="service_cpu"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="service_memory"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:7%;">
		    <div class="netgrid_celltitles" id="service_offerha"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:7%;">
		    <div class="netgrid_celltitles" id="service_networktype"></div>
	    </div>
	     <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="service_tags"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="service_created"></div>
	    </div>	   
	    <div class="grid_smallgenrow_cell" style="width:8%;">
		    <div class="netgrid_celltitles"><a id="service_action_edit" href="#">Edit</a> | <a id="service_action_delete" href="#">Delete</a></div>
	    </div>
	</div>
</div>
<!-- END Service offerings Template -->

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

<!-- Edit Service Offering Dialog -->
<div id="dialog_edit_service" title="Edit Service Offering" style="display:none">
	<p>Please edit the following data for your service offering: <b><span id="service_name"></span></b>, and confirm your changes when you are done.</p>
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label for="user_name">Name:</label>
					<input class="text" type="text" name="edit_service_name" id="edit_service_name"/>
					<div id="edit_service_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>
				<li>
					<label for="user_name">Display Text:</label>
					<input class="text" type="text" name="edit_service_display" id="edit_service_display"/>
					<div id="edit_service_display_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
				</li>				
				<li>
					<label>Offer HA?</label>
					<select class="select" id="edit_service_offerha">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>					
			</ol>
		</form>
	</div>
</div>

<!--Disk Offerings -->
<div class="maincontent" style="display:none;" id="submenu_content_disk">
	<div id="maincontent_title">
    	<div class="maintitle_icon"> <img src="images/diskofftitle_icons.gif" title="Disk offering" /> </div>
		<h1>Disk Offerings</h1>
		<a class="add_diskoffbutton" href="#" id="disk_add_disk"></a>
		<div class="search_formarea">
			<form action="#" method="post">
				<ol>
					<li><input class="text" type="text" name="search_input" id="search_input" /></li>
				</ol>
			</form>
			<a class="search_button" id="search_button" href="#"></a>
			
			<div id="advanced_search_link" class="advsearch_link" style="display: none;">Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>Advance Search</h3>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                      
                    </ol>
                    </form>
                    <div id="advanced_search_close" class="adv_search_actionbox">
                        <a href="#">Close </a>
                    </div>
                </div>
            </div>
			
		</div>
    </div>
    <div class="filter_actionbox">
    	<div class="selection_formarea" style="display:none;">
        	<form action="#" method="post">
            	<label for="filter">Filters:</label>
				<select class="select" id="template_type">
                  <option value="true">Public</option>
                  <option value="false">Private</option>
         		</select>
			</form>
         </div>
    </div>
	<div class="grid_container">
		<div class="grid_header">
		    <div class="grid_genheader_cell" style="width:5%;">
            	<div class="grid_headertitles">ID</div>
            </div>
            <div class="grid_genheader_cell" style="width:19%;">
            	<div class="grid_headertitles">Name</div>
            </div>
            <div class="grid_genheader_cell" style="width:24%;">
            	<div class="grid_headertitles">Description</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Disk size</div>
            </div>
            <div class="grid_genheader_cell" style="width:20%;">
            	<div class="grid_headertitles">Tags</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Domain</div>
            </div> 
            <div class="grid_genheader_cell" style="width:10%;">
            	<div class="grid_headertitles">Actions</div>
            </div>  
            <!--         
            <div class="gridadmin_offeringsheader_cell2">
            	<div class="grid_headertitles">Mirrored</div>
            </div>
            !-->
		</div>
		<div id="grid_content">
            <div id="loading_gridtable" class="loading_gridtable">
                      <div class="loading_gridanimation"></div>
                       <p>Loading...</p>
                 </div>
        </div>
	</div>
    <div id="pagination_panel" class="pagination_panel" style="display:none;">
    	<p id="grid_rows_total" />
    	<div class="pagination_actionbox">
        	<div class="pagination_actions">
            	<div class="pagination_actionicon"><img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#"> Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#"> Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
            	<div class="pagination_actionicon"><img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#"> Next</a>
            </div>
        </div>
    </div>
</div>
<!-- Disk offerings Template -->
<div id="disk_template" style="display:none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">  
	    <div class="grid_smallgenrow_cell" style="width:5%;">
		    <div class="netgrid_celltitles" id="disk_id">Disk ID</div> 
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:19%;">
		    <div class="netgrid_celltitles" id="disk_name">Disk name</div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:24%;">
		    <div class="netgrid_celltitles" id="disk_description">Disk description</div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="disk_disksize">Disk size</div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:20%;">
		    <div class="netgrid_celltitles" id="disk_tags"></div>
	    </div>
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles" id="disk_domain">Domain</div>
	    </div>
	    <!--
	    <div class="gridadmin_offeringsrow_cell2">
		    <div class="netgrid_celltitles" id="disk_ismirrored">Is mirrored</div>
	    </div>	
	    !-->
	    <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles"><a id="disk_action_edit" href="#">Edit</a> | <a id="disk_action_delete" href="#">Delete</a></div>
	    </div>
	</div>
</div>
<!-- END Disk offerings Template -->

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
				<!--
				<li>
					<label for="user_name">Mirrored?:</label>
					<select class="select" name="add_disk_mirrored" id="add_disk_mirrored">
						<option value="true">Yes</option>
						<option value="false" selected>No</option>
					</select>
				</li>
				!-->
			</ol>
		</form>
	</div>
</div>

<!-- Edit Disk Offering Dialog -->
<div id="dialog_edit_disk" title="Edit Disk Offering" style="display:none">
  <p>
    Please edit the following data for your disk offering: <b>
      <span id="edit_disk_name_display"></span>
    </b>, and confirm your changes when you are done.
  </p>
  <div class="dialog_formcontent">
    <form action="#" method="post" id="form2">
      <ol>
        <li>
          <label for="user_name">Name:</label>
          <input class="text" type="text" name="edit_disk_name" id="edit_disk_name"/>
          <div id="edit_disk_name_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
        </li>
        <li>
          <label for="user_name">Display Text:</label>
          <input class="text" type="text" name="edit_disk_display" id="edit_disk_display"/>
          <div id="edit_disk_display_errormsg" class="dialog_formcontent_errormsg" style="display:none;" ></div>
        </li>
      </ol>
    </form>
  </div>
</div>