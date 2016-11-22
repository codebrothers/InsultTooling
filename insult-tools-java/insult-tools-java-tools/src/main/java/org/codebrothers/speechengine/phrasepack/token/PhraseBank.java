package org.codebrothers.speechengine.phrasepack.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PhraseBank implements PhraseToken {

  private static final Random RANDOM = new Random();

  private final String id;
  private final List<Phrase> phrases = new ArrayList<Phrase>();

  public PhraseBank(String id) {
    this.id = id;
  }


  public String getId() {
    return id;
  }

  public List<Phrase> getPhrases() {
    return phrases;
  }

  public void addPhrase(Phrase phrase) {
    phrases.add(phrase);
  }

  public String generatePhrase() {
    StringBuilder stringBuilder = new StringBuilder();
    renderPhrase(stringBuilder);
    return stringBuilder.toString();
  }

  public int size() {
    return phrases.size();
  }

  @Override
  public String toString() {
    return "PhraseBank [" + id + "]";
  }

  @Override
  public void renderPhrase(StringBuilder stringBuilder) {
    phrases.get(RANDOM.nextInt(phrases.size())).renderPhrase(stringBuilder);
  }

}
