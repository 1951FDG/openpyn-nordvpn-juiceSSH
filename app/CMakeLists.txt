# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library(
        # Sets the name of the library.
        sqlite3ndk

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/sqlite3ndk.cpp)

add_library(
        # Sets the name of the library.
        sqlite-android-3240000

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        IMPORTED)

string(TOLOWER ${CMAKE_BUILD_TYPE} BUILD_TYPE)
set_target_properties(
        # Specifies the target library.
        sqlite-android-3240000

        PROPERTIES IMPORTED_LOCATION
        # Specifies the target location.
        ../../../../build/intermediates/transforms/mergeJniLibs/${BUILD_TYPE}/0/lib/${ANDROID_ABI}/libsqliteX.so)

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library(
        # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

find_library(
        # Sets the name of the path variable.
        android-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        android)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(
        # Specifies the target library.
        sqlite3ndk

        # Links the target library to the libraries
        # included in the NDK.
        sqlite-android-3240000 ${log-lib} ${android-lib})