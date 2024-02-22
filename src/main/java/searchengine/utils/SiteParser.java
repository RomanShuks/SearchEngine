package searchengine.utils;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import searchengine.config.SitesList;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RecursiveTask;

import static java.lang.Thread.sleep;

public class SiteParser extends RecursiveTask<Integer> {

    private String page;
    private String siteName;
    private Site siteEntity;
    private static boolean stopIndexing;
    private int pageCount;

    public static CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet<>();
    private static PageRepository pageRepository;
    private static LemmaRepository lemmaRepository;
    private static IndexRepository indexRepository;
    private static SitesList sitesList = new SitesList();

    private List<SiteParser> children;

    public SiteParser(String page, String siteName, Site siteEntity) {
        children = new ArrayList<>();
        this.page = page;
        this.siteName = siteName;
        this.siteEntity = siteEntity;
        allLinks.add(page);
    }

    public SiteParser(String siteName, Site siteEntity, SitesList sitesList,
                      PageRepository pageRepository, LemmaRepository lemmaRepository,
                      IndexRepository indexRepository) {
        this(siteName, siteEntity.getUrl(), siteEntity);
        allLinks.add(siteEntity.getUrl());
        allLinks.add(siteEntity.getUrl() + "/");
        SiteParser.sitesList = sitesList;
        SiteParser.pageRepository = pageRepository;
        SiteParser.lemmaRepository = lemmaRepository;
        SiteParser.indexRepository = indexRepository;
    }

    @Override
    protected Integer compute() {
        if (stopIndexing) {
            children.clear();
            return 0;
        }
        try {
            sleep(random());
            Connection.Response response = Jsoup.connect(page)
                    .ignoreHttpErrors(true)
                    .userAgent(sitesList.getUserAgent())
                    .referrer(sitesList.getReferrer())
                    .execute();
            Document doc = response.parse();
            addPage(response, doc);
            Elements links = doc.select("a");
            processingLinks(links);
        } catch (InterruptedException | IOException | NullPointerException ex) {
            ex.printStackTrace();
        }
        for (SiteParser parser : children) {
            pageCount += parser.join();
        }
        return pageCount;
    }

    public void addAdditionalPage() {
        try {
            Connection.Response response = Jsoup.connect(page)
                    .ignoreHttpErrors(true)
                    .userAgent(sitesList.getUserAgent())
                    .referrer(sitesList.getReferrer())
                    .execute();
            Document doc = response.parse();
            addPage(response, doc);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPage(Connection.Response response, Document doc) {
        Page pageEntity = pageRepository.findByPath(page);
        if (pageEntity == null) {
            pageEntity = new Page();
        }
        pageEntity.setContent(doc.html());
        pageEntity.setPath(page);
        pageEntity.setCode(response.statusCode());
        pageEntity.setSite(siteEntity);
        pageRepository.save(pageEntity);
        if (response.statusCode() < 400) {
            LemmaSearcher lemmaSearcher = new LemmaSearcher(pageEntity, lemmaRepository, indexRepository);
            lemmaSearcher.putLemmasInBase();
        }
    }

    private void addChild(String url) {
        SiteParser child = new SiteParser(url, siteName, siteEntity);
        children.add(child);
        child.fork();
    }

    private boolean isCorrected(String url) {
        return (url.contains(siteName) &&
                !allLinks.contains(url) &&
                !url.contains("#") &&
                !url.matches("n+(.jpg|.jpeg|.png|.pdf|gif|.zip|.tar|.jar|.gz|.svg|ppt|.pptx)"));
    }

    private int random() {
        return (int) Math.round(Math.random() * 51 + 100);
    }

    public void clearListOfLinks() {
        allLinks.clear();
    }

    public static void setStopIndexing(boolean stopIndexing) {
        SiteParser.stopIndexing = stopIndexing;
    }

    private void processingLinks(Elements links) {
        for (Element link : links) {
            String url = link.attr("href");
            if (!url.contains("http")) {
                if (!url.startsWith("/") && url.length() > 1) {
                    url = "/" + url;
                }
                url = siteName + url;
            }
            if (isCorrected(url)) {
                addChild(url);
            }
        }
    }
}

