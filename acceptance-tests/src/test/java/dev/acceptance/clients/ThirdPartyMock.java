package dev.acceptance.clients;

import com.github.tomakehurst.wiremock.client.WireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public final class ThirdPartyMock {
    private final WireMock client;
    private final String baseUrl;

    public ThirdPartyMock(String baseUrl) {
        this.baseUrl = baseUrl;
        var uri = java.net.URI.create(baseUrl);
        this.client = new WireMock(uri.getHost(), uri.getPort());
    }

    public String baseUrl() { return baseUrl; }

    public void stubSuccess(String path, String expectedBody) {
        client.register(post(urlPathEqualTo(path)).withRequestBody(equalToJson(expectedBody, true, false))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody("{}")));
    }

    public void verifyRequest(String path, String expectedBody) {
        client.verifyThat(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo(path))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson(expectedBody, true, false)));
    }

    public Object requests() { return client.getServeEvents(); }
}
