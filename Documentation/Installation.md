# TimescaleDB Installation
Install PostgreSQL (preferably via apt) per instructions on their website.
Install TimescaleDB (preferably via apt) per instructions on their website.

Create the uDAQC user:

```
sudo adduser udaqc
```

Make the password udaqc

To establish an alternative storage location:
```
sudo mkdir /alt/postgres/datadirectory
sudo chown postgres /alt/postgres/datadirectory
sudo chgrp postgres /alt/postgres/datadirectory
sudo chmod 700 /alt/postgres/datadirectory
```

Start an sql session:
`sudo -u postgres psql`

Create a dedicated uDAQC database user:
`create user udaqc with password 'udaqc';`

Create a tablespace and database with the following sql commands
```
create tablespace uDAQC_tablespace location '/alt/postgres/datadirectory';
create database uDAQC_database with tablespace = uDAQC_tablespace;
grant all privileges on database uDAQC_database to udaqc;

```

Check that you can enter this database with
`sudo -u udaqc psql udaqc_database`

The uDAQC Center should now be able to access this database.



# Semi-automated uDAQC Installation

To configure a Linux computer such as a Raspberry Pi as a uDAQC Center, start by navigating to a location in your file system where the software and database will reside. For example, if you have a large external hard drive mounted for your Raspberry Pi, this is an excellent location for the database.

If you have a different version of the software installed in this location, remove it. Warning - this will also remove the database as the data structure is not guaranteed to be compatible between versions! If your database is important, you may instead wish to export your data first.
`rm -d -r uDAQC_Center`

Create a fresh directory and navigate to it.
```
mkdir uDAQC_Center
cd uDAQC_Center
```

Download the deployment zip from the GitHub repository into this directory. Be sure to get the version you'd like. For example, to download the v0.1.0 release, use the following URL:
```
wget https://github.com/overtone1000/uDAQC/raw/v0.1.0/Components/Center/uDAQC_Center/deploy/deploy.zip
```

If you'd like the deployment zip from the master branch, just put the branch name in place of the release version name in the URL:
```
wget https://github.com/overtone1000/uDAQC/raw/master/Components/Center/uDAQC_Center/deploy/deploy.zip
```

Unzip the contents of the deployment zip, remove the zip, and run the configuration script.
```
unzip deploy.zip
rm deploy.zip
bash configure.bash
```

The bash script performs the following operations:
1. Checks your Java version and offers to install using apt if java is not found.
2. Creates and starts a systemd service that executes the runnable jar.

Once the service is running, you can observe the output from the program using journalctl. For example, to see the last 100 lines of output, use the following command:
```
journalctl -u uDAQC_Center.service | tail -100
```

To start or stop the service:
```
systemctl start uDAQC_Center.service
systemctl stop uDAQC_Center.service
```

To log into the web UI for the first time on your LAN, open a browser and access the following URL, replacing your devices IP:
```
https://DEVICEIP:49154/
```

The Center creates a key pair for TLS on its first execution. Your browser will warn that a self-signed certificate is being presented. Accept that certificate to continue.
The web UI credentials initialize with the login "admin" and the password "admin" and should be changed immediately.

To log into the web UI from outside your LAN, you'll need to configure your router to forward WAN traffic to your center.
