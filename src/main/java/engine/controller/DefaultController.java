package engine.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class DefaultController {
    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
    public String admin() {
        return "/admin/admin.html";
    }


}
