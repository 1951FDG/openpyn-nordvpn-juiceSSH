/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.getsixtyfour.openvpnmgmt.cli;

import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.exceptions.OpenVpnParseException;
import com.getsixtyfour.openvpnmgmt.net.Connection;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NonNls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author Maksym Shkolnyi aka maskimko
 */

@SuppressWarnings({ "UtilityClass", "CallToSystemExit", "UseOfSystemOutOrSystemErr" })
public final class StatusCli {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCli.class);

    private StatusCli() {
    }

    public static void main(String[] args) {
        String host = "";
        Integer port = 0;
        try {
            @NonNls Options opts = new Options();
            opts.addOption("H", "host", true, "OpenVPN server management interface address");
            opts.addOption("P", "port", true, "Management interface port");
            opts.addOption("h", "help", false, "Print usage information");
            CommandLineParser parser = new DefaultParser();
            @NonNls CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption("help")) {
                printInfo(opts);
                System.exit(0);
            }
            if (!cmd.hasOption("host") || !cmd.hasOption("port")) {
                printInfo(opts);
                LOGGER.error("Missing required options");
                System.exit(1);
            }
            host = cmd.getOptionValue("host");
            port = Integer.valueOf(cmd.getOptionValue("port"));
        } catch (ParseException ex) {
            LOGGER.error("Cannot parse arguments", ex);
        }
        try {
            Connection conn = ManagementConnection.getInstance();
            conn.connect(host, port);
            Status status = conn.getOpenVPNStatus();
            conn.disconnect();
            System.out.println("OpenVPN status: " + status); //NON-NLS
        } catch (OpenVpnParseException | IOException ex) {
            LOGGER.error("Cannot get OpenVPN status.", ex);
        }
    }

    private static void printInfo(Options o) {
        @NonNls HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar StatusCli [options]", o);
    }
}
