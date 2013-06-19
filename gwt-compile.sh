cd ../realtime-channel
mvn clean install

cd ../realtime-model
mvn clean compile gwt:compile -Dgwt.module=com.goodow.realtime.RealtimeProd
#-Dgwt.draftCompile=true

cd ../realtime-server/src/main/webapp/good/realtime/
rm realtime.nocache.js
rm `ls | grep .cache.js`

cd ../../../../../../realtime-model/target/realtime-model-0.0.1-SNAPSHOT/realtime/
cp realtime.nocache.js ../../../../realtime-server/src/main/webapp/good/realtime/
cp `ls | grep .cache.js` ../../../../realtime-server/src/main/webapp/good/realtime/
