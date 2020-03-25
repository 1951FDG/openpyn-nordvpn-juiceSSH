package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnCommand;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;

import org.jetbrains.annotations.NonNls;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion", "MigrateAssertToMatcherAssert" })
public class StatusTest {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusTest.class);

    private static Status sInstance = null;

    @SuppressWarnings({ "HardcodedLineSeparator", "StringBufferReplaceableByString", "MagicNumber" })
    @BeforeClass
    public static void init() throws OpenVpnParseException {
        sInstance = new OpenVpnStatus();
        @NonNls StringBuilder stringBuilder = new StringBuilder(256);
        stringBuilder.append("INFO:OpenVPN Management Interface Version 1 -- type 'help' for more info\n");
        stringBuilder.append("status\n");
        stringBuilder.append("OpenVPN CLIENT LIST\n");
        stringBuilder.append("Updated,Wed Feb 11 00:07:24 2015\n");
        stringBuilder.append("Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since\n");
        stringBuilder.append("cloud.msk.pp.ua,192.168.1.1:59113,19192,19924,Tue Feb 10 23:30:46 2015\n");
        stringBuilder.append("ROUTING TABLE\n");
        stringBuilder.append("Virtual Address,Common Name,Real Address,Last Ref\n");
        stringBuilder.append("127.0.0.1,cloud.msk.pp.ua,192.168.1.1:59113,Tue Feb 10 23:30:52 2015\n");
        stringBuilder.append("GLOBAL STATS\n");
        stringBuilder.append("Max bcast/mcast queue length,0\n");
        stringBuilder.append("END");
        ((OpenVpnCommand) sInstance).setCommandOutput(stringBuilder.toString());
    }

    /**
     * Test of getClientList method, of class Status.
     */
    @Test
    public void testGetClientList() {
        LOGGER.info("getClientList");
        List<Client> result = sInstance.getClients();
        Assert.assertEquals(1L, result.size());
    }

    /**
     * Test of getRoutes method, of class Status.
     */
    @Test
    public void testGetRoutes() {
        LOGGER.info("getRoutes");
        Set<Route> result = sInstance.getRoutes();
        Assert.assertEquals(1L, result.size());
    }

    /**
     * Test of getUpdateTime method, of class Status.
     */
    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @Test
    public void testGetUpdateTime() {
        LOGGER.info("getUpdateTime");
        Calendar result = sInstance.getUpdateTime();
        Assert.assertNotNull(result);
    }
}
