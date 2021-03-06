package com.oskopek.transport.view;

import javafx.util.converter.DoubleStringConverter;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Integer to String and back converter, used for inputting numbers. Does regex replacement internally for non-numerical
 * characters. Logs instead of throwing a number format exception. Defaults to 0 if no value could be parsed.
 */
public class NumberDoubleStringConverter extends DoubleStringConverter {

    private static final transient Logger logger = LoggerFactory.getLogger(NumberDoubleStringConverter.class);
    private static final Pattern numberPattern = Pattern.compile("[^0-9.]");
    private static final DecimalFormat decimalFormat;

    static {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        symbols.setDecimalSeparator('.');
        decimalFormat = new DecimalFormat("0.000");
        decimalFormat.setGroupingUsed(false);
        decimalFormat.setDecimalFormatSymbols(symbols);
    }

    @Override
    public String toString(Double value) {
        return decimalFormat.format(value);
    }

    @Override
    public Double fromString(String string) {
        if (string == null) {
            return 0d;
        }
        String replaced = numberPattern.matcher(string).replaceAll("");
        return Try.of(() -> Double.parseDouble(replaced)).onFailure(e -> {
            if (e instanceof NumberFormatException) {
                logger.debug("Couldn't parse input into table (\"{}\") - NumberFormatException: {}", replaced,
                        e.getMessage());
            } else {
                logger.debug("Couldn't parse input into table (\"{}\"): ", replaced, e);
            }
        }).getOrElse(0d);
    }
}
