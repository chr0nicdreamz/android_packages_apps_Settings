LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_JAVA_LIBRARIES := bouncycastle conscrypt telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := android-support-v4 android-support-v13 jsr305

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := \
        $(call all-java-files-under, src) \
        src/com/android/settings/EventLogTags.logtags

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res

LOCAL_SRC_FILES += \
        src/com/android/location/XT/IXTSrv.aidl \
        src/com/android/location/XT/IXTSrvCb.aidl

LOCAL_PACKAGE_NAME := Settings
LOCAL_CERTIFICATE := platform
LOCAL_PRIVILEGED_MODULE := true

LOCAL_PROGUARD_FLAG_FILES := proguard.flags

LOCAL_AAPT_INCLUDE_ALL_RESOURCES := true
LOCAL_AAPT_FLAGS += --extra-packages com.koushikdutta.superuser:com.koushikdutta.widgets --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages com.brewcrewfoo.performance

LOCAL_SRC_FILES += $(call all-java-files-under,../../../external/koush/Superuser/Superuser/src) $(call all-java-files-under,../../../external/koush/Widgets/Widgets/src)
LOCAL_SRC_FILES += $(call all-java-files-under,../PerformanceControl/src)

LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res $(LOCAL_PATH)/../../../external/koush/Widgets/Widgets/res $(LOCAL_PATH)/../../../external/koush/Superuser/Superuser/res
LOCAL_RESOURCE_DIR += packages/apps/PerformanceControl/res

LOCAL_ASSET_DIR += packages/apps/PerformanceControl/assets

LOCAL_JAVA_LIBRARIES += org.cyanogenmod.hardware

include frameworks/opt/setupwizard/navigationbar/common.mk
include $(BUILD_PACKAGE)

# Use the following include to make our test apk.
ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call all-makefiles-under,$(LOCAL_PATH))
endif
