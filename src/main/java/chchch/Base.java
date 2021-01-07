package chchch;

import java.io.File;

class Base {
    static String getAppName(Class cls)
    {
        return cls.getPackage().getImplementationTitle();
    }
    static String getAppVersion(Class cls)
    {
        return cls.getPackage().getImplementationVersion();
    }
    static String getFullName(Class cls)
    {
        return Base.getAppName(cls) + ": v" + Base.getAppVersion(cls);
    }
    static boolean hasArg(String[] args, String flag)
    {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }
    static String getArgValue(String[] args, String flag, boolean optional, String defaultValue) throws Exception
    {
        boolean nextIsValue = false;
        for (String arg : args) {
            if (nextIsValue) {
                return arg;
            }
            if (arg.equals(flag)) {
                nextIsValue = true;
            }
        }
        if (optional) return defaultValue;

        if (nextIsValue) {
            throw new Exception("Value for mandatory argument \"" + flag + "\" is missing!");
        } else {
            throw new Exception("Mandatory argument \"" + flag + "\" is missing!");
        }
    }
    static String getFileExtension(File file)
    {
        String fileName = file.getName();
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            return fileName.substring(i + 1);
        }
        return "";
    }
    static <T> boolean arrayContains(T[] array, T value)
    {
        for (T arrayValue : array) {
            if (arrayValue.equals(value)) {
                return true;
            }
        }
        return false;
    }
    static String combinePath(String path1, String path2)
    {
        if (path1 == null || path1.isEmpty()) return path2;
        return addTrailingSlash(path1) + removeLeadingSlash(path2);
    }
    static String addTrailingSlash(String path)
    {
        if (path.endsWith("/")) return path;
        return path + "/";
    }
    static String removeLeadingSlash(String path)
    {
        if (path.startsWith("/")) return path.substring(1);
        return path;
    }
}
