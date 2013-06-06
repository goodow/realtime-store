.SUFFIXES: .java .m
.PHONY: default clean translate link

include ../resources/make/common.mk

MAIN_SOURCES = $(shell find $(MAIN_SRC_DIR) -name *.java ! -name ModelNative.java \
  ! -name JreModelFactory.java ! -name JsModelFactory.java)
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(MODEL_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_TEMP_SOURCES:.java=.m)
OVERRIDE_GEN_DIR = $(GDREALTIME_DIR)/Classes/override_generated/model

OCNI_SOURCES = $(shell find $(OCNI_SRC_DIR) -name *.java)
OCNI_TEMP_SOURCES = $(subst $(OCNI_SRC_DIR), $(BUILD_DIR), $(OCNI_SOURCES))
OCNI_GEN_SOURCES = $(OCNI_TEMP_SOURCES:.java=.placeholder)

TEST_SOURCES = $(shell find $(TEST_SRC_DIR) -name *.java)
TEST_TEMP_SOURCES = $(subst $(TEST_SRC_DIR), $(TEST_GEN_DIR), $(TEST_SOURCES))
TEST_GEN_SOURCES = $(TEST_TEMP_SOURCES:.java=.m)

TEMP_PATH = $(J2OBJC_DIST)/lib/guava-13.0.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-operation/0.0.1-SNAPSHOT/realtime-operation-0.0.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/goodow/realtime/realtime-channel/0.0.1-SNAPSHOT/realtime-channel-0.0.1-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar
TEMP_PATH += :$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar
CLASSPATH = $(shell echo $(TEMP_PATH) | sed 's/ //g')
    
default: clean translate pod_update test

test: translate
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test

translate: translate_main translate_test

pod_update: 
	@cd $(GDREALTIME_DIR)/Project;pod update

pre_translate_main: $(MODEL_GEN_DIR)
	@rm -f $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $(MAIN_SOURCE_LIST)`
	@touch $(MAIN_SOURCE_LIST)
        
$(MODEL_GEN_DIR)/%.m $(MODEL_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(OCNI_SRC_DIR)/%.java
	@echo $? >> $(MAIN_SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(OCNI_GEN_SOURCES)
	@if [ `cat $(MAIN_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(MODEL_GEN_DIR) -use-arc \
	    -classpath $(CLASSPATH) \
	    `cat $(MAIN_SOURCE_LIST)` ; \
	fi
	@cp -r $(OVERRIDE_GEN_DIR)/ $(MODEL_GEN_DIR)
	@cd $(MODEL_GEN_DIR);mkdir -p ../include;tar -c . | tar -x -C ../include --include=*.h

pre_translate_test: $(TEST_GEN_DIR)
	@rm -f $(TEST_SOURCE_LIST)
	@mkdir -p `dirname $(TEST_SOURCE_LIST)`
	@touch $(TEST_SOURCE_LIST)

$(TEST_GEN_DIR)/%.m $(TEST_GEN_DIR)/%.h: $(TEST_SRC_DIR)/%.java
	@echo $? >> $(TEST_SOURCE_LIST)

translate_test: pre_translate_test $(TEST_GEN_SOURCES)
	@if [ `cat $(TEST_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR):$(TEST_SRC_DIR) -d $(TEST_GEN_DIR) \
	    -classpath $(CLASSPATH):$(JUNIT_JAR) -Werror -use-arc \
	    `cat $(TEST_SOURCE_LIST)` ; \
	fi

$(MODEL_GEN_DIR):
	@mkdir -p $(MODEL_GEN_DIR)
$(TEST_GEN_DIR):
	@mkdir -p $(TEST_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(MODEL_GEN_DIR) $(TEST_GEN_DIR) $(BUILD_DIR)
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test clean
