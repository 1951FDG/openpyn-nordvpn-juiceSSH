package com.getsixtyfour.openvpnmgmt.api;

import com.getsixtyfour.openvpnmgmt.net.Commands;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.utils.ConnectionUtils;

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

    private static @NotNull Connection sConnection;

    private static @NotNull String sHost;

    private static @NotNull Integer sPort;

    private static @NotNull char[] sPassword;

    @BeforeClass
    public static void oneTimeSetUp() {
        @NonNls ResourceBundle mBundle = ResourceBundle.getBundle("test"); //NON-NLS
        sHost = mBundle.getString("management.server");
        sPort = Integer.valueOf(mBundle.getString("management.port"));
        sPassword = mBundle.getString("management.password").toCharArray();
    }

    @Before
    public void setUp() {
        sConnection = ManagementConnection.getInstance();
    }

    @After
    public void tearDown() {
        sConnection.disconnect();
    }

    /**
     * Test of connect method, of class Connection.
     */
    @Test
    public void testConnect() {
        LOGGER.debug("connect");
        sConnection.connect(sHost, sPort, sPassword);
        Assert.assertTrue(sConnection.isConnected());
        sConnection.run();
    }

    /**
     * Test of executeCommand method, of class Connection.
     */
    @Test
    public void testExecuteCommand() throws IOException {
        LOGGER.debug("executeCommand");
        sConnection.connect(sHost, sPort, sPassword);
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
    public void testGetManagementVersion() {
        LOGGER.debug("getManagementVersion");
        sConnection.connect(sHost, sPort, sPassword);
        String result = ConnectionUtils.getManagementVersion(sConnection);
        Assert.assertNotEquals("", result);
        LOGGER.debug(result);
    }

    /**
     * Test of getVpnStatus method, of class Connection.
     */
    @Test
    public void testGetVpnStatus() {
        LOGGER.debug("getVpnStatus");
        sConnection.connect(sHost, sPort, sPassword);
        Status result = ConnectionUtils.getVpnStatus(sConnection);
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getUpdateTime());
        List<Client> clientList = result.getClients();
        Assert.assertFalse(clientList.isEmpty());
        Set<Route> routes = result.getRoutes();
        Assert.assertFalse(routes.isEmpty());
        LOGGER.debug(result.toString());
        LOGGER.debug(clientList.toString());
    }

    /**
     * Test of getVpnVersion method, of class Connection.
     */
    @Test
    public void testGetVpnVersion() {
        LOGGER.debug("getVpnVersion");
        sConnection.connect(sHost, sPort, sPassword);
        String result = ConnectionUtils.getVpnVersion(sConnection);
        Assert.assertNotEquals("", result);
        LOGGER.debug(result);
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
        sConnection.connect(sHost, sPort, sPassword);
        sConnection.run();
        Assert.assertFalse(sConnection.isConnected());
    }

    /**
     * Test of stopVpn method, of class Connection.
     */
    @Test
    public void testStopVpn() throws IOException {
        LOGGER.debug("stopVpn");
        sConnection.connect(sHost, sPort, sPassword);
        Assert.assertTrue(sConnection.isConnected());
        sConnection.managementCommand(String.format(Locale.ROOT, Commands.SIGNAL_COMMAND, Commands.ARG_SIGTERM));
    }
}
