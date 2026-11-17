package examples;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Selenium: drive a real Chrome browser from Java, load the NWS hourly forecast table and print the next hour.
 * Like Playwright, this is more than the page needs (Jsoup does the same job faster), but it's what you'd
 * use if the table were rendered by JavaScript.
 *
 * Requires Chrome installed on your machine. Selenium Manager (built into Selenium 4.6+) downloads the
 * matching ChromeDriver automatically on first run.
 */
public class SeleniumExample {
    static final String URL = "https://forecast.weather.gov/MapClick.php?lat=30.2672&lon=-97.7431&lg=english&FcstType=digital";
    // Identify your scraper (NWS asks for this). Replace the contact details.
    static final String UA = "java-scraping-examples/1.0 (your-email@example.com)";

    public static void main(String[] args) {
        // ChromeOptions configures the browser before it starts. "--headless=new" runs Chrome without a window.
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--user-agent=" + UA);

        // ChromeDriver starts Chrome and lets us control it from code.
        WebDriver driver = new ChromeDriver(options);
        try {
            // get() loads the page like typing the URL in the address bar, and waits for it to load.
            driver.get(URL);

            // findElements(By.tagName("tr")) returns every table row. The XPath "./td | ./th" means
            // "this row's direct cells", and getText() returns each cell's visible text.
            List<List<String>> rows = new ArrayList<>();
            for (WebElement tr : driver.findElements(By.tagName("tr"))) {
                List<String> cells = new ArrayList<>();
                for (WebElement cell : tr.findElements(By.xpath("./td | ./th"))) {
                    cells.add(cell.getText().trim());
                }
                rows.add(cells);
            }
            printNextHour(rows);
        } finally {
            // quit() closes the browser and stops ChromeDriver. Without it, Chrome keeps running.
            driver.quit();
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