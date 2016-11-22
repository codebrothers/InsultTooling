package org.codebrothers.speechengine.util;

import java.nio.file.Path;

public class FilenameUtils {

  public static String fileName(Path path) {
    return path.getFileName().toString();
  }

  public static String fileNameWithoutExtension(Path path) {
    String filename = fileName(path);
    return filename.substring(0, filename.lastIndexOf('.'));
  }

}
