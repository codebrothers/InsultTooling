package org.codebrothers.speechengine.phrasepack;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.codebrothers.speechengine.WordBank;
import org.codebrothers.speechengine.WordBankProcessor;
import org.codebrothers.speechengine.phrasepack.token.Phrase;
import org.codebrothers.speechengine.phrasepack.token.PhraseBank;
import org.codebrothers.speechengine.phrasepack.token.PhraseToken;
import org.codebrothers.speechengine.phrasepack.token.Word;
import org.codebrothers.speechengine.util.ByteArrayUtils;

import uk.co.labbookpages.WavFileException;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * TODO - Optimization : If the C code can allow for it (which it should be able to) where a phrase consists of a single
 * word, it needn't have it's own phrase structure. Instead, it's pointer can point directly to the word. This could
 * trim a few bytes off! NB: Leaving this optimisation until the C side is implemented.
 *
 * Generates a phrase ROM for the AVR chip.
 *
 * <h2>ROM Contents</h2>
 *
 * The ROM is a single block of memory consisting of:
 *
 * <ul>
 * <li>Phrase Banks: each consisting one or more phrases.</li>
 * <li>A Word Bank: containing all of the words used by the phrase pack.</li>
 * <li>A delimiter, providing a pointer to where the word bank begins.</li>
 * </ul>
 *
 * This is output in the following format:
 *
 * <pre>
 * [--------------------------- ROM HEADER --------------------------][------- ROM DATA ------]
 * [word bank start pointer (2 bytes)][primary bank pointer (2 bytes)][phrase banks][word bank]
 * </pre>
 *
 * <h2>Word Bank Start Pointer</h2>
 *
 * The word bank start pointer can be used by the processing code to determine whether a pointer in hand is a phrase
 * bank pointer or a word pointer. This is important as phrases are made up of pointers to either phrase banks or words
 * and they need to be handled differently.
 *
 * <h2>Primary Bank Pointer</h2>
 *
 * In anticipation of perhaps building more than one phrase pack into a single ROM, I have placed a primary bank pointer
 * in the ROM header. This could, in the future, be extended to hold multiple primary bank pointers say to generate
 * different sets of phrases from the same ROM (and a shared word bank no less!)
 *
 * <h2>Phrase Banks</h2>
 *
 * The phrase banks are stored adjacent to one another in a continuous block of memory.
 *
 * <h3>Phrase Bank</h3> A phrase bank can hold up to 255 phrases.
 *
 * A phrase bank consists of:
 *
 * <ul>
 * <li>A byte giving a count of the number of phrases in the bank, this can be used to select a random phrase.</li>
 * <li>Two bytes for each phrase, pointing to the memory location of start of the phrase.</li>
 * <li>The phrase data.</li>
 * </ul>
 *
 * A phrase bank is output as follows:
 *
 * <pre>
 * [--- PHRASE COUNT ----][------------ PHRASE POINTERS -----------][-- DATA ---]
 * [phrase count (1 byte)][phrase pointers (2 bytes * phrase count)][phrase data]
 * </pre>
 *
 * <h3>Phrase Data</h3>
 *
 * Each phrase in the phrase data is a series of tokens. Each token, of two bytes in length, points to to either a word
 * or another phrase.
 *
 * <ul>
 * <li>A byte giving a count of the number of tokens in the phrase, so we know how many tokens to process.</li>
 * <li>Two bytes for each token, pointing to either a word or a phrase bank.</li>
 * </ul>
 *
 * A phrase is output as follows:
 *
 * <pre>
 * [--- PHRASE COUNT ---][----------- PHRASE POINTERS ----------]
 * [token count (1 byte)][token pointers (2 bytes * token count)]
 * </pre>
 *
 * <h2>Word Bank:</h2>
 *
 * The word bank is a continuous block of memory containing LPC encoded speech.
 *
 * Each word is delimited by a frame with an energy of 15, talkie automatically stops when it hits such a frame.
 *
 * These pointers can be passed directly to talkie to play a word.
 */
