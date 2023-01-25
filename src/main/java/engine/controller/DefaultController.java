package engine.controller;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.ui.LoadMode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Controller
public class DefaultController {
    @RequestMapping(value = "/admin", method = RequestMethod.GET) // ищет файл index.html в resources/templates
//    public String index() {
//        return "index.html";
//    }

    public void index() {
        UI.getCurrent().getPage().addJavaScript("index.html", LoadMode.EAGER);
    }
}
