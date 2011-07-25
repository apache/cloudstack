<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<script language="javascript">
dictionary = { 
	'label.action.attach.disk' : '<fmt:message key="label.action.attach.disk"/>',
	'label.action.attach.disk.processing' : '<fmt:message key="label.action.attach.disk.processing"/>',
	'label.action.detach.disk' : '<fmt:message key="label.action.detach.disk"/>',
	'label.action.detach.disk.processing' : '<fmt:message key="label.action.detach.disk.processing"/>',
	'label.action.create.template' : '<fmt:message key="label.action.create.template"/>',
	'label.action.create.template.processing' : '<fmt:message key="label.action.create.template.processing"/>',
	'label.action.delete.volume' : '<fmt:message key="label.action.delete.volume"/>',
	'label.action.delete.volume.processing' : '<fmt:message key="label.action.delete.volume.processing"/>',
	'message.action.delete.volume' : '<fmt:message key="message.action.delete.volume"/>',
	'label.action.take.snapshot' : '<fmt:message key="label.action.take.snapshot"/>',
	'label.action.take.snapshot.processing' : '<fmt:message key="label.action.take.snapshot.processing"/>',
	'message.action.take.snapshot' : '<fmt:message key="message.action.take.snapshot"/>',
	'label.action.recurring.snapshot' : '<fmt:message key="label.action.recurring.snapshot"/>',
	'label.action.download.volume' : '<fmt:message key="label.action.download.volume"/>',
	'label.action.create.volume' : '<fmt:message key="label.action.create.volume"/>',
	'label.action.create.volume.processing' : '<fmt:message key="label.action.create.volume.processing"/>',
	'label.action.delete.snapshot' : '<fmt:message key="label.action.delete.snapshot"/>',
	'label.action.delete.snapshot.processing' : '<fmt:message key="label.action.delete.snapshot.processing"/>',
	'message.action.delete.snapshot' : '<fmt:message key="message.action.delete.snapshot"/>',
	'message.download.volume' : '<fmt:message key="message.download.volume"/>',
	'message.disable.snapshot.policy' : '<fmt:message key="message.disable.snapshot.policy"/>',
	'message.apply.snapshot.policy' : '<fmt:message key="message.apply.snapshot.policy"/>'
};	
</script>

<!-- volume detail panel (begin) -->
<div class="main_title" id="right_panel_header">
  
    <div class="main_titleicon">
        <img src="images/title_volumeicons.gif" /></div>
    
    <h1>
        <fmt:message key="label.volume"/>
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <fmt:message key="label.details"/></div>
        <div class="content_tabs off" id="tab_snapshot">
            <fmt:message key="label.snapshots"/></div>
    </div>
    <!--Details tab (start)-->
    <div id="tab_content_details">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
	        <div class="grid_container">
	        	<div class="grid_header">
	            	<div class="grid_header_title">Title</div>
	                    <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
	                        <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                            <ul class="actionsdropdown_boxlist" id="action_list">
	                                <li><fmt:message key="label.no.actions"/></li>
	                            </ul>
	                        </div>
	                    </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                display: none;">
	                    <div class="gridheader_loader" id="icon">
	                    </div>
	                    <p id="description">
	                        Detaching Disk &hellip;</p>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.id"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="id">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.name"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="name">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.type"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="type">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.zone"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zonename">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.instance.name"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vm_name">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.device.id"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="device_id">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.size"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="size">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.state"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="state">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.created"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
	                    </div>
	                </div>
	            </div>	            
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.account"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
	                    </div>
	                </div>
	            </div>
				<div class="grid_rows odd">
					<div class="grid_row_cell" style="width: 20%;">
						<div class="row_celltitles">
							<fmt:message key="label.domain"/>:</div>
					</div>
					<div class="grid_row_cell" style="width: 79%;">
						<div class="row_celltitles" id="domain">
						</div>
					</div>
    			</div>  
    			<div class="grid_rows even" id="storage_container" style="display:none">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <fmt:message key="label.storage"/>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="storage">
	                    </div>
	                </div>
	            </div>
    			
    			
	        </div>
	    </div>    
    </div>
    <!--Details tab (end)-->
    <!--Snapshot tab (start)-->
    <div style="display: none;" id="tab_content_snapshot">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p><fmt:message key="label.loading"/> &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Snapshot tab (end)-->
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_volume_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Volume" /></div>
            <div class="actionpanel_button_links">
                <fmt:message key="label.add.volume"/>
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Create Template Dialog -->
<div id="dialog_create_template" title='<fmt:message key="label.action.create.template"/>' style="display: none">
    <!--  
    <p><fmt:message key="message.create.template.volume"/></p>
    -->
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    <fmt:message key="label.display.text"/>:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    <fmt:message key="label.os.type"/>:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li id="create_template_public_container" style="display:none">
                <label for="create_template_public">
                    <fmt:message key="label.public"/>:</label>
                <select class="select" name="create_template_public" id="create_template_public">
                    <option value="false"><fmt:message key="label.no"/></option>
                    <option value="true"><fmt:message key="label.yes"/></option>
                </select>
            </li>
            <li>
                <label for="user_name">
                    <fmt:message key="label.password.enabled"/>:</label>
                <select class="select" name="create_template_password" id="create_template_password">
                    <option value="false"><fmt:message key="label.no"/></option>
                    <option value="true"><fmt:message key="label.yes"/></option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>

