package org.codebrothers.speechengine.util;

import java.util.Collections;
import java.util.Map;

public class ByteArrayUtils {

  public static String toHexString(byte[] byteArray) {
    return toHexString(byteArray, -1).toString();
  }

  public static String toHexString(byte[] byteArray, int bytesPerLine) {
    return toHexString(byteArray, bytesPerLine, Collections.emptyMap());
  }

  /**
   * Spits out a hex string, keeping to a maximum number of bytes per line.
   *
   * Injects the comments (using toString) when a particular address is hit.
   */
  public static String toHexString(byte[] byteArray, int bytesPerLine, Map<Integer, Object> comments) {
    int commentOffset = 0;
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < byteArray.length; i++) {
      if (i > 0) {
        builder.append(", ");
        if (bytesPerLine > 0 && ((i - commentOffset) % bytesPerLine) == 0) {
          builder.append("\n");
        }
      }

      if (comments.containsKey(i)) {
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
          builder.append("\n");
        }
        builder.append("\n// Address: 0x");
        builder.append(String.format("%02X", (byte) (i & 0xFF)));
        builder.append(", 0x");
        builder.append(String.format("%02X", (byte) ((i >> 8) & 0xFF)));
        builder.append(" - ");
        builder.append(comments.get(i));
        builder.append("\n\n");
        commentOffset = i;
      }

      builder.append("0x");
      builder.append(String.format("%02X", byteArray[i]));
    }
    return builder.toString();
  }

}
