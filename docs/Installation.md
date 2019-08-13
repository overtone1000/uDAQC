# Automated Installation

The following commands should be wrapped in a bash script.

rm uDAQC_Center
rm -d -r uDAQC_Center
mkdir uDAQC_Center
cd uDAQC_Center
wget https://github.com/overtone1000/uDAQC/raw/v0.1.0/src/Center/uDAQC_Center/deploy/deploy.zip
unzip deploy.zip
bash install.bash
