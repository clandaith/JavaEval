package com.clicklease.app.bureau;

import com.clicklease.app.util.TaxIdMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * HTTP client for the credit bureau. Every call costs money, so this is the
 * single choke point for vendor calls. Read timeout defaults to 2s per the
 * vendor SLA so a slow bureau can never hang the UI.
 */
@Component
public class BureauClient {

    private static final Logger logger = LoggerFactory.getLogger(BureauClient.class);

    private final RestClient restClient;
    private final WebServerApplicationContext webServerContext;

    public BureauClient(WebServerApplicationContext webServerContext,
                        @Value("${bureau.timeout-ms:2000}") long timeoutMs) {
        this.webServerContext = webServerContext;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(500));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public int fetchScore(String taxId) {
        String url = "http://localhost:" + webServerContext.getWebServer().getPort()
                + "/mock/bureau/score";
        logger.info("DEBUG MSG: Calling credit bureau for taxId={}", TaxIdMasker.mask(taxId));
        try {
            ScoreResponse response = restClient.post()
                    .uri(url)
                    .body(new MockCreditBureauController.ScoreRequest(taxId))
                    .retrieve()
                    .body(ScoreResponse.class);
            if (response == null) {
                throw new BureauUnavailableException("Bureau returned an empty response", null);
            }
            return response.score();
        } catch (BureauUnavailableException e) {
            throw e;
        } catch (Exception e) {
            logger.info("DEBUG MSG: Credit bureau call failed for taxId={}: {}",
                    TaxIdMasker.mask(taxId), e.getMessage());
            throw new BureauUnavailableException("Credit bureau call failed: " + e.getMessage(), e);
        }
    }

    public record ScoreResponse(int score) {
    }
}
