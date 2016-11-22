package org.codebrothers.speechengine.phrasepack;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.codebrothers.speechengine.phrasepack.token.Phrase;
import org.codebrothers.speechengine.phrasepack.token.PhraseBank;
import org.codebrothers.speechengine.phrasepack.token.PhraseToken;
import org.codebrothers.speechengine.phrasepack.token.Word;
import org.codebrothers.speechengine.util.FilenameUtils;
import org.codebrothers.speechengine.util.PathPreconditions;

public class PhrasePackParser {

  private static final Pattern WORD_PATTERN = Pattern.compile("^[a-z_]+$");
  private static final Pattern PHRASE_PATTERN = Pattern.compile("^\\{([a-z_]+)\\}$");
  private static final String PHRASE_BANK_FILENAME_PATTERN = "^[a-z_]+\\.txt$";

  public PhrasePack parse(Path phrasePackPath) throws IOException {
    PathPreconditions.checkReadableDirectory(phrasePackPath);

    // grab a list of paths to the phrase bank files
    Map<String, Path> paths = phraseBankPaths(phrasePackPath);

    // phrase banks created eagerly so they can be inserted as phrase tokens by reference
    // also lets us validate phrases contain valid references.
    LinkedHashMap<String, PhraseBank> phraseBanks = createPhraseBanks(paths);

    // gather words separately, so they can be re-used
    LinkedHashMap<String, Word> words = new LinkedHashMap<>();

    for (Entry<String, Path> pathEntry : paths.entrySet()) {
      PhraseBank phraseBank = phraseBanks.get(pathEntry.getKey());

      try (Stream<String> phraseStream = Files.lines(pathEntry.getValue())) {
        phraseStream.forEach(string -> parsePhrase(string, phraseBank, phraseBanks, words));
      }

      // validate populated
      if (phraseBank.size() == 0) {
        throw new IllegalStateException(MessageFormat
                .format("All phrase banks must be populated. Phrase bank \"{0}\" was empty.", pathEntry.getKey()));
      }
    }

    // return immutable phrase pack
    return new PhrasePack(words, phraseBanks);
  }

  private void parsePhrase(String phrase, PhraseBank phraseBank, LinkedHashMap<String, PhraseBank> phraseBanks,
          LinkedHashMap<String, Word> words) {
    String[] tokens = phrase.split("\\s+");
    if (tokens.length == 0) {
      return;
    }
    List<PhraseToken> phraseTokens =
            Arrays.stream(tokens).map(token -> parseToken(token, phraseBanks, words)).collect(Collectors.toList());
    phraseBank.addPhrase(new Phrase(phraseTokens));
  }

  private PhraseToken parseToken(String token, LinkedHashMap<String, PhraseBank> phraseBanks,
          LinkedHashMap<String, Word> words) {
    // deal with words
    Matcher matcher = WORD_PATTERN.matcher(token);
    if (matcher.matches()) {
      return words.computeIfAbsent(token, Word::new);
    }

    // deal with phrases
    matcher = PHRASE_PATTERN.matcher(token);
    if (matcher.matches()) {
      String phraseBankKey = matcher.group(1);
      PhraseBank phraseBank = phraseBanks.get(phraseBankKey);
      if (phraseBank == null) {
        throw new TokenParseException(MessageFormat.format("Phrase bank \"{0}\" did not exist.", phraseBankKey));
      }
      return phraseBank;
    }

    // damn
    throw new TokenParseException(MessageFormat.format("Token \"{0}\" did not parse.", token));
  }

  private LinkedHashMap<String, PhraseBank> createPhraseBanks(Map<String, Path> paths) {
    LinkedHashMap<String, PhraseBank> phraseBanks = new LinkedHashMap<>();
    paths.keySet().stream().forEach(key -> phraseBanks.put(key, new PhraseBank(key)));
    return phraseBanks;
  }

  private Map<String, Path> phraseBankPaths(Path phrasePackPath) throws IOException {
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(phrasePackPath, PhrasePackParser::isPathValid)) {
      return StreamSupport.stream(ds.spliterator(), false)
              .collect(Collectors.toMap(FilenameUtils::fileNameWithoutExtension, Function.identity()));
    }
  }

  /**
   * Must be regular file, readable and match the filename pattern.
   */
  private static boolean isPathValid(Path path) throws IOException {
    return Files.isRegularFile(path) && Files.isReadable(path)
            && FilenameUtils.fileName(path).matches(PHRASE_BANK_FILENAME_PATTERN);
  }

}