<!-- Download Volume Dialog (begin) -->
<div id="dialog_download_volume" title='<fmt:message key="label.action.download.volume"/>' style="display: none">    
  <!--Loading box-->
  <div id="spinning_wheel" class="ui_dialog_loaderbox">
      <div class="ui_dialog_loader">
      </div>
      <p>
          <fmt:message key="label.generating.url"/>....</p>
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
<!-- Download Volume Dialog (end) -->

<!-- Recurring Snapshots Dialog (begin) -->
<div id="dialog_recurring_snapshot" title='<fmt:message key="label.action.recurring.snapshot"/>' style="display:none;">
    <div class="dialog_snapshotcontainer">
        <div class="dialog_snapshotleft" id="dialog_snapshotleft" >
			<p><fmt:message key="message.snapshot.schedule"/>:<br/><br/></p>
        </div>
        <div class="dialog_snapshotright" id="dialog_snapshotright">
            <div class="dialog_snapshots_editcontent" style="display:block;">
                <div class="dialog_snapshots_editcontent_title">
					<div class="dialog_snapshotleft_label" style="width:120px">&nbsp;<fmt:message key="label.snapshot.schedule"/>: &nbsp;&nbsp;</div>
                    <select class="snapselect" id="snapshot_interval">
						<option id="snapshot_interval_0" value="0"><fmt:message key="label.hourly"/></option>
						<option id="snapshot_interval_1" value="1"><fmt:message key="label.daily"/></option>
						<option id="snapshot_interval_2" value="2"><fmt:message key="label.weekly"/></option>
						<option id="snapshot_interval_3" value="3"><fmt:message key="label.monthly"/></option>
					</select> : <i id="policy_enabled">Enabled</i>
                </div>
                <div class="dialog_formcontent" id="snapshot_form">
                    <form action="#" method="post" id="form4">
                    <ol>
                        <li>
                            <label for="add_volume_name" style="width:75px">
                                <fmt:message key="label.time"/>:</label>
                            <span id="edit_hardcoding_hour" style="display:none">00</span>
                            <span id="edit_hour_container">
                            	
                                <select class="snapselect" id="edit_hour">
                                    <option value="01">01</option>
                                    <option value="02">02</option>
                                    <option value="03">03</option>
                                    <option value="04">04</option>
                                    <option value="05">05</option>
                                    <option value="06">06</option>
                                    <option value="07">07</option>
                                    <option value="08">08</option>
                                    <option value="09">09</option>
                                    <option value="10">10</option>
                                    <option value="11">11</option>  
									<option value="00">12</option>
                                </select>                               
                            </span>                            
                            
                            <span id="edit_time_colon">:</span>
                            
                            <span id="edit_minute_container">
                                <select class="snapselect" id="edit_minute">
                                    <option value="00">00</option>
                                    <option value="01">01</option>
                                    <option value="02">02</option>
                                    <option value="03">03</option>
                                    <option value="04">04</option>
                                    <option value="05">05</option>
                                    <option value="06">06</option>
                                    <option value="07">07</option>
                                    <option value="08">08</option>
                                    <option value="09">09</option>                                
                                    <option value="10">10</option>
                                    <option value="11">11</option>
                                    <option value="12">12</option>
                                    <option value="13">13</option>
                                    <option value="14">14</option>
                                    <option value="15">15</option>
                                    <option value="16">16</option>
                                    <option value="17">17</option>
                                    <option value="18">18</option>
                                    <option value="19">19</option>                                
                                    <option value="20">20</option>                                
                                    <option value="21">21</option>
                                    <option value="22">22</option>
                                    <option value="23">23</option>
                                    <option value="24">24</option>
                                    <option value="25">25</option>
                                    <option value="26">26</option>
                                    <option value="27">27</option>
                                    <option value="28">28</option>
                                    <option value="29">29</option>                                
                                    <option value="30">30</option>                                
                                    <option value="31">31</option>
                                    <option value="32">32</option>
                                    <option value="33">33</option>
                                    <option value="34">34</option>
                                    <option value="35">35</option>
                                    <option value="36">36</option>
                                    <option value="37">37</option>
                                    <option value="38">38</option>
                                    <option value="39">39</option>                                
                                    <option value="40">40</option>                                
                                    <option value="41">41</option>
                                    <option value="42">42</option>
                                    <option value="43">43</option>
                                    <option value="44">44</option>
                                    <option value="45">45</option>
                                    <option value="46">46</option>
                                    <option value="47">47</option>
                                    <option value="48">48</option>
                                    <option value="49">49</option>                                
                                    <option value="50">50</option>                                
                                    <option value="51">51</option>
                                    <option value="52">52</option>
                                    <option value="53">53</option>
                                    <option value="54">54</option>
                                    <option value="55">55</option>
                                    <option value="56">56</option>
                                    <option value="57">57</option>
                                    <option value="58">58</option>
                                    <option value="59">59</option>                        
                                </select>                                
                            </span>
							
							<span id="edit_past_the_hour" style="display:none"> <fmt:message key="label.minute.past.hour"/></span>
                            
                            <span id="edit_meridiem_container">
                                <select class="snapselect"id="edit_meridiem">                                                                
                                    <option value="AM">AM</option>
                                    <option value="PM">PM</option>                                   
                                </select>
                            </span>                     
                        </li>                     
                        <li style="margin-top:10px;" id="edit_day_of_week_container">
                            <label for="filter" style="width:75px">
                                <fmt:message key="label.day.of.week"/>:</label>
                            <select class="snapselect"id="edit_day_of_week">
                                <option value="1"><fmt:message key="label.sunday"/></option>
                                <option value="2"><fmt:message key="label.monday"/></option>
                                <option value="3"><fmt:message key="label.tuesday"/></option>
                                <option value="4"><fmt:message key="label.wednesday"/></option>
                                <option value="5"><fmt:message key="label.thursday"/></option>
                                <option value="6"><fmt:message key="label.friday"/></option>
                                <option value="7"><fmt:message key="label.saturday"/></option>                                
                            </select>                            
                        </li>
                        <li style="margin-top:10px;" id="edit_day_of_month_container">
                            <label for="filter" style="width:75px">
                                <fmt:message key="label.day.of.month"/>:</label>
                            <select class="snapselect" id="edit_day_of_month">
                                <option value="1">1</option>
                                <option value="2">2</option>
                                <option value="3">3</option>
                                <option value="4">4</option>
                                <option value="5">5</option>
                                <option value="6">6</option>
                                <option value="7">7</option>
                                <option value="8">8</option>
                                <option value="9">9</option>
                                <option value="10">10</option>
                                <option value="11">11</option>
                                <option value="12">12</option>
                                <option value="13">13</option>
                                <option value="14">14</option>
                                <option value="15">15</option>
                                <option value="16">16</option>
                                <option value="17">17</option>
                                <option value="18">18</option>
                                <option value="19">19</option>
                                <option value="20">20</option>
                                <option value="21">21</option>
                                <option value="22">22</option>
                                <option value="23">23</option>
                                <option value="24">24</option>
                                <option value="25">25</option>
                                <option value="26">26</option>
                                <option value="27">27</option>
                                <option value="28">28</option>
                            </select>                            
                        </li>
                        <li style="margin-top:10px;">
                            <label for="edit_timezone" style="width:75px">
                                <fmt:message key="label.time.zone"/>:</label>                             
                            <select class="snapselect" id="edit_timezone" style="width:240px">
								<option value='Etc/GMT+12'>[UTC-12:00] GMT-12:00</option>
								<option value='Etc/GMT+11'>[UTC-11:00] GMT-11:00</option>
								<option value='Pacific/Samoa'>[UTC-11:00] Samoa Standard Time</option>
								<option value='Pacific/Honolulu'>[UTC-10:00] Hawaii Standard Time</option>
								<option value='US/Alaska'>[UTC-09:00] Alaska Standard Time</option>
								<option value='America/Los_Angeles'>[UTC-08:00] Pacific Standard Time</option>
								<option value='Mexico/BajaNorte'>[UTC-08:00] Baja California</option>
								<option value='US/Arizona'>[UTC-07:00] Arizona</option>
								<option value='US/Mountain'>[UTC-07:00] Mountain Standard Time</option>
								<option value='America/Chihuahua'>[UTC-07:00] Chihuahua, La Paz</option>
								<option value='America/Chicago'>[UTC-06:00] Central Standard Time</option>
								<option value='America/Costa_Rica'>[UTC-06:00] Central America</option>
								<option value='America/Mexico_City'>[UTC-06:00] Mexico City, Monterrey</option>
								<option value='Canada/Saskatchewan'>[UTC-06:00] Saskatchewan</option>
								<option value='America/Bogota'>[UTC-05:00] Bogota, Lima</option>
								<option value='America/New_York'>[UTC-05:00] Eastern Standard Time</option>
								<option value='America/Caracas'>[UTC-04:00] Venezuela Time</option>
								<option value='America/Asuncion'>[UTC-04:00] Paraguay Time</option>
								<option value='America/Cuiaba'>[UTC-04:00] Amazon Time</option>
								<option value='America/Halifax'>[UTC-04:00] Atlantic Standard Time</option>
								<option value='America/La_Paz'>[UTC-04:00] Bolivia Time</option>
								<option value='America/Santiago'>[UTC-04:00] Chile Time</option>
								<option value='America/St_Johns'>[UTC-03:30] Newfoundland Standard Time</option>
								<option value='America/Araguaina'>[UTC-03:00] Brasilia Time</option>
								<option value='America/Argentina/Buenos_Aires'>[UTC-03:00] Argentine Time</option>
								<option value='America/Cayenne'>[UTC-03:00] French Guiana Time</option>
								<option value='America/Godthab'>[UTC-03:00] Greenland Time</option>
								<option value='America/Montevideo'>[UTC-03:00] Uruguay Time]</option>
								<option value='Etc/GMT+2'>[UTC-02:00] GMT-02:00</option>
								<option value='Atlantic/Azores'>[UTC-01:00] Azores Time</option>
								<option value='Atlantic/Cape_Verde'>[UTC-01:00] Cape Verde Time</option>
								<option value='Africa/Casablanca'>[UTC] Casablanca</option>
								<option value='Etc/UTC'>[UTC] Coordinated Universal Time</option>
								<option value='Atlantic/Reykjavik'>[UTC] Reykjavik</option>
								<option value='Europe/London'>[UTC] Western European Time</option>
								<option value='CET'>[UTC+01:00] Central European Time</option>
								<option value='Europe/Bucharest'>[UTC+02:00] Eastern European Time</option>
								<option value='Africa/Johannesburg'>[UTC+02:00] South Africa Standard Time</option>
								<option value='Asia/Beirut'>[UTC+02:00] Beirut</option>
								<option value='Africa/Cairo'>[UTC+02:00] Cairo</option>
								<option value='Asia/Jerusalem'>[UTC+02:00] Israel Standard Time</option>
								<option value='Europe/Minsk'>[UTC+02:00] Minsk</option>
								<option value='Europe/Moscow'>[UTC+03:00] Moscow Standard Time</option>
								<option value='Africa/Nairobi'>[UTC+03:00] Eastern African Time</option>
								<option value='Asia/Karachi'>[UTC+05:00] Pakistan Time</option>
								<option value='Asia/Kolkata'>[UTC+05:30] India Standard Time</option>
								<option value='Asia/Bangkok'>[UTC+05:30] Indochina Time</option>
								<option value='Asia/Shanghai'>[UTC+08:00] China Standard Time</option>
								<option value='Asia/Kuala_Lumpur'>[UTC+08:00] Malaysia Time</option>
								<option value='Australia/Perth'>[UTC+08:00] Western Standard Time (Australia)</option>
								<option value='Asia/Taipei'>[UTC+08:00] Taiwan</option>
								<option value='Asia/Tokyo'>[UTC+09:00] Japan Standard Time</option>
								<option value='Asia/Seoul'>[UTC+09:00] Korea Standard Time</option>
								<option value='Australia/Adelaide'>[UTC+09:30] Central Standard Time (South Australia)</option>
								<option value='Australia/Darwin'>[UTC+09:30] Central Standard Time (Northern Territory)</option>
								<option value='Australia/Brisbane'>[UTC+10:00] Eastern Standard Time (Queensland)</option>
								<option value='Australia/Canberra'>[UTC+10:00] Eastern Standard Time (New South Wales)</option>
								<option value='Pacific/Guam'>[UTC+10:00] Chamorro Standard Time</option>
								<option value='Pacific/Auckland'>[UTC+12:00] New Zealand Standard Time</option>
         		            </select>                                                          
                        </li>
						<li style="margin-top:10px;">
                            <label for="edit_max" style="width:75px">
                                <fmt:message key="label.keep"/>:</label>
                            <input class="text" style="width: 68px;" type="text" id="edit_max"/>
                            <span><fmt:message key="label.snapshot.s"/></span>
                            <div id="edit_max_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>                             
                        </li>  						
                    </ol>
                    </form>
                </div>
            </div>
        </div>
    </div>
	<div id="info_container" class="ui_dialog_messagebox" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon">
        </div>
        <div id="info" class="ui_dialog_messagebox_text">
            (info)</div>
    </div>
