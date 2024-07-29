package my.poo.mtgcrawl.mtgprint;

import java.nio.charset.Charset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class MtgprintApplication {

	public static void main(String[] args) {
		SpringApplication.run(MtgprintApplication.class, args);
	}

	@Bean
	public RestTemplate restTemplate() {
		RestTemplate template = new RestTemplate();
		template.getMessageConverters()
        .add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
		return template;
	}

	@Bean
	public RestClient restClient(){
		return RestClient.create();
	}
}
