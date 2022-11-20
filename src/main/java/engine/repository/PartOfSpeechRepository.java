package engine.repository;

import engine.entity.PartsOfSpeech;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PartOfSpeechRepository extends JpaRepository<PartsOfSpeech,Integer> {
    @Modifying
    @Transactional
    @Query(value = "Insert Into Parts_Of_Speech (include, Name, Short_Name) values " +
            "(false,'Вводное слово','ВВОДН')," +
            "(true,'Глагол','Г'), " +
            "(true,'Деепричастие','ДЕЕПРИЧАСТИЕ')," +
            "(true,'Инфинитив','ИНФИНИТИВ')," +
            "(true,'Прилагательное  [краткая форма]','КР_ПРИЛ')," +
            "(false,'Междометие','МЕЖД')," +
            "(false,'Местоимение','МС')," +
            "(false,'Местоимение Принадлежащее','МС-П')," +
            "(true,'Наречие','Н')," +
            "(true,'Прилагательное','П')," +
            "(true,'Предикатив','ПРЕДК')," +
            "(true,'Предлог', 'ПРЕДЛ')," +
            "(true,'Причастие','ПРИЧАСТИЕ')," +
            "(true,'Существительное','С')," +
            "(false,'Союз','СОЮЗ')," +
            "(false,'Частица','ЧАСТ')," +
            "(true,'Числительное','ЧИСЛ')," +
            "(true,'Числительное порядковое','ЧИСЛ-П')", nativeQuery = true)
    void initData();

    List<PartsOfSpeech> findByInclude(Boolean include);
}
