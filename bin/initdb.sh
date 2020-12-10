#!/bin/bash
mysql -P 3306 -h localhost -u root -proot davinci0.3 < $FASTBI_HOME/bin/davinci.sql
