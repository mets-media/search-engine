package engine.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DefaultController {
    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
    public String index() {
        return "index.html";
    }
}
