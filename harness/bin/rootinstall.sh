#!/bin/sh

UID=`id -u`

if [ $UID != 0 ] ; then
    echo "$0: Needs to be run as superuser" >&2
    exit 1
fi

BINDIR=`dirname $0`
cd $BINDIR

FILELIST="SunOS/x86/fastsu SunOS/sparc/fastsu Linux/i386/nicstat"

for i in $FILELIST
do
    chown root $i
    chmod 7655 $i
done
