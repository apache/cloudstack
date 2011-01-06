<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_resourceicon.gif" /></div>
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
                    	<p id="zone_total">0</p>
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
                    	<p id="pod_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_pod_shortcut" class="resadd_button" title="Add Pod"></div>
                </div>
            </div>
            
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none; ">
                    <div class="resource_titlebox">
                        <h2><span> # of </span> Clusters</h2>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="cluster_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_cluster_shortcut" class="resadd_button" title="Add Cluster"></div>
                </div>
            </div>
            
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Host</h2>
                         
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="host_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_host_shortcut" class="resadd_button" title="Add Host"></div>
                </div>
            </div>
            
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
                        <h2><span> # of </span> Primary Storage</h2>
                      
                    </div>
                </div>
                 <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="primarystorage_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_primarystorage_shortcut" class="resadd_button" title="Add Primary Storage"></div>
                </div>
            </div>       
        </div>
    </div>
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_zone_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Zone" /></div>
            <div class="actionpanel_button_links">
                Add Zone
            </div>
        </div>
    </div>    
    <div class="actionpanel_button_wrapper" id="Update_SSL_Certificate_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Update SSL Certificate" /></div>
            <div class="actionpanel_button_links">
                Update SSL Certificate
            </div>
        </div>
    </div>    
</div>
<!--  top buttons (end) -->

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
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
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
                        Please select a network model for your zone.
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepopup_selectionpanel">
                      
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="Basic" id="Basic" class="radio" />
                            <label class="label">
                                Basic Mode</label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>
                                        Choose this network model if you do <b>*<u>not</u>*</b> want to enable any VLAN support.  All virtual instances created under this network model will be assigned an IP directly from the network and security groups are used to provide security and segregation.
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="Advanced" id="Advanced" class="radio" />
                            <label class="label">
                               Advanced Mode</label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>
                                        Choose this network model if you wish to enable VLAN support.  This network model provides the most flexibility in allowing administrators to provide custom network offerings such as providing firewall, vpn, or load balancer support as well as enabling direct vs virtual networking.
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
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
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
                                <li id="add_zone_guestcidraddress_container" style="display:none">
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
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 4</div>   
             <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
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
                                    Netmask:</label>
                                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
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
                            <li id="guestgateway_container">
                                <label style="width: 115px;">
                                    Guest Gateway:</label>
                                <input class="text" type="text" id="guestgateway" />
                                <div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
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
                    <div class="vmpop_nextbutton" id="go_to_step_4" style="display: none;">
                        Go to Step 4</div>
                    <div class="vmpop_nextbutton" id="submit_in_step3" style="display: block;">
                        Submit
                    </div>
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- step 3 (end) -->
    
    <!-- step 4 (begin) -->
    <div id="step4" style="display: none;">
        <div class="zonepopup_container_top">
           <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
             <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 4</div>   
             <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
            <div class="vmpopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
                        Step 4: <strong>Add an IP range to public network in zone</strong></h2>
                    <p>                        
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                        <ol>
                            <li id="add_publicip_vlan_container">
                                <label for="add_publicip_vlan_tagged">
                                    VLAN:</label>
                                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                                    <option value="untagged">untagged</option>
                                    <option value="tagged">tagged</option>
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_vlan_container">
                                <label for="user_name">
                                    VLAN ID:</label>
                                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>
                            <li id="add_publicip_vlan_scope_container">
                                <label for="add_publicip_vlan_scope">
                                    Scope:</label>
                                <select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
                                    <!--  
                                    <option value="zone-wide">zone-wide</option>
                                    <option value="account-specific">account-specific</option>
                                    -->
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_pod_container">
                                <label for="user_name">
                                    Pod:</label>
                                <select class="select" name="add_publicip_vlan_pod" id="add_publicip_vlan_pod">
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_domain_container">
                                <label for="user_name">
                                    Domain:</label>
                                <select class="select" name="add_publicip_vlan_domain" id="add_publicip_vlan_domain">
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_account_container">
                                <label for="user_name">
                                    Account:</label>
                                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    Gateway:</label>
                                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    Netmask:</label>
                                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    IP Range:</label>
                                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_startip"
                                    id="add_publicip_vlan_startip" /><span>-</span>
                                <input class="text" style="width: 67px" type="text" name="add_publicip_vlan_endip"
                                    id="add_publicip_vlan_endip" />
                                <div id="add_publicip_vlan_startip_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                                <div id="add_publicip_vlan_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>         
                        </ol>
                        </form>
                    </div>
                </div>
                <div class="zonepopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="back_to_step_3" style="display: block;">
                        Back
                    </div>                    
                    <div class="vmpop_nextbutton" id="submit_in_step4" style="display: block;">
                        Submit
                    </div>                    
                </div>
            </div>
        </div>
        <div class="zonepopup_container_bot">
        </div>
    </div>
    <!-- step 4 (end) -->
    
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
                    
                   <!-- <div id="after_action_message" class="zonepoup_msgbox"></div>-->
                    
                   <div class="zonepopup_reviewbox odd">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_zone_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              Zone:</div>
                           <span id="add_zone_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
                    
                    <div class="zonepopup_reviewbox even">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_pod_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              Pod:</div>
                           <span id="add_pod_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
                    
                    <div class="zonepopup_reviewbox odd" id="add_iprange_message_container">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_iprange_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              IP Range:</div>
                           <span id="add_iprange_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
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

<!-- Add Pod Dialog in resource page (begin) -->
<div id="dialog_add_pod_in_resource_page" title="Add Pod" style="display: none">   
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
                    Netmask:</label>
                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
            <li id="guestip_container" style="display: none">
                <label style="width: 115px;">
                    Guest IP Range:</label>
                <input class="text" style="width: 92px" type="text" id="startguestip" /><span>-</span>
                <input class="text" style="width: 92px" type="text" id="endguestip" />
                <div id="startguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 133px;">
                </div>
                <div id="endguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="guestnetmask_container" style="display: none">
                <label style="width: 115px;">
                    Guest Netmask:</label>
                <input class="text" type="text" id="guestnetmask" />
                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
                </div>
            </li>
            <li id="guestgateway_container" style="display: none">
                <label style="width: 115px;">
                    Guest Gateway:</label>
                <input class="text" type="text" id="guestgateway" />
                <div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
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
<!-- Add Pod Dialog in resource page (end) -->

<!-- Add Cluster Dialog in resource page (begin) -->
<div id="dialog_add_external_cluster_in_resource_page" title="Add Cluster" style="display: none">
    <p>
        Add a hypervisor managed cluster for zone <b><span id="zone_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="cluster_hypervisor">Hypervisor:</label>
                <select class="select" id="cluster_hypervisor">
                    <option value="XenServer" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>										
                </select>
            </li>
            <li input_group="vmware">
                <label>
                    Cluster Type:</label>
                <select class="select" id="type_dropdown">
                    <option value="CloudManaged">Cloud.com Managed</option>		
                    <option value="ExternalManaged" SELECTED>vSphere Managed</option>										
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            
            <li>
                <label>
                    Zone:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_hostname">
                    vCenter Server:</label>
                <input class="text" type="text" name="cluster_hostname" id="cluster_hostname" />
                <div id="cluster_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_username">
                    vCenter User:</label>
                <input class="text" type="text" name="cluster_username" id="cluster_username" />
                <div id="cluster_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_password">
                    Password:</label>
                <input class="text" type="password" name="cluster_password" id="cluster_password" autocomplete="off" />
                <div id="cluster_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_datacenter">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="cluster_datacenter" id="cluster_datacenter" />
                <div id="cluster_datacenter_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="cluster_name" id="cluster_name_label">
                    vCenter Cluster:</label>
                <input class="text" type="text" name="cluster_name" id="cluster_name" />
                <div id="cluster_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Cluster Dialog in resource page (end) -->

<!-- Add Host Dialog in resource page (begin) -->
<div id="dialog_add_host_in_resource_page" title="Add Host" style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li>
            	<label for="host_hypervisor">Hypervisor:</label>
                <select class="select" id="host_hypervisor">
                    <option value="XenServer" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>										
                    <option value="">Auto</option>									
                </select>
            </li>
            <li>
                <label>
                    Zone:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>               
            <li>
                <label>
                    Cluster:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>                    
            <li>
                <label for="host_hostname">
                    Host name:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    User name:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Password:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Host Dialog in resource page (end) -->

<!-- Add Primary Storage Dialog  in resource page (begin) -->
<div id="dialog_add_pool_in_resource_page" title="Add Primary Storage" style="display: none">    
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    Zone:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li>
                <label>
                    Pod:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    Cluster:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Name:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">
                    Protocol:</label>
                <select class="select" id="add_pool_protocol">
                    <option value="nfs">NFS</option>
                    <option value="iscsi">ISCSI</option>
                    <option value="vmfs">VMFS</option>
                </select>
            </li>
            <li id="add_pool_server_container">
                <label for="add_pool_nfs_server">
                    Server:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container" input_group="nfs">
                <label for="add_pool_path">
                    Path:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none" input_group="iscsi">
                <label for="add_pool_iqn">
                    Target IQN:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none" input_group="iscsi">
                <label for="add_pool_lun">
                    LUN #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_dc">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="add_pool_vmfs_dc" id="add_pool_vmfs_dc" />
                <div id="add_pool_vmfs_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_ds">
                    vCenter Datastore:</label>
                <input class="text" type="text" name="add_pool_vmfs_ds" id="add_pool_vmfs_ds" />
                <div id="add_pool_vmfs_ds_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_tags_container">
                <label for="add_pool_tags">
                    Tags:</label>
                <input class="text" type="text" id="add_pool_tags" />
                <div id="add_pool_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Primary Storage Dialog  in resource page (end) -->

