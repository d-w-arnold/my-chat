package com.mindlinksoft.recruitment.mychat;

/**
 * Represents the configuration for the exporter.
 */
public final class ConversationExporterConfiguration
{
    // The input file path.
    private final String inputFilePath;
    // The output file path.
    private final String outputFilePath;
    // Filter by a specific user.
    private final String user;
    // Filter by a specific keyword.
    private final String keyword;
    // Hide specific words.
    private final String[] wordsToHide;

    /**
     * Initializes a new instance of the {@link ConversationExporterConfiguration} class.
     *
     * @param inputFilePath  The input file path.
     * @param outputFilePath The output file path.
     */
    public ConversationExporterConfiguration(String inputFilePath, String outputFilePath, String user, String keyword, String[] wordsToHide)
    {
        this.inputFilePath = inputFilePath;
        this.outputFilePath = outputFilePath;
        this.user = user.toLowerCase();
        this.keyword = keyword;
        this.wordsToHide = wordsToHide;
    }

    public String getInputFilePath()
    {
        return inputFilePath;
    }

    public String getOutputFilePath()
    {
        return outputFilePath;
    }

    public String getUser()
    {
        return user;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public String[] getWordsToHide()
    {
        return wordsToHide;
    }
}
