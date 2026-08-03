package rdhxb.TrustCheck.krs;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class KrsClient {

    private final HttpClient krsClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    String url = "https://api-krs.ms.gov.pl/api/krs/OdpisAktualny/";

    public void getData() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url + "0000128080" + "?rejestr=P&format=json"))
                .GET()
                .build();

        HttpResponse<String> response = krsClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = mapper.readTree(response.body());

        for (JsonNode node : root){
            System.out.println(node);
        }

    }


}
