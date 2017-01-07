#!/bin/bash
cd /home/pi/rover-api
. ./flask/bin/activate
export FLASK_APP=app.py
pulseaudio --start
sleep 2
echo "connect 08:DF:1F:98:45:C4" | bluetoothctl
sleep 2
./flask/bin/flask  run --host=0.0.0.0
