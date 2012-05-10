#!/bin/sh

# For older versions of Solaris, we need to use id from /usr/xpg4/bin.
IDCMD=id
if [ -x "/usr/xpg4/bin/id" ] ; then
    IDCMD=/usr/xpg4/bin/id
fi

uid=`$IDCMD -u`

if [ $uid != 0 ] ; then
    echo "$0: Needs to be run as superuser" >&2
    exit 1
fi

BINDIR=`dirname $0`
cd $BINDIR

FILELIST="SunOS/x86/fastsu SunOS/sparc/fastsu Linux/*/nicstat*"

for i in $FILELIST
do
    chown root $i
    chmod 7655 $i
done
