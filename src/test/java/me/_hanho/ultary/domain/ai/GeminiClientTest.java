package me._hanho.ultary.domain.ai;

import static org.assertj.core.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.*;

class GeminiClientTest {
    HttpServer server;
    java.util.concurrent.ExecutorService executor;
    AiProperties properties;
    @BeforeEach void setup() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        executor=Executors.newCachedThreadPool();server.setExecutor(executor);
        properties=new AiProperties();properties.setApiKey("test-key-secret");properties.setReadTimeoutMs(1000);
        properties.setBaseUrl("http://127.0.0.1:"+server.getAddress().getPort());
    }
    @AfterEach void stop(){server.stop(0);executor.shutdownNow();}
    @Test void credentialsUseHeaderAndSuccessfulJsonIsExtracted() {
        var requestBody = new java.util.concurrent.atomic.AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        server.createContext("/",e->{
            requestBody.set(new ObjectMapper().readTree(e.getRequestBody()));
            assertThat(e.getRequestURI().getQuery()).isNull();
            assertThat(e.getRequestHeaders().getFirst("x-goog-api-key")).isEqualTo("test-key-secret");
            byte[] b="{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{\\\"safe\\\":true}\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();
        });server.start();
        assertThat(new GeminiClient(properties,new ObjectMapper()).generateJson("system","user")).isEqualTo("{\"safe\":true}");
        assertThat(requestBody.get().path("generationConfig").path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("low");
    }
    @Test void configuredThinkingLevelIsUsedInTheCommonRequest() {
        properties.setThinkingLevel("high");
        var requestBody = new java.util.concurrent.atomic.AtomicReference<com.fasterxml.jackson.databind.JsonNode>();
        server.createContext("/",e->{
            requestBody.set(new ObjectMapper().readTree(e.getRequestBody()));
            byte[] b="{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"{}\"}]}}]}".getBytes(StandardCharsets.UTF_8);
            e.sendResponseHeaders(200,b.length);e.getResponseBody().write(b);e.close();
        });server.start();
        new GeminiClient(properties,new ObjectMapper()).generateJson("system","user");
        assertThat(requestBody.get().path("generationConfig").path("thinkingConfig").path("thinkingLevel").asText()).isEqualTo("high");
    }
    @Test void deadlineAlsoCoversAStalledResponseBody() {
        properties.setReadTimeoutMs(200);
        server.createContext("/",e->{e.sendResponseHeaders(200,0);e.getResponseBody().flush();try{Thread.sleep(3000);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}finally{e.close();}});server.start();
        long start=System.nanoTime();
        assertThatThrownBy(()->new GeminiClient(properties,new ObjectMapper()).generateJson("system","user"))
            .isInstanceOfSatisfying(GeminiClient.GeminiException.class,e->{assertThat(e.code()).isEqualTo("GEMINI_TIMEOUT");assertThat(e.diagnostic()).doesNotContain("test-key-secret");});
        assertThat((System.nanoTime()-start)/1_000_000).isLessThan(2000);
    }
    @Test void providerStatusIsPreservedWithoutEchoingResponseText() {
        server.createContext("/",e->{byte[] b="secret provider body".getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(429,b.length);e.getResponseBody().write(b);e.close();});server.start();
        assertThatThrownBy(()->new GeminiClient(properties,new ObjectMapper()).generateJson("system","user"))
            .isInstanceOfSatisfying(GeminiClient.GeminiException.class,e->{assertThat(e.httpStatus()).isEqualTo(429);assertThat(e.retryable()).isTrue();assertThat(e.diagnostic()).doesNotContain("secret");});
    }
}
