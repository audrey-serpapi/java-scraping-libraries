package examples;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * 2. Apache HttpClient + Jsoup: HttpClient fetches the page (full control over headers, proxies, cookies,
 * retries), Jsoup parses the hourly forecast table. Prints the next hour.
 */
public class HttpClientExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";
    // Identify your scraper (NWS asks for this). Replace the contact details.
    static final String UA = "java-scraping-examples/1.0 (your-email@example.com)";

    public static void main(String[] args) throws Exception {
        // HttpClient only downloads pages: it can't read HTML. createDefault() builds a reusable client, and
        // try-with-resources closes it when we're done.
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            // HttpGet describes the request. The User-Agent header tells the site who is making it.
            HttpGet request = new HttpGet(URL);
            request.setHeader("User-Agent", UA);
            // execute() sends the request. The lambda turns the response body into a String of raw HTML.
            String html = client.execute(request, response -> EntityUtils.toString(response.getEntity()));

            // Jsoup does the reading, so the two libraries split the job: HttpClient fetches, Jsoup parses.
            // select("tr") finds table rows, and children() gives each row's direct cells.

            List<List<String>> rows = new ArrayList<>();
            for (Element tr : Jsoup.parse(html).select("tr")) {
                List<String> cells = new ArrayList<>();
                for (Element cell : tr.children()) cells.add(cell.text());
                rows.add(cells);
            }
            printNextHour(rows);
        }
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