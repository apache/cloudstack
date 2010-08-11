#!/usr/bin/env bash
# $Id: createtmplt.sh 11474 2010-08-06 05:53:02Z edison $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/qcow2/createtmplt.sh $
# createtmplt.sh -- install a template

usage() {
  printf "Usage: %s: -t <template-fs> -n <templatename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
}


#set -x


verify_cksum() {
  echo  "$1  $2" | md5sum  -c --status
  #printf "$1\t$2" | md5sum  -c --status
  if [ $? -gt 0 ] 
  then
    printf "Checksum failed, not proceeding with install\n"
    exit 3
  fi
}

untar() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local basedir=$(dirname $1)
  case $ft in
  USTAR)  local rootimg=$(tar tf $1 | grep $3)
          (cd $2; tar xf $1)
          rm -f $1
          printf "$2/$rootimg"
          ;;
      *)  printf "$1"
          return 0
	  ;;
  esac

}

uncompress() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local imgfile=${1%.*} #strip out trailing file suffix
  local tmpfile=${imgfile}.tmp

  case $ft in
  gzip)  gunzip -c $1 > $tmpfile
         ;;
  bzip2)  bunzip2 -c $1 > $tmpfile
         ;;
  ZIP)  unzip -p $1 | cat > $tmpfile
        ;;
  *)	printf "$1"
        return 0
	;;
  esac

  if [ $? -gt 0 ] 
  then
    printf "Failed to uncompress file, exiting "
    exit 1 
  fi
 
  mv $tmpfile $imgfile
  printf "$imgfile"

  return 0
}

create_from_file() {
  local tmpltfs=$1
  local tmpltimg=$2
  local tmpltname=$3


  #copy the file to the disk
  cp $tmpltimg /$tmpltfs/$tmpltname
  
  if [ "$cleanup" == "true" ]
  then
    rm -f $tmpltimg
  fi
}

tflag=
nflag=
fflag=
sflag=
hflag=
hvm=false
cleanup=false
dflag=
cflag=

while getopts 'uht:n:f:s:c:d:' OPTION
do
  case $OPTION in
  t)	tflag=1
		tmpltfs="$OPTARG"
		;;
  n)	nflag=1
		tmpltname="$OPTARG"
		;;
  f)	fflag=1
		tmpltimg="$OPTARG"
		;;
  s)	sflag=1
		volsize="$OPTARG"
		;;
  c)	cflag=1
		cksum="$OPTARG"
		;;
  d)	dflag=1
		descr="$OPTARG"
		;;
  u)	cleanup="true"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$nflag$fflag" != "111" ]
then
 usage
 exit 2
fi


if [ ! -d /$tmpltfs ] 
then
  mkdir -p /$tmpltfs
  if [ $? -gt 0 ] 
  then
    printf "Failed to create user fs $tmpltfs\n" >&2
    exit 1
  fi
fi

if [ ! -f $tmpltimg ] 
then
  printf "root disk file $tmpltimg doesn't exist\n"
  exit 3
fi


create_from_file $tmpltfs $tmpltimg $tmpltname

touch /$tmpltfs/template.properties
echo -n "" > /$tmpltfs/template.properties

today=$(date '+%m_%d_%Y')
echo "filename=$tmpltname" > /$tmpltfs/template.properties
echo "snapshot.name=$today" >> /$tmpltfs/template.properties
echo "description=$descr" >> /$tmpltfs/template.properties

if [ "$cleanup" == "true" ]
then
  rm -f $tmpltimg
fi

exit 0
