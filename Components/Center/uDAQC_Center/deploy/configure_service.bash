#!/bin/bash

echo "Modifying service file for this system and file location."
current_user_line="$(grep "User=" "uDAQC_Center.service")"
new_user_line="User=$(whoami)"

current_exec_line="$(grep "ExecStart=" "uDAQC_Center.service")"

exec_path="$(readlink -f uDAQC_Center/jar/uDAQC_Center.jar)"

escaped_exec_path='"'$exec_path'"'
#escaped_exec_path="$(systemd-escape --path "$exec_path")"
#escaped_exec_path=${escaped_exec_path//'\'/'\\'}
#escaped_exec_path=${escaped_exec_path//' '/'\x20'}
escaped_exec_path=${escaped_exec_path//'"'/'\\\x22'}

new_exec_line="ExecStart=/bin/bash -c \\\"java -jar "$escaped_exec_path"\\\""

echo "Changing to current user."
sed -i 's@'"$current_user_line"'@'"$new_user_line"'@g' uDAQC_Center.service
echo "Adding absolute file path."
sed -i 's@'"$current_exec_line"'@'"$new_exec_line"'@g' uDAQC_Center.service

echo "Stopping uDAQC_Center service."
sudo systemctl stop uDAQC_Center

echo "Copying service file to systemd directory."
sudo cp uDAQC_Center.service /etc/systemd/system/uDAQC_Center.service

echo "Reloading systemctl daemon."
sudo systemctl daemon-reload

echo "Starting and enabling uDAQC_Center service."
sudo systemctl start uDAQC_Center
sudo systemctl enable uDAQC_Center
