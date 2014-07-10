#!/bin/bash

# update all OS packages
yum update
yum install aide

# create linux user for FLAREclient
echo ""
echo "Creating new user flareClient"
id -u flareClient &>/dev/null  || (useradd flareClient; passwd flareClient)

# install FLAREclient
echo ""
echo "Installing FLAREclient..."
cp -r ../FLAREclient* /home/flareClient/
chown -R flareClient /home/flareClient/*

# update crontab to run aide
echo ""
echo "Configuring & initializing aide..."
crontab -l > /tmp/mycron;
if [ "$(grep aide /tmp/mycron)" == '' ]; then
    /usr/sbin/aide --init
    ln -s /var/lib/aide/aide.db.new.gz /var/lib/aide/aide.db.gz
    echo "05 4 * * * root /usr/sbin/aide --check" >> /tmp/mycron
    crontab /tmp/mycron
fi
rm -f /tmp/mycron

# configure audit system
if [ "$(grep FLAREhub /etc/audit/audit.rules)" == '' ]; then
    echo ""
    echo "Configuring audit system..."
    echo "# These four lines have been added by FLAREhub install" >> /etc/audit/audit.rules
    echo "-w /sbin/insmod -p x -k modules" >> /etc/audit/audit.rules
    echo "-w /sbin/rmmod -p x -k modules" >> /etc/audit/audit.rules
    echo "-w /sbin/modprobe -p x -k modules" >> /etc/audit/audit.rules
    echo "-a always,exit -f arch=b64 -S init_module -S delete_module -k modules" >> /etc/audit/audit.rules
fi

# adjust permissions on boot.log
echo ""
echo "Adjusting permissions on boot.log"
chmod 0600 /var/log/boot.log

# override ctrl-alt-delete
echo ""
echo "Overriding control-alt-delete..."
echo -e "start on control-alt-delete\n\nexec /usr/bin/logger -p security.info \"Control-Alt-Delete pressed\"" > /etc/init/control-alt-delete.override

# add "monitored" email address to /etc/aliases
#echo ""
#echo "Adding 'monitored' email address to /etc/aliases..."
#echo "root: monitoredEmail@example.com" >> /etc/aliases
#newaliases

echo "Done!"
