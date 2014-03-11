cd ../realtime-channel
mvn clean install

cd ../realtime-store
mvn clean compile gwt:compile -Dgwt.module=com.goodow.realtime.store.RealtimeProd
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

cd ../realtime-server-appengine/src/main/webapp/good/realtime/
rm realtime.js
# rm `ls | grep .cache.js`

cd ../../../../../../realtime-store/target/realtime-store-0.5.5-SNAPSHOT/realtime/
cp realtime.nocache.js ../../../../realtime-server-appengine/src/main/webapp/good/realtime/realtime.js
# cp `ls | grep .cache.js` ../../../../realtime-server-appengine/src/main/webapp/good/realtime/
