#!/bin/bash          
echo "Checking java version."

if type -p java; then
    echo "Java found."
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