# Semi-automated uDAQC Installation

To configure a Linux computer such as a Raspberry Pi as a uDAQC Center, start by navigating to a location in your file system where the software will reside.

If you have a different version of the software installed in this location, remove it.
```
rm -d -r uDAQC_Center
```

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

Unzip the contents of the deployment zip and then delete the zip file.
```
unzip deploy.zip
rm deploy.zip
```

## Java Installation
Check that the java installation is adequate with the bash:
```
sudo bash check_java.bash
```

If an adequate java version is not found on the path, the script will attempt an installation.

## TimescaleDB Installation
Install PostgreSQL v11 per instructions on their website.
https://www.postgresql.org/docs/11/tutorial-install.html
* For Debian, this is available in apt. `sudo apt-get install postgresql-11`

Install TimescaleDB (preferably via apt) per instructions on their website.
https://docs.timescale.com/latest/getting-started/installation

Run the database configuration bash:
```
sudo bash configure_database.bash
```

First this will create a user named udaqc with the password udaqc. 

Unless you enable remote connections by changing `postgresql.conf`, this user will only be able to connect via localhost, and no external network connections will be accepted. If you do enable network access to this database, be sure to exclude the udaqc user from this access in `pg_hba.conf` to avoid a network security vulnerability. For example, the following line (if placed before any other lines specifying the "host" type) would reject connections from the udaqc user:
```
host   all  udaqc   all reject
```

Next, this script will prompt you for a dedicated directory to contain the databaase. Be sure this directory is in a partition of the file system that is large enough. For example, configure an external drive to be automatically mounted and choose a location on this external drive.

Finally, the script will switch to the PostgreSQL superuser `postgres` and it will:
* Create a dedicated database role corresponding to the previously created udaqc user
* Create a dedicated udaqc tablespace in the directory defined above
* Create a udaqc database in that tablespace

Check that the `udaqc` user can connect to the `udaqc_database` database:
```
sudo -u udaqc psql udaqc_database
````

## Service Configuration

Run the service configuration bash:
```
sudo bash configure_service.bash
```

The bash script creates and starts a systemd service that executes the runnable jar.

Once the service is running, you can observe the output from the program using journalctl. For example, to see the last 100 lines of output, use the following command:
```
journalctl -u uDAQC_Center.service | tail -100
```

To start or stop the service:
```
systemctl start uDAQC_Center.service
systemctl stop uDAQC_Center.service
```

To log into the web UI for the first time on your LAN, open a browser and access the following URL, replacing your device's IP:
```
https://DEVICEIP:49154/
```

The Center creates a key pair for TLS on its first execution. Your browser will warn that a self-signed certificate is being presented. Accept that certificate to continue.

The web UI credentials initialize with the login "admin" and the password "admin" and should be changed immediately.

To log into the web UI from outside your LAN, you'll need to configure your router to forward WAN traffic to your center.
