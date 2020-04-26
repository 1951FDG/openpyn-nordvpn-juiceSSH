package com.getsixtyfour.openvpnmgmt.utils;

import org.jetbrains.annotations.NonNls;

import org.junit.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion", "MigrateAssertToMatcherAssert" })
public class StringUtilsTest {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtilsTest.class);

    @NonNls
    private final String mExpected = "fQDaoFx\\\\Fi\\\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^";

    @NonNls
    private final String mOriginal = "fQDaoFx\\Fi\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^";

    /**
     * Test of escapeString method, of class StringUtils.
     */
    @Test
    public void testEscapeString() {
        LOGGER.debug("escapeString");
        Assert.assertEquals(mExpected, StringUtils.escapeString(mOriginal));
    }
}
