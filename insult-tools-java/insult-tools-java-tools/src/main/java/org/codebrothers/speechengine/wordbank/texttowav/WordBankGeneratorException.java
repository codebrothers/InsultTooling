package org.codebrothers.speechengine.wordbank.texttowav;

public class WordBankGeneratorException extends Exception {

  private static final long serialVersionUID = 1L;

  public WordBankGeneratorException(String message, Exception e) {
    super(message, e);
  }

}
