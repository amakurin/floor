#!/bin/sh

PATH=/bin:/usr/bin:/sbin:/usr/sbin
DAEMON=depl/run.sh
FLOOR_HOME=/home/user/cljr/floor16
PIDFILE="$FLOOR_HOME/tmp/pids/floor16.pid"
USER="user"

export PATH="${PATH:+$PATH:}/usr/sbin:/sbin"

if [ ! -d $PIDDIR ]; then
  mkdir $PIDDIR
  chown $USER.$USER $PIDDIR
fi

case "$1" in
  start)
    start-stop-daemon --start --quiet --oknodo --background --pidfile $PIDFILE --exec $DAEMON --chuid $USER --chdir $FLOOR_HOME
    sleep 5
    $0 status
  ;;

  stop)
    if start-stop-daemon --stop --quiet --pidfile $PIDFILE; then
      exit 0
    else
      exit 1
    fi
  ;;
esac
exit 0
