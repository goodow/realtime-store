.SUFFIXES: .java .m
.PHONY: default clean translate link

include ../resources/make/common.mk

MAIN_SOURCES = $(subst ./,,$(shell cd $(MAIN_SRC_DIR); find . -name *.java \
  ! -name ModelNative.java ! -name JreModelFactory.java ! -name JsModelFactory.java))
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(API_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_SOURCES:%.java=$(API_GEN_DIR)/%.m)
OVERRIDE_GEN_DIR = $(GDREALTIME_DIR)/Classes/override_generated/api

OCNI_SOURCES = $(subst ./,,$(shell cd $(OCNI_SRC_DIR); find . -name *.java))
OCNI_GEN_SOURCES = $(OCNI_SOURCES:%.java=$(BUILD_DIR)/%.placeholder)

TEST_SOURCES = $(subst ./,,$(shell cd $(TEST_SRC_DIR); find . -name *.java))
TEST_GEN_SOURCES = $(TEST_SOURCES:%.java=$(TEST_GEN_DIR)/%.m)

TEMP_PATH = $(J2OBJC_DIST)/lib/guava-13.0.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-operation/0.3.0-SNAPSHOT/realtime-operation-0.3.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-channel/0.3.0-SNAPSHOT/realtime-channel-0.3.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')
    
default: clean translate pod_update test

test: translate
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test

translate: translate_main translate_test

pod_update: 
	@cd $(GDREALTIME_DIR)/Project;pod update

pre_translate_main: $(API_GEN_DIR)
	@rm -f $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $(MAIN_SOURCE_LIST)`
	@touch $(MAIN_SOURCE_LIST)
        
$(API_GEN_DIR)/%.m $(API_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(OCNI_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(OCNI_GEN_SOURCES)
	@if [ `cat $(MAIN_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(API_GEN_DIR) \
	    -classpath $(CLASSPATH) \
	    `cat $(MAIN_SOURCE_LIST)` ; \
	fi
	@cp -r $(OVERRIDE_GEN_DIR)/ $(API_GEN_DIR)
	@cd $(API_GEN_DIR);mkdir -p ../include;tar -c . | tar -x -C ../include --include=*.h

pre_translate_test: $(TEST_GEN_DIR)
	@rm -f $(TEST_SOURCE_LIST)
	@mkdir -p `dirname $(TEST_SOURCE_LIST)`
	@touch $(TEST_SOURCE_LIST)

$(TEST_GEN_DIR)/%.m $(TEST_GEN_DIR)/%.h: $(TEST_SRC_DIR)/%.java
	@echo $? >> $(TEST_SOURCE_LIST)

translate_test: pre_translate_test $(TEST_GEN_SOURCES)
	@if [ `cat $(TEST_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR):$(TEST_SRC_DIR) -d $(TEST_GEN_DIR) \
	    -classpath $(CLASSPATH):$(JUNIT_JAR) -Werror \
	    `cat $(TEST_SOURCE_LIST)` ; \
	fi

$(API_GEN_DIR):
	@mkdir -p $(API_GEN_DIR)
$(TEST_GEN_DIR):
	@mkdir -p $(TEST_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(API_GEN_DIR) $(TEST_GEN_DIR) $(BUILD_DIR)
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test clean
