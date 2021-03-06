/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.net;

import com.getsixtyfour.openvpnmgmt.core.LogLevel;
import com.getsixtyfour.openvpnmgmt.listeners.ByteCountManager.ByteCountListener;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.Log;
import com.getsixtyfour.openvpnmgmt.listeners.LogManager.LogListener;

import junit.framework.AssertionFailedError;

import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 1951FDG
 */

@SuppressWarnings({ "JUnitTestNG", "MessageMissingOnJUnitAssertion" })
public class ManagementConnectionTest {

    private static ManagementConnection connection = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagementConnectionTest.class);

    @BeforeClass
    public static void setUpClass() {
        connection = ManagementConnection.getInstance();
    }

    /**
     * Test of processByteCount method, of class ManagementConnection.
     */
    @Test
    public void testProcessByteCount() throws InvocationTargetException {
        connection.addByteCountListener(new ByteCountListener() {
            @Override
            public void onByteCountChanged(long in, long out, long diffIn, long diffOut) {
                Assert.assertEquals(1L, in);
                Assert.assertEquals(1L, out);
                Assert.assertEquals(1L, diffIn);
                Assert.assertEquals(1L, diffOut);
            }
        });
        String line = ">BYTECOUNT:1,1";
        invokeParseInput(line);
    }

    /**
     * Test of processLog method, of class ManagementConnection.
     */
    @Test
    public void testProcessLog() throws InvocationTargetException {
        /*
         * (a) unix integer date/time
         * (b) zero or more message flags in a single string
         * (c) message text
         */
        {
            LogListener listener = new LogListener() {
                @Override
                public void onLog(@NotNull Log log) {
                    Assert.assertEquals(LogLevel.VERBOSE, log.getLevel());
                }
            };
            connection.addLogListener(listener);
            String line = ">LOG:,,";
            invokeParseInput(line);
            connection.removeLogListener(listener);
        }
        {
            LogListener listener = new LogListener() {
                @Override
                public void onLog(@NotNull Log log) {
                    Assert.assertEquals(LogLevel.ERROR, log.getLevel());
                    LOGGER.error("{}", log.getMessage());
                }
            };
            connection.addLogListener(listener);
            String line = ">LOG:,N,event_wait";
            invokeParseInput(line);
            connection.removeLogListener(listener);
        }
    }

    /**
     * Test of processPassword method, of class ManagementConnection.
     */
    @SuppressWarnings({ "ReuseOfLocalVariable", "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
    @Test(expected = IOException.class)
    public void testProcessPassword() throws InvocationTargetException, IOException {
        String line = ">PASSWORD:Auth-Token:";
        invokeParseInput(line);
        line = ">PASSWORD:Verification Failed: 'Auth'";
        invokeParseInput(line);
        line = ">PASSWORD:Need 'Auth' username/password";
        connection.setUsernamePasswordHandler(new UsernamePasswordHandler() {
            @NotNull
            @Override
            public String getUserName() {
                return null;
            }

            @NotNull
            @Override
            public String getUserPass() {
                return null;
            }
        });
        try {
            invokeParseInput(line);
        } catch (InvocationTargetException e) {
            // throw the InvocationTargetException unless the target
            // exception is NullPointerException, which is expected
            Throwable targetException = e.getTargetException();
            if (targetException instanceof IOException) {
                throw (IOException) targetException;
            } else {
                throw e;
            }
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
        String line = ">STATE:,CONNECTED,SUCCESS,,,,,";
        invokeParseInput(line);
        line = ">STATE:,WAIT,,,,,,";
        invokeParseInput(line);
        line = ">STATE:,AUTH,,,,,,";
        invokeParseInput(line);
    }

    private static void invokeParseInput(String line) throws InvocationTargetException {
        Class[] argClasses = { String.class };
        Object[] argObjects = { line };
        invokeStaticMethod(ManagementConnection.class, "parseInput", argClasses, argObjects);
    }

    @SuppressWarnings({ "SameParameterValue", "ThrowInsideCatchBlockWhichIgnoresCaughtException" })
    private static void invokeStaticMethod(Class<?> targetClass, String methodName, Class[] argClasses, Object[] argObjects)
            throws InvocationTargetException {
        try {
            Method method = targetClass.getDeclaredMethod(methodName, argClasses);
            method.setAccessible(true);
            method.invoke(connection, argObjects);
        } catch (NoSuchMethodException e) {
            // Should happen only rarely, because most times the
            // specified method should exist. If it does happen, just let
            // the test fail so the programmer can fix the problem.
            throw new AssertionFailedError(e.getMessage());
        } catch (SecurityException e) {
            // Should happen only rarely, because the setAccessible(true)
            // should be allowed in when running unit tests. If it does
            // happen, just let the test fail so the programmer can fix
            // the problem.
            throw new AssertionFailedError(e.getMessage());
        } catch (IllegalAccessException e) {
            // Should never happen, because setting accessible flag to
            // true. If setting accessible fails, should throw a security
            // exception at that point and never get to the invoke. But
            // just in case, wrap it in a TestFailedException and let a
            // human figure it out.
            throw new AssertionFailedError(e.getMessage());
        } catch (IllegalArgumentException e) {
            // Should happen only rarely, because usually the right
            // number and types of arguments will be passed. If it does
            // happen, just let the test fail so the programmer can fix
            // the problem.
            throw new AssertionFailedError(e.getMessage());
        }
    }
}
