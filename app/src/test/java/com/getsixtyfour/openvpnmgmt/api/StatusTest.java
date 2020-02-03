/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnCommand;
import com.getsixtyfour.openvpnmgmt.implementation.OpenVpnStatus;

import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings({ "UseOfObsoleteDateTimeApi", "JUnitTestNG", "MigrateAssertToMatcherAssert", "MessageMissingOnJUnitAssertion" })
public class StatusTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusTest.class);

    private static Status instance = null;

    @BeforeClass
    public static void init() throws OpenVpnParseException {
        String input = "INFO:OpenVPN Management Interface Version 1 -- type 'help' for more info\n" + "status\n" + "OpenVPN CLIENT LIST\n"
                + "Updated,Wed Feb 11 00:07:24 2015\n" + "Common Name,Real Address,Bytes Received,Bytes Sent,Connected Since\n"
                + "cloud.msk.pp.ua,192.168.1.1:59113,19192,19924,Tue Feb 10 23:30:46 2015\n" + "ROUTING TABLE\n"
                + "Virtual Address,Common Name,Real Address,Last Ref\n"
                + "127.0.0.1,cloud.msk.pp.ua,192.168.1.1:59113,Tue Feb 10 23:30:52 2015\n" + "GLOBAL STATS\n"
                + "Max bcast/mcast queue length,0\n" + "END";
        instance = new OpenVpnStatus();
        ((OpenVpnCommand) instance).setCommandOutput(input);
    }

    /**
     * Test of getClientList method, of class Status.
     */
    @Test
    public void testGetClientList() {
        LOGGER.info("getClientList");
        List<Client> result = instance.getClients();
        Assert.assertEquals(1L, result.size());
    }

    /**
     * Test of getRoutes method, of class Status.
     */
    @Test
    public void testGetRoutes() {
        LOGGER.info("getRoutes");
        Set<Route> result = instance.getRoutes();
        Assert.assertEquals(1L, result.size());
    }

    /**
     * Test of getUpdateTime method, of class Status.
     */
    @Test
    public void testGetUpdateTime() {
        LOGGER.info("getUpdateTime");
        Calendar result = instance.getUpdateTime();
        Assert.assertNotNull(result);
    }
}
