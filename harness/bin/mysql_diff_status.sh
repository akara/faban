#!/bin/sh

if [ $# -ne 3 ]
then
   echo "Usage: $0 logfile1 logfile2 outfile" >&2
   exit 1
fi
log1=$1
log2=$2
outfile=$3
nawk '
{
   if ($1 in counts && $2-counts[$1] > 0 ) {
       printf("%-30.30s %10d\n", $1, $2 - counts[$1]);
   }
   counts[$1] = $2;
}
' $log1 $log2 > $outfile
