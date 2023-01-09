package engine.repository;

import engine.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface ConfigRepository extends JpaRepository<Config,Integer> {

    @Transactional
    @Modifying
    @Query(value = "Insert into Config (Id,Key,Name,Value) values " +
            "(-5,'delay','Пауза при обращении к страницам, м.сек.','150'), " +
            "(-4,'isPoS','Учитывать части речи при индексации','true'), " +
            "(-3,'batch','Размер блока для записи','10'), " +
            "(-2,'sLiSF','Короткая запись ссылок (boolean)','false'), " +
            "(-1,'tps','Потоков на один сайт (Thread)','8')", nativeQuery = true)
    void initData();

    Config findByKey(String key);

}
