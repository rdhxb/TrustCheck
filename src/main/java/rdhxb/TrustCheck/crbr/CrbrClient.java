package rdhxb.TrustCheck.crbr;

import jakarta.xml.ws.BindingProvider;
import org.springframework.stereotype.Service;
import pl.gov.mf.crbr.client.*;
import rdhxb.TrustCheck.crbr.CrbrProperties;

import java.util.Map;

@Service
public class CrbrClient {

    private final ApiPrzegladoweCRBRService service = new ApiPrzegladoweCRBRService();
    private final CrbrProperties props;

    public CrbrClient(CrbrProperties props) {
        this.props = props;
    }

    public PobierzInformacjeOSpolkachIBeneficjentachOdpowiedzDaneTyp poPesel(String pesel) {
        var szczegoly = new SzczegolyWnioskuTyp();
        szczegoly.setPESEL(pesel);

        var dane = new PobierzInformacjeOSpolkachIBeneficjentachDaneTyp();
        dane.setSzczegolyWniosku(szczegoly);

        return port().pobierzInformacjeOSpolkachIBeneficjentach(dane);
    }

    private ApiPrzegladoweCRBR port() {
        ApiPrzegladoweCRBR port = service.getApiPrzegladoweCRBRPort();
        Map<String, Object> ctx = ((BindingProvider) port).getRequestContext();
        ctx.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, props.endpoint());
        ctx.put("com.sun.xml.ws.connect.timeout", props.connectTimeout());
        ctx.put("com.sun.xml.ws.request.timeout", props.requestTimeout());
        return port;
    }
}