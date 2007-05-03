#!/bin/sh
#
# agent.sh Script to launch the Faban driver agent in distributed mode.
# You'll need to launch the registry before the agent and the master
# after launching the agent. Please check registry.sh and master.sh
#

BINDIR=`dirname $0`
. ${BINDIR}/setenv.sh

$JAVA_HOME/bin/java -Djava.security.policy=security/driver.policy \
    -Djava.util.logging.config.file=logging.properties \
    -Dbenchmark.config=run.xml com.sun.faban.driver.core.AgentImpl \
    MyDriver 1 sr1-umpk-18
