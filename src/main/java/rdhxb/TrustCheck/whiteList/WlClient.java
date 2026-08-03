package rdhxb.TrustCheck.whiteList;


import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class WlClient {

    private final HttpClient wlClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final LocalDate date = LocalDate.now();

    public void getData() throws IOException, InterruptedException {


        String url = "https://wl-api.mf.gov.pl/api/search/nip/";

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url + "5261044039?date="+date))
                .GET()
                .build();


        HttpResponse<String> response = wlClient.send(request, HttpResponse.BodyHandlers.ofString());


        JsonNode root = mapper.readTree(response.body());

        JsonNode subject = root.path("result").path("subject");
        System.out.println(subject);

        String krs = subject.path("krs").asString("brak");
        String region = subject.path("regon").asString("brak");

        System.out.println("KRS = " + krs);
        System.out.println("Region = " + region);





    }


}
