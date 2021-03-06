package net.ssehub.kernel_haven.incremental.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Utility class for performing various actions with folders.
 * 
 * @author Moritz
 */
public class FolderUtil {

    /**
     * Hides the implicit empty constructor.
     */
    private FolderUtil() {

    }

    /**
     * Copy folder content.
     *
     * @param sourceFolder the source folder
     * @param targetFolder the target folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void copyFolderContent(File sourceFolder, File targetFolder) throws IOException {
        sourceFolder = sourceFolder.getAbsoluteFile();
        targetFolder = targetFolder.getAbsoluteFile();
        Collection<File> files = FolderUtil.listAbsoluteFilesAndFolders(sourceFolder, true);
        for (File file : files) {
            // create target path based on the relative path that the file has
            // in the source
            // folder
            Path targetPath = targetFolder.toPath().resolve(sourceFolder.toPath().relativize(file.toPath()));
            // create parent directories if they do not exist
            if (file.isDirectory()) {
                file.mkdir();
            } else {
                targetPath.toFile().getParentFile().mkdirs();
                Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Checks if folder content is equal.
     *
     * @param folderA the folder A
     * @param folderB the folder B
     * @return true, if successful
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean folderContentEquals(File folderA, File folderB) throws IOException {
        boolean equals = true;
        Collection<File> filesFolderA = FolderUtil.listAbsoluteFilesAndFolders(folderA, true);
        Collection<File> filesFolderB = FolderUtil.listAbsoluteFilesAndFolders(folderB, true);
        equals = filesFolderA.size() == filesFolderB.size();
        if (equals) {
            for (File folderAFile : folderA.listFiles()) {
                File correspondingFileInB = folderB.toPath().resolve(folderA.toPath().relativize(folderAFile.toPath()))
                        .toFile();
                equals = FileUtil.textContentIsEqual(folderAFile, correspondingFileInB);
                if (!equals) {
                    break;
                }
            }
        }
        return equals;
    }

    /**
     * Delete folder contents.
     *
     * @param folder the folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void deleteFolderContents(File folder) throws IOException {
        deleteFolder(folder, false);
    }

    /**
     * Delete folder.
     *
     * @param folder the folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void deleteFolder(File folder) throws IOException {
        deleteFolder(folder, true);
    }

    /**
     * Delete folder.
     *
     * @param folder           the folder
     * @param deleteRootFolder the delete root folder
     * @throws IOException Signals that an I/O exception has occurred.
     */
    private static void deleteFolder(File folder, boolean deleteRootFolder) throws IOException {
        Files.walk(folder.toPath()).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        if (!deleteRootFolder) {
            folder.mkdir();
        }
    }

    /**
     * Gets the new or changed files from newDirectory compared to
     * referenceDirectory. This does not list deleted files.
     *
     * @param referenceDirectory the reference directory
     * @param newDirectory       the new directory
     * @param absolutePaths      the absolute paths
     * @return the new or changed files
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Collection<File> getNewOrChangedFiles(File referenceDirectory, File newDirectory,
            boolean absolutePaths) throws IOException {
        Path referenceDirectoryPath = referenceDirectory.getAbsoluteFile().toPath();
        Path newDirectorydPath = newDirectory.getAbsoluteFile().toPath();

        // Create a list of relative Paths to all files in newDirectory
        Collection<Path> pathsForFilesInNewDirectory = new ArrayList<Path>();
        for (File file : FolderUtil.listAbsoluteFilesAndFolders(newDirectorydPath.toFile(), true)) {
            if (!file.isDirectory()) {
                pathsForFilesInNewDirectory.add(newDirectorydPath.relativize(file.toPath()));
            }
        }

        Collection<File> newOrChangedFiles = new ArrayList<File>();

        for (Path filePath : pathsForFilesInNewDirectory) {
            File fileInRefDir = referenceDirectoryPath.resolve(filePath).toFile();
            File fileInNewDir = newDirectorydPath.resolve(filePath).toFile();

            // if the file does not exist in the reference directory or the file
            // content is
            // not equal add it to the list of changed files
            if (!fileInRefDir.exists() || !FileUtil.textContentIsEqual(fileInRefDir, fileInNewDir)) {
                if (absolutePaths) {
                    newOrChangedFiles.add(fileInNewDir);
                } else {
                    newOrChangedFiles.add(filePath.toFile());
                }
            }
        }
        return newOrChangedFiles;
    }

    /**
     * List files and folders within a directory.
     *
     * @param directory                    the directory
     * @param includeFilesInSubDirectories the include files in sub directories
     * @return the collection
     */
    public static Collection<File> listAbsoluteFilesAndFolders(File directory, boolean includeFilesInSubDirectories) {
        Collection<File> files = new ArrayList<>();
        listFilesAndFolders(directory, files, includeFilesInSubDirectories, true, true, directory);
        return files;
    }

    /**
     * List relative files and folders.
     *
     * @param directory                    the directory
     * @param includeFilesInSubDirectories the include files in sub directories
     * @return the collection
     */
    public static Collection<File> listRelativeFilesAndFolders(File directory, boolean includeFilesInSubDirectories) {
        Collection<File> files = new ArrayList<>();
        listFilesAndFolders(directory, files, includeFilesInSubDirectories, true, false, directory);
        return files;
    }

    /**
     * List files and folders within a directory.
     *
     * @param directory                    the directory
     * @param includeFilesInSubDirectories the include files in sub directories
     * @return the collection
     */
    public static Collection<File> listAbsoluteFiles(File directory, boolean includeFilesInSubDirectories) {
        Collection<File> files = new ArrayList<>();
        listFilesAndFolders(directory, files, includeFilesInSubDirectories, false, true, directory);
        return files;
    }

    /**
     * List relative files.
     *
     * @param directory                    the directory
     * @param includeFilesInSubDirectories the include files in sub directories
     * @return the collection
     */
    public static Collection<File> listRelativeFiles(File directory, boolean includeFilesInSubDirectories) {
        Collection<File> files = new ArrayList<>();
        listFilesAndFolders(directory, files, includeFilesInSubDirectories, false, false, directory);
        return files;
    }

    // CHECKSTYLE:OFF
    /**
     * This method is used by listFilesAndFolders to recursively determine the
     * folder content.
     *
     * @param directory     the directory
     * @param files         the files
     * @param recursive     defines whether to process directories recursively
     * @param listFolders   defines whether to list folders
     * @param absoluteFiles defines whether to return absolute files
     * @param rootDirectory the root directory
     */
    private static void listFilesAndFolders(File directory, Collection<File> files, boolean recursive,
            boolean listFolders, boolean absoluteFiles, File rootDirectory) {
        File[] fList = directory.listFiles();
        for (File file : fList) {
            File fileToAdd = file.getAbsoluteFile();
            if (!absoluteFiles) {
                fileToAdd = rootDirectory.toPath().relativize(file.toPath()).toFile();
            }
            if (file.isFile()) {
                files.add(fileToAdd);
            } else if (file.isDirectory()) {
                if (listFolders) {
                    files.add(fileToAdd);
                }
                if (recursive) {
                    listFilesAndFolders(file, files, recursive, listFolders, absoluteFiles, rootDirectory);
                }
            }
        }
    }
    // CHECKSTYLE:ON

}
