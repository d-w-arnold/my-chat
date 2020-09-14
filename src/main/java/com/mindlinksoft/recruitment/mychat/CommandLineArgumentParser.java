package com.mindlinksoft.recruitment.mychat;

import com.mindlinksoft.recruitment.mychat.constructs.ConversationExporterConfiguration;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a helper to parse command line arguments.
 */
public final class CommandLineArgumentParser
{
    // The flags used on the command line.
    private final Set<String> FLAGS;
    // The index in a message split where the content starts.
    private final int CONTENT_START_INDEX;
    // The separator used when specifying multiple words to hide.
    private final String SEP_WORDS_TO_HIDE;
    // Filter messages from this user.
    private String user;
    // Filter messages containing this word.
    private String keyword;
    // List of blacklisted words which need redacting.
    private String[] wordsToHide;
    // Whether to hide credit card and phone numbers, that appear in message content.
    private boolean hideCCPN;
    // Whether to obfuscate user IDs.
    private boolean obf;
    // Whether to add a report to the conversation that details the most active users.
    private boolean report;
    // The command line arguments.
    private String[] arguments;

    /**
     * Initializes a new instance of the {@link CommandLineArgumentParser} class.
     */
    public CommandLineArgumentParser()
    {
        FLAGS = new HashSet<String>()
        {{
            add("-u");
            add("-k");
            add("-w");
            add("-hideCCPN");
            add("-obfUsers");
            add("-report");
        }};
        CONTENT_START_INDEX = 2;
        SEP_WORDS_TO_HIDE = ",";
        user = "";
        keyword = "";
        wordsToHide = new String[0];
        hideCCPN = false;
        obf = false;
        report = false;
    }

    /**
     * Parses the given {@code arguments} into the exporter configuration.
     *
     * @param arguments The command line arguments.
     * @return The exporter configuration representing the command line arguments.
     */
    public ConversationExporterConfiguration parseCommandLineArguments(String[] arguments)
    {
        this.arguments = arguments;
        for (int i = CONTENT_START_INDEX; i < this.arguments.length; ) {
            switch (arguments[i]) {
                case "-u":
                    if ((i + 1) < arguments.length && !FLAGS.contains(arguments[i + 1])) {
                        user = arguments[i + 1];
                        i += 2;
                    } else {
                        throw new IllegalArgumentException("No user specified after: " + arguments[i]);
                    }
                    break;
                case "-k":
                    if ((i + 1) < arguments.length && !FLAGS.contains(arguments[i + 1])) {
                        keyword = arguments[i + 1];
                        i += 2;
                    } else {
                        throw new IllegalArgumentException("No keyword specified after: " + arguments[i]);
                    }
                    break;
                case "-w":
                    if ((i + 1) < arguments.length && !FLAGS.contains(arguments[i + 1])) {
                        wordsToHide = arguments[i + 1].split(SEP_WORDS_TO_HIDE);
                        i += 2;
                    } else {
                        throw new IllegalArgumentException("No words to hide (blacklisted words) specified after: " + arguments[i]);
                    }
                    break;
                case "-hideCCPN":
                    hideCCPN = true;
                    i++;
                    break;
                case "-obfUsers":
                    obf = true;
                    i++;
                    break;
                case "-report":
                    report = true;
                    i++;
                    break;
                default:
                    throw new IllegalStateException("Unexpected command line parameter: " + arguments[i]);
            }
        }
        return new ConversationExporterConfiguration(arguments[0], arguments[1], user, keyword, wordsToHide, hideCCPN, obf, report);
    }
}
