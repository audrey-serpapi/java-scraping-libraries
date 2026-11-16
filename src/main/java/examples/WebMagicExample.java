package examples;

import org.jsoup.nodes.Element;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * 7. WebMagic: a PageProcessor says how to read each page; the framework handles downloading, scheduling
 * and output. Here it reads the NWS hourly table and prints the next hour. For a single page this is
 * more structure than you need, but it shows the shape you'd use when crawling many locations.
 */
public class WebMagicExample implements PageProcessor {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";

    private final Site site = Site.me()
            .setRetryTimes(2)
            .setSleepTime(1000)
            .setUserAgent("java-scraping-examples/1.0 (your-email@example.com)");   // replace with your contact

    // WebMagic splits a crawl into parts: the Spider runs the crawl and downloads pages, this PageProcessor
    // decides what to extract from each one, and Site holds settings such as retries, delay and user agent.
    // process() is called once for every page the Spider downloads.
    @Override
    public void process(Page page) {
        // getHtml().getDocument() gives a Jsoup Document, so reading the table works just like in JsoupExample.
        List<List<String>> rows = new ArrayList<>();
        for (Element tr : page.getHtml().getDocument().select("tr")) {
            List<String> cells = new ArrayList<>();
            for (Element cell : tr.children()) cells.add(cell.text());
            rows.add(cells);
        }
        printNextHour(rows);
    }

    @Override
    public Site getSite() {
        return site;
    }

    public static void main(String[] args) {
        // Spider.create() wires the processor into a crawler, addUrl() sets the starting page, and run() starts it.
        Spider.create(new WebMagicExample()).addUrl(URL).thread(1).run();
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