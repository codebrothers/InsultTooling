package org.codebrothers.speechengine.phrasepack;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codebrothers.speechengine.phrasepack.token.PhraseBank;

/**
 * Just a hacky class to demo the phrase pack, takes the phrase pack directory as an argument and prints out 10 phrases.
 */
public class SayPhrase {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage: SayPhrase [path_to_phrase_pack]");
      System.exit(1);
    }
    PhrasePack phrasePack = parsePhrasePack(Paths.get(args[0]));
    PhraseBank phraseBank = phrasePack.getPhraseBanks().get("primary");
    for (int i = 0; i < 10; i++) {
      System.out.println(phraseBank.generatePhrase());
    }
  }

  private static PhrasePack parsePhrasePack(Path phrasePackPath) throws IOException {
    PhrasePackParser phrasePackParser = new PhrasePackParser();
    return phrasePackParser.parse(phrasePackPath);
  }

}
