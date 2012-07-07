#!/bin/bash
usage() {
  printf "Usage: %s:\n
	[-t path to tests ]  \n
	[-m mgmt-server ] \n
	[-c config-file ] \n
	[-d db node url ]\n" $(basename $0) >&2
}

failed() {
	exit $1
}

#defaults
FMT=$(date +"%d_%I_%Y_%s")
MGMT_SVR="localhost"
BASEDIR="/root/cloudstack-oss/test"
TESTDIR="/root/cloudstack-oss/test/integration/smoke-simulator/"
CONFIG="/root/cloudstack-oss/test/integration/smoke-simulator/simulator-smoke.cfg"
DB_SVR="localhost"

while getopts 't:d:m:c:b:' OPTION
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
  c)    cflag=1
		CONFIG="$OPTARG"
		;;
  b)    bflag=1
		BASEDIR="$OPTARG"
		;;
  ?)	usage
		failed 2
		;;
  esac
done

ostypeid=$(mysql -uroot -Dcloud -h$MGMT_SVR -s -N -r -e"select uuid from guest_os where id=11")
$(mysql -uroot -Dcloud -h$MGMT_SVR -s -N -r -e"update configuration set value='8096' where name='integration.api.port'")

pushd $BASEDIR
for file in `find $TESTDIR -name *.py -type f`
do
	old_ostypeid=$(grep ostypeid $file | head -1 | cut -d: -f2 | tr -d " ,'")
	if [[ $old_ostypeid != "" ]]
	then
		echo "replacing:" $old_ostypeid, "with:" $ostypeid,"in " $file
		sed -i "s/$old_ostypeid/$ostypeid/g" $file
		sed -i "s/http:\/\/iso.linuxquestions.org\/download\/504\/1819\/http\/gd4.tuwien.ac.at\/dsl-4.4.10.iso/http:\/\/nfs1.lab.vmops.com\/isos_32bit\/dsl-4.4.10.iso/g" $file
		sed -i "s/fr3sca/password/g" $file
	fi
done

version_tuple=$(python -c 'import sys; print(sys.version_info[:2])')

if [[ $version_tuple == "(2, 7)" ]]
then
    python -m marvin.deployAndRun -c $CONFIG -t /tmp/t.log -r /tmp/r.log -d /tmp
    sleep 60
    python -m marvin.deployAndRun -c $CONFIG -t /tmp/t.log -r /tmp/r.log -f $TESTDIR/testSetupSuccess.py -l
    cat /tmp/r.log
    python -m marvin.deployAndRun -c $CONFIG -t /tmp/t.log -r /tmp/r.log -d $TESTDIR -l -n
    echo "Done"
else
    echo "Python version 2.7 not detected on system. Aborting"
fi
popd
