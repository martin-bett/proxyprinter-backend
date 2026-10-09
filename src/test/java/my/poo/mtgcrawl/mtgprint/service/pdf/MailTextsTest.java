package my.poo.mtgcrawl.mtgprint.service.pdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MailTextsTest {

    @Test
    void unknownOrMissingLanguageMeansEnglish() {
        assertEquals("en", MailTexts.normalize(null));
        assertEquals("en", MailTexts.normalize("xx"));
        assertEquals("de", MailTexts.normalize(" DE-at "));
        assertEquals("Your proxy PDF is ready", MailTexts.get("xx", "ready.subject"));
    }

    @Test
    void everyLanguageHasEveryTextWithoutLeftoverQuotes() {
        List<String> keys = List.of("greeting", "ready.subject", "ready.body", "ready.size", "ready.validOne",
                "ready.validMany", "failed.subject", "failed.body", "failed.again");
        for (String language : MailTexts.LANGUAGES) {
            for (String key : keys) {
                String text = MailTexts.get(language, key, "7");
                assertFalse(text.isBlank(), language + " " + key);
                assertFalse(text.contains("''"), language + " " + key);
            }
        }
    }

    @Test
    void germanTextsAreUsedAndParametersFilledIn() {
        assertEquals("Der Link funktioniert 7 Tage.", MailTexts.get("de", "ready.validMany", 7));
        assertTrue(MailTexts.get("fr", "failed.subject").contains("n'a pas"));
    }
}
