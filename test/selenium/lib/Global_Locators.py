# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

'''
Variable Names are as follows
Logical Page Descriptor_____What Element Represents and/or where it is_____LocatorType


For Example ::

instances_xpath = "//div[@id='navigation']/ul/li[2]/span[2]"

Means this is:: xpath link for Instances which is present on Dashboard.
Any test cases that requires to go into Instances from Dashboard can use this variable now.

This may not be intuitive as you go deep into the tree.



for example

stopinstanceforce_id

The best way to know what this represents is to track by variable name
Under Instances / any instance is click on any instance (applies to any instance) / stop instance has a force stop check box when you click.
This link represents that.


Steps below do not have global locators.

PF rule steps including and after filling port numbers. (Refer to vmLifeAndNetwork.py / def test_PF)
FW rule steps including and after filling port numbers. (Refer to vmLifeAndNetwork.py / def test_PF)
ADD Disk Offering page has Names, description, storage type etc etc
ADD Compute Offering page has Names, description, CPU Cores, CPU clocks type etc etc

Create Acc, Delete Acc, Login and Logout are for test flow and are not test cases. They do not have global Locators.

Such and many more data entry points that appear only once and hence we do not need glonal names for them. They are hard coded as and when needed in the scripts.


'''

################################################################################################################################################################################################

## Links on the Main UI page (Dash board). Listed in the order they appear on screen
dashboard_xpath = "//div[@id='navigation']/ul/li"
instances_xpath = "//div[@id='navigation']/ul/li[2]/span[2]" # Link for Instance and following as self explanatory
storage_xpath = "//div[@id='navigation']/ul/li[3]/span[2]"
network_xpath = "//div[@id='navigation']/ul/li[4]/span[2]"
templates_xpath = "//div[@id='navigation']/ul/li[5]/span[2]"
events_xpath = "//div[@id='navigation']/ul/li[6]/span[2]"
projects_xpath = "//div[@id='navigation']/ul/li[7]/span[2]"
accounts_xpath = "//div[@id='navigation']/ul/li[8]/span[2]"
domains_xpath = "//div[@id='navigation']/ul/li[9]/span[2]"
infrastructure_xpath = "//div[@id='navigation']/ul/li[10]/span[2]"
globalSettings_xpath = "//div[@id='navigation']/ul/li[11]/span[2]"
serviceOfferings_xpath = "//div[@id='navigation']/ul/li[12]/span[2]"

################################################################################################################################################################################################

## Instances Page
## Instances Main page 


# Add Instance Button on top right corner of Instances page
add_instance_xpath = "//div[2]/div/div[2]/div/div[2]/span"

# Add Instance Wizard next button
add_instance_next_xpath = "//div[4]/div[2]/div[3]/div[3]/span"

# Table that lists all VM's under Instances page; General usage is to traverse through this table and search for the VM we are interested in.
instances_table_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div[2]/table/tbody/tr/td/span"


# Click any instance and following are available

# Click ok on confirmation pop-up box for most actions listed below
actionconfirm_xpath = ("//button[@type='button']")

# status of VM running. Click on VM > 3rd row in table
state_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div[2]/div[2]/div/div/div[2]/div/table/tbody/tr[3]/td[2]/span"

# Stop instance icon
stopinstance_css = "a[alt=\"Stop Instance\"] > span.icon"

# stop instance forcefully check box available after stop instance is executed in separate pop up
stopinstanceforce_id = ("force_stop")

# start instance icon
startinstance_css = "a[alt=\"Start Instance\"] > span.icon"

yesconfirmation_xapth = "(//button[@type='button'])[2]"


# Destroy instance icon
destroyinstance_css = "a[alt=\"Destroy Instance\"] > span.icon"

#Restore Instance icon
restoreinstance_css = "a[alt=\"Restore Instance\"] > span.icon"

# Reboot instance
rebootinstance_css = "a[alt=\"Reboot Instance\"] > span.icon"

################################################################################################################################################################################################


## Network Page

# Table that lists all Networks under Network page; General usage is to traverse through this table and search for the network we are interested in.
network_networktable_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div[3]/div[2]/div/div[2]/table/tbody/tr/td/span"

# View IP addresses button on each network page
viewIp_css="div.view-all > a > span"

# Acquire a new ip
acquireIP_xpath="//div[2]/div/div/div[2]/span"
# List of IP's within a netork table
network_iptables_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div[3]/div[2]/div/div[2]/table/tbody/tr/td/span"
# Configuration tab for each IP
ipConfiguration_text="Configuration"
# PF under configuration for each IP
ip_PF = "li.portForwarding > div.view-details"


################################################################################################################################################################################################


## Servivce Offering Page

# Selects Compute offering from drop down menu
Offering_compute_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[1]"

# Selects System offering from drop down menu
Offering_system_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[2]"

# Selects Disk offering from drop down menu
Offering_disk_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[3]"

# Selects Network offering from drop down menu
Offering_network_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[4]"

# Add Offering
Offering_add_xpath ="//div[3]/span"

# Points to tbale that lists Offerings
Offering_table_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div[2]/table/tbody/tr/td/span"

# Edit Button
Offering_edit_css = "a[alt=\"Edit\"] > span.icon"

# Edit name box
Offering_editname_name = "name"

# Edit description box
Offering_editdescription_name = "displaytext" 

# Edit finished click ok
Offering_editdone_css="div.button.done"

# delete offering button for Disk only
Offering_delete_css = "a[alt=\"Delete Disk Offering\"] > span.icon"

# delete offering button for Compute only
Offering_deletecompute_css = "a[alt=\"Delete Service Offering\"] > span.icon"




################################################################################################################################################################################################


#### Templates Page

# Selects Templates from drop down
template_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[1]"

# Selects ISO from drop down
iso_xpath = "/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div/div/div/select/option[2]"

# Add Template
AddTemplate_xpath = "//div[3]/span"

# Points to table where all templates are
template_table_xpath ="/html/body/div/div/div[2]/div[2]/div[2]/div/div[2]/div[2]/table/tbody/tr/td/span"

# Edit Template Button
template_edit_css = "a[alt=\"Edit\"] > span.icon"

# Edit finished click OK
template_editdone_css = "div.button.done"

# Delete Template button
template_delete_css = "a[alt=\"Delete Template\"] > span.icon"


################################################################################################################################################################################################


## Login Page

# Username box
login_username_css = "body.login > div.login > form > div.fields > div.field.username > input[name=\"username\"]" # Login>Username TextBox

# Password Box
login_password_css = "body.login > div.login > form > div.fields > div.field.password > input[name=\"password\"]" # LoginPassword TextBox

# Click ok to login
login_submit_css = "body.login > div.login > form > div.fields > input[type=\"submit\"]" # Login>Login Button (Submit button)


