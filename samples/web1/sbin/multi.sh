#!/bin/sh
#
# multi.sh Script to launch the Faban driver sample in distributed mode on
# a single system.
#

BINDIR=`dirname $0`
. ${BINDIR}/setenv.sh

trap kill_all 2

kill_all() {
    if [ -n "${REGISTRY_PID}" ] ; then
        kill ${MASTER_PID} ${AGENT_PID} ${REGISTRY_PID}
    fi
}

$JAVA_HOME/bin/java -XX:+DisableExplicitGC \
    -Djava.security.policy=security/driver.policy \
    -Djava.util.logging.config.file=logging.properties \
    com.sun.faban.common.RegistryImpl &
REGISTRY_PID="$!"
sleep 2

$JAVA_HOME/bin/java -XX:+DisableExplicitGC \
    -Djava.security.policy=security/driver.policy \
    -Djava.util.logging.config.file=logging.properties \
    com.sun.faban.driver.core.AgentImpl MyDriver 1 localhost &
AGENT_PID="$!"

sleep 2

$JAVA_HOME/bin/java -XX:+DisableExplicitGC \
    -Djava.security.policy=security/driver.policy \
    -Djava.util.logging.config.file=logging.properties \
    -Dbenchmark.config=run.xml \
    com.sun.faban.driver.core.MasterImpl &
MASTER_PID="$!"


wait ${MASTER_PID}
kill ${AGENT_PID} ${REGISTRY_PID}
