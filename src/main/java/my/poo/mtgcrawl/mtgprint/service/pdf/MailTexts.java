package my.poo.mtgcrawl.mtgprint.service.pdf;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/** Texts of the PDF mails in the language of the user interface (bundles in {@code i18n/mail_<lang>.properties}). */
final class MailTexts {
    private static final String BUNDLE = "i18n.mail";
    static final List<String> LANGUAGES = List.of("en", "de", "fr", "it", "es", "ru", "ja", "zh");

    private MailTexts() {
    }

    /** Known language code in lower case, English for anything else. */
    static String normalize(String language) {
        if (language == null) {
            return "en";
        }
        String code = language.trim().toLowerCase(Locale.ROOT);
        int dash = code.indexOf('-');
        if (dash > 0) {
            code = code.substring(0, dash);
        }
        return LANGUAGES.contains(code) ? code : "en";
    }

    static String get(String language, String key, Object... args) {
        // No fallback to the JVM default locale: unknown means the English base bundle.
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(normalize(language)),
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        String pattern = bundle.getString(key);
        return new MessageFormat(pattern, Locale.forLanguageTag(normalize(language))).format(args);
    }
}
