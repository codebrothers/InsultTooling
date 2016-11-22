package org.codebrothers.speechengine;

import static org.codebrothers.speechengine.util.ByteArrayUtils.toHexString;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map.Entry;

import org.codebrothers.speechengine.util.FilenameUtils;
import org.codebrothers.speechengine.util.PathPreconditions;

import uk.co.labbookpages.WavFileException;
import v9t9.engine.speech.encode.LPCSpeechAnalyzer;

/**
 * Utility for crunching together a word bank from a bunch of WAV files.
 *
 * Files are expected to all be in the same directory and are expected to end in ".wav"
 *
 * A little rough and ready, but it works. A word bank could also possibly be constructed from a set of words and a
 * desktop TTS application. That would be nice!
 */
public class WordBankProcessor {

  /*
   * Lower case file names with underscores ending in .wav
   */
  private static final String WAV_FILENAME_PATTERN = "^[a-z_]+\\.wav$";

  private final LPCSpeechAnalyzer lpcSpeechAnalyzer;
  private final TalkieSpeechEncoder talkieSpeechEncoder;

  public WordBankProcessor() {
    this(new LPCSpeechAnalyzer(), new TalkieSpeechEncoder());
  }

  public WordBankProcessor(LPCSpeechAnalyzer lpcSpeechAnalyzer, TalkieSpeechEncoder talkieSpeechEncoder) {
    super();
    this.lpcSpeechAnalyzer = lpcSpeechAnalyzer;
    this.talkieSpeechEncoder = talkieSpeechEncoder;
  }

  public static void main(String[] args) throws IOException, WavFileException {
    if (args.length != 1) {
      System.out.println("Usage: WordBankProcessor [path_to_directory]");
      System.exit(1);
    }

    Path path = Paths.get(args[0]);
    WordBankProcessor wordBankProcessor = new WordBankProcessor();
    WordBank wordBank = wordBankProcessor.process(path);
    for (Entry<String, byte[]> wordEntry : wordBank.entrySet()) {
      System.out.println("uint8_t " + wordEntry.getKey() + "[] PROGMEM = {" + toHexString(wordEntry.getValue()) + "};");
    }
  }

  public WordBank process(Path wordBankPath) throws IOException, WavFileException {
    PathPreconditions.checkReadableDirectory(wordBankPath);

    WordBank wordBank = new WordBank();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(wordBankPath, WordBankProcessor::isPathValid)) {
      for (Path path : ds) {
        // encode word data
        byte[] data = talkieSpeechEncoder.encode(lpcSpeechAnalyzer.analyze(path));

        // get word string, just trim the file extension
        String word = FilenameUtils.fileNameWithoutExtension(path);

        // add word to builder
        wordBank.put(word, data);
      }
    }
    return wordBank;
  }

  /**
   * Must be regular file, readable and match the filename pattern.
   */
  private static boolean isPathValid(Path path) throws IOException {
    return Files.isRegularFile(path) && Files.isReadable(path)
            && FilenameUtils.fileName(path).matches(WAV_FILENAME_PATTERN);
  }

}
