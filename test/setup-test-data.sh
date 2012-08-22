#!/bin/bash
usage() {
  printf "Usage: %s:\n
	[-t path to tests ]  \n
	[-m mgmt-server ] \n
	[-p hypervisor root password ] \n
	[-d db node url ]\n" $(basename $0) >&2
}

failed() {
	exit $1
}

#defaults
TESTDIR="/root/cloudstack/test/"
MGMT_SVR="localhost"
DB_SVR="localhost"
HV_PASSWD="password"

while getopts 't:d:m:p:' OPTION
do
  case $OPTION in
  d)    dflag=1
		DB_SVR="$OPTARG"
		;;
  t)    tflag=1
		TESTDIR="$OPTARG"
		;;
  m)    mflag=1
		MGMT_SVR="$OPTARG"
		;;
  p)    pflag=1
		HV_PASSWD="$OPTARG"
		;;
  ?)	usage
		failed 2
		;;
  esac
done

#Damn Small Linux ISO type
ostypeid=$(mysql -uroot -Dcloud -h$DB_SVR -s -N -r -e"select uuid from guest_os where display_name='CentOS 5.3 (64-bit)'")
if [[ $ostypeid == "" ]]; then
    echo "Unable to contact DB server @ $DB_SVR"
    exit 2
fi

nc -z $MGMT_SVR 8096
if [[ $? -ne 0 ]]; then
    echo "$MGMT_SVR doesn't have port 8096 open"
    exit 2
fi

if [[ ! -d $TESTDIR ]]; then
    echo "No directory $TESTDIR found"
    exit 2
fi
for file in `find $TESTDIR -name *.py -type f`
do
	old_ostypeid=$(grep ostypeid $file | head -1 | cut -d: -f2 | tr -d " ,'")
	if [[ $old_ostypeid != "" ]]
	then
		echo "replacing:" $old_ostypeid, "with:" $ostypeid,"in " $file
		sed -i "s/$old_ostypeid/$ostypeid/g" $file
		#sed -i "s/http:\/\/iso.linuxquestions.org\/download\/504\/1819\/http\/gd4.tuwien.ac.at\/dsl-4.4.10.iso/http:\/\/nfs1.lab.vmops.com\/isos_32bit\/dsl-4.4.10.iso/g" $file
		sed -i "s/fr3sca/$HV_PASSWD/g" $file
	fi
done

#Python version check
version_tuple=$(python -c 'import sys; print(sys.version_info[:2])')
if [[ $version_tuple == "(2, 7)" ]]
then
    echo "Done"
else
    echo "WARN: Python version 2.7 not detected on system."
fi
