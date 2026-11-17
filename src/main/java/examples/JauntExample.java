package examples;

import com.jaunt.Element;
import com.jaunt.Elements;
import com.jaunt.JauntException;
import com.jaunt.UserAgent;

import java.util.ArrayList;
import java.util.List;

/**
 * Jaunt: fetch the NWS hourly forecast table and print the next hour.
 */
public class JauntExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";
    // NWS requires an identifying User-Agent
    static final String UA = "java-scraping-examples/1.0 (your-email@example.com)";

    public static void main(String[] args) {
        try {
            UserAgent userAgent = new UserAgent();
            
            // Set required User-Agent header
            userAgent.settings.defaultRequestHeaders.put("User-Agent", UA);
            
            // Visit page
            userAgent.visit(URL);

            List<List<String>> rows = new ArrayList<>();
            
            // findEvery returns com.jaunt.Elements
            Elements tableRows = userAgent.doc.findEvery("<tr>");
            for (Element tr : tableRows) {
                List<String> cells = new ArrayList<>();

                // getChildElements() returns direct child elements (td/th)
                for (Element cell : tr.getChildElements()) {
                    cells.add(cell.getTextContent().trim());
                }

                if (!cells.isEmpty()) {
                    rows.add(cells);
                }
            }

            printNextHour(rows);

        } catch (JauntException e) {
            System.err.println("Jaunt error: " + e.getMessage());
        }
    }

    /**
     * Finds the "Hour", "Temperature" and "Precipitation Potential"
     * rows of the first table and prints the first hourly column.
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