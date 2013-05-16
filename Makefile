.SUFFIXES: .java .m
.PHONY: default clean translate link

J2OBJC_DIST = ~/dev/tools/lib/j2objc/dist
M2_REPO = ~/.m2/repository

ROOT_DIR = ..
GDREALTIME_DIR = $(ROOT_DIR)/GDRealtime
BUILD_DIR = target/j2objc
SOURCE_LIST = $(BUILD_DIR)/java.sources.list
MAIN_SRC_DIR = src/main/java
MAIN_GEN_DIR = $(GDREALTIME_DIR)/Classes/generated
MAIN_SOURCES = $(shell find $(MAIN_SRC_DIR) -name *.java ! -name NativeInterface.java \
  ! -name JreNativeInterfaceFactory.java ! -name JsNativeInterfaceFactory.java) 
MAIN_TEMP_SOURCES = $(subst $(MAIN_SRC_DIR), $(MAIN_GEN_DIR), $(MAIN_SOURCES))
MAIN_GEN_SOURCES = $(MAIN_TEMP_SOURCES:.java=.m)
OVERRIDE_GEN_DIR = $(GDREALTIME_DIR)/Classes/override_generated

JNI_SRC_DIR = src/main/objectivec
JNI_SOURCES = $(shell find $(JNI_SRC_DIR) -name *.java)
JNI_TEMP_SOURCES = $(subst $(JNI_SRC_DIR), $(BUILD_DIR), $(JNI_SOURCES))
JNI_GEN_SOURCES = $(JNI_TEMP_SOURCES:.java=.placeholder)

TEST_SRC_DIR = src/test/java
TEST_GEN_DIR = $(GDREALTIME_DIR)/Classes/test_generated
TEST_SOURCES = $(shell find $(TEST_SRC_DIR) -name *.java)
TEST_SOURCE_LIST = $(BUILD_DIR)/test.sources.list
TEST_TEMP_SOURCES = $(subst $(TEST_SRC_DIR), $(TEST_GEN_DIR), $(TEST_SOURCES))
TEST_GEN_SOURCES = $(TEST_TEMP_SOURCES:.java=.m)

J2OBJC = $(J2OBJC_DIST)/j2objc --prefixes $(ROOT_DIR)/resources/j2objc/package-prefixes.properties \
  --mapping $(ROOT_DIR)/resources/j2objc/method-mappings.properties
JUNIT_JAR = $(J2OBJC_DIST)/lib/junit-4.10.jar

default: clean translate
	@cd $(GDREALTIME_DIR)/Project;pod update;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test

test: translate
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test

translate: translate_main translate_test

pre_translate_main: $(MAIN_GEN_DIR)
	@rm -f $(SOURCE_LIST)
	@mkdir -p `dirname $(SOURCE_LIST)`
	@touch $(SOURCE_LIST)
        
$(MAIN_GEN_DIR)/%.m $(MAIN_GEN_DIR)/%.h: $(MAIN_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
$(BUILD_DIR)/%.placeholder: $(JNI_SRC_DIR)/%.java
	@echo $? >> $(SOURCE_LIST)
	@mkdir -p `dirname $@`
	@touch $@

translate_main: pre_translate_main $(MAIN_GEN_SOURCES) $(JNI_GEN_SOURCES)
	@if [ `cat $(SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR) -d $(MAIN_GEN_DIR) \
	    -classpath '$(J2OBJC_DIST)/lib/guava-13.0.jar:$(M2_REPO)/org/timepedia/exporter/gwtexporter/2.5.0-SNAPSHOT/gwtexporter-2.5.0-SNAPSHOT.jar:\
$(M2_REPO)/com/goodow/gwt/gwt-elemental/2.5.1-SNAPSHOT/gwt-elemental-2.5.1-SNAPSHOT.jar:$(M2_REPO)/com/google/gwt/gwt-user/2.5.1/gwt-user-2.5.1.jar' \
	    `cat $(SOURCE_LIST)` ; \
	fi
	@cp -r $(OVERRIDE_GEN_DIR)/ $(MAIN_GEN_DIR)

pre_translate_test: $(TEST_GEN_DIR)
	@rm -f $(TEST_SOURCE_LIST)
	@mkdir -p `dirname $(TEST_SOURCE_LIST)`
	@touch $(TEST_SOURCE_LIST)

$(TEST_GEN_DIR)/%.m $(TEST_GEN_DIR)/%.h: $(TEST_SRC_DIR)/%.java
	@echo $? >> $(TEST_SOURCE_LIST)

translate_test: pre_translate_test $(TEST_GEN_SOURCES)
	@if [ `cat $(TEST_SOURCE_LIST) | wc -l` -ge 1 ] ; then \
	  $(J2OBJC) -sourcepath $(MAIN_SRC_DIR):$(TEST_SRC_DIR) -d $(TEST_GEN_DIR) \
	    -classpath $(JUNIT_JAR) -Werror -use-arc \
	    `cat $(TEST_SOURCE_LIST)` ; \
	fi

$(MAIN_GEN_DIR):
	@mkdir -p $(MAIN_GEN_DIR)
$(TEST_GEN_DIR):
	@mkdir -p $(TEST_GEN_DIR)
$(BUILD_DIR):
	@mkdir -p $(BUILD_DIR)

clean:
	@rm -rf $(MAIN_GEN_DIR) $(TEST_GEN_DIR) $(BUILD_DIR)
	@cd $(GDREALTIME_DIR)/Project;xcodebuild -workspace GDRealtime.xcworkspace/ -scheme test clean
