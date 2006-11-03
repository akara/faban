#!/bin/sh
# -----------------------------------------------------------------------------
# Start Script for the CATALINA Server
#
# $Id: startup.sh,v 1.5 2006/11/03 09:43:43 akara Exp $
# -----------------------------------------------------------------------------

# Allow JAVA_HOME env setting before starting...
if [ -z "$JAVA_HOME" ] ; then
    JAVA_HOME=/usr/java
    export JAVA_HOME
fi

if [ ! -x "${JAVA_HOME}/bin/java" ] ; then
    echo "Could not find java. Please set JAVA_HOME correctly." >&2
    exit 1
fi

JAVA_VERSION=`${JAVA_HOME}/bin/java -version 2>&1 | \
              awk '/java version/{ print substr($3, 2, length($3) - 2)}'`

case $JAVA_VERSION in
    1.5*);;
    *) echo "Java version is ${JAVA_VERSION}. Faban needs 1.5 or later." >&2
       echo "Please install the appropriate JDK and set JAVA_HOME accordingly." >&2
       exit 1;;
esac

JAVA_OPTS="-Xms64m -Xmx1024m -Djava.awt.headless=true"
export JAVA_OPTS

# resolve links - $0 may be a softlink
PRGDIR=`dirname $0`

if [ -n "$PRGDIR" ]
then
   PRGDIR=`cd $PRGDIR > /dev/null 2>&1 && pwd`
fi

EXECUTABLE=catalina.sh

# Check that target executable exists
if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
  echo "Cannot find $PRGDIR/$EXECUTABLE"
  echo "This file is needed to run this program"
  exit 1
fi

# Added by Ramesh and Akara

HOST=`hostname`

echo "Starting Faban Server"

# Since Faban uses root context, make sure it is unjarred before startup
cd "$PRGDIR"/../webapps

# Avoid version conflicts - re-unjar faban.war before each start.
rm -rf faban xanadu
mkdir faban
cd faban
$JAVA_HOME/bin/jar xf ../faban.war

cd "$PRGDIR"/../logs

echo "Please point your browser to http://$HOST:9980/"
exec "$PRGDIR"/"$EXECUTABLE" start "$@"
