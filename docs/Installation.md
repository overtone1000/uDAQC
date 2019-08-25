# Semi-automated Installation

To configure a Linux computer such as a Raspberry Pi as a uDAQC Center, start by navigating to a location in your file system where the software and database will reside. For example, if you have a large external hard drive mounted for your Raspberry Pi, this is an excellent location for the database.

If you have a different version of the software installed in this location, remove it. Warning - this will also remove the database as the data structure is not guaranteed to be compatible between versions! If your database is important, you may instead wish to export your data first.
```
rm uDAQC_Center
rm -d -r uDAQC_Center
```

Create a fresh directory and navigate to it.
```
mkdir uDAQC_Center
cd uDAQC_Center
```

Download the deployment zip from the GitHub repository into this directory. Be sure to get the version you'd like. For example, to download the v0.1.0 release, use the following URL:
`wget https://github.com/overtone1000/uDAQC/raw/v0.1.0/src/Center/uDAQC_Center/deploy/deploy.zip`

If you'd like the deployment zip from the master branch, just put the branch name in place of the release version name in the URL:
`wget https://github.com/overtone1000/uDAQC/raw/master/src/Center/uDAQC_Center/deploy/deploy.zip`

Unzip the contents of the deployment zip, remove the zip, and run the installation bash script.
```
unzip deploy.zip
rm deploy.zip
bash install.bash
```

The bash script performs the following operations:
1. Checks your Java version and offers to install using apt if java is not found.
2. Creates and starts a systemd service that executes the runnable jar.
