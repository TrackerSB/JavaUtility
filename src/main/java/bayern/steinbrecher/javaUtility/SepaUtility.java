package bayern.steinbrecher.javaUtility;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Pattern;
import org.xml.sax.SAXException;

/**
 * Contains methods for checking some Sepa Direct Debit attributes.
 *
 * @author Stefan Huber
 * @since 0.1
 */
public final class SepaUtility {

    /**
     * Holds the count of days the MessageId has to be unique.
     */
    public static final int UNIQUE_DAYS_MESSAGEID = 15;
    /**
     * The maximum length of the message id.
     */
    public static final int MAX_CHAR_MESSAGE_ID = 35;
    /**
     * Holds the count of month the PmtInfId has to be unique.
     */
    public static final int UNIQUE_MONTH_PMTINFID = 3;
    /**
     * The maximum length of payment information id (PmtInfId).
     */
    public static final int MAX_CHAR_PMTINFID = 35;
    /**
     * The maximum length of an IBAN.
     */
    public static final int MAX_CHAR_IBAN = 34;
    /**
     * The maximum length of the name of the party creating the SEPA Direct Debit.
     */
    public static final int MAX_CHAR_NAME_FIELD = 70;

    /**
     * Length of country code (CC);
     */
    private static final int SEPA_CC_LENGTH = 2;
    /**
     * Length of country code (CC) and checksum together.
     */
    private static final int SEPA_CC_CHECKSUM_LENGTH = SEPA_CC_LENGTH + 2;
    private static final int SEPA_MIN_LENGTH = SEPA_CC_CHECKSUM_LENGTH + 1;
    private static final String SEPA_BUSINESS_CODE = "ZZZ";
    /**
     * A=10, B=11, C=12 etc.
     */
    private static final int SEPA_SHIFT_CC = 10;
    private static final int IBAN_CHECKSUM_MODULO = 97;
    /**
     * Regex describing a possible valid IBAN (the checksum of the IBAN is not checked by this regex).
     */
    public static final String IBAN_REGEX
            = "[A-Z]{" + SEPA_CC_LENGTH + "}\\d{2," + (MAX_CHAR_IBAN - SEPA_CC_LENGTH) + "}";
    private static final Pattern IBAN_PATTERN = Pattern.compile(IBAN_REGEX);
    /**
     * The regex representing all valid BICs.
     */
    public static final String BIC_REGEX = "[A-Z]{6}[A-Z2-9][A-NP-Z0-9]([A-Z0-9]{3})?";
    private static final Pattern BIC_PATTERN = Pattern.compile(BIC_REGEX);
    /**
     * The regex for checking whether a message id is valid. Which characters are supported by Sepa is taken from
     * http://www.sepaforcorporates.com/sepa-implementation/valid-xml-characters-sepa-payments/
     */
    public static final String MESSAGE_ID_REGEX = "([a-zA-Z0-9]|/| |-|\\?|:|\\(|\\)|\\.|,|'|\\+)*";
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile(MESSAGE_ID_REGEX);
    /**
     * Source of schema:
     * https://github.com/w2c/sepa-sdd-xml-generator/blob/master/validation_schemes/pain.008.003.02.xsd
     */
    private static final URL SEPA_XSD_SCHEMA = SepaUtility.class.getResource("pain.008.003.02.xsd");

    /**
     * Prohibit instantiation.
     */
    private SepaUtility() {
        throw new UnsupportedOperationException("Construction of an object no allowed.");
    }

