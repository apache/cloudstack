hypervisor="kvm"

#Isolate the run into a virtualenv
/usr/local/bin/virtualenv-2.7 -p /usr/local/bin/python2.7 nightly-smoke-kvm-$BUILD_NUMBER

#Copy the tests into the virtual env
rsync -az test nightly-smoke-kvm-$BUILD_NUMBER/
cd nightly-smoke-kvm-$BUILD_NUMBER

## Start
source bin/activate

#Get Marvin and install
tar=$(wget -O - http://jenkins.cloudstack.org:8080/job/build-marvin-4.0/lastSuccessfulBuild/artifact/tools/marvin/dist/ | grep Marvin |  sed -e :a -e 's/<[^>]*>//g;/</N;//ba' | sed -e 's/[ \t]*//g' | cut -d"z" -f1)'z'
url='http://jenkins.cloudstack.org:8080/job/build-marvin-4.0/lastSuccessfulBuild/artifact/tools/marvin/dist/'$tar
wget $url

#Latest deployment configs for marvin
git clone https://github.com/vogxn/cloud-autodeploy.git
cd cloud-autodeploy
git checkout acs-infra-test
cd ..

#Install necessary python eggs
pip -q install $tar
pip -q install netaddr
pip -q install /opt/xunitmp ## Plugin is not in nose-mainline yet: https://github.com/nose-devs/nose/issues/2 ##
#Install marvin-nose plugin
pip -q install lib/python2.7/site-packages/marvin/

#Deploy the configuration - yes/no
if [[ $DEPLOY == "yes" ]]; then
    cd cloud-autodeploy
    if [[ $hypervisor == 'xen' ]];then
        profile='xen602'
    else
        profile='rhel63-kvm'
    fi
    python configure.py -v $hypervisor -d $distro -p $profile -l $LOG_LVL
    cd ../test
    nosetests -v --with-marvin --marvin-config=../cloud-autodeploy/$hypervisor.cfg -w /tmp
    #Restart to apply global settings
    python ../cloud-autodeploy/restartMgmt.py --config ../cloud-autodeploy/$hypervisor.cfg
    cd $WORKSPACE/nightly-smoke-kvm-$BUILD_NUMBER
fi

#Health Check
nosetests -v --with-marvin --marvin-config=cloud-autodeploy/$hypervisor.cfg --load cloud-autodeploy/testSetupSuccess.py

#Setup Test Data
cd test
bash setup-test-data.sh -t integration/smoke -m 10.223.75.41 -p password -d 10.223.75.41 -h $hypervisor
for file in `find integration/smoke/ -name *.py -type f`
do
    sed -i "s/http:\/\/iso.linuxquestions.org\/download\/504\/1819\/http\/gd4.tuwien.ac.at\/dsl-4.4.10.iso/http:\/\/nfs1.lab.vmops.com\/isos_32bit\/dsl-4.4.10.iso/g" $file
done
if [[ $? -ne 0 ]]; then
    echo "Problem seeding test data"
    exit 2
fi

if [[ $DEBUG == "yes" ]]; then
    nosetests -v --with-marvin --marvin-config=../cloud-autodeploy/$hypervisor.cfg -w integration/smoke --load --with-xunitmp --collect-only
else
    set +e
    nosetests -v --processes=5 --process-timeout=3600 --with-marvin --marvin-config=`pwd`/../cloud-autodeploy/$hypervisor.cfg -w integration/smoke --load --with-xunitmp
    set -e
fi

cp -fv integration/smoke/nosetests.xml $WORKSPACE
#deactivate, cleanup and exit
deactivate
rm -rf nightly-smoke-kvm-$BUILD_NUMBER
