package org.codebrothers.speechengine.phrasepack.token;

public class Word implements PhraseToken {

  private final String word;

  public Word(String word) {
    this.word = word;
  }

  public String getWord() {
    return word;
  }

  @Override
  public void renderPhrase(StringBuilder stringBuilder) {
    if (stringBuilder.length() > 0) {
      stringBuilder.append(" ");
    }
    stringBuilder.append(word);
  }

  @Override
  public String toString() {
    return "Word [" + word + "]";
  }



}
