package org.codebrothers.speechengine.phrasepack.token;

import java.util.List;

public class Phrase implements PhraseGenerator {

  private final List<PhraseToken> tokens;

  public Phrase(List<PhraseToken> tokens) {
    this.tokens = tokens;
  }

  public List<PhraseToken> getTokens() {
    return tokens;
  }

  @Override
  public void renderPhrase(StringBuilder stringBuilder) {
    for (PhraseToken token : tokens) {
      token.renderPhrase(stringBuilder);
    }
  }

  @Override
  public String toString() {
    return "Phrase: [tokens=" + tokens + "]";
  }

}
