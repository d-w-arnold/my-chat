package com.mindlinksoft.recruitment.mychat;

import com.mindlinksoft.recruitment.mychat.constructs.Conversation;
import com.mindlinksoft.recruitment.mychat.constructs.ConversationExporterConfiguration;
import com.mindlinksoft.recruitment.mychat.constructs.Message;

import java.io.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a conversation exporter that can read a conversation and write it out in JSON.
 */
public class ConversationExporter
{
    // The messages in the conversation.
    private final List<Message> messages;
    // The configuration for the exporter.
    private ConversationExporterConfiguration config;

    /**
     * Initializes a new instance of the {@link ConversationExporter} class.
     */
    public ConversationExporter()
    {
        messages = new ArrayList<>();
    }

    /**
     * The application entry point.
     *
     * @param args The command line arguments.
     * @throws Exception Thrown when something bad happens.
     */
    public static void main(String[] args) throws Exception
    {
        ConversationExporter exporter = new ConversationExporter();
        ConversationExporterConfiguration config = new CommandLineArgumentParser().parseCommandLineArguments(args);
        exporter.exportConversation(config);
    }

    /**
     * Exports the conversation at {@code inputFilePath} as JSON to {@code outputFilePath}.
     *
     * @param conversationExporterConfiguration The configuration for the exporter.
     * @throws Exception Thrown when something bad happens.
     */
    public void exportConversation(ConversationExporterConfiguration conversationExporterConfiguration) throws Exception
    {
        config = conversationExporterConfiguration;
        Conversation conversation = readConversation(config.getInputFilePath());
        writeConversation(conversation, config.getOutputFilePath());
        writeToLog();
    }

