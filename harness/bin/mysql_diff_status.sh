#!/bin/sh

if [ $# -ne 3 ]
then
   echo "Usage: $0 logfile1 logfile2 logfile" >&2
   exit 1
fi
logfile1=$1
logfile2=$2
logfile=$3

# For this trick, we need nawk or gawk.
# Plain old awk usually does not work.
# But we still fall back to it if
# everything else falls apart.
AWK=/usr/bin/awk
if [ -x "/usr/bin/nawk" ] ; then
    AWK=/usr/bin/nawk
elif [ -x "/usr/bin/gawk" ] ; then
        AWK=/usr/bin/gawk
fi

$AWK '
{
   if ($1 in counts && $2-counts[$1] > 0 ) {
       printf("%-30.30s %10d\n", $1, $2 - counts[$1]);
   }
   counts[$1] = $2;
}
' $logfile1 $logfile2 > $logfile