</div>
<!-- Take Snapshots Dialog (end) -->

<!-- Add Volume Dialog (begin) -->
<div id="dialog_add_volume" title='<fmt:message key="label.add.volume"/>' style="display: none">
    <p><fmt:message key="message.add.volume"/></p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form2">
        <ol>
            <li>
                <label for="add_volume_name">
                    <fmt:message key="label.name"/>:</label>
                <input class="text" type="text" name="add_volume_name" id="add_volume_name" />
                <div id="add_volume_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="volume_zone">
                    <fmt:message key="label.availability.zone"/>:</label>
                <select class="select" name="volume_zone" id="volume_zone">
                    <option value="default">Please wait...</option>
                </select>
            </li>
            <li>
                <label for="volume_diskoffering">
                    <fmt:message key="label.disk.offering"/>:</label>
                <select class="select" name="volume_diskoffering" id="volume_diskoffering">
                    <option value="default"><fmt:message key="label.please.wait"/>...</option>
                </select>
            </li>
            <li id="size_container">
                <label>
                    <fmt:message key="label.disk.size.gb"/>:</label>
                <input class="text" type="text" id="size" />
                <div id="size_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog (end) -->

<!-- Attach Volume Dialog (begin) -->
<div id="dialog_attach_volume" title='<fmt:message key="label.action.attach.disk"/>' style="display: none">
    <p><fmt:message key="message.attach.volume"/></p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form3">
        <ol>
            <li>
                <label for="volume_vm">
                    <fmt:message key="label.instance"/>:</label>
                <select class="select" name="volume_vm" id="volume_vm">
                </select>
                <div id="volume_vm_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Attach Volume Dialog (end) -->

