package org.codebrothers.speechengine;


import static org.codebrothers.speechengine.util.ByteArrayUtils.toHexString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import uk.co.labbookpages.WavFileException;
import v9t9.common.speech.ILPCParameters;
import v9t9.engine.speech.encode.LPCSpeechAnalyzer;
import ejs.base.utils.BitOutputStream;

/**
 * Experiment in getting the correct data format for Talkie.
 */
public class TalkieSpeechEncoder {

  public static void main(String[] args) throws IOException, LineUnavailableException, WavFileException {
    if (args.length != 1) {
      System.out.println("Usage: TalkieSpeechEncoder [path_to_wav_file]");
      System.exit(1);
    }

    // encode the file to a byte array
    LPCSpeechAnalyzer lpcSpeechAnalyzer = new LPCSpeechAnalyzer();
    TalkieSpeechEncoder talkieSpeechEncoder = new TalkieSpeechEncoder();
    byte[] byteArray = talkieSpeechEncoder.encode(lpcSpeechAnalyzer.analyze(Paths.get(args[0])));

    // output in hex format
    System.out.println(toHexString(byteArray));
  }

  public byte[] encode(List<ILPCParameters> analyzedFrames) throws IOException {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    outputFrames(analyzedFrames, byteArrayOutputStream);
    return reverseBytes(byteArrayOutputStream.toByteArray());
  }

  /**
   * The byte reversing is a little bit nasty.
   *
   * It's just how talkie is configured, something to do with the original ROMS having their bytes reversed.
   *
   * Might be better to work with a modified version of talkie which doesn't reverse the bytes on the way in, after all
   * we'll probably be making changes to allow talkie to read from an external EEPROM.
   */
  private byte[] reverseBytes(byte[] byteArray) {
    for (int i = 0; i < byteArray.length; i++) {
      byteArray[i] = reverseByte(byteArray[i]);
    }
    return byteArray;
  }

  /**
   * Yikes! Let's how about a lookup table?
   */
  private byte reverseByte(byte value) {
    return (byte) (Integer.reverse(value) >>> 24);
  }

  private static void outputFrames(List<ILPCParameters> analyzedFrames, ByteArrayOutputStream byteArrayOutputStream)
          throws IOException {
    try (BitOutputStream bitOutputStream = new BitOutputStream(byteArrayOutputStream)) {
      for (ILPCParameters frameParameters : analyzedFrames) {
        if (!frameParameters.isLast()) {
          frameParameters.toBytes(bitOutputStream);
        }
      }
      bitOutputStream.writeBits(15, 4);
    }
  }

}
