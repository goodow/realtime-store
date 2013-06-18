cp src/main/resources/js/realtime.nocache.js ../realtime-server/src/main/webapp/good/realtime

mvn gwt:run-codeserver -Dgwt.module=com.goodow.realtime.Realtime
