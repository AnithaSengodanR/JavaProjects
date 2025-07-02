package com.example;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Test {

    private static final boolean LOG_HEADERS = true;
    private static final Logger successLogger = LogManager.getLogger("successLogger");
    private static final Logger errorLogger = LogManager.getLogger("errorLogger");
    
    public static void main(String[] args) {
        System.out.println("Log4j config: " + System.getProperty("log4j.configurationFile"));
        successLogger.info("Success log test");
errorLogger.error("Error log test");
System.setProperty("log4j.configurationFile", "log4j2.xml");
        WebClient webClient = WebClient.builder()
            .baseUrl("https://demoapps.tcsbancs.com/Core/accountManagement")
            .build();

      //  AccountRequest request = new AccountRequest("123456", "SAVINGS", "INR", 1000);

//int userId = 1;
  //      int languageCode = 1;
    //    String entity = "GPRDTTSTOU";
        HttpHeaders headers = new HttpHeaders();
headers.add("entity", "GPRDTTSTOU");
headers.add("userid", "1");
headers.add("languagecode", "1");
        // Assuming the request body is a JSON string, you can use a Map or a custom object
        // For simplicity, using a string here. In a real application, you might want to
        // use a proper JSON library to create the request body.
        // String requestBody = "{\"        
        //     \"customerId\": 146039,\n" +
        //     "\"accountLanguage\": 1,\n" +    
        //     "\"accountUsage\": 40,\n" +
        //     "\"accountCurrency\": \"USD\",\n" +  
        //     "\"accountType\": 1,\n" +
        //     "\"accountName\": \"Test Account\",\n" +
        //     "\"branchId\": \"101\",\n" +
        //     "\"product\": 1037\n" +
        //     "}";
        // String requestBody = "{\"customerId\": 146039, \"accountLanguage\": 1, \"accountUsage\": 40, \"accountCurrency\": \"USD\", \"accountType\": 1, \"accountName\": \"Test Account\", \"branchId\": \"101\", \"product\": 1037}";
      // String requestBody = "{\"customerId\": 146039, \"accountLanguage\": 1, \"accountUsage\": 40, \"accountCurrency\": \"USD\", \"accountType\": 1, \"accountName\": \"Test Account\", \"branchId\": \"101\", \"product\": 1037}";
      // requestBody = requestBody.replaceAll("\\s+", ""); // Remove all whitespace
String request = ""
            + "{\n"
            + "  \"account\": {\n"
            + "    \"customerId\": 146039,\n"
            + "    \"accountLanguage\": 1,\n"
            + "    \"accountUsage\": 40,\n"
            + "    \"accountCurrency\": \"USD\",\n"
            + "    \"accountType\": 1,\n"
            + "    \"accountName\": \"Test Account API Async\",\n"
            + "    \"branchId\": \"101\",\n"
            + "    \"product\": 1037\n"
            + "  }\n"
            + "}";

        String requestBody = request.replaceAll("\\s+", ""); // Remove all whitespace
        // Example of a request body in JSON format
        // String requestBody = """
        //
      
                  // Start the timer  
       long startTime = System.currentTimeMillis();

        webClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/account")
                 .build())
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(requestBody)
            .headers(httpHeaders -> httpHeaders.addAll(headers))
            .exchangeToMono(response -> s(response, startTime))
            .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                .filter(ex -> {
                   errorLogger.warn("Retrying due to: " + ex.getMessage());
                    return true;
                }))
            .doOnError(e -> errorLogger.error("error.log", "Final failure: " + e.getMessage()))
            .subscribe();

        System.out.println("API fired asynchronously. Main thread continues...");
        System.out.println("Outgoing HttpHeaders:");
    headers.forEach((key, values) -> {
    values.forEach(value -> System.out.println("  " + key + ": " + value));
});
        System.out.println("📥 Request Body: " + requestBody);
    }

    @SuppressWarnings("unused")
    private static Mono<Void> handleAndLogResponse(ClientResponse response, long startTime) {
        int statusCode = response.statusCode().value();
        long durationMs = System.currentTimeMillis() - startTime;

        return response.bodyToMono(String.class).flatMap(body -> {
            StringBuilder log = new StringBuilder();
            log.append("Response Time: ").append(durationMs).append(" ms\n")
               .append("Status Code: ").append(statusCode).append("\n")
               .append(" Response Body:\n").append(body).append("\n");

            if (LOG_HEADERS) {
                log.append("Headers:\n");
                response.headers().asHttpHeaders().forEach((k, v) ->
                    log.append("  ").append(k).append(": ").append(v).append("\n"));
            }

            if (statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode >= 500) {
                logToFile("error.log", log.toString());
            } else {
                logToFile("success.log", log.toString());
            }

            return Mono.empty();
        });
    }

    private static void logToFile(String fileName, String content) {
        try {
            String path = Path.of(fileName).toAbsolutePath().toString();
            FileWriter writer = new FileWriter(path, true);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            writer.write("\n[" + timestamp + "]\n" + content + "\n");
            writer.close();
            System.out.println("Logged to " + fileName);
        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }
    private static Mono<Void> handleAndLogWithLog4j(ClientResponse response, long startTime) {
        int statusCode = response.statusCode().value();
        long duration = System.currentTimeMillis() - startTime;

        return response.bodyToMono(String.class).flatMap(body -> {
            StringBuilder log = new StringBuilder();
            log.append("Response Time: ").append(duration).append(" ms\n")
               .append("Status: ").append(statusCode).append("\n")
               .append("Body:\n").append(body).append("\n")
               .append("Headers:\n");

            response.headers().asHttpHeaders().forEach((k, v) ->
                log.append("  ").append(k).append(": ").append(v).append("\n"));

            if (statusCode >= 200 && statusCode < 300) {
                successLogger.info(log.toString());
            } else if (statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode >= 500) {
                errorLogger.error(log.toString());
            } else {
                errorLogger.warn("Unhandled status:\n" + log);
            }
            return Mono.empty();
        });
    }

}


