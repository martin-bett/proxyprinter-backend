package my.poo.mtgcrawl.mtgprint.service.mapper;

import java.util.Arrays;

import org.springframework.stereotype.Component;

import io.magicthegathering.javasdk.resource.Card;
import my.poo.mtgcrawl.mtgprint.entitiy.CardImageDto;
import my.poo.mtgcrawl.mtgprint.entitiy.CompleteCard;
import my.poo.mtgcrawl.mtgprint.entitiy.CompleteCardDto;
import my.poo.mtgcrawl.mtgprint.entitiy.LanguageCard;

@Component
public class CardMapper {
    public CompleteCardDto toDto(CompleteCard cc){
        if(cc == null) return null;
        CompleteCardDto dto = new CompleteCardDto();
        dto.setName(cc.getName());
        dto.setCardStyles(cc.getCardStyles().stream().map(this::toDto).toList());
        dto.setCustomArts(cc.getCustomArts().stream().map(ca -> ca.getIconLink()).toList());
        return dto;
    }
    public CardImageDto toDto(Card c){
        if(c == null) return null;
        CardImageDto dto = new CardImageDto();
        if(c.getForeignNames() != null && c.getForeignNames().length > 0){
            dto.setForeig(Arrays.stream(c.getForeignNames()).map(LanguageCard::new).toList());
        }
        dto.setIcon(c.getImageUrl());
        dto.setName(c.getName());
        dto.setId(c.getId());
        dto.setNumber(c.getNumber());
        dto.setSet(c.getSet());
        return dto;
    }
}
