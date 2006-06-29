#!/bin/sh

# -----------------------------------------------------------------------------
#
# Script for starting Catalina using the Launcher
#
# -----------------------------------------------------------------------------

# Resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`
if [ -r "$PRGDIR"/setenv.sh ]; then
  . "$PRGDIR"/setenv.sh
fi

# Execute the Launcher using the "catalina" target
exec "$JAVA_HOME"/bin/java -classpath "$PRGDIR" LauncherBootstrap -launchfile catalina.xml -verbose catalina "$@" start
