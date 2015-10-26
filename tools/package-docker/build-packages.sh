#! /bin/bash
set -e

OUT_DIR="debian/out"

VERSION=""
DIST=""

while [ -n "$1" ] ; do
    case $1 in
    -h|--help)
        echo "$0 [arguments]"
        echo "Arguments:"
        echo "  --version VERSION    Override version"
        echo "  --dist DIST          Override distribution (implies --version)"
        echo "  --help               Display this message"
        exit 0
        ;;
    --version)
        if [ -z $2 ] ; then echo "Error: Missing argument after $1"; exit 2 ; fi
        VERSION=$2
        shift
        ;;
    --dist)
        if [ -z $2 ] ; then echo "Error: Missing argument after $1"; exit 2 ; fi
        DIST=$2
        shift
        ;;
    -d|--debug)
        set -x
        ;;
    -*)
        echo "unknown option $1.  use --help for usage."
        exit 3
        ;;
    *)
        # End of options.  Positional variables starting.
        break
        ;;
    esac
    shift
done


REL_VERSION_STR="Release revision ${VERSION}"

if [ -n "${DIST}" ]; then
    echo "Overriding distribution ${DIST}"
    if [ -z "${VERSION}" ] ; then
        echo "You must specify --version if you specify --dist"
        exit 3
    fi
    dch --force-distribution --distribution "${DIST}" -b -v "${VERSION}" "${REL_VERSION_STR}"
fi

if [ -n "${VERSION}" ]; then
    echo "Overriding version with ${VERSION}"
    dch --release "${REL_VERSION_STR}"
fi

# Make sure the user running the process owns the files (problem with volume mounts in docker build).
find . -user 1000 -group 1000 | xargs chown --no-dereference root:root

dpkg-buildpackage

# Copy build files from parent folder to debian/out folder
mkdir -p "${OUT_DIR}"
find /build -maxdepth 1 -type f | xargs -I file cp file "${OUT_DIR}"

echo "Done packaging.  Files are in ${OUT_DIR}"