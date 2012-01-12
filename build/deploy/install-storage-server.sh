#!/usr/bin/env bash
# Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
#     
# This software is licensed under the GNU General Public License v3 or later.
# 
# It is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or any later version.
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
# 



 

# install-storage-server.sh: Installs a VMOps Storage Server

choose_correct_filename() {
	local default_filename=$1
	local user_specified_filename=$2
	
	if [ -f "$user_specified_filename" ]
	then
		echo $user_specified_filename
		return 0
	else
		if [ -f "$default_filename" ]
		then
			echo $default_filename
			return 0
		else
			echo ""
			return 1
		fi
	fi
}

install_opensolaris_package() {
	pkg_name=$1
	
	pkg info $pkg_name >> /dev/null
	
	if [ $? -gt 0 ]
	then
		# The package is not installed, so install it
		pkg install $pkg_name
		return $?
	else
		# The package is already installed
		return 0
	fi
}

exit_if_error() {
	return_code=$1
	msg=$2
	
	if [ $return_code -gt 0 ]
	then
		echo $msg
		exit 1
	fi
}

usage() {
  printf "Usage: ./install-storage-server.sh <path to agent.zip> <path to templates.tar.gz>"
}

AGENT_FILE=$(choose_correct_filename "./agent.zip" $1)
exit_if_error $? "Please download agent.zip to your Storage Server."

TEMPLATES_FILE=$(choose_correct_filename "./templates.tar.gz" $2)
exit_if_error $? "Please download templates.tar.gz to your Storage Server."

VMOPS_DIR="/usr/local/vmops"
AGENT_DIR="/usr/local/vmops/agent"
CONF_DIR="/etc/vmops"
TEMPLATES_DIR="/root/template"

# Make all the necessary directories if they don't already exist

echo "Creating VMOps directories..."
for dir in $VMOPS_DIR $CONF_DIR $TEMPLATES_DIR
do
  mkdir -p $dir
done

# Unzip agent.zip to $AGENT_DIR

echo "Uncompressing and installing VMOps Storage Agent..."
unzip -o $AGENT_FILE -d $AGENT_DIR >> /dev/null

# Remove agent/conf/agent.properties, since we should use the file in the real configuration directory

rm $AGENT_DIR/conf/agent.properties

# Backup any existing VMOps configuration files, if there aren't any backups already

if [ ! -d $CONF_DIR/BACKUP ]
then
	echo "Backing up existing configuration files..."
	mkdir -p $CONF_DIR/BACKUP
	cp $CONF_DIR/*.properties $CONF_DIR/BACKUP >> /dev/null
fi

# Copy all the files in storagehdpatch to their proper places

echo "Installing system files..."
(cd $AGENT_DIR/storagehdpatch; tar cf - .) | (cd /; tar xf -)
exit_if_error $? "There was a problem with installing system files. Please contact VMOps Support."

# Make vsetup executable
chmod +x /usr/sbin/vsetup

# Make vmops executable
chmod +x /lib/svc/method/vmops

# Uncompress the templates and copy them to the templates directory

echo "Uncompressing templates..."
tar -xzf $TEMPLATES_FILE -C $TEMPLATES_DIR >> /dev/null
exit_if_error $? "There was a problem with uncompressing templates. Please contact VMOps Support."

# Install the storage-server package, if it is not already installed
echo "Installing OpenSolaris storage server package..."
install_opensolaris_package "storage-server"
exit_if_error $? "There was a problem with installing the storage server package. Please contact VMOps Support."

echo "Installing COMSTAR..."
install_opensolaris_package "SUNWiscsit"
exit_if_error $? "Unable to install COMSTAR iscsi target.  Please contact VMOps Support."

# Install the SUNWinstall-test package, if it is not already installed

echo "Installing OpenSolaris test tools package..."
install_opensolaris_package "SUNWinstall-test"
exit_if_error $? "There was a problem with installing the test tools package. Please contact VMOps Support."

# Print a success message 
printf "\nSuccessfully installed the VMOps Storage Server.\n"
printf "Please complete the following steps to configure your networking settings and storage pools:\n\n"
printf "1. Specify networking settings in /etc/vmops/network.properties\n"
printf "2. Run \"vsetup networking\" and then specify disk settings in /etc/vmops/disks.properties\n"
printf "3. Run \"vsetup zpool\" and reboot the machine when prompted.\n\n"
     

