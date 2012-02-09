help() {
  printf " -o path of old system iso, by default is /usr/lib64/cloud/agent/vms/systemvm.iso \n"
  printf " -n path of new system iso \n"
  printf " -v version of mgt server[2.1 or 2.2], by default is 2.2\n"
  printf " -c don't upgrade console proxy jar, by default, it's no\n"
}

oflag=
iflag=
vflag=
cflag=
oldPath=
newPath=
version=
upgradeConsole=

while getopts 'o:n:v:c' OPTION
do
  case $OPTION in
     o) oflag=1
        oldPath="$OPTARG"
        ;;
     n) iflag=1
        newPath="$OPTARG"
        ;;
     v) vflag=1
        version="$OPTARG"
        ;;
     c) cflag=1
        ;;
     ?) help 
        ;; 
   esac
done

if [ "$iflag" != "1" ]
then
   help
   exit 1
fi

if [ "$oflag" != "1" ]
then
  oldPath="/usr/lib64/cloud/agent/vms/systemvm.iso" 
fi

if [ "$vflag" != "1" ]
then
  version="2.2"
fi

if [ ! -f "$newPath" ]
then
   printf "Can't find new system iso: $newPath\n"
   exit 1
fi

patchIso22() {
oldIsoMount=`mktemp -d`
newIsoMount=`mktemp -d`
newPathTemp=`mktemp -d`
mkISOTemp=`mktemp -d`


mount $newPath $newIsoMount -o loop
cp $newIsoMount/systemvm.zip $newPathTemp
cd $newPathTemp
unzip $newPathTemp/systemvm.zip &> /dev/null

umount $newPath

mount $oldPath $oldIsoMount -o loop
cp -fr $oldIsoMount/* $mkISOTemp
mkdir $newPathTemp/oldsystemvm
cp -fr $mkISOTemp/systemvm.zip $newPathTemp/oldsystemvm/ 
cd $newPathTemp/oldsystemvm
unzip systemvm.zip &> /dev/null

if [ "$cflag" != "1" ]
then
   cp -f ../cloud-console-proxy.jar . 
fi

cp -f ../certs/realhostip.* ./certs/
rm -f systemvm.zip
zip -r systemvm.zip * &> /dev/null
cp -f systemvm.zip $mkISOTemp/

newIsoName=/tmp/`uuidgen`.iso
mkisofs -quiet -r -o $newIsoName $mkISOTemp
umount $oldPath
cp -f $newIsoName $oldPath

rm -rf $oldIsoMount
rm -rf $newIsoMount
rm -rf $newPathTemp
rm -rf $mkISOTemp
rm -rf $newIsoName
}

patchIso21() {
  newTemp=`mktemp -d`
  oldTemp=`mktemp -d`
  cp $oldPath $oldTemp/
  cd $oldTemp
  unzip systemvm-premium.zip &>/dev/null
  cp $newPath $newTemp/
  cd $newTemp
  unzip systemvm-premium.zip &>/dev/null
  cp -fr $newTemp/cloud-console-proxy-premium.jar $oldTemp/
  cp -fr $newTemp/certs/realhostip.* $oldTemp/certs/
  cd $oldTemp
  rm systemvm-premium.zip
  zip -r systemvm-premium.zip * &> /dev/null
  cp -fr systemvm-premium.zip $oldPath
  rm -rf $oldTemp
  rm -rf $newTemp
}

losetup -a |grep "$oldPath"
if [ $? -eq 0 ]
then
   printf "please umount $oldPath\n"
   exit 1
fi

losetup -a |grep "$newPath"
if [ $? -eq 0 ]
then
   printf "please umount $newPath\n"
   exit 1
fi

if [ "$version" == "2.2" ]
then 
  patchIso22
else
  patchIso21
fi

