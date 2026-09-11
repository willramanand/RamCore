package dev.willram.ramcore.input;

import org.jetbrains.annotations.NotNull;

/**
 * Turns the raw text a player entered into a typed value. A thrown exception is treated as a failed
 * attempt and consumes a retry.
 *
 * @param <T> the parsed type
 */
@FunctionalInterface
public interface InputParser<T> {

    /**
     * Parses the input.
     *
     * @param input the raw text
     * @return the parsed value
     * @throws Exception when the text cannot be parsed; consumes a retry
     */
    @NotNull
    T parse(@NotNull String input) throws Exception;

    /**
     * The identity parser, returning the text unchanged.
     *
     * @return a parser producing the raw string
     */
    @NotNull
    static InputParser<String> identity() {
        return input -> input;
    }
}
