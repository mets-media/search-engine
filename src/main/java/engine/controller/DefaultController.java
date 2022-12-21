package engine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class DefaultController {

    @RequestMapping("/admin") // ищет файл index.html в resources/templates
    public String index() {
        return "index";
    }


}
