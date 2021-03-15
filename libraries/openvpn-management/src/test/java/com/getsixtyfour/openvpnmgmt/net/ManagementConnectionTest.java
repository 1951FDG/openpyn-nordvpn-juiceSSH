package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.core.ManagementConnection;
import com.getsixtyfour.openvpnmgmt.core.UsernamePasswordHandler;
import com.getsixtyfour.openvpnmgmt.listeners.OnByteCountChangedListener;
import com.getsixtyfour.openvpnmgmt.listeners.OnRecordChangedListener;
import com.getsixtyfour.openvpnmgmt.model.OpenVpnLogRecord;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.junit.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion", "MigrateAssertToMatcherAssert" })
public class ManagementConnectionTest {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnectionTest.class);

    private static @NotNull ManagementConnection sConnection;

    private static void invokeParseInput(String line) throws InvocationTargetException {
        Class<ManagementConnection> targetClass = ManagementConnection.class;
        Class[] argClasses = { String.class };
        Object[] argObjects = { line };
        @NonNls String methodName = "parseInput";
        invokeStaticMethod(targetClass, methodName, argClasses, argObjects);
    }

    @SuppressWarnings({ "TryWithIdenticalCatches", "StaticVariableUsedBeforeInitialization" })
    private static void invokeStaticMethod(@NotNull Class<?> targetClass, @NotNull String methodName, Class[] argClasses,
                                           Object[] argObjects) throws InvocationTargetException {
        try {
            Method method = targetClass.getDeclaredMethod(methodName, argClasses);
            method.setAccessible(true);
            method.invoke(sConnection, argObjects);
        } catch (NoSuchMethodException e) {
            // Should happen only rarely, because most times the specified method should exist.
            // If it does happen, just let the test fail so the programmer can fix the problem.
            throw new AssertionError(e);
        } catch (SecurityException e) {
            // Should happen only rarely, because the setAccessible(true) should be allowed in when running unit tests.
            // If it does happen, just let the test fail so the programmer can fix the problem.
            throw new AssertionError(e);
        } catch (IllegalAccessException e) {
            // Should never happen, because setting accessible flag to true.
            // If setting accessible fails, should throw a security exception at that point and never get to the invoke.
            // But just in case, wrap it in a AssertionError and let a human figure it out.
            throw new AssertionError(e);
        } catch (IllegalArgumentException e) {
            // Should happen only rarely, because usually the right number and types of arguments will be passed.
            // If it does happen, just let the test fail so the programmer can fix the problem.
            throw new AssertionError(e);
        }
    }

    @Before
    public void setUp() {
        sConnection = ManagementConnection.getInstance();
    }

    /**
     * Test of processByteCount method, of class ManagementConnection.
     */
    @SuppressWarnings("Convert2Lambda")
    @Test
    public void testProcessByteCount() throws InvocationTargetException {
        sConnection.addOnByteCountChangedListener(new OnByteCountChangedListener() {
            @Override
            public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
                Assert.assertEquals(1L, in);
                Assert.assertEquals(1L, out);
                Assert.assertEquals(1L, diffIn);
                Assert.assertEquals(1L, diffOut);
            }
        });
        @NonNls String line = ">BYTECOUNT:1,1";
        invokeParseInput(line);
    }

    /**
     * Test of processLog method, of class ManagementConnection.
     */
    @SuppressWarnings("Convert2Lambda")
    @Test
    public void testProcessLog() throws InvocationTargetException {
        /*
         * (a) unix integer date/time
         * (b) zero or more message flags in a single string
         * (c) message text
         */
        {
            OnRecordChangedListener listener = new OnRecordChangedListener() {
                @Override
                public void onRecordChanged(@NotNull OpenVpnLogRecord record) {
                    Assert.assertEquals(LogLevel.VERBOSE, record.getLevel());
                }
            };
            sConnection.addOnRecordChangedListener(listener);
            @NonNls String line = ">LOG:,,";
            invokeParseInput(line);
            sConnection.removeOnRecordChangedListener(listener);
        }
        {
            OnRecordChangedListener listener = new OnRecordChangedListener() {
                @Override
                public void onRecordChanged(@NotNull OpenVpnLogRecord record) {
                    Assert.assertEquals(LogLevel.ERROR, record.getLevel());
                    LOGGER.error(record.getMessage());
                }
            };
            sConnection.addOnRecordChangedListener(listener);
            @NonNls String line = ">LOG:,N,event_wait";
            invokeParseInput(line);
            sConnection.removeOnRecordChangedListener(listener);
        }
    }

    /**
     * Test of processPassword method, of class ManagementConnection.
     */
    @SuppressWarnings("ReuseOfLocalVariable")
    @Test(expected = IOException.class)
    public void testProcessPassword() throws InvocationTargetException, IOException {
        @NonNls String line = ">PASSWORD:Auth-Token:";
        invokeParseInput(line);
        line = ">PASSWORD:Verification Failed: 'Auth'";
        invokeParseInput(line);
        line = ">PASSWORD:Verification Failed: 'Private Key'";
        invokeParseInput(line);
        line = ">PASSWORD:Need 'Auth' username/password";
        sConnection.setUsernamePasswordHandler(new UsernamePasswordHandler() {
            @Nullable
            @Override
            public String getUser() {
                return null;
            }

            @Nullable
            @Override
            public String getPassword() {
                return null;
            }
        });
        try {
            invokeParseInput(line);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof IOException) {
                throw (IOException) targetException;
            }
            throw e;
        }
    }

    /**
     * Test of processState method, of class ManagementConnection.
     */
    @SuppressWarnings("ReuseOfLocalVariable")
    @Test
    public void testProcessState() throws InvocationTargetException {
        /*
         * (a) the integer unix date/time
         * (b) the state name
         * (c) optional descriptive string (used mostly on RECONNECTING and EXITING to show the reason for the disconnect)
         * (d) optional TUN/TAP local IP address
         * (e) optional address of remote server (OpenVPN 2.1 or higher)
         * (f) optional port of remote server (OpenVPN 2.4 or higher)
         * (g) optional local address (OpenVPN 2.4 or higher)
         * (h) optional local port (OpenVPN 2.4 or higher)
         * (i) optional TUN/TAP local IPv6 address (OpenVPN 2.4 or higher)
         *
         * (e)-(h) are shown for CONNECTED state
         * (d) and (i) are shown for ASSIGN_IP and CONNECTED states
         */
        @NonNls String line = ">STATE:,CONNECTED,SUCCESS,,,,,";
        invokeParseInput(line);
        line = ">STATE:,WAIT,,,,,,";
        invokeParseInput(line);
        line = ">STATE:,AUTH,,,,,,";
        invokeParseInput(line);
    }
}