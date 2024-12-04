package my.poo.mtgcrawl.mtgprint.resource;

import org.apache.commons.io.IOUtils;
import org.springframework.http.*;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@Slf4j
@RequestMapping("/api/proxy")
@AllArgsConstructor
public class ProxyController {

    private final RestTemplate restTemplate;
    private final FileDownloadService fileDownloadService;
    private final HashService hashService;

    @GetMapping("/ping")
    public Boolean ping() {
        return true;
    }

    @PostMapping
    public ResponseEntity<String> proxyRequest(
            @RequestHeader("X-Target-URL") String targetUrl,
            @RequestHeader("X-Request-Format") String requestFormat,
            @RequestHeader("X-Request-Method") String method,
            HttpServletRequest request) throws IOException {

        HttpHeaders headers = new HttpHeaders();
        copyRelevantHeaders(request, headers);

        HttpEntity<?> requestEntity;
        HttpMethod httpMethod;
        if ("post".equals(method)) {
            httpMethod = HttpMethod.POST;
            if ("multipart".equals(requestFormat)) {
                MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
                MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                Map<String, Map<String, String>> paramMap = new HashMap<>();

                // Fügen Sie alle Parameter zur MultipartBodyBuilder hinzu
                multipartRequest.getParameterMap().forEach((name, values) -> {
                    if (name.contains("[") && name.contains("]")) {
                        String mapName = name.substring(0, name.indexOf("["));
                        if (paramMap.get(mapName) == null) {
                            paramMap.put(mapName, new HashMap<>());
                        }
                        paramMap.get(mapName).put(name.substring(name.indexOf("[") + 1, name.indexOf("]")), values[0]);
                    }
                });
                for (String value : paramMap.keySet()) {
                    multipartBodyBuilder.part(value, paramMap.get(value));
                }

                MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();

                HttpEntity<MultiValueMap<String, HttpEntity<?>>> requestEntity1 = new HttpEntity<>(multipartBody,
                        headers);

                log.debug("Request URL: " + targetUrl);
                log.debug("Request Headers: " + headers);
                log.debug("Request Body Parts: " + multipartBody.keySet());

                ResponseEntity<String> response = restTemplate.exchange(
                        targetUrl,
                        HttpMethod.POST,
                        requestEntity1,
                        String.class);

                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.putAll(response.getHeaders());

                return ResponseEntity.status(response.getStatusCode())
                        .headers(responseHeaders)
                        .body(response.getBody());
            } else if ("json".equals(requestFormat)) {
                String jsonBody = request.getParameter("body"); // Assuming JSON is sent as a 'body' parameter
                headers.setContentType(MediaType.APPLICATION_JSON);
                requestEntity = new HttpEntity<>(jsonBody, headers);
            } else {
                MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
                request.getParameterMap().forEach((name, values) -> formBody.addAll(name, Arrays.asList(values)));
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                requestEntity = new HttpEntity<>(formBody, headers);
            }
        } else {
            httpMethod = HttpMethod.GET;
            requestEntity = null;
        }

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    httpMethod,
                    requestEntity,
                    String.class);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());

            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
        } catch (Exception e) {
            log.error("blabla", e);
            return null;
        }
    }

    @GetMapping("/image")
    public ResponseEntity<byte[]> proxyImageRequest(@RequestHeader("X-Target-URL") String targetUrl) {
        String fileName =  hashService.curFileName(targetUrl);
        String exisitingFileName = fileDownloadService.checkFileExists(fileName);
        
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setCacheControl("no-store"); 

        if(exisitingFileName != null){
            try {
                byte[]  fileContent = IOUtils.toByteArray(new FileInputStream(exisitingFileName));
                return new ResponseEntity<>(fileContent, responseHeaders, HttpStatus.OK);
            } catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.ALL));
            HttpEntity<?> entity = new HttpEntity<>(headers);
            ResponseEntity<byte[]> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, byte[].class);
            if (response.getStatusCode().is3xxRedirection()) {
                String redirectUrl = response.getHeaders().getLocation().toString();
                response = restTemplate.exchange(redirectUrl, HttpMethod.GET, entity, byte[].class);
            }
            byte[] fileContent = response.getBody();
            fileDownloadService.saveByteArray(fileContent,fileName);
            responseHeaders.setContentType(response.getHeaders().getContentType());
            return new ResponseEntity<>(fileContent, responseHeaders, response.getStatusCode());
        }
    }

    @GetMapping("/csrf-token")
    public ResponseEntity<String> getCsrfToken(@RequestHeader("X-Target-URL") String targetUrl,
            @RequestParam("csrfName") String csrfName) {
        ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);
        String html = response.getBody();
        String csrfToken = extractCsrfToken(html, csrfName);
        return ResponseEntity.ok(csrfToken);
    }

    private String extractCsrfToken(String html, String csrfName) {
        // Use a regular expression or HTML parser to extract the CSRF token
        // This is a simple regex example and might need to be adjusted based on the
        // actual HTML structure
        Pattern pattern = Pattern.compile("name=\"" + csrfName + "\" value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("CSRF token not found");
    }

    private void copyRelevantHeaders(HttpServletRequest request, HttpHeaders headers) {
        Arrays.asList("User-Agent", "Accept", "Accept-Language", "Referer", "Cookie")
                .forEach(headerName -> copyHeader(request, headers, headerName));
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            headers.set(headerName, headerValue);
        }
    }

}