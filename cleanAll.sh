#!/bin/bash

./gradlew clean 
./gradlew api:clean
./gradlew catalogue:clean 
./gradlew cli:clean 
./gradlew common:clean 
./gradlew core-impl:clean 
./gradlew core-int:clean 
./gradlew dashboard:clean 
./gradlew exception:clean 
./gradlew main:clean 
./gradlew monitoring:clean 
./gradlew plugin:clean 
./gradlew registration:clean 
./gradlew repository:clean 
./gradlew security:clean 
./gradlew tosca-parser:clean 
./gradlew vim-drivers:clean 
./gradlew vim-impl:clean 
./gradlew vim-int:clean 
./gradlew vnfm-impl:clean 
./gradlew vnfm-int:clean 
./gradlew cleanEclipse

 
rm -rf api/bin
rm -rf catalogue/bin 
rm -rf cli/bin
rm -rf common/bin 
rm -rf core-impl/bin 
rm -rf core-int/bin 
rm -rf dashboard/bin 
rm -rf exception/bin 
rm -rf main/bin 
rm -rf monitoring/bin 
rm -rf plugin/bin 
rm -rf registration/bin 
rm -rf repository/bin 
rm -rf security/bin 
rm -rf tosca-parser/bin 
rm -rf vim-drivers/bin 
rm -rf vim-impl/bin 
rm -rf vim-int/bin 
rm -rf vnfm-impl/bin 
rm -rf vnfm-int/bin 

