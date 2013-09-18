cd ../realtime-channel
mvn clean install

cd ../realtime-api
mvn clean compile gwt:compile -Dgwt.module=com.goodow.realtime.RealtimeProd
# -Dgwt.draftCompile=true -Dgwt.style=DETAILED -Dgwt.compiler.compileReport=true

cd ../realtime-server-appengine/src/main/webapp/good/realtime/
rm realtime.js
# rm `ls | grep .cache.js`

cd ../../../../../../realtime-api/target/realtime-api-0.3.0-SNAPSHOT/realtime/
cp realtime.nocache.js ../../../../realtime-server-appengine/src/main/webapp/good/realtime/realtime.js
# cp `ls | grep .cache.js` ../../../../realtime-server-appengine/src/main/webapp/good/realtime/
