package rdhxb.TrustCheck.krsPdf;


import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Path;
import java.nio.file.Paths;

//scrape PDF from krs web using playwright
public class KrsPdfProvider {

    public static void main(String[] args) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Navigate to Playwright website
            page.navigate("https://wyszukiwarka-krs.ms.gov.pl/");

            System.out.println("Page Title: " + page.title());


//            input do wpisania krs
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Numer KRS")).fill("0000543759");

//            przycisk wyszukaj
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Wyszukaj")).click();
//            link wyswietl szczgoly
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Wyświetl szczegóły")).click();
//            page.pause();

//            nowe okno i tu pobieramy pdf
            Locator pdfButton = page.getByRole(AriaRole.BUTTON,
                    new Page.GetByRoleOptions().setName("Pobierz PDF")).first();

            Download download = page.waitForDownload(() -> pdfButton.click());

            Path target = Paths.get("pobrane", "KRS_" + "0000543759" + ".pdf");
            download.saveAs(target);



            // Take screenshot
            page.screenshot(new Page.ScreenshotOptions().setPath(java.nio.file.Paths.get("screenshot.png")));

            browser.close();
        } catch (Exception e) {
            System.out.println("error");
        }
    }
}
