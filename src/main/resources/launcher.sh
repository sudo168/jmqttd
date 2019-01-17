#!/bin/sh
SHELL_PROG=./launcher.sh
DIR="${BASH_SOURCE-$0}"
DIR=`dirname ${BASH_SOURCE-$0}`
ROOT_HOME=$DIR;export ROOT_HOME
RESOURCE_HOME=$DIR/resources/;export RESOURCE_HOME
DATA_HOME=$DIR/data/;export DATA_HOME
MAPPER_HOME=$DIR/mapper/;export MAPPER_HOME
LIB_HOME=$DIR/lib/*;export LIB_HOME
CLASSPATH=$ROOT_HOME:$RESOURCE_HOME:$DATA_HOME:$LIB_HOME:$MAPPER_HOME;export CLASSPATH
LC_ALL=zh_CN.UTF-8;export LC_ALL

#ulimit -n 102297

#process name, need to change
MAINPROG=net.ewant.jmqttd.server.ServerBootstrap

start() {
        echo "[`date`] Begin starting $MAINPROG ... "
        nohup java -Xms64m -Xmx512m -Xmn128m -Xss228k -XX:+UseParNewGC -XX:+PrintGCDetails -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCTimeStamps -Xloggc:gc.log -classpath $CLASSPATH $MAINPROG  >console.log 2>&1 &
        if [ $? -eq 0 ]
        then
			echo "[`date`] Startup $MAINPROG success."
			return 0
        else
			echo "[`date`] Startup $MAINPROG fail."
			return  0
        fi
}

debug() {
        echo "[`date`] Begin starting $MAINPROG... "
        java -Xdebug -Xrunjdwp:transport=dt_socket,address=6777,server=y,suspend=y -Xms100m -Xmx200m -Xmn256m -Xss256k  -classpath $CLASSPATH $MAINPROG &
        if [ $? -eq 0 ]
        then
			echo "[`date`] Startup $MAINPROG success."
			return 0
        else
			echo "[`date`] Startup $MAINPROG fail."
			return  0
        fi
}

stop() {
    echo "[`date`] Begin stop $MAINPROG... "
    PROGID=`ps -ef|grep "$MAINPROG"|grep -v "grep"|sed -n '1p'|awk '{print $2}'`
	if [ -z "$PROGID" ]
	then
		echo "[`date`] Stop $MAINPROG fail, service is not exist."
		return  0
	fi
	
    kill -9 $PROGID
    if [ $? -eq 0 ]
    then
		echo "[`date`] Stop $MAINPROG success."
		return 0
    else
		echo "[`date`] Stop $MAINPROG fail."
		return  0
    fi
}


case "$1" in
start)
  start
  exit $?
  ;;
stop)
  stop
  exit $?
  ;;
restart)
  stop
  start
  exit $?
  ;;
debug)
  debug
  exit $?
  ;;
*)
  echo "[`date`] Usage: $SHELL_PROG {start|debug|stop|restart}"
  exit 1
  ;;
esac
