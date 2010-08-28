<!--
<script type="text/javascript" src="scripts/cloud.core.instances.js"></script>
-->

<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%

    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<!-- VM detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/instancetitle_icons.gif" alt="Instance" /></div>
    <h1 id="vm_name">
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display:none">
        <p id="after_action_info"></p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
        <div class="content_tabs off">
            <%=t.t("Volume")%></div>
        <div class="content_tabs off">
            <%=t.t("Statistics")%></div>
    </div>
    <div class="grid_container">
        <div class="grid_rows odd">
            <div class="vm_statusbox">
                <div class="vm_consolebox">
                </div>
                <div class="vm_status_textbox">
                    <div class="vm_status_textline green" id="state">
                    </div>
                    <br />
                    <p id="ipAddress">
                    </p>
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Zone")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="zoneName">
                    </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Template")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="templateName">
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Service")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="serviceOfferingName">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("HA")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                    <div class="cross_icon" id="ha" style="display:none">
                    </div>
                </div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Created")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="created">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
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
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("Host")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles" id="hostName">
                </div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 20%;">
                <div class="row_celltitles">
                    <%=t.t("ISO")%>:</div>
            </div>
            <div class="grid_row_cell" style="width: 79%;">
                <div class="row_celltitles">
                    <div class="cross_icon" id="iso" style="display:none">
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<!-- VM detail panel (end) -->
<!-- VM wizard (begin)-->
<div id="vm_popup" class="vmpopup_container" style="display: none">
    <div id="step1" style="display: block;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step1_bg.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 1: <strong>Select a Template</strong></h2>
                    <p>
                        Please select a template for your new virtual instance. You can also choose to select
                        a blank template from which an ISO image can be installed onto.
                    </p>
                </div>
                <div class="vmpopup_contentpanel">
                    <div class="rev_tempsearchpanel">
                        <label for="wizard_zone">
                            Availability Zone:</label>
                        <select class="select" id="wizard_zone" name="zone">
                        </select>
                        <div class="rev_tempsearchbox">
                            <form method="post" action="#">
                            <ol>
                                <li>
                                    <input id="search_input" class="text" type="text" name="search_input" />
                            </ol>
                            </form>
                            <div id="search_button" class="rev_searchbutton">
                                Search</div>
                        </div>
                    </div>
                    <div class="rev_wizformarea">
                        <div class="revwiz_message_container" style="display: none;" id="wiz_message">
                            <div class="revwiz_message_top">
                                <p id="wiz_message_text">
                                    Please select a template or ISO to continue</p>
                            </div>
                            <div class="revwiz_message_bottom">
                                <div class="revwizcontinue_button" id="wiz_message_continue">
                                </div>
                            </div>
                        </div>
                        <div class="rev_wizmid_tempbox">
                            <div class="revwiz_loadingbox" id="wiz_template_loading" style="display: none">
                                <div class="loading_gridanimation">
                                </div>
                                <p>
                                    Loading...</p>
                            </div>
                            <div class="rev_wizmid_tempbox_left" id="wiz_template_filter">
                                <div class="rev_wizmid_selectedtempbut" id="wiz_featured">
                                    Featured Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_my">
                                    My Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_community">
                                    Community Template</div>
                                <div class="rev_wizmid_nonselectedtempbut" id="wiz_blank">
                                    Blank Template</div>
                            </div>
                            <div class="rev_wizmid_tempbox_right">
                                <div class="rev_wiztemplistpanel" id="template_container">
                                    <!--
                                    <div class="rev_wiztemplistbox_selected">
                                        <div class="rev_wiztemo_centosicons">
                                        </div>
                                        <div class="rev_wiztemp_listtext">
                                            CentOS 5.4 64-bit Web Server (Apache)</div>
                                        <div class="rev_wiztemp_ownertext">
                                            System</div>
                                    </div>
                                    <div class="rev_wiztemplistbox">
                                        <div class="rev_wiztemo_centosicons">
                                        </div>
                                        <div class="rev_wiztemp_listtext">
                                            CentOS 5.4 64-bit Web Server (Apache)</div>
                                        <div class="rev_wiztemp_ownertext">
                                            System</div>
                                    </div>
                                    -->
                                </div>
                                <div class="rev_wiztemplistactions">
                                    <div class="rev_wiztemplist_actionsbox">
                                        <a href="#" id="prev_page">Prev</a> <a href="#" id="next_page">Next</a>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step" style="display: none;">
                    </div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 2</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step2" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 2: <strong>Select a Service</strong></h2>
                    <p>
                        Please select the CPU, Memory and Storage requirement you need for your new Virtual
                        Instance</p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        Service Offering</h3>
                    <div class="vmpopup_offeringpanel" id="service_offering_container">
                        <!--
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                X-Large Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Large Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Medium Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Small Instance</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        4 x 2.00 Ghz CPU, 16.00 GB of Memory, High Availability Enabled</p>
                                </div>
                            </div>
                        </div>
                        -->
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 3</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step3" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 3: <strong>Additional Data Disk Offering</strong></h2>
                    <p>
                        Please select the CPU, Memory and Storage requirement you need for your new Virtual
                        Instance</p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        Data Disk Offering</h3>
                    <div class="vmpopup_offeringpanel" id="data_disk_offering_container" style="display:none">
                    </div>
                    <div class="vmpopup_offeringpanel" id="root_disk_offering_container" style="display:none">
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 4</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step4" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="background: url(images/step2_bg.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_bg.gif) no-repeat top left">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 4: <strong>Choose your Network</strong></h2>
                    <p>
                        Please select the CPU, Memory and Storage requirement you need for your new Virtual
                        Instance</p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        Network Offering</h3>
                    <div class="vmpopup_offeringpanel">
                        <div class="vmpopup_offeringbox">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Virtual Network</label>
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        Some info about virtual network will appear here..Some info about virtual network
                                        will appear here..Some info about virtual network will appear here..Some info about
                                        virtual network will appear here..Some info about virtual network will appear here..Some
                                        info about virtual network will appear here..
                                    </p>
                                </div>
                            </div>
                        </div>
                        <div class="vmpopup_offeringbox" style="margin-top: 15px;">
                            <input type="radio" name="radiogroup" class="radio" />
                            <label class="label">
                                Shared Network:</label>
                            <input type="text" name="disksize" class="text" />
                            <div class="vmpopup_offdescriptionbox">
                                <div class="vmpopup_offdescriptionbox_top">
                                </div>
                                <div class="vmpopup_offdescriptionbox_bot">
                                    <p>
                                        Some info about virtual network will appear here..Some info about virtual network
                                        will appear here..Some info about virtual network will appear here..Some info about
                                        virtual network will appear here..Some info about virtual network will appear here..Some
                                        info about virtual network will appear here.. Some info about virtual network will
                                        appear here..Some info about virtual network will appear here..</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Go to Step 5</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
    <div id="step5" style="display: none;">
        <div class="vmpopup_container_top">
            <div class="vmpopup_steps" style="background: url(images/step1_bg_unselected.png) no-repeat top left">
                Step 1</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 2</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 3</div>
            <div class="vmpopup_steps" style="background: url(images/othersteps_bg.gif) no-repeat top left">
                Step 4</div>
            <div class="vmpopup_steps" style="color: #FFF; background: url(images/step2_selected.gif) no-repeat top left">
                Step 5</div>
            <div class="vmpopup_steps" style="background: url(images/laststep_slectedbg.gif) no-repeat top left">
            </div>
        </div>
        <div class="vmpopup_container_mid">
            <div class="vmpopup_maincontentarea">
                <div class="vmpopup_titlebox">
                    <h2>
                        Step 5: <strong>Last Step</strong></h2>
                    <p>
                        Please select the CPU, Memory and Storage requirement you need for your new Virtual
                        Instance</p>
                </div>
                <div class="vmpopup_contentpanel">
                    <h3>
                        Network Offering</h3>
                    <div class="vmpopup_offeringpanel" style="margin-top: 10px;">
                        <div class="vmpopup_reviewbox_odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Zone:</div>
                                <span>US West</span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox_even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Template:
                                </div>
                                <span>CentOS 5.4 64-bit Web Server (Apache)</span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox_odd">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Zone:</div>
                                <span>US West</span>
                            </div>
                        </div>
                        <div class="vmpopup_reviewbox_even">
                            <div class="vmopopup_reviewtextbox">
                                <div class="vmpopup_reviewtick">
                                </div>
                                <div class="vmopopup_review_label">
                                    Template:
                                </div>
                                <span>CentOS 5.4 64-bit Web Server (Apache)</span>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="vmpopup_navigationpanel">
                    <div class="vmpop_prevbutton" id="prev_step">
                        Back</div>
                    <div class="vmpop_nextbutton" id="next_step">
                        Submit</div>
                </div>
            </div>
        </div>
        <div class="vmpopup_container_bot">
        </div>
    </div>
