package net.ssehub.kernel_haven.incremental.diff.parser;

import java.nio.file.Path;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Represents an entry for a file in the changeset. Used by {@link DiffFile} to
 * describe changes for a git-diff-file.
 * 
 * @author moritz
 */
public class FileEntry {

    /**
     * Type of change in terms of file operation. Changes of variability are
     * reflected by {@link VariabilityChange}
     */
    public enum FileChange {

    /** Modification of a file. */
    MODIFICATION,

    /** Addition of a file. */
    ADDITION,

    /** Deletion of a file. */
    DELETION
    }

    /**
     * The Enum VariabilityChange.
     */
    public enum VariabilityChange {

        /** Indicates changed variability. */
        CHANGE,

        /** Indicates no changed variability. */
        NO_CHANGE,

        /**
         * Indicates that the file was not considered to be a file carrying
         * variability information.
         */
        NOT_A_VARIABILITY_FILE,

        /**
         * Indicates that no analysis on variability information was performed
         * on the file represented by this {@link FileEntry}.
         */
        NOT_ANALYZED
    }

    /** The file. */
    private Path file;

    /** The lines. */
    private List<Lines> lines;

    /** The type. */
    private FileChange type;

    /** The variability change. */
    private VariabilityChange variabilityChange;

    /**
     * Instantiates a new file entry.
     *
     * @param file
     *            the file
     * @param type
     *            the type
     * @param variabilityChange
     *            the variability change
     * @param lines
     *            the lines
     */
    public FileEntry(Path file, FileChange type,
        VariabilityChange variabilityChange, List<Lines> lines) {
        this.file = file;
        this.type = type;
        this.variabilityChange = variabilityChange;
        this.lines = lines;
    }

    /**
     * Instantiates a new file entry.
     *
     * @param file
     *            the file
     * @param type
     *            the type
     */
    public FileEntry(Path file, FileChange type) {
        this.file = file;
        this.type = type;
        this.variabilityChange = VariabilityChange.NOT_ANALYZED;
    }

    /**
     * Gets the path.
     *
     * @return the path
     */
    public Path getPath() {
        return file;
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public FileChange getType() {
        return type;
    }

    /**
     * Gets the variability change.
     *
     * @return the variability change
     */
    public VariabilityChange getVariabilityChange() {
        return variabilityChange;
    }

    /**
     * Gets the lines.
     *
     * @return the lines
     */
    public List<Lines> getLines() {
        return lines;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((file == null) ? 0 : file.hashCode());
        result = prime * result + ((lines == null) ? 0 : lines.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result
            + ((variabilityChange == null) ? 0 : variabilityChange.hashCode());
        return result;
    }

    /**
     * Equals.
     *
     * @param obj
     *            the obj
     * @return true, if successful
     */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FileEntry other = (FileEntry) obj;
        if (file == null) {
            if (other.file != null) {
                return false;
            }
        } else if (!file.equals(other.file)) {
            return false;
        }
        if (lines == null) {
            if (other.lines != null) {
                return false;
            }
        } else if (!lines.equals(other.lines)) {
            return false;
        }
        if (type != other.type) {
            return false;
        }
        if (variabilityChange != other.variabilityChange) {
            return false;
        }
        return true;
    }

    /**
     * The Class Lines.
     */
    public static class Lines {
        /** The count. */
        private int count;

        /** The type. */
        private LineType type;

        /** The content. */
        private String content;

        /**
         * Instantiates a new lines.
         *
         * @param type
         *            the type
         * @param count
         *            the count
         * @param content
         *            the content
         */
        public Lines(LineType type, int count, String content) {
            this.type = type;
            this.count = count;
            this.content = content;
        }

        /**
         * Gets the content.
         *
         * @return the content
         */
        public String getContent() {
            return content;
        }

        /**
         * To string.
         *
         * @return the string
         */
        public String toString() {
            return "Lines [count=" + count + ", type=" + type + "]";
        }

        /**
         * The Enum LineType.
         */
        public enum LineType {
            /** The added. */
            ADDED,
            /** The deleted. */
            DELETED,
            /** The unmodified. */
            UNMODIFIED,
            /** The between chunks. */
            BETWEEN_CHUNKS
        }

        /**
         * Gets the type.
         *
         * @return the type
         */
        public LineType getType() {
            return type;
        }

        /**
         * Gets the count.
         *
         * @return the count
         */
        public int getCount() {
            return count;
        }

        /**
         * Hash code.
         *
         * @return the int
         */
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result =
                prime * result + ((content == null) ? 0 : content.hashCode());
            result = prime * result + count;
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        /**
         * Equals.
         *
         * @param obj
         *            the obj
         * @return true, if successful
         */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Lines other = (Lines) obj;
            if (content == null) {
                if (other.content != null) {
                    return false;
                }
            } else if (!content.equals(other.content)) {
                return false;
            }
            if (count != other.count) {
                return false;
            }
            if (type != other.type) {
                return false;
            }
            return true;
        }

    }

}
