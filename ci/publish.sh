#!/bin/bash
usage() {
    echo "Usage: $0 -d <directory_with_packages> -a <artifcatory_repo_url> -u <user name> -p <password> -c <component> -i <distribution>" > /dev/stderr
    exit 1
}

while getopts "d:a:u:p:c:i:" o
do
    case "$o" in
        d)
            directory=$OPTARG
            ;;
        a)
            artifactory_repo_url=$(sed 's/\/$//' <<< $OPTARG) # remove trailing slash
            ;;
        u)
            user=$OPTARG
            ;;
        p)
            password=$OPTARG
            ;;
        c)
            component=$OPTARG
            ;;
        i)
            distribution=$OPTARG
            ;;
        *)
            usage
    esac
done

if [ -z "$directory" ] || [ -z "$artifactory_repo_url" ] || [ -z "$user" ] || [ -z "$password" ] || [ -z "$component" ] || [ -z "$distribution" ]
then
    usage
fi

publish() {
    filename=$1
    basename=$(basename $1)
    package_name=$(cut -d_ -f1 <<< $basename)
    arch=$(cut -d_ -f3 <<< $basename | cut -d. -f1)
    subdir=''

    if [[ "x$package_name" = "xlib*" ]]
    then
        subdir="${package_name:0:4}"
    else
        subdir="${package_name:0:1}"

    fi

    curl --fail --user "$user:$password" -X PUT \
    "${artifactory_repo_url}/pool/${component}/${subdir}/${package_name}/${basename};deb.distribution=${distribution};deb.component=${component};deb.architecture=${arch}" \
    -T $filename
    if [ $? -ne 0 ]
    then
        echo "Failed to publish file $filename" > /dev/stderr
        exit 1
    fi
}

cd $directory
for file in $(ls -1 | grep \.deb)
do
    echo "publishing $file"
    publish $file
done
