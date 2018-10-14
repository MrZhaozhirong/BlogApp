cmake_minimum_required(VERSION 3.4.1)

# 设置生成的so动态库最后输出的路径
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

add_library( # 生成动态库的名称
             fmod-voice-lib
             # 指定是动态库SO
             SHARED
             # 编译库的源代码文件
             src/main/cpp/fmod/play_sound.cpp
             src/main/cpp/fmod/common.cpp
             src/main/cpp/fmod/common_platform.cpp)

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

# 在系统找出预编译的log-lib库，指定在CMake脚本下的别名为log
find_library( log-lib log )

target_link_libraries( # 指定目标链接库
                       fmod-voice-lib
                       # 添加预编译库到目标链接库中
                       ${log-lib}
                       fmod
                       fmodL)

