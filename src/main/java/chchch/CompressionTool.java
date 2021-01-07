package chchch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class CompressionTool
{
    private static String ExtractMode_Append = "append";
    private static String ExtractMode_Wipe = "wipe";

    private static byte[] _sharedBuffer;

    private static byte[] getSharedBuffer(int size)
    {
        if (_sharedBuffer == null || _sharedBuffer.length < size) {
            _sharedBuffer = new byte[size];
        }
        return _sharedBuffer;
    }
    private static boolean includeFile(File file, boolean includeHiddenFiles, Pattern excludeByRegexpPattern)
    {
        if (!includeHiddenFiles && file.isHidden()) return false;
        return excludeByRegexpPattern == null || !excludeByRegexpPattern.matcher(file.getName()).matches();
    }
    private static boolean includeFolder(File file, File[] files, boolean includeEmptyFolders, boolean includeHiddenFiles, Pattern excludeByRegexpPattern)
    {
        if (!includeFile(file, includeHiddenFiles, excludeByRegexpPattern)) return false;
        if (!includeEmptyFolders && (files == null || files.length == 0)) return false;
        if (files == null) return false;
        for (File innerFile : files) {
            if (!includeFile(innerFile, includeHiddenFiles, excludeByRegexpPattern)) continue;
            if (innerFile.isFile()) return true;
            if (innerFile.isDirectory() && includeFolder(innerFile, innerFile.listFiles(), includeEmptyFolders, includeHiddenFiles, excludeByRegexpPattern)) return true;
        }
        return false;
    }
    private static void zipNext(boolean includeRootFolder, File file, String parentEntryName, ZipOutputStream zipOut, int bufferSize, boolean includeEmptyFolders, boolean includeHiddenFiles, Pattern excludeByRegexpPattern) throws IOException
    {
        if (!includeFile(file, includeHiddenFiles, excludeByRegexpPattern)) return;
        if (file.isDirectory()) {
            File[] innerFiles = file.listFiles();
            if (includeFolder(file, innerFiles, includeEmptyFolders, includeHiddenFiles, excludeByRegexpPattern)) {
                String entryName = Base.addTrailingSlash(Base.combinePath(parentEntryName, file.getName()));
                if (includeRootFolder) {
                    zipOut.putNextEntry(new ZipEntry(entryName));
                    zipOut.closeEntry();

                    System.out.println("Added folder: " + entryName);
                }

                if (innerFiles != null) {
                    for (File innerFile : innerFiles) {
                        zipNext(true, innerFile, includeRootFolder ? entryName : null, zipOut, bufferSize, includeEmptyFolders, includeHiddenFiles, excludeByRegexpPattern);
                    }
                }
            }
        } else {
            String entryName = Base.combinePath(parentEntryName, file.getName());
            FileInputStream fileInputStream = new FileInputStream(file);
            ZipEntry zipEntry = new ZipEntry(entryName);
            zipOut.putNextEntry(zipEntry);
            byte[] bytes = getSharedBuffer(bufferSize);
            int length;
            while ((length = fileInputStream.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fileInputStream.close();

            System.out.println("Added file: " + entryName);
        }
    }
    private static void compress(String sourcePath, String destinationPath, int bufferSize, boolean overwrite, boolean includeRootFolder, boolean includeEmptyFolders, boolean includeHiddenFiles, Pattern excludeByRegexpPattern, int compressionLevel) throws IOException
    {
        File rootFile = new File(sourcePath);
        File zipFile = new File(destinationPath);
        if (!overwrite && zipFile.exists()) {
            throw new IOException("File already exists!");
        }

        FileOutputStream fileOutputStream = new FileOutputStream(zipFile.getAbsolutePath());
        ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);

        zipOutputStream.setLevel(compressionLevel);

        zipNext(includeRootFolder, rootFile, null, zipOutputStream, bufferSize, includeEmptyFolders, includeHiddenFiles, excludeByRegexpPattern);

        zipOutputStream.close();
        fileOutputStream.close();
    }
    private static File initEntryFile(File rootFile, String entryName) throws IOException
    {
        Path rootPath = Paths.get(rootFile.getAbsolutePath()).normalize();
        Path entryPath = Paths.get(rootFile.getAbsolutePath(), entryName).normalize();

        if (!entryPath.startsWith(rootPath)) {
            throw new IOException("Entry \"" + entryName + "\" is outside of zip!");
        }

        return entryPath.toFile();
    }
    private static boolean deleteFolderRecursively(File file)
    {
        File[] innerFiles = file.listFiles();
        if (innerFiles != null) {
            for (File innerFile : innerFiles) {
                deleteFolderRecursively(innerFile);
            }
        }
        return file.delete();
    }
    private static void extract(String sourcePath, String destinationPath, int bufferSize, boolean overwrite, boolean wipe) throws IOException
    {
        File rootFile = new File(destinationPath);

        if (rootFile.exists()) {
            if (rootFile.isFile()) {
                if (!overwrite) {
                    throw new IOException("Target path already exists!");
                }
                if (!rootFile.delete()) {
                    throw new IOException("Target path could not be overwritten!");
                }
                System.out.println("Deleted target file for override: " + rootFile.getName());
            } else if (wipe) {
                if (!overwrite) {
                    throw new IOException("Target path already exists!");
                }
                if (!deleteFolderRecursively(rootFile)) {
                    throw new IOException("Target path could not be wiped!");
                }
                System.out.println("Wiped target folder: " + rootFile.getName());
            }
        }

        FileInputStream fileInputStream = new FileInputStream(sourcePath);
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        byte[] buffer = getSharedBuffer(bufferSize);
        ZipEntry zipEntry = zipInputStream.getNextEntry();
        while (zipEntry != null) {
            File entryFile = initEntryFile(rootFile, zipEntry.getName());
            if (entryFile.isFile()) {
                if (!overwrite) {
                    throw new IOException("Entry \"" + zipEntry.getName() + "\" already exists!");
                }
                if (!entryFile.delete()) {
                    throw new IOException("Entry \"" + zipEntry.getName() + "\" could not be overwritten!");
                }
                System.out.println("Deleted file for entry override: " + zipEntry.getName());
            }

            if (zipEntry.isDirectory()) {
                if (!entryFile.isDirectory() && !entryFile.mkdirs()) {
                    throw new IOException("Could not create folder for entry \"" + zipEntry.getName() + "\"!");
                }
                System.out.println("Created folder: " + zipEntry.getName());
            } else {
                File parent = entryFile.getParentFile();
                if (!parent.isDirectory() && !parent.mkdirs()) {
                    throw new IOException("Could not create folder for entry \"" + zipEntry.getName() + "\"!");
                }
                System.out.println("Created parent folder for entry: " + zipEntry.getName());

                FileOutputStream fileOutputStream = new FileOutputStream(entryFile);
                int len;
                while ((len = zipInputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, len);
                }
                fileOutputStream.close();
                System.out.println("Created file for entry: " + zipEntry.getName());
            }
            zipEntry = zipInputStream.getNextEntry();
        }
        zipInputStream.closeEntry();
        zipInputStream.close();
        fileInputStream.close();
    }
    public static void main(String[] args)
    {
        try
        {
            System.out.println(Base.getFullName(CompressionTool.class));
            if (Base.hasArg(args, "-version")) {
                System.exit(0);
            }

            System.out.print("Processing command line arguments...");
            String sourcePath = Base.getArgValue(args, "-sourcePath", false, null);
            String destinationPath = Base.getArgValue(args, "-destinationPath", false, null);
            boolean overwrite = Base.hasArg(args, "-overwrite");
            int bufferSize = Integer.parseInt(Base.getArgValue(args, "-bufferSize", true, "1024" ));
            boolean compress = Base.hasArg(args, "-compress");
            int compressionLevel = Integer.parseInt(Base.getArgValue(args, "-compressionLevel", true, "-1" ));
            boolean includeEmptyFolders = Base.hasArg(args, "-includeEmptyFolders");
            boolean includeRootFolder = Base.hasArg(args, "-includeRootFolder");
            boolean includeHiddenFiles = Base.hasArg(args, "-includeHiddenFiles");
            String excludeByRegexp = Base.getArgValue(args, "-excludeByRegexp", true, null);
            boolean extract = Base.hasArg(args, "-extract");
            String extractMode = Base.getArgValue(args, "-extractMode", true, ExtractMode_Append);
            ArrayList<String> list = new ArrayList<>();
            list.add(ExtractMode_Append);
            list.add(ExtractMode_Wipe);
            if (!list.contains(extractMode)) {
                throw new Exception("Unknown extractMode value \"" + extractMode + "\". Available values: " + String.join(", ", list));
            }

            if (compress == extract) {
                throw new Exception("Either -compress or -extract must be specified!");
            }
            Pattern excludeByRegexpPattern = null;
            if (excludeByRegexp != null) {
                excludeByRegexpPattern = Pattern.compile(excludeByRegexp);
            }

            System.out.println("Done.");

            System.out.println();
            System.out.println("Settings: ");
            System.out.println(" - sourcePath: " + sourcePath);
            System.out.println(" - destinationPath: " + destinationPath);
            System.out.println(" - overwrite: " + overwrite);
            System.out.println(" - bufferSize: " + bufferSize);
            if (compress) {
                System.out.println(" - compress: " + compress);
                System.out.println(" - compressionLevel: " + compressionLevel);
                System.out.println(" - includeEmptyFolders: " + includeEmptyFolders);
                System.out.println(" - includeRootFolder: " + includeRootFolder);
                System.out.println(" - includeHiddenFiles: " + includeHiddenFiles);
                System.out.println(" - excludeByRegexp: " + excludeByRegexp);
            }
            if (extract) {
                System.out.println(" - extract: " + extract);
                System.out.println(" - extractMode: " + extractMode);
            }

            System.out.println();

            if (compress) {
                System.out.println("Compressing...");
                compress(sourcePath, destinationPath, bufferSize, overwrite, includeRootFolder, includeEmptyFolders, includeHiddenFiles, excludeByRegexpPattern, compressionLevel);
                System.out.println("Done.");
            }
            if (extract) {
                System.out.println("Extracting...");
                extract(sourcePath, destinationPath, bufferSize, overwrite, extractMode.equals(ExtractMode_Wipe));
                System.out.println("Done.");
            }

            System.exit(0);
        }
        catch (Exception exception)
        {
            System.out.println("Failed.");
            System.err.println("Error: " + exception.getMessage());
            System.exit(1);
        }
    }
}