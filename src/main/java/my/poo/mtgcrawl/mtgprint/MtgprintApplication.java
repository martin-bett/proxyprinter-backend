package my.poo.mtgcrawl.mtgprint;

import java.nio.charset.Charset;
import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import my.poo.mtgcrawl.mtgprint.service.net.GuardedRequestFactory;
import my.poo.mtgcrawl.mtgprint.service.net.OutboundUrlGuard;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "my.poo.mtgcrawl.mtgprint")
public class MtgprintApplication {

    public static void main(String[] args) {
        SpringApplication.run(MtgprintApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(OutboundUrlGuard guard) {
        RestTemplate template = new RestTemplate();
        
        // Add UTF-8 encoding support
        StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(Charset.forName("UTF-8"));
        stringConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.TEXT_HTML, MediaType.APPLICATION_JSON));
        template.getMessageConverters().add(0, stringConverter);
        
        // Add support for multipart/form-data
        FormHttpMessageConverter formConverter = new FormHttpMessageConverter();
        formConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA));
        template.getMessageConverters().add(formConverter);
        
        // Ensure JSON support
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        template.getMessageConverters().add(jsonConverter);

		template.getMessageConverters().add(new ByteArrayHttpMessageConverter());
        
        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
        template.setUriTemplateHandler(uriBuilderFactory);

        // Abrufe im Auftrag von Besuchern: nur http(s) ins öffentliche Netz, Weiterleitungen werden einzeln geprüft
        GuardedRequestFactory requestFactory = new GuardedRequestFactory(guard);
        template.setRequestFactory(requestFactory);
        template.getInterceptors().add(requestFactory.redirectInterceptor());
 
        return template;
    }

    @Bean
    public RestClient restClient(){
        return RestClient.create();
    }
}
