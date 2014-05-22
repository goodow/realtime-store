realtime-store [![Build Status](https://travis-ci.org/goodow/realtime-store.svg?branch=master)](https://travis-ci.org/goodow/realtime-store)
==============

Google Docs–style collaboration via the use of operational transforms

## Build from source and launch server

### Pre-requisites
- [JDK 7+](https://jdk8.java.net/download.html)
- [Apache Maven](http://maven.apache.org/download.html)
- [Git](https://help.github.com/articles/set-up-git)
- [ElasticSearch](http://www.elasticsearch.org/download/)
- [Redis](http://redis.io/download)

### Check out sources and run the server with Maven
```bash
git clone https://github.com/goodow/realtime-store.git
cd realtime-store
mvn clean package vertx:runMod
```

### Configuration
https://github.com/goodow/realtime-store/blob/master/src/main/resources/store.conf