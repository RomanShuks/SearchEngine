package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;
import searchengine.model.Site;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {
    Page findByPath(String path);

    Page findById(int id);

    int countBySite(Site site);

    long count();
}
