package org.codebrothers.speechengine.phrasepack.texttowav;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import javax.sound.sampled.AudioInputStream;

import marytts.LocalMaryInterface;
import marytts.MaryInterface;
import marytts.exceptions.MaryConfigurationException;
import marytts.util.data.audio.AudioConverterUtils;
import marytts.util.data.audio.MaryAudioUtils;

import org.codebrothers.speechengine.phrasepack.PhrasePack;
import org.codebrothers.speechengine.phrasepack.PhrasePackParser;
import org.codebrothers.speechengine.util.PathPreconditions;

import com.google.common.base.Preconditions;
import com.ibm.icu.text.MessageFormat;

/**
 * Generates a word bank from a phrase bank using Mary TTS.
 */
public class WordBankGenerator {

  private static final int TRIM_SAMPLES = 3000;

  private static final String DEFAULT_VOICE = "cmu-rms-hsmm";

  private final MaryInterface mary;

  public WordBankGenerator() throws MaryConfigurationException {
    this(createDefaultMaryInterface());
  }

  public WordBankGenerator(MaryInterface mary) {
    this.mary = mary;
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("Usage: WordBankGenerator [phrase_pack_directory] [word_output_directory]");
      System.exit(1);
    }

    WordBankGenerator wordBankGenerator = new WordBankGenerator();
    wordBankGenerator.generateWordBank(Paths.get(args[0]), Paths.get(args[1]));
  }

  public void generateWordBank(Path phrasePackPath, Path outputDirectoryPath) throws WordBankGeneratorException,
          IOException {
    PathPreconditions.checkReadableDirectory(phrasePackPath);

    PhrasePack phrasePack = new PhrasePackParser().parse(phrasePackPath);
    generateWordBank(phrasePack.getWords().keySet(), outputDirectoryPath);
  }

  public void generateWordBank(Set<String> words, Path outputDirectoryPath) throws WordBankGeneratorException {
    Preconditions.checkNotNull(words, "words may not be null");
    PathPreconditions.checkReadableDirectory(outputDirectoryPath);

    for (String word : words) {
      generateWord(word, outputDirectoryPath);
    }
  }

  private void generateWord(String word, Path outputDirectoryPath) throws WordBankGeneratorException {
    try (AudioInputStream audio = AudioConverterUtils.downSampling(mary.generateAudio(word), 8000)) {
      double[] samplesAsDoubleArray = MaryAudioUtils.getSamplesAsDoubleArray(audio);
      samplesAsDoubleArray =
              Arrays.copyOfRange(samplesAsDoubleArray, 0, Math.max(0, samplesAsDoubleArray.length - TRIM_SAMPLES));
      MaryAudioUtils.writeWavFile(samplesAsDoubleArray, outputDirectoryPath.resolve(word + ".wav").toString(),
              audio.getFormat());
    } catch (Exception e) {
      throw new WordBankGeneratorException(MessageFormat.format("Problem generating word \"{0}\"", word), e);
    }
  }

  /*
   * Generates a default mary interface with a female voice.
   */
  private static MaryInterface createDefaultMaryInterface() throws MaryConfigurationException {
    LocalMaryInterface mary = new LocalMaryInterface();
    mary.setVoice(DEFAULT_VOICE);
    System.out.println(mary.getAvailableVoices());
    return mary;
  }

}
