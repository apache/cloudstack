<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_resourceicon.gif" alt="Resource" /></div>
    <h1>
        Resource
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div id="resource_page" style="display: block">
    	<div class="grid_container" style="width:480px; border:none;">
            
            <div class="dbrow even" style="border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                    <div class="resource_titlebox">
                        <h2><span> # of </span> Zone</h2>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                    <div class="resources_totalbg">
                    	<p>10</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                </div>
            </div>
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none; ">
                    <div class="resource_titlebox">
                        <h2><span> # of </span> Pod</h2>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p>120</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                </div>
            </div>
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Cluster - Host</h2>
                         
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p>15</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div class="resadd_button"></div>
                </div>
            </div>
            
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Cluster - Primary Storage</h2>
                      
                    </div>
                </div>
                 <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p>12</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div class="resadd_button"></div>
                </div>
            </div>
       

    </div>
</div>


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
				<li>
					<label>Public?:</label>
					<select class="select" id="add_zone_public">
					    <option value="true">Yes</option>		
						<option value="false">No</option>										
					</select>
				</li>	
				
				
				<li id="domain_dropdown_container" style="display:none">
					<label>Domain:</label>
					<select class="select" id="domain_dropdown">					
					</select>
				</li>
				
				
			</ol>
		</form>
	</div>
   <!--Loading box-->
   <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
   	<div class="ui_dialog_loader"></div>
       <p>Adding....</p>
   </div>
   
   <!--Confirmation msg box-->
   <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
   	<div id="icon" class="ui_dialog_msgicon error"></div>
       <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
   </div>
</div>
<!-- END Add Zone Dialog -->

<!-- Update Certificate Dialog -->
<div id="dialog_update_cert" title="Update Console Proxy SSL Certificate" style="display:none">
	<p>Please submit a new X.509 compliant SSL certificate to be updated to each console proxy virtual instance:</p>

	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label>Certificate:</label>
					<textarea class="text" name="update_cert" id="update_cert" style="height: 300px; width: 400px" />
					<div id="update_cert_errormsg" class="dialog_formcontent_errormsg" style="display:none; width:300px" ></div>
				</li>
			</ol>
		</form>
	</div>
   <!--Loading box-->
   <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
   	<div class="ui_dialog_loader"></div>
       <p>Updating....</p>
   </div>
   
   <!--Confirmation msg box-->
   <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
	<div id="info_container" class="ui_dialog_messagebox error" style="display:none;">
		<div id="icon" class="ui_dialog_msgicon error"></div>
        <div id="info" class="ui_dialog_messagebox_text error">(info)</div>
   </div>
</div>
<!-- End Update Certificate Dialog -->