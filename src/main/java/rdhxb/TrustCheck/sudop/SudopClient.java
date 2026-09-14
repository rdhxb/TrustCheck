package rdhxb.TrustCheck.sudop;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Klient SUDOP (UOKiK) - rejestr pomocy publicznej.
 *
 * SUDOP jest API asynchronicznym: zapytanie nie zwraca danych od razu,
 * tylko rejestruje wyszukiwanie i odsyla (303) pod adres, gdzie wynik sie pojawi.
 *
 * Endpoint "bez-kolejki" przekierowuje od razu na /api/wynik/{requestId}.
 * Zwykly /przypadki-pomocy idzie najpierw przez /api/kolejka/{queueId},
 * ktora zwraca 200 z tekstem "Przygotowywanie odpowiedzi..." dopoki liczy,
 * a dopiero po zakonczeniu odsyla (303) na /api/wynik/{requestId}.
 */
@Component
public class SudopClient {

    // HttpClient domyslnie NIE podaza za przekierowaniami (Redirect.NEVER)
    // i o to chodzi - musimy sami odczytac naglowek Location z odpowiedzi 303.
    private final HttpClient sudopClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private static final String BASE = "https://api-sudop.uokik.gov.pl";

    String url = BASE + "/sudop-api/v1/api/przypadki-pomocy-bez-kolejki"
            + "?nip-beneficjenta=5260211587&strona=1"
            + "&dzien-udzielenia-pomocy-od=2015-01-01&dzien-udzielenia-pomocy-do=2026-09-01";

    public void getData() throws IOException, InterruptedException {

        // Krok 1: rejestracja wyszukiwania. Sukcesem jest tu 303, nie 200.
        HttpResponse<String> registered = send(url);

        if (registered.statusCode() != 303) {
            System.out.println("Nieoczekiwany status: " + registered.statusCode() + " " + registered.body());
            return;
        }

        // Location jest wzgledny (/sudop-api/api/wynik/...), wiec doklejamy host.
        String wynikUrl = BASE + registered.headers().firstValue("location").orElseThrow();

        // Krok 2: czekamy, az backend policzy wynik.
        JsonNode root = pollWynik(wynikUrl);
        if (root == null) {
            System.out.println("Wynik nie byl gotowy na czas");
            return;
        }

        // Krok 3: odpowiedz ma ksztalt {"liczba-wynikow": N, "wyniki": [...]}.
        // Uwaga: nazwy pol zawieraja myslniki, wiec czytamy je przez path(), nie przez gettery.
        System.out.println("Liczba wynikow = " + root.path("liczba-wynikow").asInt());

        for (JsonNode wynik : root.path("wyniki")) {
            System.out.println(wynik.path("dzien-udzielenia-pomocy").asString("brak")
                    + " | " + wynik.path("nazwa-beneficjenta").asString("brak")
                    + " | " + wynik.path("forma-pomocy-nazwa").asString("brak")
                    + " | " + wynik.path("wartosc-brutto-pln").asString("brak") + " PLN");
        }
    }

    /**
     * Odpytuje adres wyniku co 2 sekundy (max ok. 60 s).
     * Zwraca sparsowany JSON albo null, jesli wynik nie pojawil sie na czas.
     */
    private JsonNode pollWynik(String wynikUrl) throws IOException, InterruptedException {
        for (int i = 0; i < 30; i++) {
            HttpResponse<String> response = send(wynikUrl);

            // 200 = wynik gotowy.
            if (response.statusCode() == 200) {
                return mapper.readTree(response.body());
            }

            // 404 = backend jeszcze liczy - to normalny stan, probujemy dalej.
            // Cokolwiek innego (np. 500) to realny blad, nie ma sensu czekac.
            if (response.statusCode() != 404) {
                System.out.println("Blad wyniku: " + response.statusCode() + " " + response.body());
                return null;
            }

            Thread.sleep(2000);
        }
        return null;
    }

    private HttpResponse<String> send(String uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(uri)).GET().build();
        return sudopClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

}
