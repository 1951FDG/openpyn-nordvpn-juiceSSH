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
public class PosixPathNameCheckerTest {

    private static final PosixPathNameChecker CHECKER = new PosixPathNameChecker();

    private static void doTestIsValidPosixFileName() {
        Assert.assertTrue(CHECKER.isValidPosixFileName("test1"));
        Assert.assertFalse(CHECKER.isValidPosixFileName("-test1"));
        Assert.assertTrue(CHECKER.isValidPosixFileName("test-1"));
        Assert.assertTrue(CHECKER.isValidPosixFileName("test.-1"));
        Assert.assertTrue(CHECKER.isValidPosixFileName("tEsT.1-.TesT"));
    }

    private static void doTestIsValidPosixPath() {
        // TODO: test "//" on Windows
        Assert.assertTrue(CHECKER.isValidPath("/abs/ddd.dd"));
        // assertFalse(CHECKER.isValidPath("//abs/ddd.dd"));
        Assert.assertFalse(CHECKER.isValidPath("/abs:/ddd.dd"));
        // assertFalse(CHECKER.isValidPath("/abs///ddd.dd"));
        Assert.assertTrue(CHECKER.isValidPath("/test-1/test.-1/tEsT.1-.TesT"));
        Assert.assertFalse(CHECKER.isValidPath("/*&343-!@#"));
    }

    @Test
    public void testIsValidPosixPath() {
        doTestIsValidPosixFileName();
        doTestIsValidPosixPath();
    }
}
