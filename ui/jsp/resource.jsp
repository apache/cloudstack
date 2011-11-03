<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_resourceicon.gif" /></div>
    <h1>
        <fmt:message key="label.resource"/>
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
						<fmt:message key="message.number.zones"/>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                    <div class="resources_totalbg">
                    	<p id="zone_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_zone_shortcut" class="resadd_button" title='<fmt:message key="label.add.zone"/>'></div>
                </div>
            </div>
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none; ">
                    <div class="resource_titlebox">
						<fmt:message key="message.number.pods"/>
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="pod_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_pod_shortcut" class="resadd_button" title='<fmt:message key="label.add.pod"/>'></div>
                </div>
            </div>
            
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none; ">
                    <div class="resource_titlebox">
						<fmt:message key="message.number.clusters"/>                       
					</div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="cluster_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_cluster_shortcut" class="resadd_button" title='<fmt:message key="label.add.cluster"/>'></div>
                </div>
            </div>
            
            <div class="dbrow odd" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
						<fmt:message key="message.number.hosts"/>   
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="host_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_host_shortcut" class="resadd_button" title='<fmt:message key="label.add.host"/>'></div>
                </div>
            </div>
            
            <div class="dbrow even" style="margin-top:10px; border:1px solid #CCC;">
                <div class="dbrow_cell" style="width: 64%; border: none;">
                     <div class="resource_titlebox">
						<fmt:message key="message.number.storage"/>
                    </div>
                </div>
                 <div class="dbrow_cell" style="width: 25%; border: none; background:#cacaca repeat top left; ">
                     <div class="resources_totalbg">
                    	<p id="primarystorage_total">0</p>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 10%; border: none;">
                	<div id="add_primarystorage_shortcut" class="resadd_button" title='<fmt:message key="label.add.primary.storage"/>'></div>
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
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.zone"/>
            </div>
        </div>
    </div>    
    <div class="actionpanel_button_wrapper" id="Update_SSL_Certificate_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.update.ssl"/>
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
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="zonepopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
						<fmt:message key="label.zone.step.1.title"/>
					</h2>
                    <p>
						<fmt:message key="message.zone.step.1.desc"/>
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepopup_selectionpanel">
                      
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="Basic" id="Basic" class="radio" />
                            <label class="label">
                                <fmt:message key="label.basic.mode"/></label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>
										<fmt:message key="message.basic.mode.desc"/>
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="zonepopup_selectionbox">
                            <input type="radio" name="basic_advanced" value="Advanced" id="Advanced" class="radio" />
                            <label class="label">
                               <fmt:message key="label.advanced.mode"/></label>
                            <div class="zonepopup_selectiondescriptionbox">
                                <div class="zonepopup_selectiondescriptionbox_top">
                                </div>
                                <div class="zonepopup_selectiondescriptionbox_bot">
                                    <p>	
										<fmt:message key="message.advanced.mode.desc"/>
                                    </p>
                                    
                                    <div class="zonepopup_subselectionbox_helptext">Isolation Mode</div>
                                    
                                    <div class="zonepopup_subselectionbox">
                                        <form>
                                            <ol>
                                                <li>
                                                    <input type="radio" name="isolation_mode" value="false" id="advanced_virtual" class="radio" />
                                                    <label class="label"><fmt:message key="label.vlan"/></label>
                                                    <span><fmt:message key="message.advanced.virtual"/></span>
                                                </li>
                                                
                                                <li>
                                                    <input type="radio" name="isolation_mode" value="true" id="advanced_securitygroup" class="radio" />
                                                    <label class="label"><fmt:message key="label.security.groups"/></label>
                                                    <span><fmt:message key="message.advanced.security.group"/></span>
                                                </li>
                                            </ol>
                                        </form>
                                    </div>
                                </div>
                            </div>
                            
                            
                        </div>
	                 </div>
                </div>
                <div class="zonepopup_navigationpanel">                    
                    <div class="vmpop_nextbutton" id="go_to_step_2">
                        <fmt:message key="label.go.step.2"/></div>
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
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="zonepopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
						<fmt:message key="label.zone.step.2.title"/>
					</h2>
                    <p>
						<fmt:message key="message.zone.step.2.desc"/>
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                            <ol>
                                <li>
                                    <label><fmt:message key="label.name"/>:</label>
                                    <input class="text" type="text" name="add_zone_name" id="add_zone_name"/>
                                    <div id="add_zone_name_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label><fmt:message key="label.dns.1"/>:</label>
                                    <input class="text" type="text" name="add_zone_dns1" id="add_zone_dns1"/>
                                    <div id="add_zone_dns1_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label><fmt:message key="label.dns.2"/>:</label>
                                    <input class="text" type="text" name="add_zone_dns2" id="add_zone_dns2"/>
                                    <div id="add_zone_dns2_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label><fmt:message key="label.internal.dns.1"/>:</label>
                                    <input class="text" type="text" id="add_zone_internaldns1"/>
                                    <div id="add_zone_internaldns1_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li>
                                    <label><fmt:message key="label.internal.dns.2"/>:</label>
                                    <input class="text" type="text" id="add_zone_internaldns2"/>
                                    <div id="add_zone_internaldns2_errormsg" class="dialog_formcontent_errormsg" style="display:none; margin-left:0;" ></div>
                                </li>
                                <li id="add_zone_vlan_container">
                                    <label><fmt:message key="label.vlan.range"/>:</label>
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
                                    <label><fmt:message key="label.public"/>:</label>
                                    <select class="select" id="add_zone_public">
                                        <option value="true"><fmt:message key="label.yes"/></option>		
                                        <option value="false"><fmt:message key="label.no"/></option>										
                                    </select>
                                </li>	                                                                
                                <li id="domain_container" style="display:none">
                                    <label><fmt:message key="label.domain"/>:</label>                                    
                                    <input class="text" type="text" id="domain" />
                					<div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                                    <!--  
                                    <select class="select" id="domain_dropdown">					
                                    </select>
                                    -->
                                </li>   
                            </ol>
                        </form>
                    </div>
                </div>
                <div class="zonepopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="back_to_step_1" style="display: block;">
                    <fmt:message key="label.back"/>
                    </div>
                    <div class="vmpop_nextbutton" id="go_to_step_3">
                        <fmt:message key="label.go.step.3"/></div>
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
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>   
             <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="zonepopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
					<h2>
						<fmt:message key="label.zone.step.3.title"/>
					</h2>
                    <p>
						<fmt:message key="message.zone.step.3.desc"/>
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                        <ol>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    <fmt:message key="label.name"/>:</label>
                                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="add_pod_gateway" style="width: 115px;">
                                    <fmt:message key="label.gateway"/>:</label>
                                <input class="text" type="text" id="add_pod_gateway" />
                                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    <fmt:message key="label.netmask"/>:</label>
                                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name" style="width: 115px;">
                                    <fmt:message key="label.reserved.system.ip"/>:</label>
                                <input class="text" style="width: 92px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                                <input class="text" style="width: 92px" type="text" name="add_pod_endip" id="add_pod_endip" />
                                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:133px;">
                                </div>
                                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none; ">
                                </div>
                            </li>              
                        </ol>
                        </form>
                    </div>
                </div>
                <div class="zonepopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="back_to_step_2" style="display: block;">
                        <fmt:message key="label.back"/>
                    </div>
                    <div class="vmpop_nextbutton" id="go_to_step_4" style="display: block;">
                        <fmt:message key="label.go.step.4"/></div>                    
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
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
             <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>   
             <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
            <div class="zonepopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">
                <div class="zonepopup_titlebox">
                    <h2>
						<fmt:message key="label.zone.step.4.title"/>
                    </h2>
                    <p>                        
                    </p>
                </div>
                <div class="zonepopup_contentpanel">
                    <div class="zonepoup_formcontent">
                        <form action="#" method="post" id="form_acquire">
                        <ol id="create_direct_vlan">   
                        
                            <li style="display: none" id="vlan_id_container">
                                <label style="width: 115px;">
                                    <fmt:message key="label.vlan.id"/>:</label>
                                <input class="text" type="text" id="vlan_id" />
                                <div id="vlan_id_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>                                                 
                            <li id="guestgateway_container">
                                <label style="width: 115px;">
                                    <fmt:message key="label.guest.gateway"/>:</label>
                                <input class="text" type="text" id="guestgateway" />
                                <div id="guestgateway_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>       
							<li id="guestnetmask_container">
                                <label style="width: 115px;">
                                    <fmt:message key="label.guest.netmask"/>:</label>
                                <input class="text" type="text" id="guestnetmask" />
                                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:0;">
                                </div>
                            </li>	
                            <li id="guestip_container">
                                <label style="width: 115px;">
                                   <fmt:message key="label.guest.ip.range"/>:</label>
                                <input class="text" style="width: 92px" type="text" id="startguestip" /><span>-</span>
                                <input class="text" style="width: 92px" type="text" id="endguestip" />
                                <div id="startguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none; margin-left:133px;">
                                </div>
                                <div id="endguestip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>  
                        </ol>
                        <ol id="create_virtual_vlan" style="display:none">
                            <li id="add_publicip_vlan_container">
                                <label for="add_publicip_vlan_tagged">
                                    <fmt:message key="label.vlan"/>:</label>
                                <select class="select" name="add_publicip_vlan_tagged" id="add_publicip_vlan_tagged">
                                    <option value="untagged">untagged</option>
                                    <option value="tagged">tagged</option>
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_vlan_container">
                                <label for="user_name">
                                    <fmt:message key="label.vlan.id"/>:</label>
                                <input class="text" type="text" name="add_publicip_vlan_vlan" id="add_publicip_vlan_vlan" />
                                <div id="add_publicip_vlan_vlan_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                                </div>
                            </li>
                            <li id="add_publicip_vlan_scope_container">
                                <label for="add_publicip_vlan_scope">
                                    <fmt:message key="label.scope"/>:</label>
                                <select class="select" name="add_publicip_vlan_scope" id="add_publicip_vlan_scope">
                                    <!--  
                                    <option value="zone-wide">zone-wide</option>
                                    <option value="account-specific">account-specific</option>
                                    -->
                                </select>
                            </li>
                            <li style="display: none" id="add_publicip_vlan_pod_container">
                                <label for="user_name">
                                    <fmt:message key="label.pod"/>:</label>
                                <select class="select" name="add_publicip_vlan_pod" id="add_publicip_vlan_pod">
                                </select>
                            </li>
                            <li style="display: none" id="vlan_domain_container">
                                <label>
                                    <fmt:message key="label.domain"/>:</label>
                                <input class="text" type="text" id="vlan_domain" />
                				<div id="vlan_domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                				<!--  
                                <select class="select" id="vlan_domain">
                                </select>
                                -->
                            </li>
                            <li style="display: none" id="add_publicip_vlan_account_container">
                                <label for="user_name">
                                    <fmt:message key="label.account"/>:</label>
                                <input class="text" type="text" name="add_publicip_vlan_account" id="add_publicip_vlan_account" />
                                <div id="add_publicip_vlan_account_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    <fmt:message key="label.gateway"/>:</label>
                                <input class="text" type="text" name="add_publicip_vlan_gateway" id="add_publicip_vlan_gateway" />
                                <div id="add_publicip_vlan_gateway_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    <fmt:message key="label.netmask"/>:</label>
                                <input class="text" type="text" name="add_publicip_vlan_netmask" id="add_publicip_vlan_netmask" />
                                <div id="add_publicip_vlan_netmask_errormsg" class="dialog_formcontent_errormsg"
                                    style="display: none;">
                                </div>
                            </li>
                            <li>
                                <label for="user_name">
                                    <fmt:message key="label.ip.range"/>:</label>
                                <input class="text" style="width: 92px" type="text" name="add_publicip_vlan_startip"
                                    id="add_publicip_vlan_startip" /><span>-</span>
                                <input class="text" style="width: 92px" type="text" name="add_publicip_vlan_endip"
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
                        <fmt:message key="label.back"/>
                    </div>                     
                    <div class="vmpop_nextbutton" id="submit" style="display: block;">
                        <fmt:message key="label.submit"/>
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
                <fmt:message key="label.step.1"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.2"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.3"/></div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                <fmt:message key="label.step.4"/></div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
            <div class="zonepopup_container_closebutton" id="close_button">
            </div>
        </div>
        <div class="zonepopup_container_mid">
            <div class="zonepopup_maincontentarea">                
                
                <div class="zonepopup_contentpanel">
                	<div id="spinning_wheel" class="zonepoup_loadingbox" style="display:none;">
                    	<div class="zonepoup_loader"></div>
                        <p> <fmt:message key="label.adding"/>....</p>
                    </div>
                    
                   <!-- <div id="after_action_message" class="zonepoup_msgbox"></div>-->
                    
                   <div class="zonepopup_reviewbox odd">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_zone_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              <fmt:message key="label.zone"/>:</div>
                           <span id="add_zone_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
                    
                    <div class="zonepopup_reviewbox even">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_pod_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              <fmt:message key="label.pod"/>:</div>
                           <span id="add_pod_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
                    
                    <div class="zonepopup_reviewbox odd" id="add_iprange_message_container">
                      <div class="zonepopup_reviewtextbox">
                           <div id="add_iprange_tick_cross"> <!-- class "zonepopup_reviewtick" or class "zonepopup_reviewcross" -->
                           </div>
                           <div class="zonepopup_review_label">
                              <fmt:message key="label.ip.range"/>:</div>
                           <span id="add_iprange_message"> <!-- add class "error" if in error -->
                           </span>
                      </div>
                    </div>
                </div>
                <div class="zonepopup_navigationpanel">                    
                    <div class="vmpop_nextbutton" id="close_button">
                        <fmt:message key="label.close"/></div>
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
<div id="dialog_update_cert" title='<fmt:message key="label.update.ssl"/>' style="display:none">
	<p><fmt:message key="message.update.ssl"/></p>

	<div class="dialog_formcontent">
		<form action="#" method="post" id="form_acquire">
			<ol>
				<li>
					<label style="width:400px"><fmt:message key="label.certificate"/>:</label>
					<textarea class="text" name="update_cert" id="update_cert" style="height: 200px; width: 400px" />
					<div id="update_cert_errormsg" class="dialog_formcontent_errormsg" style="display:none; width:300px" ></div>
				</li>
                <li>
                    <label style="width:400px"><fmt:message key="label.privatekey"/>:</label>
                    <textarea class="text" name="update_privatekey" id="update_privatekey" style="height: 150px; width: 400px" />
                    <div id="update_key_errormsg" class="dialog_formcontent_errormsg" style="display:none; width:300px" ></div>
                </li>
                <li>
                    <label style="width:400px"><fmt:message key="label.domain.suffix"/>:</label>
                    <input class="text" name="update_domainsuffix" id="update_domainsuffix" style="width: 400px" />
                    <div id="update_domainsuffix_errormsg" class="dialog_formcontent_errormsg" style="display:none; width:300px" ></div>
                </li>
			</ol>
		</form>
	</div>
   <!--Loading box-->
   <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display:none;">
   	<div class="ui_dialog_loader"></div>
       <p><fmt:message key="label.updating"/>....</p>
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
<div id="dialog_add_pod_in_resource_page" title='<fmt:message key="label.add.pod"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
           
            <li>
                <label style="width: 115px;">
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
           
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="add_pod_name" id="add_pod_name" />
                <div id="add_pod_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pod_gateway" style="width: 115px;">
                    <fmt:message key="label.gateway"/>:</label>
                <input class="text" type="text" id="add_pod_gateway" />
                <div id="add_pod_gateway_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.netmask"/>:</label>
                <input class="text" type="text" name="add_pod_netmask" id="add_pod_netmask" />
                <div id="add_pod_netmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name" style="width: 115px;">
                    <fmt:message key="label.private.ip.range"/>:</label>
                <input class="text" style="width: 67px" type="text" name="add_pod_startip" id="add_pod_startip" /><span>-</span>
                <input class="text" style="width: 67px" type="text" name="add_pod_endip" id="add_pod_endip" />
                <div id="add_pod_startip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
                <div id="add_pod_endip_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
            <li id="guestip_container" style="display: none">
                <label style="width: 115px;">
                    <fmt:message key="label.guest.ip.range"/>:</label>
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
                    <fmt:message key="label.guest.netmask"/>:</label>
                <input class="text" type="text" id="guestnetmask" />
                <div id="guestnetmask_errormsg" class="dialog_formcontent_errormsg" style="display: none;
                    margin-left: 0;">
                </div>
            </li>
            <li id="guestgateway_container" style="display: none">
                <label style="width: 115px;">
                    <fmt:message key="label.guest.gateway"/>:</label>
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_external_cluster_in_resource_page" title='<fmt:message key="label.add.cluster"/>' style="display: none">
    <p>
		<fmt:message key="message.add.cluster.zone"/>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
            	<label for="cluster_hypervisor"><fmt:message key="label.hypervisor"/>:</label>
                <select class="select" id="cluster_hypervisor">               						
                </select>
            </li>
            
