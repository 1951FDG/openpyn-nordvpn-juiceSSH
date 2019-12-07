/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.api.Client;
import com.getsixtyfour.openvpnmgmt.api.Route;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;

import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.runners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MigrateAssertToMatcherAssert", "MessageMissingOnJUnitAssertion" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionTest {

    private static Connection connection = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionTest.class);

    private final ResourceBundle bundle = ResourceBundle.getBundle("test");

    private final String host = bundle.getString("management.server");

    private final Integer port = Integer.valueOf(bundle.getString("management.port"));

    @BeforeClass
    public static void setUpClass() {
        connection = ManagementConnection.getInstance();
        connection.setUsernamePasswordHandler(new UsernamePasswordHandler() {
            @NotNull
            @Override
            public String getUserName() {
                return "";
            }

            @NotNull
            @Override
            public String getUserPass() {
                return "";
            }
        });
    }

    @AfterClass
    public static void tearDownClass() {
        connection.disconnect();
    }

    /**
     * Test of connect method, of class Connection.
     */
    @Test(expected = IOException.class)
    public void testConnect() throws IOException {
        LOGGER.info("connect");
        connection.connect(host, port);
        Assert.assertTrue(connection.isConnected());
        connection.run();
    }

    /**
     * Test of executeCommand method, of class Connection.
     */
    @Test
    public void testExecuteCommand() throws IOException {
        LOGGER.info("executeCommand");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        String result = connection.executeCommand(Commands.HELP_COMMAND);
        Assert.assertNotEquals("", result);
        String[] lines = result.split(System.lineSeparator());
        Assert.assertTrue(lines.length > 1);
        LOGGER.info(result);
    }

    /**
     * Test of getManagementVersion method, of class Connection.
     */
    @Test
    public void testGetManagementVersion() throws IOException {
        LOGGER.info("getManagementVersion");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        String result = connection.getManagementVersion();
        Assert.assertNotEquals("", result);
        LOGGER.info(result);
    }

    /**
     * Test of getOpenVPNVersion method, of class Connection.
     */
    @Test
    public void testGetOpenVPNVersion() throws IOException {
        LOGGER.info("getOpenVPNVersion");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        String result = connection.getOpenVPNVersion();
        Assert.assertNotEquals("", result);
        LOGGER.info(result);
    }

    /**
     * Test of getOpenVPNStatus method, of class Connection.
     */
    @Test
    public void testGetStatus() throws IOException, OpenVpnParseException {
        LOGGER.info("getOpenVPNStatus");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        Status result = connection.getOpenVPNStatus();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getUpdateTime());
        List<Client> clientList = result.getClientList();
        Assert.assertFalse(clientList.isEmpty());
        Set<Route> routes = result.getRoutes();
        Assert.assertFalse(routes.isEmpty());
        LOGGER.info(result.toString());
    }

    /**
     * Test of isOpenVPNActive method, of class Connection.
     */
    @Test
    public void testIsOpenVPNActive() {
        LOGGER.info("isOpenVPNActive");
        Assert.assertFalse(connection.isOpenVPNActive());
    }

    /**
     * Test of run method, of class Connection.
     */
    @Test(expected = IOException.class)
    public void testRun() throws IOException {
        LOGGER.info("run");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        connection.run();
        Assert.assertFalse(connection.isConnected());
    }

    /**
     * Test of stopOpenVPN method, of class Connection.
     */
    @Test
    public void testStopOpenVPN() throws IOException {
        LOGGER.info("stopOpenVPN");
        if (!connection.isConnected()) {
            connection.connect(host, port);
        }
        Assert.assertTrue(connection.isConnected());
        connection.stopOpenVPN();
    }
}
