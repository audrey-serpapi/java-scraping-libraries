package examples;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetch the National Weather Service hourly forecast table for Austin, TX 
 * and print the next hour using only Jsoup.
 */
public class JsoupExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";
    static final String UA = "java-scraping-examples/1.0 (your-email@example.com)";

    public static void main(String[] args) {
        try {
            Document doc = Jsoup.connect(URL)
                    .userAgent(UA)
                    .timeout(15_000)
                    .get();

            List<List<String>> rows = new ArrayList<>();
            for (Element tr : doc.select("tr")) {
                List<String> cells = new ArrayList<>();
                for (Element cell : tr.children()) {
                    cells.add(cell.text().trim());
                }
                if (!cells.isEmpty()) {
                    rows.add(cells);
                }
            }

            printNextHour(rows);

        } catch (IOException e) {
            System.err.println("Network or parsing error: " + e.getMessage());
        }
    }

    /**
     * Finds the "Hour", "Temperature", and "Precipitation Potential" rows 
     * and prints the first hourly forecast column.
     */
    static void printNextHour(List<List<String>> rows) {
        List<String> hours = null;
        List<String> temps = null;
        List<String> precip = null;

        for (List<String> cells : rows) {
            if (cells.size() < 2) continue;
            String label = cells.get(0).replace('\u00A0', ' ').trim();

            // Only accept data rows: the first value must be a number.
            // This skips legend rows that have a similar label.
            boolean numeric = cells.get(1).trim().matches("-?\\d+");

            if (hours == null && label.startsWith("Hour")) {
                hours = cells;
            } else if (temps == null && numeric && label.startsWith("Temperature")) {
                temps = cells;
            } else if (precip == null && numeric && label.startsWith("Precipitation Potential")) {
                precip = cells;
            }
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