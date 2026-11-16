package examples;


import crawlercommons.filters.basic.BasicURLNormalizer;
import de.hshn.mi.crawler4j.frontier.HSQLDBFrontierConfiguration;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.frontier.FrontierConfiguration;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 8. crawler4j (the maintained Heilbronn University fork): a small multi-threaded crawler framework.
 * You write a crawler class with two methods, shouldVisit (which links to follow) and visit (what to do
 * with each downloaded page), then start it from a controller. Here it fetches the single NWS forecast
 * page, reads the hourly table and prints the next hour.
 *
 * Dependency (HSQLDB variant, avoids the Sleepycat license):
 *   de.hs-heilbronn.mi:crawler4j-with-hsqldb:5.1.4
 *
 * crawler4j obeys robots.txt. If nothing prints, check that robots.txt allows this page.
 */
public class Crawler4jExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";
    // Identify your crawler (NWS asks for this). Replace the contact details.
    static final String UA = "java-scraping-examples/1.0 (your-email@example.com)";

    /** The crawler: crawler4j creates it and calls these two methods for us. */
    static class WeatherCrawler extends WebCrawler {

        // Called for every link found on a page. We return false because we only want the seed page,
        // not the pages it links to.
        @Override
        public boolean shouldVisit(Page referringPage, WebURL url) {
            return false;
        }

        // Called after a page has been downloaded and parsed.
        @Override
        public void visit(Page page) {
            if (!(page.getParseData() instanceof HtmlParseData)) return;

            // getHtml() returns the page's raw HTML. We read the table with Jsoup, as in JsoupExample.
            String html = ((HtmlParseData) page.getParseData()).getHtml();
            List<List<String>> rows = new ArrayList<>();
            for (Element tr : Jsoup.parse(html).select("tr")) {
                List<String> cells = new ArrayList<>();
                for (Element cell : tr.children()) cells.add(cell.text());
                rows.add(cells);
            }
            printNextHour(rows);
        }
    }

    public static void main(String[] args) throws Exception {
        // CrawlConfig holds the settings. crawler4j keeps its crawl state in a storage folder, so we use a
        // temporary one. We also cap the crawl at one page.
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(Files.createTempDirectory("crawler4j").toString());
        config.setUserAgentString(UA);
        config.setMaxPagesToFetch(1);

        // The controller wires the parts together: the fetcher downloads pages, the robots.txt server checks
        // what we're allowed to crawl, and the frontier is the queue of URLs waiting to be visited.
        BasicURLNormalizer normalizer = BasicURLNormalizer.newBuilder()
                .idnNormalization(BasicURLNormalizer.IdnNormalization.NONE).build();
        PageFetcher pageFetcher = new PageFetcher(config, normalizer);
        FrontierConfiguration frontier = new HSQLDBFrontierConfiguration(config, 10);
        RobotstxtServer robotstxtServer = new RobotstxtServer(new RobotstxtConfig(), pageFetcher, frontier.getWebURLFactory());
        CrawlController controller = new CrawlController(config, normalizer, pageFetcher, robotstxtServer, frontier);

        // The seed is the first URL to fetch. start() runs one crawler thread and waits until it finishes.
        controller.addSeed(URL);
        CrawlController.WebCrawlerFactory<WeatherCrawler> factory = WeatherCrawler::new;
        controller.start(factory, 1);
    }

    /**
     * Each row is the list of its cell texts. Finds the "Hour", "Temperature" and "Precipitation Potential"
     * rows of the first table and prints the first hourly column, which is the next forecast hour.
     */
    static void printNextHour(List<List<String>> rows) {
        List<String> hours = null, temps = null, precip = null;
        for (List<String> cells : rows) {
            if (cells.size() < 2) continue;
            String label = cells.get(0).replace('\u00A0', ' ').trim();
            // Only accept data rows: the first value must be a number. This skips legend rows with similar labels.
            boolean numeric = cells.get(1).trim().matches("-?\\d+");
            if (hours == null && label.startsWith("Hour")) hours = cells;
            else if (temps == null && numeric && label.startsWith("Temperature")) temps = cells;
            else if (precip == null && numeric && label.startsWith("Precipitation Potential")) precip = cells;
        }
        if (hours == null || temps == null) {
            System.out.println("Hourly table not found (blocked, or the page layout changed).");
            return;
        }
        String zone = hours.get(0).replaceAll(".*\\((.*)\\).*", "$1");   // "Hour (CST)" -> "CST"
        String p = (precip != null && precip.size() > 1) ? precip.get(1) : "?";

        System.out.println("Next forecast hour:      " + hours.get(1) + ":00 " + zone);
        System.out.println("Temperature:             " + temps.get(1) + "°F");
        System.out.println("Chance of precipitation: " + p + "%");
    }
}