<!-- CloudManaged cluster for VMware is disabled for now, it may be added back when we want to manage ESXi host directly  -->            
<!--              
            <li input_group="vmware">
                <label>
                    <fmt:message key="label.cluster.type"/>:</label>
                <select class="select" id="type_dropdown">
                    <option value="CloudManaged"><fmt:message key="label.cloud.managed"/></option>		
                    <option value="ExternalManaged" SELECTED><fmt:message key="label.vsphere.managed"/></option>										
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
-->            
            <li>
                <label>
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_hostname">
                    <fmt:message key="label.vcenter.host"/>:</label>
                <input class="text" type="text" name="cluster_hostname" id="cluster_hostname" />
                <div id="cluster_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_username">
                    <fmt:message key="label.vcenter.username"/>:</label>
                <input class="text" type="text" name="cluster_username" id="cluster_username" />
                <div id="cluster_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_password">
                    <fmt:message key="label.vcenter.password"/>:</label>
                <input class="text" type="password" name="cluster_password" id="cluster_password" autocomplete="off" />
                <div id="cluster_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" input_sub_group="external">
                <label for="cluster_datacenter">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="cluster_datacenter" id="cluster_datacenter" />
                <div id="cluster_datacenter_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="cluster_name" id="cluster_name_label">
                    <fmt:message key="label.vcenter.cluster"/>:</label>
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_host_in_resource_page" title='<fmt:message key="label.add.host"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>            
            <li>
                <label>
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>               
            <li>
                <label>
                    <fmt:message key="label.cluster"/>:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>                    
            <li input_group="general">
                <label for="host_hostname">
                    <fmt:message key="label.host.name"/>:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    <fmt:message key="label.username"/>:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="host_password">
                    <fmt:message key="label.password"/>:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
