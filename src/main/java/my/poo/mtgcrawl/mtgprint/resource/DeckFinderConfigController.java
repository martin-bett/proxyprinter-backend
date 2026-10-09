package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import my.poo.mtgcrawl.mtgprint.entitiy.DeckFinderConfig;
import my.poo.mtgcrawl.mtgprint.entitiy.VisitorIdentifier;
import my.poo.mtgcrawl.mtgprint.entitiy.dto.DeckFinderConfigDto;
import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderException;
import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderFetchService;
import my.poo.mtgcrawl.mtgprint.service.deckfinder.DeckFinderFetchService.DeckFinderResponse;
import my.poo.mtgcrawl.mtgprint.service.rest.DeckFinderConfigService;

@RestController
@RequestMapping("/api/deckfinder")
@AllArgsConstructor
public class DeckFinderConfigController {

    private final DeckFinderConfigService service;
    private final DeckFinderFetchService fetchService;

    @GetMapping
    public List<DeckFinderConfigDto> getAllConfigs(HttpServletRequest httpRequest) {
        return service.findAllVisible(extractVisitorIdentifier(httpRequest));
    }

    @GetMapping("/create")
    public ResponseEntity<Long> createConfig(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(service.createConfig(extractVisitorIdentifier(httpRequest)));
    }

    @PostMapping("/copy")
    public ResponseEntity<Long> copyConfig(@RequestBody DeckFinderConfigDto request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(service.copyConfig(request, extractVisitorIdentifier(httpRequest)));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteConfig(@RequestBody DeckFinderConfigDto request, HttpServletRequest httpRequest) {
        service.deleteConfig(request, extractVisitorIdentifier(httpRequest));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/publish")
    public ResponseEntity<Long> publishConfig(@RequestBody DeckFinderConfigDto request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(service.publishConfig(request, extractVisitorIdentifier(httpRequest)));
    }

    @PostMapping("/edit")
    public ResponseEntity<Long> editConfig(@RequestBody DeckFinderConfigDto request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(service.editConfig(request, extractVisitorIdentifier(httpRequest)));
    }

    @GetMapping("/{configId}/check")
    public ResponseEntity<Boolean> checkOwner(@PathVariable Long configId, HttpServletRequest httpRequest) {
        DeckFinderConfig config = service.findVisible(configId, extractVisitorIdentifier(httpRequest));
        return ResponseEntity.ok(config != null && service.isOwner(config, extractVisitorIdentifier(httpRequest)));
    }

    /** Deck per Link oder ID laden; Antwort der Seite unverändert (JSON oder Text). */
    @GetMapping("/{configId}/fetch")
    public ResponseEntity<String> fetchDeck(@PathVariable Long configId, @RequestParam("input") String input,
            HttpServletRequest httpRequest) {
        DeckFinderConfig config = visibleConfig(configId, httpRequest);
        return toResponse(fetchService.fetchDeck(config, input));
    }

    /** Decks per Name suchen; Antwort der Seite unverändert (JSON). */
    @GetMapping("/{configId}/search")
    public ResponseEntity<String> search(@PathVariable Long configId, @RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") int page, HttpServletRequest httpRequest) {
        DeckFinderConfig config = visibleConfig(configId, httpRequest);
        return toResponse(fetchService.search(config, query, page));
    }

    @ExceptionHandler(DeckFinderException.class)
    public ResponseEntity<Map<String, Object>> handle(DeckFinderException e) {
        return ResponseEntity.status(e.getStatus()).body(e.toBody());
    }

    private DeckFinderConfig visibleConfig(Long configId, HttpServletRequest httpRequest) {
        DeckFinderConfig config = service.findVisible(configId, extractVisitorIdentifier(httpRequest));
        if (config == null) {
            throw new DeckFinderException(HttpStatus.NOT_FOUND, "deckfinder.configNotFound", "Deckfinder config not found.", Map.of());
        }
        return config;
    }

    private static ResponseEntity<String> toResponse(DeckFinderResponse response) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, response.contentType() + ";charset=UTF-8")
                .body(response.body());
    }

    private VisitorIdentifier extractVisitorIdentifier(HttpServletRequest request) {
        return new VisitorIdentifier(request.getRemoteAddr(), request.getHeader("X-Fingerprint"));
    }
}
