package com.getsixtyfour.openvpnmgmt.cli;

import com.getsixtyfour.openvpnmgmt.api.Connection;
import com.getsixtyfour.openvpnmgmt.api.Status;
import com.getsixtyfour.openvpnmgmt.net.ManagementConnection;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Maksym Shkolnyi aka maskimko
 * @author 1951FDG
 */

@SuppressWarnings({ "UtilityClass", "CallToSystemExit", "UseOfSystemOutOrSystemErr" })
public final class StatusCli {

    @NonNls
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusCli.class);

    private static final String HELP = "help";

    private static final String HOST = "host";

    private static final String PORT = "port";

    private StatusCli() {
    }

    public static void main(@NotNull String[] args) {
        String host = "";
        Integer port = 0;
        try {
            @NonNls Options opts = new Options();
            opts.addOption("H", HOST, true, "OpenVPN server management interface address");
            opts.addOption("P", PORT, true, "Management interface port");
            opts.addOption("h", HELP, false, "Print usage information");
            CommandLineParser parser = new DefaultParser();
            @NonNls CommandLine cmd = parser.parse(opts, args);
            if (cmd.hasOption(HELP)) {
                printInfo(opts);
                System.exit(0);
            }
            if (!cmd.hasOption(HOST) || !cmd.hasOption(PORT)) {
                printInfo(opts);
                LOGGER.error("Missing required options");
                System.exit(1);
            }
            host = cmd.getOptionValue(HOST);
            port = Integer.valueOf(cmd.getOptionValue(PORT));
        } catch (ParseException e) {
            LOGGER.error("Cannot parse arguments", e);
        }
        Connection connection = ManagementConnection.getInstance();
        connection.connect(host, port);
        Status status = connection.getVpnStatus();
        connection.disconnect();
        System.out.println("OpenVPN status: " + status); //NON-NLS
    }

    private static void printInfo(Options o) {
        @NonNls HelpFormatter hf = new HelpFormatter();
        hf.printHelp("java -jar StatusCli [options]", o);
    }
}
