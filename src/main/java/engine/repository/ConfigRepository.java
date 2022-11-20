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
            "(-3,'batch','batch save size (Integer)','500'), " +
            "(-2,'sLiSF','saveLinksInShortFormat (boolean)','false'), " +
            "(-1,'tps','Threads per Site','4')", nativeQuery = true)
    void initData();

    Config findByKey(String key);

}
