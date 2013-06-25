#!/bin/bash

mvn
rm /srv/minecraft/plugins/FlooNetwork*.jar
cp target/FlooNetwork*.jar /srv/minecraft/plugins
