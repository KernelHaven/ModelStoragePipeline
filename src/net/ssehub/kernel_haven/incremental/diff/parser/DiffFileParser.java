// TODO: Auto-generated Javadoc
    /** The Constant LOGGER. */
     * @return the diff file
    // CHECKSTYLE:OFF
    public static DiffFile parse(File commitFile) {
        // CHECKSTYLE:ON
            // Reading four lines in total, as the fourth line contains
            // information on whether the diff for the file was a binary diff
            // An outer loop finds the beginning of entries within the diff file
            // An inner loop then processes the entries to extract information
            // about which lines changed within the file described bty the entry
                    && nextLine3 != null
                    FileEntry.FileChange type;
                        type = FileEntry.FileChange.ADDITION;
                        type = FileEntry.FileChange.DELETION;
                        type = FileEntry.FileChange.MODIFICATION;
     * @return the list
    private static List<Lines> parseChangeBlock(String string)
        throws IOException {