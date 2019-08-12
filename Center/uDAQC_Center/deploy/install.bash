#!/bin/bash          
echo "Checking java version."

if type -p java; then
    echo "Java found."
    _java=java
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" > "1.5" ]]; then
        echo version is more than 1.5
    else         
        echo version is less than 1.5
    fi
else
    echo "Java was not found on the PATH. uDAQC Center is a Java application."
	read -p "Install default JDK with apt?" -n 1 -r
	echo
	if [[ $REPLY =~ ^[Yy]$ ]]
	then
		sudo apt update
		sudo apt install default-jdk
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

sudo cp uDAQC_Center.service /etc/systemd/system/uDAQC_Center.service
sudo systemctl stop uDAQC_Center
sudo systemctl daemon-reload
sudo systemctl start uDAQC_Center
sudo systemctl enable uDAQC_Center