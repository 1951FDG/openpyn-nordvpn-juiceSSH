package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.net.Commands;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import org.junit.*;
import org.junit.runners.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion", "MigrateAssertToMatcherAssert" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ConnectionTest {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionTest.class);

    private static Connection sConnection = null;

    @NonNls
    private final ResourceBundle mBundle = ResourceBundle.getBundle("test");

    private final String mHost = mBundle.getString("management.server");

    private final Integer mPort = Integer.valueOf(mBundle.getString("management.port"));

    @BeforeClass
    public static void setUpClass() {
        sConnection = ManagementConnection.getInstance();
        sConnection.setUsernamePasswordHandler(new UsernamePasswordHandler() {
            @NotNull
            @Override
            public String getUser() {
                return "";
            }

            @NotNull
            @Override
            public String getPassword() {
                return "";
            }
        });
    }

    @AfterClass
    public static void tearDownClass() {
        sConnection.disconnect();
    }

    /**
     * Test of connect method, of class Connection.
     */
    @Test(expected = IOException.class)
    public void testConnect() throws IOException {
        LOGGER.info("connect");
        sConnection.connect(mHost, mPort);
        Assert.assertTrue(sConnection.isConnected());
        sConnection.run();
    }

    /**
     * Test of executeCommand method, of class Connection.
     */
    @Test
    public void testExecuteCommand() throws IOException {
        LOGGER.info("executeCommand");
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        String result = sConnection.executeCommand(Commands.HELP_COMMAND);
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
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        String result = sConnection.getManagementVersion();
        Assert.assertNotEquals("", result);
        LOGGER.info(result);
    }

    /**
     * Test of getVpnVersion method, of class Connection.
     */
    @Test
    public void testGetVpnVersion() throws IOException {
        LOGGER.info("getVpnVersion");
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        String result = sConnection.getVpnVersion();
        Assert.assertNotEquals("", result);
        LOGGER.info(result);
    }

    /**
     * Test of getVpnStatus method, of class Connection.
     */
    @Test
    public void testGetVpnStatus() throws IOException {
        LOGGER.info("getVpnStatus");
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        Status result = sConnection.getVpnStatus();
        Assert.assertNotNull(result.getUpdateTime());
        List<Client> clientList = result.getClients();
        Assert.assertFalse(clientList.isEmpty());
        Set<Route> routes = result.getRoutes();
        Assert.assertFalse(routes.isEmpty());
        LOGGER.info(result.toString());
        LOGGER.info(clientList.toString());
    }

    /**
     * Test of isVpnActive method, of class Connection.
     */
    @Test
    public void testIsVpnActive() {
        LOGGER.info("isVpnActive");
        Assert.assertFalse(sConnection.isVpnActive());
    }

    /**
     * Test of run method, of class Connection.
     */
    @Test(expected = IOException.class)
    public void testRun() throws IOException {
        LOGGER.info("run");
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        sConnection.run();
        Assert.assertFalse(sConnection.isConnected());
    }

    /**
     * Test of stopVpn method, of class Connection.
     */
    @Test
    public void testStopVpn() throws IOException {
        LOGGER.info("stopVpn");
        if (!sConnection.isConnected()) {
            sConnection.connect(mHost, mPort);
        }
        Assert.assertTrue(sConnection.isConnected());
        sConnection.stopVpn();
    }
}
