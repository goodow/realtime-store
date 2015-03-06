#!/bin/bash
set -ev

#mvn compile gwt:compile -o -Dgwt.module=com.goodow.realtime.store.StoreProd \
#    -Dgwt.disableCastChecking=true -Dgwt.disableClassMetadata=true \
#    -Dgwt.compiler.optimizationLevel=9 -Dgwt.compiler.enableClosureCompiler=true \
#    -Dgwt.extraJvmArgs="-Xmx2048M"
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

CLASSPATH=./target/classes:./src/main/java
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/google/gwt/gwt-dev/2.7.0/gwt-dev-2.7.0.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/org/ow2/asm/asm/5.0.3/asm-5.0.3.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/google/gwt/gwt-user/2.7.0/gwt-user-2.7.0.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA-sources.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT-sources.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-operation/0.5.5-SNAPSHOT/realtime-operation-0.5.5-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-operation/0.5.5-SNAPSHOT/realtime-operation-0.5.5-SNAPSHOT-sources.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-channel/0.5.5-SNAPSHOT/realtime-channel-0.5.5-SNAPSHOT.jar
CLASSPATH=$CLASSPATH:$HOME/.m2/repository/com/goodow/realtime/realtime-channel/0.5.5-SNAPSHOT/realtime-channel-0.5.5-SNAPSHOT-sources.jar

java -cp $CLASSPATH \
    com.google.gwt.dev.Compiler -war target/realtime-store-0.5.5-SNAPSHOT \
    -XnoclassMetadata -XnocheckCasts -XjsInteropMode JS -XclosureCompiler -draftCompile \
    com.goodow.realtime.store.StoreProd

#    -saveSource -saveSourceOutput ../realtime-web-playground/app/bower_components/realtime-store/sourcemaps/ \
#    -style PRETTY -deploy ../realtime-web-playground/app/bower_components/realtime-store/ \
#    com.goodow.realtime.store.StoreProd


cp target/realtime-store-0.5.5-SNAPSHOT/store/store.nocache.js bower-realtime-store/realtime.store.js
# cp target/realtime-store-0.5.5-SNAPSHOT/store/*cache.js ../realtime-web-playground/app/bower_components/realtime-store/
