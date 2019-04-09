#!/bin/bash
cd /home/max/Android/Sdk/platform-tools/
./adb kill-server
sudo ./adb devices
echo "Done!"
