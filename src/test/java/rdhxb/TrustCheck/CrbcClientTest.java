package rdhxb.TrustCheck;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import rdhxb.TrustCheck.crbr.CrbrClient;

@SpringBootTest
public class CrbcClientTest {

    @Autowired
    CrbrClient client;

    @Test
    void strzal() {
        System.setProperty(
                "com.sun.xml.ws.transport.http.client.HttpTransportPipe.dump", "true");
        System.setProperty("com.sun.xml.ws.transport.http.HttpAdapter.dumpTreshold", "999999");
//        Rafal brzozka INPOST
        var odp = client.poPesel("77111309319");
        System.out.println(odp.getListaInformacjiOSpolkachIBeneficjentach().getSpolkaIBeneficjenci().getFirst().getNazwa());   // sprawdź nazwę gettera
    }
}
