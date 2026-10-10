package my.poo.mtgcrawl.mtgprint.config;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;

@Configuration
public class JsonConfig {

    /**
     * Ein leerer Text ("") zählt für Map-Felder als fehlender Wert (null). Formulare schicken für leere Felder "",
     * ohne diese Regel lehnt Jackson z. B. "hintTranslations": "" mit 400 ab.
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer emptyStringAsNullForMaps() {
        return builder -> builder.postConfigurer(mapper ->
                mapper.coercionConfigFor(LogicalType.Map).setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull));
    }
}
