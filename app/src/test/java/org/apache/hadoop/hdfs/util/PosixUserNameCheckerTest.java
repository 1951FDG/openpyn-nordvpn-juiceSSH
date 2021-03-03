/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.hdfs.util;

import org.junit.*;

@SuppressWarnings("HardCodedStringLiteral")
public class TestPosixUserNameChecker {

    private static final PosixUserNameChecker CHECKER = new PosixUserNameChecker();

    private static void check(String username, boolean expected) {
        Assert.assertEquals(expected, CHECKER.isValidUserName(username));
    }

    @Test
    public void testIsValidPosixUser() {
        check("abc", true);
        check("a_bc", true);
        check("a1234bc", true);
        check("a1234bc45", true);
        check("_a1234bc45$", true);
        check("_a123-4bc45$", true);
        check("abc_", true);
        check("ab-c_$", true);
        check("_abc", true);
        check("ab-c$", true);
        check("-abc", false);
        check("-abc_", false);
        check("a$bc", false);
        check("9abc", false);
        check("-abc", false);
        check("$abc", false);
        check("", false);
    }
}
