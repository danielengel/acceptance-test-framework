package dev.acceptance.two;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class HelloWorldController {
    @GetMapping("/hello-world")
    Map<String, String> helloWorld() {
        return Map.of("message", "Hello, world!");
    }
}
