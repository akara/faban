#!/bin/sh
# -----------------------------------------------------------------------------
# Start Script for the CATALINA Server
#
# $Id: startup.sh,v 1.1 2006/06/29 18:51:52 akara Exp $
# -----------------------------------------------------------------------------

# Allow JAVA_HOME env setting before starting...
if [ -z "$JAVA_HOME" ] ; then
    JAVA_HOME=/usr/dist/share/java,v1.5.0/5.x-sun4
    export JAVA_HOME
fi

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
if [ ! -d faban ] ; then
    mkdir faban
    cd faban
    $JAVA_HOME/bin/jar xf ../faban.war
fi

cd "$PRGDIR"/../logs

echo "Please point your browser to http://$HOST:9980/"
exec "$PRGDIR"/"$EXECUTABLE" start "$@"
