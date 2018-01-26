#!/bin/sh
# -----------------------------------------------------------------------------
# Start Script for the CATALINA Server
#
# $Id: startup.sh,v 1.8 2007/11/13 07:17:02 akara Exp $
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

export MAJORVER=""

JAVA_VER_STRING=`${JAVA_HOME}/bin/java -version 2>&1`

JAVA_VERSION=`echo $JAVA_VER_STRING | \
               awk '{ print substr($3, 2, length($3) - 2)}'`
if [[ "$JAVA_VERSION" =~ "\." ]] ; then
   MAJORVER=$JAVA_VERSION
   MINORVER="0"
else
   MAJORVER=`echo $JAVA_VERSION | \
     awk '{ split($1, a, "."); print(a[1])}'`
   MINORVER=`echo $JAVA_VERSION | \
     awk '{ split($1, a, "."); print(a[2])}'`
fi

if [ "$MAJORVER" -lt 1 ] || [ "$MAJORVER" -eq 1 ] && [ "$MINORVER" -lt 5 ]; then
  echo "Java version is ${JAVA_VERSION}. Faban needs 1.5 or later." >&2
  echo "Please install the appropriate JDK and set JAVA_HOME accordingly." >&2
  exit 1
fi
export MAJORVER MINORVER

# resolve links - $0 may be a softlink
PRGDIR=`dirname $0`

if [ -n "$PRGDIR" ]
then
   PRGDIR=`cd $PRGDIR > /dev/null 2>&1 && pwd`
fi

# The IBM JVM does not want the contents of the endorsed dir, others do.
unendorse() {
    cd "$PRGDIR"/../common/endorsed
    FILECOUNT=`ls | wc -l`
    if [ "$FILECOUNT" -gt 0 ] ; then
        cd ..
        rm -rf unendorsed
        mv endorsed unendorsed
        mkdir endorsed
    fi
}

endorse() {
    cd "$PRGDIR"/../common/endorsed
    FILECOUNT=`ls | wc -l`
    if [ "$FILECOUNT" -eq 0 ] ; then
        cd ..
        if [ -d unendorsed ] ; then
            rmdir endorsed
            mv unendorsed endorsed
        else
            echo "WARNING: Cannot find endorsed jars!" >&2
        fi
    fi
}

# up to JRE 9 contents of endorsed extension dir are needed
case $JAVA_VER_STRING in
    *IBM*) unendorse;;
    *)
        if [ "$MAJORVER" -lt 9 ]; then
            endorse
        else
            unendorse
        fi
        ;;
esac

JAVA_OPTS="-Xms64m -Xmx1024m -Djava.awt.headless=true"
export JAVA_OPTS

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
rm -rf faban fenxi xanadu xanadu.war
mkdir faban
cd faban
$JAVA_HOME/bin/jar xf ../faban.war

cd "$PRGDIR"/../logs

echo "Please point your browser to http://$HOST:9980/"
exec "$PRGDIR"/"$EXECUTABLE" start "$@"
