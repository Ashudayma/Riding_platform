# Spring Boot WebClient Example

```java
@Component
public class AiRiskScoringClient {

    private final WebClient webClient;

    public AiRiskScoringClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
                .baseUrl("http://risk-scoring-service:8090/api/v1")
                .build();
    }

    public Mono<FraudRiskPredictionResponse> scoreFraud(FraudRiskPredictionRequest request, String correlationId) {
        return webClient.post()
                .uri("/inference/fraud-risk")
                .header("X-Correlation-Id", correlationId)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudRiskPredictionResponse.class)
                .timeout(Duration.ofMillis(500))
                .onErrorResume(exception -> Mono.empty());
    }
}
```

Suggested production additions:

- Resilience4j circuit breaker
- metrics around latency, error rate, and fallback rate
- structured audit logging with returned `model_version`
