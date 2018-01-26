#!/bin/sh
# -----------------------------------------------------------------------------
# Stop script for the CATALINA Server
#
# $Id: shutdown.sh,v 1.2 2006/08/31 21:03:33 akara Exp $
# -----------------------------------------------------------------------------

if [ -z "$JAVA_HOME" ] ; then
    JAVA_HOME=/usr/java
    export JAVA_HOME
fi

if [ ! -x "${JAVA_HOME}/bin/java" ] ; then
    echo "Could not find java. Please set JAVA_HOME correctly." >&2
    exit 1
fi

JAVA_VER_STRING=`${JAVA_HOME}/bin/java -version 2>&1`

JAVA_VERSION=`echo $JAVA_VER_STRING | \
               awk '{ print substr($3, 2, length($3) - 2)}'`

MAJORVER=`echo $JAVA_VERSION | \
               awk '{ split($1, a, "."); print(a[1])}'`

export MAJORVER

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ] ; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done
 
PRGDIR=`dirname "$PRG"`
EXECUTABLE=catalina.sh

# Check that target executable exists
if [ ! -x "$PRGDIR"/"$EXECUTABLE" ]; then
  echo "Cannot find $PRGDIR/$EXECUTABLE"
  echo "This file is needed to run this program"
  exit 1
fi

exec "$PRGDIR"/"$EXECUTABLE" stop "$@"
