package examples;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.ArrayList;
import java.util.List;
/**
 * 4. Playwright for Java: load the NWS hourly forecast table in headless Chromium and print the next hour.
 * This page doesn't need a browser (Jsoup does the same job faster), but Playwright works on it too
 * and is what you'd switch to if the site started rendering the table with JavaScript.
 *
 * First run: Playwright downloads its browser binaries automatically.
 */
public class PlaywrightExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";

    public static void main(String[] args) {
        // Playwright starts a real browser (here Chromium) and controls it from code.
        // "Headless" means it runs without showing a window.
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = browser.newPage();
            // navigate() loads the page like typing the URL in the address bar, and waits for it to load.
            page.navigate(URL);

            // A Locator points at elements on the page. locator("tr").all() gives one Locator per table row.
            // The XPath "./td | ./th" means "this row's direct cells", and allInnerTexts() returns the text
            // of each cell as the browser displays it.
            List<List<String>> rows = new ArrayList<>();
            for (Locator tr : page.locator("tr").all()) {
                rows.add(tr.locator("xpath=./td | ./th").allInnerTexts());   // direct cells only
            }
            printNextHour(rows);
            browser.close();
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