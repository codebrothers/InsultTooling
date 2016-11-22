package org.codebrothers.speechengine.phrasepack;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.codebrothers.speechengine.phrasepack.token.Word;

/**
 * Just a hacky class to list the words used by a phrase pack. Helps me know what words to record!
 */
public class ListWords {

  public static void main(String[] args) throws IOException {
    if (args.length != 1) {
      System.out.println("Usage: SayPhrase [path_to_phrase_pack]");
      System.exit(1);
    }
    PhrasePack phrasePack = parsePhrasePack(Paths.get(args[0]));
    phrasePack.getWords().values().stream().map(Word::getWord).forEach(System.out::println);
  }

  private static PhrasePack parsePhrasePack(Path phrasePackPath) throws IOException {
    PhrasePackParser phrasePackParser = new PhrasePackParser();
    return phrasePackParser.parse(phrasePackPath);
  }


}
