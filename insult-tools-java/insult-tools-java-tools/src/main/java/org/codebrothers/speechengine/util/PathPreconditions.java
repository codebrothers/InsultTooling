package org.codebrothers.speechengine.util;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.base.Preconditions;

public class PathPreconditions {

  public static void checkReadableDirectory(Path path) {
    Preconditions.checkNotNull(path, "path may not be null");
    Preconditions.checkState(Files.exists(path), "Path \"%s\" did not exist.", path);
    Preconditions.checkState(Files.isDirectory(path), "Path \"%s\" was not a directory.", path);
    Preconditions.checkState(Files.isReadable(path), "Path \"%s\" was not readable.", path);
  }

}