<!--  Snapshot tab template (begin) -->
<div class="grid_container" id="snapshot_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="action_link"><p><fmt:message key="label.actions"/></p>
            <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                <fmt:message key="label.waiting"/> &hellip;
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
                <fmt:message key="label.id"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.snapshot.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.volume.name"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="volumename">
            </div>
        </div>
    </div>  
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.state"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="state">
            </div>
        </div>
    </div>    
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.interval.type"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="intervaltype">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.created"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.account"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <fmt:message key="label.domain"/>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>    
</div>
<!--  Snapshot tab template (end) -->

<!-- Add Volume Dialog from Snapshot (begin) -->
<div id="dialog_add_volume_from_snapshot" title='<fmt:message key="label.action.create.volume"/>' style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form5">
        <ol>
            <li>
                <label><fmt:message key="label.name"/>:</label>
                <input class="text" type="text" id="name" />
                <div id="name_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
            </li>           
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog from Snapshot (end) -->

<!-- Create template from snapshot (begin) -->
<div id="dialog_create_template_from_snapshot" title='<fmt:message key="label.action.create.template"/>' style="display:none">	
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form6">
			<ol>
				<li>
					<label><fmt:message key="label.name"/>:</label>
					<input class="text" type="text" id="name" style="width:250px"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label><fmt:message key="label.display.text"/>:</label>
					<input class="text" type="text" id="display_text" style="width:250px"/>
					<div id="display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
					<label><fmt:message key="label.os.type"/>:</label>
					<select class="select" id="os_type">
					</select>
				</li>				
				<li id="create_template_public_container" style="display:none">
	                <label for="ispublic">
	                    <fmt:message key="label.public"/>:</label>
	                <select class="select" name="ispublic" id="ispublic">
	                    <option value="false"><fmt:message key="label.no"/></option>
	                    <option value="true"><fmt:message key="label.yes"/></option>
	                </select>
	            </li>						
				<li>
					<label><fmt:message key="label.password.enabled"/>:</label>
					<select class="select" id="password">						
						<option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>
					</select>
				</li>
				<li id="isfeatured_container" style="display:none">
					<label><fmt:message key="label.featured"/>:</label>
					<select class="select" id="isfeatured">
					    <option value="false"><fmt:message key="label.no"/></option>
						<option value="true"><fmt:message key="label.yes"/></option>						
					</select>
				</li>		
			</ol>
		</form>
	</div>
</div>
<!-- Create template from snapshot (end) -->

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>               
                <li>
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
                <li id="adv_search_domain_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="domain" value='<fmt:message key="label.by.domain" />' />
                    <div id="domain_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
                    <!--  
                    <select class="select" id="adv_search_domain">
                    </select>
                    -->
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value='<fmt:message key="label.by.account" />' />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
