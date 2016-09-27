#!/usr/bin/env bash

#
# Copyright (c) 2016 Red Hat and/or its affiliates
#
#  All rights reserved. This program and the accompanying materials
#  are made available under the terms of the Eclipse Public License v1.0
#  which accompanies this distribution, and is available at
#  http://www.eclipse.org/legal/epl-v10.html
#
#  Contributors:
#     Red Hat - Initial API and implementation
#     Amit Kumar Mondal (admin@amitinside.com)
#

mvn "$@" -f target-platform/pom.xml clean install &&
mvn "$@" -f kura/manifest_pom.xml clean install -DskipTests &&

askForProfileInput() {
	echo "Which profile do you like to build? (-nn denotes no network management)"
	select yn in "default" "can" "intel-edison" "intel-edison-nn" "beaglebone" "beaglebone-nn" "raspberry-pi" "raspberry-pi-nn" "raspberry-pi-bplus" "raspberry-pi-bplus-nn" "raspberry-pi-2" "raspberry-pi-2-nn" "Win64-nn" "dev-env"; do
	    case $yn in
	        default ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web; break;;
			can ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P can; break;;
			intel-edison ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,intel-edison; break;;
			intel-edison-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,intel-edison-nn; break;;
			beaglebone ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,beaglebone; break;;
			beaglebone-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,beaglebone-nn; break;;
			raspberry-pi ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi; break;;
			raspberry-pi-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-nn; break;;
			raspberry-pi-bplus ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-bplus; break;;
			raspberry-pi-bplus-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-bplus-nn; break;;
	    	raspberry-pi-2 ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-2; break;;
			raspberry-pi-2-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-2-nn; break;;
	    	Win64-nn ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,raspberry-pi-2; break;;
	    	dev-env ) mvn "$@" -f kura/pom_pom.xml clean install -DskipTests -P web,dev-env; break;;
	    esac
	done
}

read -p "Do you want to build Kura distribution (y/n)? " answer
case ${answer:0:1} in
    y|Y )
        askForProfileInput
    ;;
    * )
        exit
    ;;
esac
