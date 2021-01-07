# CompressionTool

## About
**CompressionTool** is a command-line tool designed to compress and decompress files stored on local filesystem.

Tool currently supports only zip format.

## CompressionTool options

##### Print tool version to stdout and exit
```bash
-version
```

##### Common
```bash
-bufferSize #Size of internal buffer for I/O operations. (Default value is 1024)
-overwrite #Must be set if you want to override existing files
```

##### Compression
```bash
-compress
-compressionLevel # Valid values are whole number from 0 to 9 inclusive or -1 for default compression level. (Default value is -1)
-sourcePath "path/to/folder/or/file"
-destinationPath "path/to/compressed.zip"
-includeEmptyFolders #Must be set if you want to include empty folders
-includeRootFolder #Must be set if you want to include root folder into compressed file
-includeHiddenFiles #Must be set if you want to include hidden files and folders
-excludeByRegexp #Exclude files and folders by regular expression
```

##### Decompression
```bash
-extract
-extractMode #One of [append, wipe]. (Default value is append) 
-sourcePath "path/to/compressed.zip"
-destinationPath "path/to/extract/folder"
```

## Releases
Latest release: [1.1.0](https://github.com/chorobochrontochor/CompressionTool/releases)
