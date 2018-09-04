import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import net.ssehub.kernel_haven.incremental.diff.parser.FileEntry.FileChange;
import net.ssehub.kernel_haven.incremental.util.PosixUtil;
 * used to extract change information from a given git diff file.
    private static final String LINE_NUMBER_MATCH_PATTERN = "@@\\s*-(\\d*),?\\d*\\s*\\+\\d*,?\\d*\\s*@@(.*)";

    /** The Constant GIT_BINARY_PATCH_START_PATTERN. */
    private static final String GIT_BINARY_PATCH_START_PATTERN = "GIT binary patch";
     * Blocks public constructor access.
     */
    private DiffFileParser() {

    }

    /**
     * Parses a given git diff file to a
     * {@link net.ssehub.kernel_haven.incremental.diff.parser.DiffFile} object.
     * Unifies merges consisting of a deletion entry and a addition entry into a
     * single merge entry. Skips entries for binary patches.
     * @param commitFile the commit file
        try (BufferedReader br = new BufferedReader(new FileReader(commitFile))) {
            // This is instanciated as a list because sometimes a modification is
            // split into deletion and subsequent addition of a file within a diff file.
            // To cover this case we check (and if needed modify)
            // the previous entry which requires an ordered list.
            List<FileEntry> fileEntries = new ArrayList<>();
            while (nextLine1 != null && currentLine != null) {

                // find beginnning of file change description within diff file, skip binary
                // patches
                // CHECKSTYLE:OFF
                if (currentLine.startsWith(DIFF_START_PATTERN) && nextLine3 != null
                        && !nextLine3.startsWith(GIT_BINARY_PATCH_START_PATTERN) && nextLine2 != null
                        && !nextLine2.startsWith(GIT_BINARY_PATCH_START_PATTERN)
                        && !nextLine1.startsWith(GIT_BINARY_PATCH_START_PATTERN)) {
                    // CHECKSTYLE:ON
                    Set<PosixFilePermission> permissions = new HashSet<>();
                    String filePathString = currentLine.substring(currentLine.indexOf("a/") + "a/".length(),
                            currentLine.indexOf(" b/"));
                    LOGGER.logDebug("Parsing entry for file " + filePathString + " from diff file.");
                        String posixFlag = nextLine1.substring("new file mode 10".length());
                        permissions.addAll(PosixUtil.getPosixFilePermissionForNumberString(posixFlag));
                        String posixFlag = nextLine1.substring("deleted file mode 10".length());
                        permissions.addAll(PosixUtil.getPosixFilePermissionForNumberString(posixFlag));
                    } else if (nextLine1.startsWith("index")) {
                        type = FileEntry.FileChange.MODIFICATION;
                        Pattern modificationPosisxFlagPattern = Pattern.compile("index\\s.+\\.+\\S+\\s10(\\d*)");
                        Matcher matcher = modificationPosisxFlagPattern.matcher(nextLine1);
                        if (matcher.find()) {
                            String posixFlag = matcher.group(1);
                            permissions.addAll(PosixUtil.getPosixFilePermissionForNumberString(posixFlag));
                        } else {
                            LOGGER.logError(
                                    "Failed to get posix-flag for file " + filePathString + " in line:" + nextLine1);
                        LOGGER.logWarning("Unusual pattern in entry for file " + filePathString + ":" + nextLine1);
                        if (nextLine1 != null && !nextLine1.startsWith(DIFF_START_PATTERN)) {
                    List<Lines> lines = parseChangeBlock(changeBlock.toString());

                    FileEntry previousEntry = null;
                    if (!fileEntries.isEmpty()) {
                        previousEntry = fileEntries.get(fileEntries.size() - 1);
                    }
                    if (type.equals(FileChange.ADDITION) && previousEntry != null
                            && previousEntry.getType().equals(FileChange.DELETION)
                            && previousEntry.getPath().equals(filePath)) {
                        previousEntry.setType(FileChange.MODIFICATION);
                        previousEntry.setPermissions(permissions);
                        previousEntry.addLines(lines);
                        previousEntry.setNoNewLineAtEndOfFile(
                                changeBlock.toString().endsWith("\\ No newline at end of file"));
                    } else {
                        FileEntry entry = new FileEntry(filePath, type, VariabilityChange.NOT_ANALYZED, lines,
                                permissions);
                        entry.setNoNewLineAtEndOfFile(changeBlock.toString().endsWith("\\ No newline at end of file"));
                        fileEntries.add(entry);
                    }
     * @param string the string
     * @throws IOException Signals that an I/O exception has occurred.
    private static List<Lines> parseChangeBlock(String string) throws IOException {
        List<Lines> lineChanges = new ArrayList<>();

                    if (typeCounter > 0) {
                        lineChanges.add(new Lines(type, typeCounter, chunkContent.toString()));
                    }
                    if (typeCounter > 0) {
                        lineChanges.add(new Lines(type, typeCounter, chunkContent.toString()));
                    }
                    if (typeCounter > 0) {
                        lineChanges.add(new Lines(type, typeCounter, chunkContent.toString()));
                    }
                    lineChanges.add(new Lines(type, typeCounter, chunkContent.toString()));
                    lineChanges.add(new Lines(Lines.LineType.BETWEEN_CHUNKS, startNewChunk - endOfChunk,
                            chunkContent.toString()));
                    typeCounter++;
            lineChanges.add(new Lines(type, typeCounter, chunkContent.toString()));