    /**
     * Checks whether the IBAN of the given member has a valid checksum.
     *
     * @param iban The IBAN to check.
     * @return {@code true} only if IBAN has a valid checksum.
     * @see #IBAN_PATTERN
     */
    public static boolean isValidIban(String iban) {
        boolean isValid = false;
        if (iban != null && !iban.isEmpty()) {
            String trimmedIban = iban.replace(" ", "");
            //Check whether it CAN be a valid IBAN
            if (IBAN_PATTERN.matcher(trimmedIban).matches()) {
                //Check the checksum
                int posAlphabetFirstChar = ((int) trimmedIban.charAt(0)) - ((int) 'A') + SEPA_SHIFT_CC;
                int posAlphabetSecondChar = ((int) trimmedIban.charAt(1)) - ((int) 'A') + SEPA_SHIFT_CC;
                if (trimmedIban.length() >= SEPA_MIN_LENGTH && posAlphabetFirstChar >= SEPA_SHIFT_CC
                        && posAlphabetSecondChar >= SEPA_SHIFT_CC) {
                    trimmedIban = trimmedIban.substring(SEPA_CC_CHECKSUM_LENGTH) + posAlphabetFirstChar
                            + posAlphabetSecondChar + trimmedIban.substring(SEPA_CC_LENGTH, SEPA_CC_CHECKSUM_LENGTH);
                    isValid = new BigInteger(trimmedIban)
                            .mod(BigInteger.valueOf(IBAN_CHECKSUM_MODULO))
                            .equals(BigInteger.ONE);
                }
            }
        }

        return isValid;
    }

    /**
     * Checks whether the given BIC is valid. Currently only the length and the allowed characters are checked.
     *
     * @param bic The BIC to check.
     * @return {@code false} only if the BIC is invalid.
     * @see #BIC_PATTERN
     */
    public static boolean isValidBic(String bic) {
        return bic != null
                && BIC_PATTERN.matcher(bic)
                        .matches();
    }

    /**
     * Checks whether the given creditor id is valid.
     *
     * @param creditorId The creditor id to check.
     * @return {@code true} only if the given creditor id is valid.
     */
    public static boolean isValidCreditorId(String creditorId) {
        boolean isValid;
        if (creditorId == null) {
            isValid = false;
        } else {
            String trimmedCreditorId = creditorId.replaceAll(" ", "");
            isValid = trimmedCreditorId.contains(SEPA_BUSINESS_CODE)
                    && isValidIban(trimmedCreditorId.replace(SEPA_BUSINESS_CODE, ""));
        }
        return isValid;
    }

    /**
     * Checks whether the given message id would be valid for being used in a Sepa Direct Debit.
     *
     * @param messageId The message id to check.
     * @return {@code true} only if the message id is valid.
     */
    public static boolean isValidMessageId(String messageId) {
        return messageId != null
                && MESSAGE_ID_PATTERN.matcher(messageId).matches()
                && messageId.length() <= MAX_CHAR_MESSAGE_ID;
    }

    /**
     * Returns a {@link String} representation of the given {@link LocalDateTime} which is valid for SEPA Direct Debits.
     *
     * @param dateTime The date to convert.
     * @return The valid representation of the given {@link LocalDateTime}.
     */
    public static String getSepaDate(LocalDateTime dateTime) {
        /*
         * The DateTimeFormatter could accept any subtype of TemporalAccessor but a Sepa Direct Debit only accepts
         * DateTime values and TemporalAccessor does not support all fields needed.
         */
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(dateTime);
    }

    /**
     * Returns a {@link String} representation of the given {@link LocalDate} which is valid for SEPA Direct Debits.
     *
     * @param date The date to convert.
     * @return The valid representation of the given {@link LocalDate}.
     */
    public static String getSepaDate(LocalDate date) {
        /*
         * The DateTimeFormatter could accept any subtype of TemporalAccessor but a Sepa Direct Debit only accepts
         * DateTime values and TemporalAccessor does not support all fields needed.
         */
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    /**
     * Checks whether the given {@link String} contains valid SEPA DD content.
     *
     * @param xml The XML content to check.
     * @return An {@link Optional} containing the error output if the {@code XML} is erroneous. If
     * {@link Optional#empty()} is returned the XML does not contain errors but it may still contain warnings. If so
     * these are logged.
     * @throws SAXException If any parse error occurs.
     * @throws IOException If any I/O error occurs.
     * @see XMLUtility#isValidXML(java.lang.String, java.net.URL)
     */
    public static Optional<String> validateSepaXML(String xml) throws SAXException, IOException {
        return XMLUtility.isValidXML(xml, SEPA_XSD_SCHEMA);
    }
}
