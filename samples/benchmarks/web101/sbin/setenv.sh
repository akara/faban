#!/bin/sh
#
# setenv.sh Script to be execed from all launch scripts to setup the
# execution environment. 
#


# Check and set all variables...
BINDIR=`dirname $0`

if [ -n "$BINDIR" ] ; then
    BENCH_HOME=`cd $BINDIR/.. > /dev/null 2>&1 && pwd`
    export BENCH_HOME
fi

if [ -z "$JAVA_HOME" ] ; then
    JAVA_HOME=/usr/dist/share/java,v1.5.0/5.x-sun4
    export JAVA_HOME;
fi

# Guess mode for FABAN_HOME
GUESS=""
if [ -z "${FABAN_HOME}" ] ; then
    # This would be the location if the sample
    # were run from the Faban distribution.
    FABAN_HOME=../../..
    FABAN_HOME=`cd ${FABAN_HOME} > /dev/null 2>&1 && pwd`
    export FABAN_HOME
    GUESS=true;
fi

if [ ! -f "${FABAN_HOME}/lib/fabandriver.jar" ] ; then
    if [ -n "${GUESS}" ] ; then
      echo "FABAN_HOME variable not set, exiting."
    else
      echo "Sorry, did not find Faban at FABAN_HOME=${FABAN_HOME}, exiting."
    fi
    exit 1
fi

# Set the necessary classpaths
CLASSPATH=${FABAN_HOME}/lib/fabancommon.jar:${FABAN_HOME}/lib/fabandriver.jar

for i in ${BENCH_HOME}/lib/*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done

CLASSPATH=${CLASSPATH}:${BENCH_HOME}/build/classes

for i in ${SAMPLE_HOME}/lib/*.jar
do
    CLASSPATH=${CLASSPATH}:$i
done

export CLASSPATH

cd ../config
