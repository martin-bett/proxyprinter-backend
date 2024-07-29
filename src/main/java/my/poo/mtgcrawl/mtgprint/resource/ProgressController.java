package my.poo.mtgcrawl.mtgprint.resource;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    @GetMapping("/{id}")
    public SseEmitter getProgress(@PathVariable String id) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.put(id, emitter);
        emitter.onCompletion(() -> emitters.remove(id));
        emitter.onTimeout(() -> emitters.remove(id));
        return emitter;
    }

    public void sendProgress(String id, String message) {
        SseEmitter emitter = emitters.get(id);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(message));
            } catch (IOException e) {
                emitters.remove(id);
            }
        }
    }
}