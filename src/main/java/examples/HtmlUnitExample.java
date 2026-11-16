package examples;

import org.htmlunit.WebClient;
import org.htmlunit.html.HtmlPage;
import org.htmlunit.html.HtmlTableCell;
import org.htmlunit.html.HtmlTableRow;

import java.util.ArrayList;
import java.util.List;

/**
 * 6. HtmlUnit: a GUI-less browser written in Java. JavaScript is switched off here because the NWS table
 * is plain HTML, which also makes it fast. Prints the next hour.
 */
public class HtmlUnitExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";

    public static void main(String[] args) throws Exception {
        // HtmlUnit's WebClient is a browser written entirely in Java: no window and no Chrome install needed.
        try (WebClient client = new WebClient()) {
            // This page is plain HTML, so JavaScript and CSS can be switched off to keep things fast.
            client.getOptions().setJavaScriptEnabled(false);
            client.getOptions().setCssEnabled(false);

            // getPage() downloads the page and parses it into an HtmlPage.
            HtmlPage page = client.getPage(URL);

            // getByXPath("//tr") finds every table row. XPath is a query language for HTML trees: "//" means
            // "anywhere in the page". getCells() returns a row's cells, and asNormalizedText() their visible text.
            List<List<String>> rows = new ArrayList<>();
            for (Object o : page.getByXPath("//tr")) {
                List<String> cells = new ArrayList<>();
                for (HtmlTableCell cell : ((HtmlTableRow) o).getCells()) cells.add(cell.asNormalizedText().trim());
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
            if (hours == null && label.startsWith("Hour")) hours = cells;
            else if (temps == null && label.startsWith("Temperature")) temps = cells;
            else if (precip == null && label.startsWith("Precipitation Potential")) precip = cells;
        }
        if (hours == null || temps == null) {
            System.out.println("Hourly table not found (blocked, or the page layout changed).");
            return;
        }
        String zone = hours.get(0).replaceAll(".*\\((.*)\\).*", "$1");   // "Hour (CST)" -> "CST"
        String p = (precip != null && precip.size() > 1) ? precip.get(1) : "?";
        System.out.printf("Next hour: %s:00 %s  %s°F  precip %s%%%n", hours.get(1), zone, temps.get(1), p);
    }
}