@echo off
set SHELL_PROG=./launcher.bat
set classpath=%classpath%;./lib/*;../common-lib/*;../jdbc-lib/*;./mapper/
set main=net.ewant.jmqttd.server.ServerBootstrap
title %main% %1

if "%1"=="start" (
  echo [%date%] Begin starting %main% ... 
  java -Xms64m -Xmx512m -Xmn128m -Xss228k -XX:+UseParNewGC -XX:+PrintGCDetails -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCTimeStamps -Xloggc:gc.log %main%
) else if "%1"=="stop" ( 
  echo [%date%] Begin stop %main% ... 
  setlocal enabledelayedexpansion
  for /f "usebackq delims=" %%i in (`jps -l^|findStr "%main%"`) do (
     set proinfo=%%i
     set pid=!proinfo:~0,5!
     TASKKILL /PID !pid!
  )
) else  (
  echo [%date%] Usage: %SHELL_PROG% {start^|stop}
)
