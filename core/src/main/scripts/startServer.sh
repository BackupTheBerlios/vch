#!/bin/sh

VCH_HOME=`dirname $0`
cd $VCH_HOME

java -Xmx128m -Djava.util.logging.config.file=logging.properties -jar VodcatcherHelper.jar