</div>
<!-- VM wizard (end)-->
<!-- VM Wizard - Service Offering template (begin) -->
<div class="vmpopup_offeringbox" id="vm_popup_service_offering_template" style="display: none">
    <input type="radio" name="service_offering_radio" class="radio" />
    <label class="label" id="name">
    </label>
    <div class="vmpopup_offdescriptionbox">
        <div class="vmpopup_offdescriptionbox_top">
        </div>
        <div class="vmpopup_offdescriptionbox_bot">
            <p id="description">
            </p>
        </div>
    </div>
</div>
<!-- VM Wizard - Service Offering template (end) -->
<!-- VM Wizard - disk Offering template (begin)-->
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template_no" style="display: none">
    <input type="radio" name="disk_offering_radio" class="radio" />
    <label class="label">
        No Thanks</label>
</div>
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template_custom" style="display: none">
    <input type="radio" name="disk_offering_radio" class="radio" />
    <label class="label">
        Custom:</label>
    <label class="label1">
        Disk Size:</label>
    <input type="text" name="disksize" class="text" />
    <span>GB</span>
</div>
<div class="vmpopup_offeringbox" id="vm_popup_disk_offering_template" style="display: none">
    <input type="radio" name="disk_offering_radio" class="radio" />
    <label class="label" id="name">
    </label>
    <div class="vmpopup_offdescriptionbox">
        <div class="vmpopup_offdescriptionbox_top">
        </div>
        <div class="vmpopup_offdescriptionbox_bot">
            <p id="description">
            </p>
        </div>
    </div>
</div>
<!-- VM Wizard - disk Offering template (end)-->

<!-- Attach ISO Dialog -->
<div id="dialog_attach_iso" title="Attach ISO" style="display:none">
    <p>
        Please specify the ISO you wish to attach to your Virtual Instance(s)
        </span></b>.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Available ISO:</label>
                <select class="select" name="attach_iso_select" id="attach_iso_select">
                    <option value="none">No Available ISO</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
