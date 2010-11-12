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
                        <h2><span> # of </span> Zones</h2>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                    <div class="resources_totalbg">
                    	<p id="zone_total">N</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_zone_shortcut" class="resadd_button" title="Add Zone"></div>
                </div>
            </div>
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none; ">
                    <div class="resource_titlebox">
                        <h2><span> # of </span> Pods</h2>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="pod_total">N</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_pod_shortcut" class="resadd_button" title="Add Pod"></div>
                </div>
            </div>
            
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Host</h2>
                         
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="host_total">N</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_host_shortcut" class="resadd_button" title="Add Host"></div>
                </div>
            </div>
            
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Primary Storage</h2>
                      
                    </div>
                </div>
                 <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="primarystorage_total">N</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_primarystorage_shortcut" class="resadd_button" title="Add Primary Storage"></div>
                </div>
            </div>
       

    </div>
</div>


<!-- Zone wizard (begin)-->
<div id="wizard_overlay" class="ui-widget-overlay" style="display:none;"></div>

<div id="add_zone_wizard" class="zonepopup_container" style="display: none">
    <!-- step 1 (begin) -->
    <div id="step1" style="display: block;">
        <div class="zonepopup_container_top">
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step1_bg.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
                        Step 1: <strong>Select a Network</strong></h2>
                    <p>
                        Please select a Network for your new zone. 
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepopup_selectionpanel">
                      
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="basic_mode" id="basic_mode" class="radio" checked />
                            <label class="label">
                                Basic Mode</label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>
                                        Create VLAN when adding a pod    
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="advanced_mode" id="advanced_mode" class="radio" />
                            <label class="label">
                               Advanced Mode</label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>
                                        Create VLAN when add a zone  
                                    </p>
                                </div>
                            </div>
                        </div>
	                 </div>
                </div>
                <div class="zonepopup_navigationpanel">                    
                    <div class="vmpop_nextbutton" id="go_to_step_2">
                        Go to Step 2</div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- step 1 (end) -->
    
    <!-- step 2 (begin) -->
    <div id="step2" style="display: none;">
        <div class="zonepopup_container_top">
           <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
                        Step 2: <strong>Add a Zone</strong></h2>
                    <p>
                        Please enter the following info to add a new zone 
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                            <ol>
                                <li>
                                    <label>Name:</label>
                                    <input class="text" type="text" name="add_zone_name" id="add_zone_name"/>
                                    <div id="add_zone_name_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label>DNS 1:</label>
                                    <input class="text" type="text" name="add_zone_dns1" id="add_zone_dns1"/>
                                    <div id="add_zone_dns1_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label>DNS 2:</label>
                                    <input class="text" type="text" name="add_zone_dns2" id="add_zone_dns2"/>
                                    <div id="add_zone_dns2_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label>Internal DNS 1:</label>
                                    <input class="text" type="text" id="add_zone_internaldns1"/>
                                    <div id="add_zone_internaldns1_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label>Internal DNS 2:</label>
                                    <input class="text" type="text" id="add_zone_internaldns2"/>
                                    <div id="add_zone_internaldns2_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li id="add_zone_vlan_container">
                                    <label>VLAN Range:</label>
                                    <input class="text" style="width:92px" type="text" name="add_zone_startvlan" id="add_zone_startvlan"/><span>-</span>
                                   <input class="text" style="width:92px" type="text" name="add_zone_endvlan" id="add_zone_endvlan"/>
                                    <div id="add_zone_startvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                    <div id="add_zone_endvlan_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label for="add_zone_guestcidraddress">Guest CIDR:</label>
                                    <input class="text" type="text" id="add_zone_guestcidraddress" value="10.1.1.0/24"/>
                                    <div id="add_zone_guestcidraddress_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
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
                </div>
                <div class="zonepopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="back_to_step_1" style="display: block;">
                    Back
                    </div>
                    <div class="vmpop_nextbutton" id="go_to_step_3">
                        Go to Step 3</div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- step 2 (end) -->
    
    <!-- step 3 (begin) -->
    <div id="step3" style="display: none;">
        <div class="zonepopup_container_top">
           <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
                        Step 3: <strong>Add a Pod</strong></h2>
                    <p>
                        Please enter the following info to add a new pod 
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                        <ol>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    Name:</label>
                                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="add_pod_gateway" style="width: 115px;">
                                    Gateway:</label>
                                <input class="text" type="text" id="add_pod_gateway" />
                                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    CIDR:</label>
                                <input class="text" type="text" name="add_pod_cidr" id="add_pod_cidr" />
                                <div id="add_pod_cidr_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    Reserved System IP:</label>
                                <input class="text" style="width: 92px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                                <input class="text" style="width: 92px" type="text" name="add_pod_endip" id="add_pod_endip" />
                                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:133px;">
                                </div>
                                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none; ">
                                </div>
                            </li>
                            <li id="guestip_container">
                                <label style="width: 115px;">
                                    Guest IP Range:</label>
                                <input class="text" style="width: 92px" type="text" id="startguestip" /><span>-</span>
                                <input class="text" style="width: 92px" type="text" id="endguestip" />
                                <div id="startguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:133px;">
                                </div>
                                <div id="endguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>  
							<li id="guestnetmask_container">
                                <label style="width: 115px;">
                                    Guest Netmask:</label>
                                <input class="text" type="text" id="guestnetmask" />
                                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>	
                        </ol>
                        </form>
                    </div>
                </div>
                <div class="zonepopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="back_to_step_2" style="display: block;">
                        Back
                    </div>
                    <div class="vmpop_nextbutton" id="submit_button">
                        Submit
                    </div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- step 3 (end) -->
    
    <!-- after submit screen (begin) -->
    <div id="after_submit_screen" style="display: none;">
        <div class="zonepopup_container_top">
           <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                
                <div class="zonepopup_contentpanel">
                	<div id="spinning_wheel" class="zonepoup_loadingbox" style="display:none;">
                    	<div class="zonepoup_loader"></div>
                        <p> Adding zone and pod....</p>
                    </div>
                    
                    <div id="after_action_message" class="zonepoup_msgbox"></div>
                </div>
                <div class="zonepopup_navigationpanel">                    
                    <div class="vmpop_nextbutton" id="close_button">
                        Close</div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- after submit screen (end) -->
    
</div>
<!-- Zone wizard (end)-->

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

<!-- Add Pod Dialog (begin) -->
<div id="dialog_add_pod" title="Add Pod" style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
           
            <li>
                <label style="width: 115px;">
                    Zone:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
           
            <li>
                <label for="user_name" style="width: 115px;">
                    Name:</label>
                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pod_gateway" style="width: 115px;">
                    Gateway:</label>
                <input class="text" type="text" id="add_pod_gateway" />
                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    CIDR:</label>
                <input class="text" type="text" name="add_pod_cidr" id="add_pod_cidr" />
                <div id="add_pod_cidr_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    Private IP Range:</label>
                <input class="text" style="width: 67px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_pod_endip" id="add_pod_endip" />
                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
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
<!-- Add Pod Dialog (end) -->
