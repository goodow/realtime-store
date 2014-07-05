.SUFFIXES: .java .m
.PHONY: default clean translate link

include ../resources/make/common.mk
# J2OBJC_DIST = GDStore/Project/Pods/J2ObjC/dist

STORE_GEN_DIR = GDStore/Classes/generated
MAIN_SOURCES = $(subst $(MAIN_SRC_DIR)/,,$(shell find $(MAIN_SRC_DIR) -name *.java ! -path "*/Html*" ! -path "*/server/*"))
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(STORE_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_SOURCES:%.java=$(STORE_GEN_DIR)/%.m)

TEST_SOURCES = $(subst $(TEST_SRC_DIR)/,,$(shell find $(TEST_SRC_DIR) -name *.java))
TEST_GEN_SOURCES = $(TEST_SOURCES:%.java=$(TEST_GEN_DIR)/%.m)

TEMP_PATH = $(M2_REPO)/com/goodow/realtime/realtime-json/0.5.5-SNAPSHOT/realtime-json-0.5.5-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-operation/0.5.5-SNAPSHOT/realtime-operation-0.5.5-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-channel/0.5.5-SNAPSHOT/realtime-channel-0.5.5-SNAPSHOT.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')
    
default: clean translate

test: translate
	@cd GDStore/Project;xcodebuild -workspace GDStore.xcworkspace/ -scheme test

translate: translate_main

pod_update: 
	@cd GDStore/Project;pod update

pre_translate_main: $(BUILD_DIR) $(STORE_GEN_DIR)
	@rm -f $(MAIN_SOURCE_LIST)
	@touch $(MAIN_SOURCE_LIST)
        
$(STORE_GEN_DIR)/%.m $(STORE_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(OCNI_GEN_SOURCES)
	@if [ `cat $(MAIN_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(STORE_GEN_DIR) \
	    -classpath $(CLASSPATH) \
	    `cat $(MAIN_SOURCE_LIST)` ; \
	fi

pre_translate_test: $(BUILD_DIR) $(TEST_GEN_DIR)
	@rm -f $(TEST_SOURCE_LIST)
	@touch $(TEST_SOURCE_LIST)

$(TEST_GEN_DIR)/%.m $(TEST_GEN_DIR)/%.h: $(TEST_SRC_DIR)/%.java
	@echo $? >> $(TEST_SOURCE_LIST)

translate_test: pre_translate_test $(TEST_GEN_SOURCES)
	@if [ `cat $(TEST_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR):$(TEST_SRC_DIR) -d $(TEST_GEN_DIR) \
	    -classpath $(CLASSPATH):$(JUNIT_JAR) -Werror \
	    `cat $(TEST_SOURCE_LIST)` ; \
	fi

$(STORE_GEN_DIR):
	@mkdir -p $(STORE_GEN_DIR)
$(TEST_GEN_DIR):
	@mkdir -p $(TEST_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(STORE_GEN_DIR)/com/goodow/realtime/store/ $(TEST_GEN_DIR) $(BUILD_DIR)
	@cd GDStore/Project;xcodebuild -workspace GDStore.xcworkspace/ -scheme test clean
