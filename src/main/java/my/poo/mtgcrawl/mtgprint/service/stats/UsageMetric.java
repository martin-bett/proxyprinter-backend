package my.poo.mtgcrawl.mtgprint.service.stats;

/** Kennzahlen in proxymaster.usage_stats. Der Schlüssel steht so in der DB und in GET /api/admin (usage). */
public enum UsageMetric {
    /** Seitenaufrufe: das Frontend holt /api/version genau einmal pro Laden der Seite */
    PAGE_VIEWS("pageViews"),
    /** Verschiedene Besucher pro Tag, siehe {@link VisitorCounter} */
    VISITORS("visitors"),
    /** Gesuchte Begriffe je Quelle (Einträge in /api/search-cache/lookup) */
    SEARCH_TERMS("searchTerms"),
    /** Fertig erzeugte PDFs */
    DECKS("decks"),
    /** Über den Deckfinder geladene Decks */
    DECK_IMPORTS("deckImports"),
    /** Erfolgreiche Logins */
    LOGINS("logins");

    private final String key;

    UsageMetric(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
