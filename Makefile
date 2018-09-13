CPP=g++
CPPFLAGS=-O2 -std=c++11 -fno-omit-frame-pointer
INCLUDES=-I$(JAVA_HOME)/include
JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar

OS:=$(shell uname -s)
ifeq ($(OS), Darwin)
	CPPFLAGS += -Wl,-undefined,dynamic_lookup
	INCLUDES += -I$(JAVA_HOME)/include/darwin
	OS_TAG=macos
else
	CPPFLAGS += -static-libstdc++
	INCLUDES += -I$(JAVA_HOME)/include/linux
	OS_TAG=linux
endif

LIB_VERSION=1.0.1
LIB_NAME=libstackimpact-$(LIB_VERSION)-$(OS_TAG)-x64.so


.PHONY: test build clean

build: src/*.cpp src/*.h
	mkdir -p build
	$(CPP) $(CPPFLAGS) $(INCLUDES) -fPIC -shared -o build/$(LIB_NAME) src/*.cpp -ldl -lpthread

test: build

clean:
