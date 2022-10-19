package engine.auxEntity;

import engine.auxRepository.MainGridRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;

public class AuxData {

    public static void fillStructure(JpaRepository mainGridRepository) {


        mainGridRepository.deleteAll();
        if (mainGridRepository.count() == 0) {
            mainGridRepository.save(new MainGrid(1l, "url", FieldType.TEXT_FIELD, "Сайт", 0, "",false));
            mainGridRepository.save(new MainGrid(2l, "page_count", FieldType.NUMBER_FIELD, "Страницы", 120, "html в БД: ",true));
            mainGridRepository.save(new MainGrid(3l, "", FieldType.EDIT_BUTTON, "Edit", 150, "",false));
            mainGridRepository.save(new MainGrid(4L,"",FieldType.PARSE_BUTTON,"Parse",0,"",false));
        }
    }
}
