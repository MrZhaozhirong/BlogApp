cmake_minimum_required(VERSION 3.4.1)

# 设置生成的so动态库最后输出的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

# 在系统找出预编译的log库，指定在CMake脚本下的别名为log-lib
find_library( log-lib log )
# Example cmake
add_library( native-lib
             SHARED
             src/main/cpp/native-lib.cpp )
target_link_libraries(native-lib
                      ${log-lib} )

# 为log_common.h 指定平台
IF (${CMAKE_HOST_SYSTEM_NAME} MATCHES "Windows")
    ADD_DEFINITIONS(-DWindows)
ELSE (${CMAKE_HOST_SYSTEM_NAME} MATCHES "Linux")
    ADD_DEFINITIONS(-DLinux)
ENDIF ()





# 引入外部so fmod 供源文件编译
add_library(fmod
            SHARED
            IMPORTED )
set_target_properties(fmod PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/fmod/prebuild/${ANDROID_ABI}/libfmod.so)
set_target_properties(fmod PROPERTIES LINKER_LANGUAGE CXX)

# 引入外部so fmodL 供源文件编译
add_library(fmodL
            SHARED
            IMPORTED )
set_target_properties(fmodL PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/fmod/prebuild/${ANDROID_ABI}/libfmodL.so)
set_target_properties(fmodL PROPERTIES LINKER_LANGUAGE CXX)

# 添加fmod demo库
add_library( # 生成动态库的名称
             fmod-voice-lib
             # 指定是动态库SO
             SHARED
             # 编译库的源代码文件
             src/main/cpp/fmod/play_sound.cpp
             src/main/cpp/fmod/common.cpp
             src/main/cpp/fmod/common_platform.cpp)
# 连接fmod demo库
target_link_libraries( # 指定目标链接库
                       fmod-voice-lib
                       # 添加预编译库到目标链接库中
                       ${log-lib}
                       fmod
                       fmodL)

# 添加fmod effect特效库
add_library( # 生成动态库的名称
             fmod-effect-lib
             # 指定是动态库SO
             SHARED
             # 编译库的源代码文件
             src/main/cpp/fmod/effect_sound.cpp)

# 连接fmod effect特效库
target_link_libraries( # 指定目标链接库
                       fmod-effect-lib
                       # 添加预编译库到目标链接库中
                       ${log-lib}
                       fmod
                       fmodL)









# 引入外部 ffmpeg so 供源文件编译
add_library(avcodec SHARED IMPORTED )
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libavcodec.so)
set_target_properties(avcodec PROPERTIES LINKER_LANGUAGE CXX)

add_library(avdevice SHARED IMPORTED )
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libavdevice.so)
set_target_properties(avdevice PROPERTIES LINKER_LANGUAGE CXX)

add_library(avfilter SHARED IMPORTED )
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libavfilter.so)
set_target_properties(avfilter PROPERTIES LINKER_LANGUAGE CXX)

add_library(avformat SHARED IMPORTED )
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libavformat.so)
set_target_properties(avformat PROPERTIES LINKER_LANGUAGE CXX)

add_library(avutil SHARED IMPORTED )
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libavutil.so)
set_target_properties(avutil PROPERTIES LINKER_LANGUAGE CXX)

add_library(postproc SHARED IMPORTED )
set_target_properties(postproc PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libpostproc.so)
set_target_properties(postproc PROPERTIES LINKER_LANGUAGE CXX)

add_library(swresample SHARED IMPORTED )
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libswresample.so)
set_target_properties(swresample PROPERTIES LINKER_LANGUAGE CXX)

add_library(swscale SHARED IMPORTED )
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/ffmpeg/lib/libswscale.so)
set_target_properties(swscale PROPERTIES LINKER_LANGUAGE CXX)

add_library( # 生成动态库的名称
             zzr-ffmpeg-utils
             # 指定是动态库SO
             SHARED
             # 编译库的源代码文件
             src/main/cpp/ffmpeg/zzr_ffmpeg_util.c)

target_link_libraries( # 指定目标链接库
                       zzr-ffmpeg-utils
                       # 添加预编译库到目标链接库中
                       ${log-lib}
                       avutil
                       avcodec
                       avformat
                       swscale
                       swresample )











# 在系统找出预编译的android库，指定在CMake脚本下的别名为android-lib
# android参数 for native windows
find_library( android-lib android )

add_library(yuv SHARED IMPORTED )
set_target_properties(yuv PROPERTIES IMPORTED_LOCATION
            ${PROJECT_SOURCE_DIR}/src/main/cpp/libyuv/libyuv.so)
set_target_properties(yuv PROPERTIES LINKER_LANGUAGE CXX)

add_library( # 生成动态库的名称
             zzr-ffmpeg-player
             # 指定是动态库SO
             SHARED
             # 编译库的源代码文件
             src/main/cpp/ffmpeg/zzr_ffmpeg_player.c)

target_link_libraries( # 指定目标链接库
                       zzr-ffmpeg-player
                       # 添加预编译库到目标链接库中
                       ${log-lib}
                       ${android-lib}
                       avutil
                       avcodec
                       avformat
                       swscale
                       swresample
                       yuv )




