package com.example.workflow.infrastructure.client;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestInvoiceClient implements InvoiceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestInvoiceClient.class);

    private final RestTemplate restTemplate;
    private final String invoiceBaseUrl;

    public RestInvoiceClient(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${invoice.service.base-url}") String invoiceBaseUrl
    ) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(2))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.invoiceBaseUrl = invoiceBaseUrl;
    }

    @Override
    public InvoiceResponse generateInvoice(GenerateInvoiceRequest request, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        LOGGER.info("Calling invoice-service generation. orderId={}, correlationId={}, amount={}, currency={}, idempotencyKey={}",
                request.orderId(), request.correlationId(), request.amount(), request.currency(), idempotencyKey);
        ResponseEntity<InvoiceResponse> response = restTemplate.exchange(
                invoiceBaseUrl + "/api/invoices",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                InvoiceResponse.class
        );
        InvoiceResponse body = response.getBody();
        if (body != null) {
            LOGGER.info("invoice-service generation completed. orderId={}, correlationId={}, invoiceId={}, status={}",
                    request.orderId(), request.correlationId(), body.invoiceId(), body.status());
        }
        return body;
    }
}
