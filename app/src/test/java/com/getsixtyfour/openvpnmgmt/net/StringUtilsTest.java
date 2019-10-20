package com.getsixtyfour.openvpnmgmt.net;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.getsixtyfour.openvpnmgmt.utils.StringUtils;

/**
 * @author 1951FDG
 */

public class StringUtilsTest {

    public static final String original = "fQDaoFx\\Fi\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^";

    public static final String expected = "\"fQDaoFx\\\\Fi\\\"QYF/R];T,zFsm&=!4^#4a?PL(d3Y^\"";

    private static final Logger LOGGER = LoggerFactory.getLogger(StringUtilsTest.class);

    /**
     * Test of escapeOpenVPN method, of class StringUtils.
     */
    @Test
    public void testEscapeOpenVPN() {
        LOGGER.info("escapeOpenVPN");
        Assert.assertEquals(expected, StringUtils.escapeOpenVPN(original));
    }
}
