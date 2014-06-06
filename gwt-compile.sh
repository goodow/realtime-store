#!/bin/bash
set -ev

mvn compile gwt:compile -Dgwt.module=com.goodow.realtime.store.StoreProd \
    -Dgwt.disableCastChecking=true -Dgwt.disableClassMetadata=true \
    -Dgwt.compiler.optimizationLevel=9 -Dgwt.compiler.enableClosureCompiler=true \
    -Dgwt.extraJvmArgs="-Xmx2048M"
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

cp target/realtime-store-0.5.5-SNAPSHOT/store/store.nocache.js bower-realtime-store/realtime-store.js