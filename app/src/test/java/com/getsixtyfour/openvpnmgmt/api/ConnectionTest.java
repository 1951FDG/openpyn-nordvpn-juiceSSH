package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.net.Commands;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.net.UsernamePasswordHandler;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
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

    private final char[] mPassword = mBundle.getString("management.password").toCharArray();

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
    @Test
    public void testConnect() {
        LOGGER.debug("connect");
        sConnection.connect(mHost, mPort, mPassword);
        Assert.assertTrue(sConnection.isConnected());
        sConnection.run();
    }

    /**
     * Test of executeCommand method, of class Connection.
     */
    @Test
    public void testExecuteCommand() throws IOException {
        LOGGER.debug("executeCommand");
        sConnection.connect(mHost, mPort, mPassword);
        String result = sConnection.executeCommand(Commands.HELP_COMMAND);
        Assert.assertNotEquals("", result);
        String[] lines = result.split(System.lineSeparator());
        Assert.assertTrue(lines.length > 1);
        LOGGER.debug(result);
    }

    /**
     * Test of getManagementVersion method, of class Connection.
     */
    @Test
    public void testGetManagementVersion() throws IOException {
        LOGGER.debug("getManagementVersion");
        sConnection.connect(mHost, mPort, mPassword);
        String result = sConnection.getManagementVersion();
        Assert.assertNotEquals("", result);
        LOGGER.debug(result);
    }

    /**
     * Test of getVpnVersion method, of class Connection.
     */
    @Test
    public void testGetVpnVersion() throws IOException {
        LOGGER.debug("getVpnVersion");
        sConnection.connect(mHost, mPort, mPassword);
        String result = sConnection.getVpnVersion();
        Assert.assertNotEquals("", result);
        LOGGER.debug(result);
    }

    /**
     * Test of getVpnStatus method, of class Connection.
     */
    @Test
    public void testGetVpnStatus() throws IOException {
        LOGGER.debug("getVpnStatus");
        sConnection.connect(mHost, mPort, mPassword);
        Status result = sConnection.getVpnStatus();
        Assert.assertNotNull(result.getUpdateTime());
        List<Client> clientList = result.getClients();
        Assert.assertFalse(clientList.isEmpty());
        Set<Route> routes = result.getRoutes();
        Assert.assertFalse(routes.isEmpty());
        LOGGER.debug(result.toString());
        LOGGER.debug(clientList.toString());
    }

    /**
     * Test of isVpnActive method, of class Connection.
     */
    @Test
    public void testIsVpnActive() {
        LOGGER.debug("isVpnActive");
        Assert.assertFalse(sConnection.isVpnActive());
    }

    /**
     * Test of run method, of class Connection.
     */
    @Test
    public void testRun() {
        LOGGER.debug("run");
        sConnection.connect(mHost, mPort, mPassword);
        sConnection.run();
        Assert.assertFalse(sConnection.isConnected());
    }

    /**
     * Test of stopVpn method, of class Connection.
     */
    @Test
    public void testStopVpn() throws IOException {
        LOGGER.debug("stopVpn");
        sConnection.connect(mHost, mPort, mPassword);
        Assert.assertTrue(sConnection.isConnected());
        sConnection.managementCommand(String.format(Locale.ROOT, Commands.SIGNAL_COMMAND, Commands.ARG_SIGTERM));
    }
}
