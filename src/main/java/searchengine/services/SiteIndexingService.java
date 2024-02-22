package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.utils.ControllerHelper;
import searchengine.utils.SiteParser;
import searchengine.config.SitesList;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Getter
public class SiteIndexingService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexRepository indexRepository;
    @Autowired
    private SitesList sitesList;
    ControllerHelper controllerHelper = new ControllerHelper();

    private static final String INDEXING = "INDEXING";
    private List<Thread> threads = new ArrayList<>();
    private List<ForkJoinPool> forkJoinPools = new ArrayList<>();

    public ResponseEntity<String> start() {
        ResponseEntity<String> result;
        if (this.indexingIsStarted()) {
            result = controllerHelper.resultError("Индексация уже запущена");
        } else {
            result = controllerHelper.resultOK();
        }
        return result;
    }

    public ResponseEntity<String> stop() {
        ResponseEntity<String> result;
        if (this.indexingIsStopped()) {
            result = controllerHelper.resultError("Индексация не запущена");
        } else {
            result = controllerHelper.resultOK();
        }
        return result;
    }

    public ResponseEntity<String> startIndexPage(String url) {
        ResponseEntity<String> result;
        if (this.indexPage(url)) {
            result = controllerHelper.resultOK();
        } else {
            result = controllerHelper.resultError("Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        return result;
    }

    public boolean indexingIsStarted() {
        AtomicBoolean indexing = new AtomicBoolean(false);
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(INDEXING)) {
                indexing.set(true);
            }
        }
        if (indexing.get()) {
            return true;
        }
        addSites();
        return false;
    }

    public void addSites() {
        threads = new ArrayList<>();
        forkJoinPools = new ArrayList<>();
        clearData();
        sitesList.getSites()
                .forEach(site -> threads.add(new Thread(() -> {
                    Site siteEntity = addSiteInRepository(site);
                    SiteParser siteParser = new SiteParser(siteEntity.getUrl(),
                            siteEntity, sitesList, pageRepository,
                            lemmaRepository, indexRepository);
                    indexForkOrSetError(siteParser, siteEntity, site);
                    siteParser.clearListOfLinks();
                })));
        threads.forEach(Thread::start);
        forkJoinPools.forEach(ForkJoinPool::shutdown);
    }

    public boolean indexingIsStopped() {
        AtomicBoolean indexing = new AtomicBoolean(false);
        for (Site site : siteRepository.findAll()) {
            if (site.getStatus().equals(INDEXING)) {
                indexing.set(true);
            }
        }
        if (!indexing.get()) {
            return true;
        }
        forkJoinPools.forEach(ForkJoinPool::shutdownNow);
        threads.forEach(Thread::interrupt);
        SiteParser.setStopIndexing(true);
        for (Site site : siteRepository.findAll()) {
            site.setLastError("Индексация остановлена пользователем");
            setFailedStatus(site);
        }
        threads.clear();
        forkJoinPools.clear();

        return false;
    }

    public boolean indexPage(String url) {
        AtomicBoolean addPage = new AtomicBoolean(false);
        if (isSiteInRepo(url)) {
            SiteParser.setStopIndexing(false);
            for (searchengine.config.Site site : sitesList.getSites()) {
                indexSinglePage(site, url, addPage);
            }
        }
        return addPage.get();
    }
    private boolean isSiteInRepo(String url) {
        String regex = "^(http(s)?://)?([\\w-]+\\.)+[\\w-]+(/[\\w- ./?%&=]*)?$";
        for (searchengine.config.Site site : sitesList.getSites()) {
            if (url.matches(regex) && (url.contains(site.getUrl()) || site.getUrl().contains(url))) {
                return true;
            }
        }
        return false;
    }

    public Site addSiteInRepository(searchengine.config.Site site) {
        String rootUrl = site.getUrl();
        Site siteEntity = new Site();
        siteEntity.setName(site.getName());
        siteEntity.setStatus(INDEXING);
        siteEntity.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteEntity.setUrl(rootUrl);
        siteRepository.save(siteEntity);
        return siteEntity;
    }

    private void setIndexedStatus(Site siteEntity) {
        siteEntity.setStatus("INDEXED");
        siteEntity.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteRepository.save(siteEntity);
    }

    private void setFailedStatus(Site siteEntity) {
        siteEntity.setStatus("FAILED");
        siteEntity.setStatusTime(new Timestamp(System.currentTimeMillis()));
        siteRepository.save(siteEntity);
    }

    public void clearData() {
        indexRepository.deleteAllInBatch();
        lemmaRepository.deleteAllInBatch();
        pageRepository.deleteAllInBatch();
        siteRepository.deleteAllInBatch();
        SiteParser.setStopIndexing(false);
    }

    private void indexForkOrSetError(SiteParser siteParser, Site siteEntity, searchengine.config.Site site) {
        try {
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPools.add(forkJoinPool);
            forkJoinPool.invoke(siteParser);
            Page mainPage = pageRepository.findByPath(site.getUrl());
            if (mainPage.getPath().equals(site.getUrl())
                    && mainPage.getCode() >= 400) {
                siteEntity.setLastError("Ошибка индексации: главная страница сайта недоступна");
                setFailedStatus(siteEntity);
            } else {
                setIndexedStatus(siteEntity);
            }
        } catch (CancellationException ex) {
            siteEntity.setLastError("Ошибка индексации: главная страница сайта недоступна");
        }
    }

    private void indexSinglePage(searchengine.config.Site site, String url, AtomicBoolean addPage ) {
        if (siteRepository.findByUrl(site.getUrl()) == null) {
            Site siteEntity = addSiteInRepository(site);
            SiteParser siteParser = new SiteParser(url, siteEntity,
                    sitesList, pageRepository,
                    lemmaRepository, indexRepository);
            siteParser.addAdditionalPage();
            setIndexedStatus(siteEntity);
            addPage.set(true);
        } else {
            Site siteEntity = siteRepository.findByUrl(site.getUrl());
            SiteParser siteParser = new SiteParser(url, siteEntity,
                    sitesList, pageRepository,
                    lemmaRepository, indexRepository);
            siteEntity.setStatus(INDEXING);
            siteParser.addAdditionalPage();
            setIndexedStatus(siteEntity);
            addPage.set(true);
        }
    }
}
