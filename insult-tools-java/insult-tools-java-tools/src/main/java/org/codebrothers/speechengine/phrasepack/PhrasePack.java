package org.codebrothers.speechengine.phrasepack;

import java.util.LinkedHashMap;

import org.codebrothers.speechengine.phrasepack.token.PhraseBank;
import org.codebrothers.speechengine.phrasepack.token.Word;

/**
 * A phrase pack consists of a set of words and a set of phrase banks.
 *
 * The phrase banks are keyed by string id and each bank consists of one or more phrases.
 *
 * See @SayPhrase for an example of how to construct and use a PhrasePack to generate a phrase.
 *
 * See @PhrasePackRomGenerator for an example of how a phrase pack can be used to build a ROM for the 8-bit target.
 */
public class PhrasePack {

  private final LinkedHashMap<String, Word> words;

  private final LinkedHashMap<String, PhraseBank> phraseBanks;

  public PhrasePack(LinkedHashMap<String, Word> words, LinkedHashMap<String, PhraseBank> phraseBanks) {
    this.words = words;
    this.phraseBanks = phraseBanks;
  }

  public LinkedHashMap<String, Word> getWords() {
    return words;
  }

  public LinkedHashMap<String, PhraseBank> getPhraseBanks() {
    return phraseBanks;
  }

}