public class PhrasePackRomGenerator {

  /*
   * Pointers are going to remain at 16-bit until we get above 1024kBits in the ROM.
   */
  private static final int POINTER_SIZE = 2;

  /*
   * We currently reserve two pointers worth for the word bank start pointer and the primary bank pointer.
   */
  private static final int START_OFFSET = 2 * POINTER_SIZE;

  private PhrasePackRomGenerator() {}

  public static void main(String[] args) throws IOException, WavFileException {
    if (args.length != 2) {
      System.out.println("Usage: PhrasePackRomGenerator [word_bank_directory] [phrase_pack_directory]");
      System.exit(1);
    }

    /*
     * BiMap is used to track offsets for given entities in the ROM data, useful for generating correct pointers also
     * used to put nice comments in the output.
     */
    BiMap<Object, Integer> pointers = HashBiMap.create();

    PhrasePackRomGenerator phrasePackRomGenerator = new PhrasePackRomGenerator();
    byte[] romData = phrasePackRomGenerator.generate(Paths.get(args[0]), Paths.get(args[1]), pointers);

    System.out.println("============= START ROM DATA =============");
    System.out.println(ByteArrayUtils.toHexString(romData, 20, pointers.inverse()));
  }

  private byte[] generate(Path wordBankPath, Path phrasePackPath, Map<Object, Integer> pointers) throws IOException,
          WavFileException {
    Preconditions.checkState(Files.exists(wordBankPath), "Path \"%s\" did not exist.", wordBankPath);
    Preconditions.checkState(Files.isDirectory(wordBankPath), "Path \"%s\" was not a directory.", wordBankPath);
    Preconditions.checkState(Files.isReadable(wordBankPath), "Path \"%s\" was not readable.", wordBankPath);
    Preconditions.checkState(Files.exists(phrasePackPath), "Path \"%s\" did not exist.", phrasePackPath);
    Preconditions.checkState(Files.isDirectory(phrasePackPath), "Path \"%s\" was not a directory.", phrasePackPath);
    Preconditions.checkState(Files.isReadable(phrasePackPath), "Path \"%s\" was not readable.", phrasePackPath);

    // parse the phrase packs and the word bank
    PhrasePack phrasePack = new PhrasePackParser().parse(phrasePackPath);
    WordBank wordBank = new WordBankProcessor().process(wordBankPath);

    // validate we have a primary bank
    PhraseBank primaryPhraseBank = phrasePack.getPhraseBanks().get("primary");
    if (primaryPhraseBank == null) {
      throw new IllegalStateException("No \"primary\" phrase bank found.");
    }

    // put a nice message in the pointers so we can output decent comment (see main method)
    pointers.put("ROM Header, word bank start pointer and the primary bank pointer.", 0);

    // calculate phrase bank offsets
    final int wordDataOffset = calculatePhraseBankOffsets(phrasePack, pointers, START_OFFSET);
    byte[] wordData = extractWordData(wordBank, phrasePack, pointers, wordDataOffset);
    int endOffset = wordDataOffset + wordData.length;
    byte[] phraseData = outputPhraseData(phrasePack, pointers, START_OFFSET);

    int primaryPhraseBankOffset = pointers.get(primaryPhraseBank);
    byte[] romData = assembleRom(wordDataOffset, primaryPhraseBankOffset, phraseData, wordData);

    System.out.println("Expected ROM size: " + endOffset + " bytes");
    System.out.println("Actual ROM size was: " + romData.length + " bytes");

    return romData;
  }

