
cmake_minimum_required(VERSION 3.6)

PROJECT(adv-surface-texture)

add_compile_options(
        -g
        -W
        -Wall
        -Werror
        -fsigned-char
        -fno-omit-frame-pointer
        -fstack-protector-strong
        -fexceptions
        -frtti
        -fPIC)

set(COMPILER_FLAGS_DEBUG "-D_DEBUG -DDEBUG -Og")
set(COMPILER_FLAGS_RELEASE "-D_NDEBUG -DNDEBUG -O2 -ffunction-sections -fdata-sections -fvisibility=hidden")
set(COMMON_LINKER_FLAGS "-Wl,--no-undefined -fPIE -fpie -pie")

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} ${COMPILER_FLAGS_DEBUG}")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} ${COMPILER_FLAGS_RELEASE}")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} ${COMPILER_FLAGS_DEBUG}")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} ${COMPILER_FLAGS_RELEASE}")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} ${COMMON_LINKER_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} ${COMMON_LINKER_FLAGS}")
set(CMAKE_SHARED_LINKER_FLAGS_RELEASE "${CMAKE_SHARED_LINKER_FLAGS_RELEASE} -Wl,--gc-sections")

set(adv_surface_texture_INCLUDES
        ./
        ./jni
        )

set(adv_surface_texture_SOURCES
        jni/com_chraac_advsurfacetexture_SurfaceExt.cc
        jni/com_chraac_advsurfacetexture_SurfaceExt.hh
        jni/jni_load.cc
        jni/jni_helper.hh
        )

find_library(ANDROID_LOG log)
find_library(ANDROID_LIB android)
find_library(ANDROID_MEDIA mediandk)
find_library(ANDROID_NATIVE_WINDOW nativewindow)
find_library(EGL_LIB EGL)
find_library(GLES_LIB GLESv2)

set(adv_surface_texture_LINKS
        ${ANDROID_LOG}
        ${ANDROID_LIB}
        ${ANDROID_MEDIA}
        ${ANDROID_NATIVE_WINDOW}
        ${EGL_LIB}
        ${GLES_LIB}
        )

add_library(adv_surface_texture SHARED ${adv_surface_texture_SOURCES})

target_include_directories(adv_surface_texture PUBLIC
        ${adv_surface_texture_INCLUDES}
        )

target_link_libraries(adv_surface_texture PUBLIC
        ${adv_surface_texture_LINKS}
        )