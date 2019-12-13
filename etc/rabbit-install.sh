#!/bin/bash

#sudo apt-get install rabbitmq-server
sudo rabbitmqctl add_user admin openbaton
sudo rabbitmqctl set_user_tags admin administrator
sudo rabbitmqctl set_permissions -p / admin ".*" ".*" ".*"
sudo service rabbitmq-server restart
