#!/bin/sh

# the arguments of this script are the compiler name and flags

# try to solve a chicken-and-egg problem on SunOS
# ucb's test program does not handle -L like the other test programs
# let's try to find another implementation
if test -x /bin/test; then
    TEST=/bin/test;
else
    if test -x /usr/bin/test; then
        TEST=/usr/bin/test;
    else
        # cross your fingers that it's not like ucb test
        TEST=test;
    fi
fi

compiler=`echo $* | sed -e 's/-static//g' -e 's/-Bstatic//g'`
if $TEST `basename $1` = "gcc" && ($compiler -v) >/dev/null 2>&1 ; then
# Cygwin writes more than one line with "version" in it
    gccversion=`$compiler -v 2>&1 | sed -e "/version/!d" | tail -n 1`
else
    gccversion="$1"
fi

libcversion=""
if ($* hello.c -o hello) >/dev/null 2>&1; then
  ldd_output=`(ldd hello) 2>&1`
  libcversion=`echo $ldd_output | sed -e 's/.*static.*/static/' \
				      -e 's/.*not a dynamic.*/static/'`
  if $TEST "$libcversion" = "static" ; then
    if ($compiler hello.c -o hello) >/dev/null 2>&1; then
      if (ldd hello) >/dev/null 2>/dev/null; then
        libcversion=`(ldd hello) 2>&1`
        libcversion=`echo $libcversion | sed -e '/libc/!d'\
			-e 's/^[ 	]*//' \
			-e 's/.*=>[ 	][ 	]*\([^ 	]*\).*/\1/'`
	# remember the current directory
      	current=`pwd`
      	while $TEST -L "$libcversion" && ! $TEST "$libcversion" = "" ; do
      	  libcitself=`basename $libcversion`
      	  libpath=`echo $libcversion | sed -e "s/$libcitself$//"`
      	  if $TEST -d "$libpath" ; then
      	    cd $libpath
      	  fi
      	  if ls $libcitself >/dev/null 2>/dev/null ; then
      	    libcversion=`ls -l $libcitself | \
			   sed -e 's/.*->[ 	][ 	]*\(.*\)$/\1/'`
      	  else
      	    # something must have gone wrong, let's bail out
      	    libcversion=""
      	  fi
      	done
      	# return to the current directory
      	cd $current
      fi
    fi
  else
    libcversion=""
  fi
fi

rm -f sysinfo.crm sysinfoc.c hello

# this bombs out on Ultrix which expect "cut -d"

compsystem=`uname -a | cut -b 1-78`
compdate=`date|cut -b1-55`

# let's hope that ctrl-c is not part of any string here
# this also will barf later if " is in any of the strings

for i in sysinfo.c sysinfoc.c ; do
 sed -e "s%CCVERSION%$gccversion" -e "s%LIBCVERSION%$libcversion"\
     -e "s%SYSTEM%$compsystem" -e "s%DATE%$compdate"\
   ${i}.template > $i
done
