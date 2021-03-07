/*
 * Copyright 2010 ALM Works Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define WRAPPER_VERSION "1.3"

#define WRAPPER_INVALID_ARG_1 (-11)
#define WRAPPER_INVALID_ARG_2 (-12)
#define WRAPPER_INVALID_ARG_3 (-13)
#define WRAPPER_INVALID_ARG_4 (-14)
#define WRAPPER_INVALID_ARG_5 (-15)
#define WRAPPER_INVALID_ARG_6 (-16)
#define WRAPPER_INVALID_ARG_7 (-17)
#define WRAPPER_INVALID_ARG_8 (-18)
#define WRAPPER_INVALID_ARG_9 (-19)

#define WRAPPER_CANNOT_TRANSFORM_STRING (-20)
#define WRAPPER_CANNOT_ALLOCATE_STRING (-21)
#define WRAPPER_OUT_OF_MEMORY (-22)

#define WRAPPER_WEIRD (-99)
#define WRAPPER_WEIRD_2 (-199)

//Solution (possibly temporary) for checking if Windows.
//Based on code from sqlite os.h. 
#ifndef SQLITE_OS_WIN
#  if defined(_WIN32) || defined(WIN32) || defined(__CYGWIN__) || defined(__MING32__) || defined(__BORLAND__)
#    define SQLITE_OS_WIN 1
#  endif
#endif

