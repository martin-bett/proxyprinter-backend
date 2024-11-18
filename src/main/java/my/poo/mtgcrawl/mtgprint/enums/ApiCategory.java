package my.poo.mtgcrawl.mtgprint.enums;

public enum ApiCategory {
        MTG,
        POKE,
        YUGIOH,
        MISC;
        public static ApiCategory byName(String name){
                if(name== null) return null;
                switch (name.toUpperCase()) {
                        case "MTG":return MTG;
                        case "POKE":return POKE;
                        case "YUGIOH":return YUGIOH;
                        default:return MISC;
                }
        }
}
