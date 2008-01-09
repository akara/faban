
start_date=$1
end_date=$2
logfile=$3
tmpfile=${logfile}.trunc

oldifs="$IFS"
IFS=","
set $start_date
smon=$1
sday=$2
stime=$3

echo Start $smon $sday $stime

set $end_date
emon=$1
eday=$2
etime=$3
echo End $emon $eday $etime
IFS="$oldifs"

#echo "${start}\n${end}" | cat - ${location}/apache.error_log.${host} > ${location}/apache.error_log.${host}.new

cat ${logfile}  | nawk -v smon="$smon" -v sday="$sday" -v stime="$stime" \
 -v emon="$emon" -v eday="$eday" -v etime="$etime" '{                
                split(stime,startArr,":")
                split(etime,endArr,":")
                xmon=$2; xday=$3; split($4,arr,":")
               
                if (smon == xmon && sday < xday) {
                    if (eday > xday) print $0
                } else if (smon == xmon && eday > xday) {
                            print $0                                                  
                } else if (smon == xmon && sday == xday) {                         
                         if (startArr[1] < arr[1] && endArr[1] > arr[1]) {
                                print $0
                         } else if (startArr[1] == arr[1] && endArr[1] > arr[1]) { 
                               if (startArr[2] <= arr[2]) {
                                  print $0
                               } 
                         } else if (startArr[1] == arr[1] && endArr[1] == arr[1]) {
                               if (startArr[2] <= arr[2] && endArr[2] >= arr[2]) {
                                  print $0
                               }

                         } else if (startArr[1] < arr[1] && endArr[1] == arr[1]) {
                               if (endArr[2] >= arr[2]) print $0
                         } 
                     
                
                 }
                if (emon ==xmon && eday == xday) {                     
                     if (endArr[1] > arr[1]) print $0
                     if (endArr[1] == arr[1] && endArr[2] > arr[2]) print $0  
                }
}' > $tmpfile

# Now move tmpfile back to logfile
mv $tmpfile $logfile
exit 0
