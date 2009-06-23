#!/bin/sh
#
# master.sh Script to launch the master for the Faban driver sample in the
# distributed mode. If launched alone without the registry and agents, it
# also assumes single process mode.
#
# Please also check registry.sh and agent.sh for launching in distributed
# mode.
#

BINDIR=`dirname $0`
. ${BINDIR}/setenv.sh

$JAVA_HOME/bin/java -XX:+DisableExplicitGC \
    -Djava.security.policy=security/driver.policy \
    -Djava.util.logging.config.file=logging.properties \
    -Dbenchmark.config=run.xml com.sun.faban.driver.core.MasterImpl