    /**
     * Represents a helper to read a conversation from the given {@code inputFilePath}.
     *
     * @param inputFilePath The path to the input file.
     * @return The {@link Conversation} represented by the input file.
     * @throws Exception Thrown when something bad happens.
     */
    public Conversation readConversation(String inputFilePath) throws Exception
    {
        // TODO: Fix handling keyword filters and blacklisted words with apostrophes (e.g. I'm)
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath)))) {
            String conversationName = r.readLine();
            String line;
            if (config.getUser() != null && config.getKeyword() != null) {
                while ((line = r.readLine()) != null) {
                    filterUserKeyword(line);
                }
            } else if (config.getUser() != null) {
                while ((line = r.readLine()) != null) {
                    filterUser(line);
                }
            } else if (config.getKeyword() != null) {
                while ((line = r.readLine()) != null) {
                    filterKeyword(line);
                }
            } else {
                while ((line = r.readLine()) != null) {
                    filterNone(line);
                }
            }
            if (config.getWordsToHide() != null) {
                for (Message m : messages) {
                    m.setContent(redactWords(m.getContent()));
                }
            }
            return new Conversation(conversationName, messages);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Input file path argument: '" + inputFilePath + "', could not be found. More details:" + e.getCause());
        } catch (IOException e) {
            throw new IOException("Input file path argument: '" + inputFilePath + "', could not be read from. More details: " + e.getCause());
        }
    }

    /**
     * Filter by both {@code user} and {@code keyword}.
     *
     * @param line The {@link Message} read in from the input file.
     */
    private void filterUserKeyword(String line)
    {
        String[] split = line.split(config.getSEP_REGEX());
        String content = String.join(config.getSEP_JOIN(), getContentSplit(split));
        if (config.getUser().equals(split[1].toLowerCase()) && containsKeyword(content)) {
            messages.add(new Message(Instant.ofEpochSecond(Long.parseUnsignedLong(split[0])), split[1], content));
        }
    }

    /**
     * Filter messages by {@code user}.
     *
     * @param line The {@link Message} read in from the input file.
     */
    private void filterUser(String line)
    {
        String[] split = line.split(config.getSEP_REGEX());
        String content = String.join(config.getSEP_JOIN(), getContentSplit(split));
        if (config.getUser().equals(split[1].toLowerCase())) {
            messages.add(new Message(Instant.ofEpochSecond(Long.parseUnsignedLong(split[0])), split[1], content));
        }
    }

    /**
     * Filter messages just by {@code keyword}.
     *
     * @param line The {@link Message} read in from the input file.
     */
    private void filterKeyword(String line)
    {
        String[] split = line.split(config.getSEP_REGEX());
        String content = String.join(config.getSEP_JOIN(), getContentSplit(split));
        if (containsKeyword(content)) {
            messages.add(new Message(Instant.ofEpochSecond(Long.parseUnsignedLong(split[0])), split[1], content));
        }
    }

    /**
     * No filter for user or keyword.
     *
     * @param line The {@link Message} read in from the input file.
     */
    private void filterNone(String line)
    {
        String[] split = line.split(config.getSEP_REGEX());
        String content = String.join(config.getSEP_JOIN(), getContentSplit(split));
        messages.add(new Message(Instant.ofEpochSecond(Long.parseUnsignedLong(split[0])), split[1], content));
    }

    /**
     * Redact any words which appear in {@code wordsToHide} (blacklisted words)
     * in the {@code content} of a {@link Message}.
     *
     * @param content The {@code content} of the {@link Message}.
     * @return A string representing the new redacted message content.
     */
    private String redactWords(String content)
    {
        String[] wordsOrig = content.split(config.getSEP_REGEX());
        String[] words = content.replaceAll(config.getLETTERS_AND_SPACES(), "").toLowerCase().split(config.getSEP_REGEX());
        for (int i = 0; i < words.length; i++) {
            for (String w : config.getWordsToHide()) {
                if (words[i].equals(w)) {
                    wordsOrig[i] = wordsOrig[i].replaceAll(String.format("(?i)%s", Pattern.quote(words[i])), config.getREDACT());
                    break;
                } else if (checkNonLetterSplit(wordsOrig[i], w)) {
                    String[] subComponents = wordsOrig[i].split(config.getLETTERS_AND_SPACES());
                    for (String subComponent : subComponents) {
                        if (subComponent.equals(w)) {
                            wordsOrig[i] = wordsOrig[i].replaceAll(String.format("(?i)%s", Pattern.quote(subComponent)), config.getREDACT());
                        }
                    }
                }
            }
        }
        return String.join(config.getSEP_JOIN(), wordsOrig);
    }

    /**
     * Get the entire {@code content} of the {@link Message}.
     * Using all entries in {@code split}, except for the first {@code startingIndex} entries, separated by {@code sep}.
     *
     * @param split The read in line from the {@code inputFilePath}.
     * @return A string representing the {@code content} of the {@link Message}.
     */
    private String[] getContentSplit(String[] split)
    {
        String[] contentSplit = new String[split.length - config.getCONTENT_START_INDEX()];
        if (contentSplit.length >= 0) {
            System.arraycopy(split, config.getCONTENT_START_INDEX(), contentSplit, 0, contentSplit.length);
        }
        return contentSplit;
    }

    /**
     * Check to see if the {@code content} of a {@link Message} contains a given {@code keyword}.
     *
     * @param content The {@code content} of a {@link Message}.
     * @return Returns true if {@code content} contains at least one {@code keyword}.
     */
    private boolean containsKeyword(String content)
    {
        String[] wordsOrig = content.split(config.getSEP_REGEX());
        String[] words = content.replaceAll(config.getLETTERS_AND_SPACES(), "").toLowerCase().split(config.getSEP_REGEX());
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(config.getKeyword()) || checkNonLetterSplit(wordsOrig[i], config.getKeyword())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks to see if a {@code word} contains any sub words when split by non-letter characters.
     *
     * @param word    The {@code word} to split by non-letters.
     * @param keyword The {@code keyword} to match against.
     * @return Returns true if {@code word} contains a sub word when split at non-letters
     * (e.g. By splitting "there's" at any non-letters, the {@code keyword} "there" should match.
     */
    private boolean checkNonLetterSplit(String word, String keyword)
    {
        String[] subComponents = word.split(config.getLETTERS_AND_SPACES());
        if (subComponents.length > 1) {
            for (String subComponent : subComponents) {
                if (subComponent.equals(keyword)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper method to write the given {@link Conversation} as JSON to the given {@code outputFilePath}.
     *
     * @param conversation   The conversation to write.
     * @param outputFilePath The file path where the conversation should be written.
     * @throws Exception Thrown when something bad happens.
     */
    public void writeConversation(Conversation conversation, String outputFilePath) throws Exception
    {
        // If outputFilePath exists, delete the file.
        File outputFile = new File(outputFilePath);
        if (outputFile.exists() && outputFile.isFile()) {
            outputFile.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath, true)))) {
            CreateGsonBuild createGsonBuild = new CreateGsonBuild();
            bw.write(createGsonBuild.convert(conversation));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException("Output file path argument: '" + outputFilePath + "', could not be found. More details:" + e.getCause());
        } catch (IOException e) {
            throw new IOException("Output file path argument: '" + outputFilePath + "', could not be written to. More details: " + e.getCause());
        }
    }

    /**
     * Write to standard output, concerning the latest exportation to JSON.
     */
    private void writeToLog()
    {
        String msg = "Conversation exported from '" + config.getInputFilePath() + "' to '" + config.getOutputFilePath() + "' ";
        String u = config.getUser();
        String k = config.getKeyword();
        String[] w = config.getWordsToHide();
        if (u != null && k != null && w != null) {
            System.out.println(msg + "-" + printUser() + printKeyword() + printWordsToHide());
        } else if (u != null && k != null) {
            System.out.println(msg + "-" + printUser() + printKeyword());
        } else if (u != null && w != null) {
            System.out.println(msg + "-" + printUser() + printWordsToHide());
        } else if (k != null && w != null) {
            System.out.println(msg + "-" + printKeyword() + printWordsToHide());
        } else if (u != null) {
            System.out.println(msg + "-" + printUser());
        } else if (k != null) {
            System.out.println(msg + "-" + printKeyword());
        } else if (w != null) {
            System.out.println(msg + "-" + printWordsToHide());
        } else {
            System.out.println(msg);
        }
    }

    private String printUser()
    {
        return " (user: " + config.getUser() + ")";
    }

    private String printKeyword()
    {
        return " (keyword: " + config.getKeyword() + ")";
    }

    private String printWordsToHide()
    {
        return " (wordsToHide: " + Arrays.toString(config.getWordsToHide()) + ")";
    }
}
