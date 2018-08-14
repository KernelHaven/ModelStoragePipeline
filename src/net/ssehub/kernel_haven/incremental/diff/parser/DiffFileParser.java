package net.ssehub.kernel_haven.incremental.diff.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ssehub.kernel_haven.incremental.diff.parser.FileEntry.Lines;
import net.ssehub.kernel_haven.incremental.diff.parser.FileEntry.VariabilityChange;
import net.ssehub.kernel_haven.util.Logger;

/**
 * {@link net.ssehub.kernel_haven.incremental.diff.parser.DiffFileParser} is
 * used to extract information about changed lines from a given git diff file.
 * 
 * @author Moritz
 */
public class DiffFileParser {

    /** The Constant DIFF_START_PATTERN. */
    private static final String DIFF_START_PATTERN = "diff --git ";

    /** The Constant LINE_NUMBER_MATCH_PATTERN. */
    private static final String LINE_NUMBER_MATCH_PATTERN =
        "@@\\s*-(\\d*),?\\d*\\s*\\+\\d*,?\\d*\\s*@@(.*)";

    private static final Logger LOGGER = Logger.get();

    /**
     * Instantiates a new
     * {@link net.ssehub.kernel_haven.incremental.diff.parser.DiffFileParser}
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public DiffFileParser() {

    }

    /**
     * Parses the lines.
     *
     * @param commitFile
     *            the commit file
     * @param ignorePaths
     *            the ignore paths
     * @param fileInclusionRegex
     *            the file inclusion regex
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public DiffFile parse(File commitFile) {
        DiffFile diffFile = null;
        try (BufferedReader br =
            new BufferedReader(new FileReader(commitFile))) {

            String currentLine = br.readLine();
            String nextLine1 = br.readLine();
            String nextLine2 = br.readLine();
            String nextLine3 = br.readLine();

            Collection<FileEntry> fileEntries = new ArrayList<FileEntry>();

            while (nextLine1 != null) {
                // only start processing diff
                if (currentLine.startsWith(DIFF_START_PATTERN)
                    && !nextLine3.startsWith("GIT binary patch")) {
                    String filePathString = currentLine.substring(
                        currentLine.indexOf("a/") + "a/".length(),
                        currentLine.indexOf(" b/"));
                    FileEntry.Type type;

                    Path filePath = Paths.get(filePathString);

                    if (nextLine1.startsWith("new file mode")) {
                        type = FileEntry.Type.ADDITION;
                    } else if (nextLine1.startsWith("deleted file mode")) {
                        type = FileEntry.Type.DELETION;
                        if (filePath == null) {
                            Logger.get().logDebug(
                                "Deletion with no filepath : ", currentLine,
                                nextLine1);
                        }
                    } else {
                        type = FileEntry.Type.MODIFICATION;
                    }

                    // Skip ahead until next diffEntry or end of file.
                    // build string with lines to parse line-info from.
                    boolean changeBlockFinished = false;
                    StringJoiner changeBlock = new StringJoiner("\n");
                    while (!changeBlockFinished) {
                        changeBlock.add(currentLine);
                        if (nextLine1 != null
                            && !nextLine1.startsWith(DIFF_START_PATTERN)) {
                            currentLine = nextLine1;
                            nextLine1 = nextLine2;
                            nextLine2 = nextLine3;
                            nextLine3 = br.readLine();
                        } else {
                            changeBlockFinished = true;
                        }
                    }

                    List<Lines> lines =
                        parseChangeBlock(changeBlock.toString());
                    fileEntries.add(new FileEntry(filePath, type,
                        VariabilityChange.NOT_ANALYZED, lines));

                }
                currentLine = nextLine1;
                nextLine1 = nextLine2;
                nextLine2 = nextLine3;
                nextLine3 = br.readLine();
                diffFile = new DiffFile(fileEntries);

            }

        } catch (IOException exc) {
            LOGGER.logException("Could not parse git diff file", exc);
            diffFile = null;
        }
        return diffFile;
    }

    /**
     * Parses the change block.
     *
     * @param string
     *            the string
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    // CHECKSTYLE:OFF
    private List<Lines> parseChangeBlock(String string) throws IOException {
        // CHECKSTYLE:ON
        BufferedReader bufReader = new BufferedReader(new StringReader(string));

        List<Lines> lineChanges = new ArrayList<Lines>();
        String currentLine = null;
        bufReader = new BufferedReader(new StringReader(string));
        Lines.LineType type = null;
        StringJoiner chunkContent = new StringJoiner("\n");
        int typeCounter = 0;

        int endOfChunk = 1;
        boolean firstChunkFound = false;
        while ((currentLine = bufReader.readLine()) != null) {
            // start with first chunk describing line changes, skip until then
            if (!firstChunkFound && !currentLine.startsWith("@@")) {
                continue;
            } else {
                firstChunkFound = true;
            }

            if (type == null) {
                type = Lines.LineType.UNMODIFIED;
            }
            if (currentLine.startsWith("+")) {
                if (!type.equals(Lines.LineType.ADDED)) {
                    lineChanges.add(
                        new Lines(type, typeCounter, chunkContent.toString()));
                    chunkContent = new StringJoiner("\n");
                    typeCounter = 0;
                    type = Lines.LineType.ADDED;
                }
                chunkContent.add(currentLine.substring(1));
                typeCounter++;
            } else if (currentLine.startsWith("-")) {
                if (!type.equals(Lines.LineType.DELETED)) {
                    lineChanges.add(
                        new Lines(type, typeCounter, chunkContent.toString()));
                    chunkContent = new StringJoiner("\n");
                    typeCounter = 0;
                    type = Lines.LineType.DELETED;
                }
                chunkContent.add(currentLine.substring(1));
                endOfChunk++;
                typeCounter++;
            } else if (currentLine.startsWith(" ")) {
                if (!type.equals(Lines.LineType.UNMODIFIED)) {
                    lineChanges.add(
                        new Lines(type, typeCounter, chunkContent.toString()));
                    chunkContent = new StringJoiner("\n");
                    typeCounter = 0;
                    type = Lines.LineType.UNMODIFIED;
                }
                chunkContent.add(currentLine.substring(1));
                endOfChunk++;
                typeCounter++;
            } else if (currentLine.startsWith("@@")) {
                // This handles lines starting with @@
                // Lines starting with @@ mark the start of a new block of
                // changes / chunk

                // add the previously collected Lines to the list
                if (typeCounter > 0) {
                    lineChanges.add(
                        new Lines(type, typeCounter, chunkContent.toString()));
                    chunkContent = new StringJoiner("\n");
                }

                // Find the start of the new block of changes / chunk
                Pattern pattern = Pattern.compile(LINE_NUMBER_MATCH_PATTERN);
                Matcher matcher = pattern.matcher(currentLine);
                matcher.find();
                String numberString = matcher.group(1);
                int startNewChunk = 1;
                if (!numberString.isEmpty()) {
                    startNewChunk = Integer.parseInt(matcher.group(1));
                }

                // Add the space between the previous block of changes / between
                // chunks
                if (startNewChunk - endOfChunk != 0) {
                    lineChanges.add(new Lines(Lines.LineType.BETWEEN_CHUNKS,
                        startNewChunk - endOfChunk, chunkContent.toString()));
                }
                chunkContent = new StringJoiner("\n");

                // Reset the end of chunk. endOfChunk will be modified while
                // processing the current chunk so that it matches
                // the actual end of the chunk when the next line starting with
                // @@ is found.
                endOfChunk = startNewChunk;

                typeCounter = 0;
                type = Lines.LineType.UNMODIFIED;
                if (!matcher.group(2).isEmpty()) {
                    chunkContent.add(matcher.group(2));
                }
            }
        }
        if (typeCounter > 0) {
            lineChanges
                .add(new Lines(type, typeCounter, chunkContent.toString()));
        }
        bufReader.close();
        return lineChanges;
    }

}