/*
 * Decompiled with CFR 0.152.
 */
package my.poo.mtgcrawl.mtgprint.entitiy;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import my.poo.mtgcrawl.mtgprint.enums.ApiCategory;

@Converter(autoApply=true)
public class ApiCategoryConverter
implements AttributeConverter<ApiCategory, String> {
    @Override
    public String convertToDatabaseColumn(ApiCategory attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public ApiCategory convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ApiCategory.valueOf(dbData);
    }
}

