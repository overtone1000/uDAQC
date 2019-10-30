#!/bin/bash

echo "Configuring PostreSQL/TimescaleDB database."

echo "Creating udaqc user."
sudo useradd udaqc
echo udaqc:udaqc | sudo chpasswd

echo "Choose a directory in the file system for the uDAQC database: "
read
database_dir="$REPLY"
echo "Installing directory at $database_dir"

sudo mkdir "$database_dir"
sudo chown "$database_dir"
sudo chgrp "$database_dir"
sudo chmod 700 "$database_dir"

sudo su postgres
