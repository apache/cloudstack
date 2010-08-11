#!/usr/bin/env bash
# $Id: createtmplt.sh 9132 2010-06-04 20:17:43Z manuel $ $HeadURL: svn://svn.lab.vmops.com/repos/vmdev/java/scripts/storage/secondary/createtmplt.sh $
# createtmplt.sh -- install a template

usage() {
  printf "Usage: %s: -t <template-fs> -n <templatename> -f <root disk file> -s <size in Gigabytes> -c <md5 cksum> -d <descr> -h  [-u]\n" $(basename $0) >&2
}


#set -x
ulimit -f 41943040 #40GiB in blocks
ulimit -c 0

rollback_if_needed() {
  if [ $2 -gt 0 ]
  then
    printf "$3\n"
    #back out all changes
    rm -rf $1
    exit 2
fi
}

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
  case $ft in
  USTAR) 
     printf "tar archives not supported\n"  >&2
     return 1
          ;;
  *) printf "$1"
     return 0
	  ;;
  esac

}

uncompress() {
  local ft=$(file $1| awk -F" " '{print $2}')
  local imgfile=${1%.*} #strip out trailing file suffix
  local tmpfile=${imgfile}.tmp

  case $ft in
  gzip)  gunzip -q -c $1 > $tmpfile
         ;;
  bzip2)  bunzip2 -q -c $1 > $tmpfile
         ;;
  ZIP)  unzip -q -p $1 | cat > $tmpfile
        ;;
  *)	printf "$1"
        return 0
	;;
  esac

  if [ $? -gt 0 ] 
  then
    printf "Failed to uncompress file (filetype=$ft), exiting "
    return 1 
  fi
 
  mv $tmpfile $imgfile
  printf "$imgfile"

  return 0
}

create_from_file() {
  local tmpltfs=$1
  local tmpltimg=$2
  local tmpltname=$3
  local volsize=$4
  local cleanup=$5

  #copy the file to the disk
  mv $tmpltimg /$tmpltfs/$tmpltname

#  if [ "$cleanup" == "true" ]
#  then
#    rm -f $tmpltimg
#  fi
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

while getopts 'uht:n:f:s:c:d:S:' OPTION
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
  S)	Sflag=1
		size=$OPTARG
                let "size>>=10"
		ulimit -f $size
		;;
  h)	hflag=1
		hvm="true"
		;;
  u)	cleanup="true"
		;;
  ?)	usage
		exit 2
		;;
  esac
done

if [ "$tflag$nflag$fflag$sflag" != "1111" ]
then
 usage
 exit 2
fi

if [ -n "$cksum" ]
then
  verify_cksum $cksum $tmpltimg
fi

#if [ ! -d /$tmpltfs ] 
#then
#  mkdir /$tmpltfs
#  if [ $? -gt 0 ] 
#  then
#    printf "Failed to create user fs $tmpltfs\n" >&2
#    exit 1
#  fi
#fi

tmpltimg2=$(uncompress $tmpltimg)
if [ $? -ne 0 ]
then
  rollback_if_needed $tmpltfs 2 "failed to uncompress $tmpltimg\n"
fi

tmpltimg2=$(untar $tmpltimg2 /$tmpltfs vmi-root)
if [ $? -ne 0 ]
then
  rollback_if_needed $tmpltfs 2 "tar archives not supported\n"
fi

if [ ! -f $tmpltimg2 ] 
then
  rollback_if_needed $tmpltfs 2 "root disk file $tmpltimg doesn't exist\n"
  exit 3
fi

# need the 'G' suffix on volume size
if [ ${volsize:(-1)} != G ]
then
  volsize=${volsize}G
fi

#determine source file size -- it needs to be less than or equal to volsize
imgsize=$(ls -lh $tmpltimg2| awk -F" " '{print $5}')
if [ ${imgsize:(-1)} == G ] 
then
  imgsize=${imgsize%G} #strip out the G 
  imgsize=${imgsize%.*} #...and any decimal part
  let imgsize=imgsize+1 # add 1 to compensate for decimal part
  volsizetmp=${volsize%G}
  if [ $volsizetmp -lt $imgsize ]
  then
    volsize=${imgsize}G  
  fi
fi

tgtfile=${tmpltfs}/vmi-root-${tmpltname}

create_from_file $tmpltfs $tmpltimg2 $tmpltname $volsize $cleanup

tgtfilename=$(echo $tmpltimg2 | awk -F"/" '{print $NF}') 
touch /$tmpltfs/template.properties
rollback_if_needed $tmpltfs $? "Failed to create template.properties file"
echo -n "" > /$tmpltfs/template.properties

today=$(date '+%m_%d_%Y')
echo "filename=$tmpltname" > /$tmpltfs/template.properties
echo "snapshot.name=$today" >> /$tmpltfs/template.properties
echo "description=$descr" >> /$tmpltfs/template.properties
echo "name=$tmpltname" >> /$tmpltfs/template.properties
echo "checksum=$cksum" >> /$tmpltfs/template.properties
echo "hvm=$hvm" >> /$tmpltfs/template.properties
echo "volume.size=$volsize" >> /$tmpltfs/template.properties


if [ "$cleanup" == "true" ]
then
  rm -f $tmpltimg
fi

exit 0
