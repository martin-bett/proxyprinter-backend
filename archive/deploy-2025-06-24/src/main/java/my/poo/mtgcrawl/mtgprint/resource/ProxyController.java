/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  jakarta.servlet.http.HttpServletRequest
 */
package my.poo.mtgcrawl.mtgprint.resource;

import jakarta.servlet.http.HttpServletRequest;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import my.poo.mtgcrawl.mtgprint.service.HashService;
import my.poo.mtgcrawl.mtgprint.service.rest.FileDownloadService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartHttpServletRequest;

@RestController
@RequestMapping(value={"/api/proxy"})
public class ProxyController {
    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);
    private final RestTemplate restTemplate;
    private final FileDownloadService fileDownloadService;
    private final HashService hashService;

    @GetMapping(value={"/ping"})
    public Boolean ping() {
        return true;
    }

    @PostMapping
    public ResponseEntity<String> proxyRequest(@RequestHeader(value="X-Target-URL") String targetUrl, @RequestHeader(value="X-Request-Format") String requestFormat, @RequestHeader(value="X-Request-Method") String method, HttpServletRequest request) throws IOException {
        HttpEntity<Object> requestEntity;
        HttpMethod httpMethod;
        HttpHeaders headers = new HttpHeaders();
        this.copyRelevantHeaders(request, headers);
        if ("post".equals(method)) {
            httpMethod = HttpMethod.POST;
            if ("multipart".equals(requestFormat)) {
                MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
                MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                HashMap paramMap = new HashMap();
                multipartRequest.getParameterMap().forEach((name, values) -> {
                    if (name.contains("[") && name.contains("]")) {
                        String mapName = name.substring(0, name.indexOf("["));
                        if (paramMap.get(mapName) == null) {
                            paramMap.put(mapName, new HashMap());
                        }
                        ((Map)paramMap.get(mapName)).put(name.substring(name.indexOf("[") + 1, name.indexOf("]")), values[0]);
                    }
                });
                for (String value : paramMap.keySet()) {
                    multipartBodyBuilder.part(value, paramMap.get(value));
                }
                MultiValueMap<String, HttpEntity<?>> multipartBody = multipartBodyBuilder.build();
                HttpEntity requestEntity1 = new HttpEntity(multipartBody, headers);
                log.debug("Request URL: " + targetUrl);
                log.debug("Request Headers: " + String.valueOf(headers));
                log.debug("Request Body Parts: " + String.valueOf(multipartBody.keySet()));
                ResponseEntity<String> response = this.restTemplate.exchange(targetUrl, HttpMethod.POST, requestEntity1, String.class, new Object[0]);
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.putAll(response.getHeaders());
                return ((ResponseEntity.BodyBuilder)ResponseEntity.status(response.getStatusCode()).headers(responseHeaders)).body((String)response.getBody());
            }
            if ("json".equals(requestFormat)) {
                String jsonBody = request.getParameter("body");
                headers.setContentType(MediaType.APPLICATION_JSON);
                requestEntity = new HttpEntity<String>(jsonBody, headers);
            } else {
                LinkedMultiValueMap formBody = new LinkedMultiValueMap();
                request.getParameterMap().forEach((name, values) -> formBody.addAll(name, Arrays.asList(values)));
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                requestEntity = new HttpEntity(formBody, headers);
            }
        } else {
            httpMethod = HttpMethod.GET;
            requestEntity = null;
        }
        try {
            ResponseEntity<String> response = this.restTemplate.exchange(targetUrl, httpMethod, requestEntity, String.class, new Object[0]);
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());
            return ((ResponseEntity.BodyBuilder)ResponseEntity.status(response.getStatusCode()).headers(responseHeaders)).body((String)response.getBody());
        }
        catch (Exception e) {
            log.error("blabla", e);
            return null;
        }
    }

    @GetMapping(value={"/image"})
    public ResponseEntity<byte[]> proxyImageRequest(@RequestHeader(value="X-Target-URL") String targetUrl) {
        String fileName = this.hashService.curFileName(targetUrl);
        String[] resultFileName = new String[]{null};
        CountDownLatch latch = new CountDownLatch(1);
        this.fileDownloadService.queueDownload(targetUrl, fileName, FileDownloadService.DownloadPriority.PROXY, resolvedFile -> {
            resultFileName[0] = resolvedFile;
            latch.countDown();
        });
        try {
            latch.await(30L, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            return ResponseEntity.internalServerError().build();
        }
        String exisitingFileName = resultFileName[0];
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setCacheControl("no-store");
        if (exisitingFileName != null) {
            try {
                byte[] fileContent = IOUtils.toByteArray(new FileInputStream(exisitingFileName));
                return new ResponseEntity<byte[]>(fileContent, (MultiValueMap<String, String>)responseHeaders, HttpStatus.OK);
            }
            catch (Exception e) {
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping(value={"/image/preload"})
    public ResponseEntity<Void> preloadImage(@RequestBody List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        this.fileDownloadService.preloadImages(imageUrls);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value={"/csrf-token"})
    public ResponseEntity<String> getCsrfToken(@RequestHeader(value="X-Target-URL") String targetUrl, @RequestParam(value="csrfName") String csrfName) {
        ResponseEntity<String> response = this.restTemplate.getForEntity(targetUrl, String.class, new Object[0]);
        String html = (String)response.getBody();
        String csrfToken = this.extractCsrfToken(html, csrfName);
        return ResponseEntity.ok(csrfToken);
    }

    private String extractCsrfToken(String html, String csrfName) {
        Pattern pattern = Pattern.compile("name=\"" + csrfName + "\" value=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException("CSRF token not found");
    }

    private void copyRelevantHeaders(HttpServletRequest request, HttpHeaders headers) {
        Arrays.asList("User-Agent", "Accept", "Accept-Language", "Referer", "Cookie").forEach(headerName -> this.copyHeader(request, headers, (String)headerName));
    }

    private void copyHeader(HttpServletRequest request, HttpHeaders headers, String headerName) {
        String headerValue = request.getHeader(headerName);
        if (headerValue != null) {
            headers.set(headerName, headerValue);
        }
    }

    public ProxyController(RestTemplate restTemplate, FileDownloadService fileDownloadService, HashService hashService) {
        this.restTemplate = restTemplate;
        this.fileDownloadService = fileDownloadService;
        this.hashService = hashService;
    }
}

