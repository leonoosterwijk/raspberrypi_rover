#!/bin/bash
pulseaudio --start
sleep 2
echo "connect 08:DF:1F:98:45:C4" | bluetoothctl

