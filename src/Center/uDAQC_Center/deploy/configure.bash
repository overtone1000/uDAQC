#!/bin/bash

echo "Checking java version."
if type -p java; then

    _java=java
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')

    echo version "$version"

    version_major=$(echo $version | cut -d'.' -f1)
    version_minor=$(echo $version | cut -d'.' -f2)

    target_major=1
    target_minor=8

    echo Target Java version is "$target_major"."$target_minor"

    if [[ "$version_major" > "$version_minor" ]]; then
        echo Adequate JRE found
    else
        echo JRE version "$version" is below the targeted version
    fi
else
    echo "Java was not found on the PATH. uDAQC Center is a Java application."
	read -p "Install default JRE with apt?" -n 1 -r
	echo
	if [[ $REPLY =~ ^[Yy]$ ]]
	then
		sudo apt update
		sudo apt install default-jre
		exec "$install.bash"

		this_script=$(readlink -f "$0")
		bash "$this_script"
		exit
	else
		echo
		echo "Please install Java manually and retry. Exiting."
		exit
	fi
fi

current_user_line="$(grep "User=" "uDAQC_Center.service")"
new_user_line="User=$(whoami)"

echo $new_user_line

current_exec_line="$(grep "ExecStart=" "uDAQC_Center.service")"
echo $current_exec_line

exec_path="$(readlink -f uDAQC_Center/jar/uDAQC_Center.jar)"
echo $exec_path

escaped_exec_path='"'$exec_path'"'
#escaped_exec_path="$(systemd-escape --path "$exec_path")"
#escaped_exec_path=${escaped_exec_path//'\'/'\\'}
#escaped_exec_path=${escaped_exec_path//' '/'\x20'}
escaped_exec_path=${escaped_exec_path//'"'/'\\\x22'}
echo $escaped_exec_path

new_exec_line="ExecStart=/bin/bash -c \\\"java -jar "$escaped_exec_path"\\\""
echo $new_exec_line

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