  private byte[] assembleRom(int wordDataOffset, int primaryPhraseBankOffset, byte[] phraseData, byte[] wordData)
          throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    outputPointer(wordDataOffset, outputStream);
    outputPointer(primaryPhraseBankOffset, outputStream);
    outputStream.write(phraseData);
    outputStream.write(wordData);
    return outputStream.toByteArray();
  }

  /**
   * We need to calculate the phrase bank offsets first so phrases can point to one-another.
   */
  private int calculatePhraseBankOffsets(PhrasePack phrasePack, Map<Object, Integer> pointers, int offset) {
    for (PhraseBank phraseBank : phrasePack.getPhraseBanks().values()) {
      // put phrase bank in pointers
      pointers.put(phraseBank, offset);
      // grab phrases
      List<Phrase> phrases = phraseBank.getPhrases();
      Preconditions.checkState(phrases.size() <= 255, "We can only support 255 phrases per bank.");
      // one byte for the phrase count
      offset++;
      // phrases count * pointer size for phrase directory
      offset += phrases.size() * POINTER_SIZE;
      for (Phrase phrase : phrases) {
        Preconditions.checkState(phrase.getTokens().size() <= 255, "We can only support 255 tokens per phrase.");
        // put phrase in pointers
        pointers.put(phrase, offset);
        // one byte for the token count
        offset++;
        // add token count * pointer size for the phrases themselves
        offset += phrase.getTokens().size() * POINTER_SIZE;
      }
    }
    return offset;
  }

  private byte[] outputPhraseData(PhrasePack phrasePack, Map<Object, Integer> pointers, int offset) throws IOException {
    ByteArrayOutputStream phraseBankOutputStream = new ByteArrayOutputStream();

    for (PhraseBank phraseBank : phrasePack.getPhraseBanks().values()) {
      // grab phrases
      List<Phrase> phrases = phraseBank.getPhrases();

      // one byte for the phrase count
      offset++;

      // phrases count * pointer size for phrase directory
      offset += phrases.size() * POINTER_SIZE;

      // one byte for the phrase count
      phraseBankOutputStream.write((byte) phrases.size());

      ByteArrayOutputStream phraseDataOutputStream = new ByteArrayOutputStream();
      for (Phrase phrase : phrases) {
        // write pointer
        outputPointer(offset, phraseBankOutputStream);

        // write a byte for token count
        phraseDataOutputStream.write(phrase.getTokens().size());

        // write tokens
        for (PhraseToken token : phrase.getTokens()) {
          Integer pointer = pointers.get(token);
          Preconditions.checkState(pointer != null, "Could not find pointer for token.");
          outputPointer(pointer, phraseDataOutputStream);
        }

        // add byte for token count
        offset++;
        // add token count * pointer size for the phrases themselves
        offset += phrase.getTokens().size() * POINTER_SIZE;
      }
      phraseBankOutputStream.write(phraseDataOutputStream.toByteArray());
    }

    return phraseBankOutputStream.toByteArray();
  }


  /*
   * Extracts the word data required to support the phrase pack.
   *
   * Adds a pointer to the pointers for each word added.
   */
  private byte[] extractWordData(WordBank wordBank, PhrasePack phrasePack, Map<Object, Integer> pointers,
          int startOffset) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    for (Entry<String, Word> word : phrasePack.getWords().entrySet()) {
      Preconditions.checkState(wordBank.containsKey(word.getKey()),
              MessageFormat.format("Word \"{0}\" was not found in word bank.", word.getKey()));
      pointers.put(word.getValue(), startOffset + outputStream.size());
      outputStream.write(wordBank.get(word.getKey()));
    }
    return outputStream.toByteArray();
  }

  /*
   * Pushes a pointer out to the byte array output stream. Note Endianness!
   *
   * The correct way to assemble the pointer at the C side will be:
   *
   * bytes[n] | (int)bytes[n+1] << 8
   */
  private void outputPointer(int pointer, ByteArrayOutputStream byteArrayOutputStream) {
    byteArrayOutputStream.write((byte) (pointer & 0xFF));
    byteArrayOutputStream.write((byte) ((pointer >> 8) & 0xFF));
  }

}
