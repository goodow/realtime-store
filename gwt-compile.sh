#!/bin/bash
set -ev

#mvn compile gwt:compile -o -Dgwt.module=com.goodow.realtime.store.StoreProd \
#    -Dgwt.disableCastChecking=true -Dgwt.disableClassMetadata=true \
#    -Dgwt.compiler.optimizationLevel=9 -Dgwt.compiler.enableClosureCompiler=true \
#    -Dgwt.extraJvmArgs="-Xmx2048M"
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

java -cp ./target/classes:./src/main/java:$HOME/.m2/repository/com/google/gwt/gwt-user/2.7.0-SNAPSHOT/gwt-user-2.7.0-SNAPSHOT.jar:$HOME/.m2/repository/com/google/gwt/gwt-dev/2.7.0-SNAPSHOT/gwt-dev-2.7.0-SNAPSHOT.jar:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA.jar:$HOME/.m2/repository/javax/validation/validation-api/1.0.0.GA/validation-api-1.0.0.GA-sources.jar:$HOME/.m2/repository/org/json/json/20090211/json-20090211.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT-sources.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-operation/0.5.5-SNAPSHOT/realtime-operation-0.5.5-SNAPSHOT.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-operation/0.5.5-SNAPSHOT/realtime-operation-0.5.5-SNAPSHOT-sources.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-channel/0.5.5-SNAPSHOT/realtime-channel-0.5.5-SNAPSHOT.jar:$HOME/.m2/repository/com/goodow/realtime/realtime-channel/0.5.5-SNAPSHOT/realtime-channel-0.5.5-SNAPSHOT-sources.jar \
    com.google.gwt.dev.Compiler -war target/realtime-store-0.5.5-SNAPSHOT \
    -XnoclassMetadata -XnocheckCasts -XjsInteropMode JS -XclosureCompiler -draftCompile \
    com.goodow.realtime.store.StoreProd

cp target/realtime-store-0.5.5-SNAPSHOT/store/store.nocache.js bower-realtime-store/realtime.store.js