LOCAL_PATH := $(call my-dir)
REAL_LOCAL_PATH:=$(LOCAL_PATH) 

include $(CLEAR_VARS)

LOCAL_MODULE := application

LOCAL_CFLAGS := -I$(LOCAL_PATH)/python2.7/include

LOCAL_CFLAGS += $(APPLICATION_ADDITIONAL_CFLAGS)

LOCAL_SRC_FILES := pythonrun.c

LOCAL_LDLIBS := -lpython2.7  -llog 

LOCAL_LDFLAGS += -L$(LOCAL_PATH)/python2.7/lib $(APPLICATION_ADDITIONAL_LDFLAGS)

/home/schalk/workspacextend/Paint3d/obj/local/armeabi/libpython2.7.so : /home/schalk/workspacextend/Paint3d/libs/armeabi/libpython2.7.so

/home/schalk/workspacextend/Paint3d/libs/armeabi/libpython2.7.so: /home/schalk/workspacextend/Paint3d/libpython/libpython2.7.so
	cp /home/schalk/workspacextend/Paint3d/libpython/libpython2.7.so /home/schalk/workspacextend/Paint3d/libs/armeabi/libpython2.7.so

include $(BUILD_SHARED_LIBRARY)