<!--              
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_address">
                    <fmt:message key="label.vcenter.host"/>:</label>
                <input class="text" type="text" name="host_vcenter_address" id="host_vcenter_address" />
                <div id="host_vcenter_address_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_username">
                    <fmt:message key="label.vcenter.username"/>:</label>
                <input class="text" type="text" name="host_vcenter_username" id="host_vcenter_username" />
                <div id="host_vcenter_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_password">
                    <fmt:message key="label.vcenter.password"/>:</label>
                <input class="text" type="password" name="host_vcenter_password" id="host_vcenter_password" autocomplete="off" />
                <div id="host_vcenter_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_dc">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="host_vcenter_dc" id="host_vcenter_dc" />
                <div id="host_vcenter_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
-->            
            <li input_group="vmware" style="display: none;">
                <label for="host_vcenter_host">
                    <fmt:message key="label.esx.host"/>:</label>
                <input class="text" type="text" name="host_vcenter_host" id="host_vcenter_host" />
                <div id="host_vcenter_host_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_cpucores">
                    # of CPU Cores:</label>
                <input class="text" type="text" name="host_baremetal_cpucores" id="host_baremetal_cpucores" />
                <div id="host_baremetal_cpucores_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_cpu">
                    CPU (in MHz):</label>
                <input class="text" type="text" name="host_baremetal_cpu" id="host_baremetal_cpu" />
                <div id="host_baremetal_cpu_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_memory">
                    Memory (in MB):</label>
                <input class="text" type="text" name="host_baremetal_memory" id="host_baremetal_memory" />
                <div id="host_baremetal_memory_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="baremetal" style="display: none;">
                <label for="host_baremetal_mac">
                    Host MAC:</label>
                <input class="text" type="text" name="host_baremetal_mac" id="host_baremetal_mac" />
                <div id="host_baremetal_mac_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="Ovm" style="display: none;">
                <label>
                    Agent Username:</label>
                <input class="text" type="text" id="agent_username" value="oracle" />
                <div id="agent_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li input_group="Ovm" style="display: none;">
                <label>
                    Agent Password:</label>
                <input class="text" type="password" id="agent_password" />
                <div id="agent_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>          
			<li>
                <label input_group="general">                    
					<fmt:message key="label.host.tags"/>:</label>
                <input class="text" type="text" name="host_tags" id="host_tags" />
                <div id="host_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
            <fmt:message key="label.adding"/>....</p>
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
<div id="dialog_add_pool_in_resource_page" title='<fmt:message key="label.add.primary.storage"/>' style="display: none">    
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label>
                    <fmt:message key="label.zone"/>:</label>
                <select class="select" id="zone_dropdown">
                </select>
                <div id="zone_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li>
                <label>
                    <fmt:message key="label.pod"/>:</label>
                <select class="select" id="pod_dropdown">
                </select>
                <div id="pod_dropdown_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>   
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    <fmt:message key="label.cluster"/>:</label>
                <select class="select" id="pool_cluster">
                </select>
                <div id="pool_cluster_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">
                    <fmt:message key="label.protocol"/>:</label>
                <select class="select" id="add_pool_protocol">                  
                </select>
            </li>
            <li id="add_pool_server_container">
                <label for="add_pool_nfs_server">
                    <fmt:message key="label.server"/>:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container" input_group="nfs">
                <label for="add_pool_path">
                    <fmt:message key="label.path"/>:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none" input_group="iscsi">
                <label for="add_pool_iqn">
                    <fmt:message key="label.target.iqn"/>:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none" input_group="iscsi">
                <label for="add_pool_lun">
                    <fmt:message key="label.lun"/> #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_clvm_vg_container" style="display: none" input_group="clvm">
                <label for="add_pool_clvm_vg">
                    <fmt:message key="label.volgroup"/>:</label>
                <input class="text" type="text" name="add_pool_clvm_vg" id="add_pool_clvm_vg" />
                <div id="add_pool_clvm_vg_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_dc">
                    <fmt:message key="label.vcenter.datacenter"/>:</label>
                <input class="text" type="text" name="add_pool_vmfs_dc" id="add_pool_vmfs_dc" />
                <div id="add_pool_vmfs_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_ds">
                    <fmt:message key="label.vcenter.datastore"/>:</label>
                <input class="text" type="text" name="add_pool_vmfs_ds" id="add_pool_vmfs_ds" />
                <div id="add_pool_vmfs_ds_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_tags_container">
                <label for="add_pool_tags">
                    <fmt:message key="label.storage.tags"/>:</label>
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
            <fmt:message key="label.adding"/>....</p>
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

