package my.poo.mtgcrawl.mtgprint.service.rest.exc;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WrongFormatException extends Exception implements WrapperException{
    private final Exception actual;
}
