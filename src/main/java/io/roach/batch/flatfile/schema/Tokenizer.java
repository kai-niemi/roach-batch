package io.roach.batch.flatfile.schema;

public class Tokenizer {
    public enum Type {
        regex, fixed, delimited
    }

    private Type type;

    private String pattern;

    private boolean strict;

    private String delimiter = ";";

    private char quoteCharacter = '"';

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public boolean isStrict() {
        return strict;
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    public String getDelimiter() {
        return delimiter;
    }

    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    public char getQuoteCharacter() {
        return quoteCharacter;
    }

    public void setQuoteCharacter(char quoteCharacter) {
        this.quoteCharacter = quoteCharacter;
    }

    @Override
    public String toString() {
        return "Tokenizer{" +
                "type=" + type +
                ", pattern='" + pattern + '\'' +
                ", strict=" + strict +
                ", delimiter='" + delimiter + '\'' +
                ", quoteCharacter=" + quoteCharacter +
                '}';
    }
}
