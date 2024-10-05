package my.poo.mtgcrawl.mtgprint.resource;

import java.util.List;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import my.poo.mtgcrawl.mtgprint.entitiy.ApiConfigDto;

@RestController
@RequestMapping("/api")
public class ApiConfigController{

    public List<ApiConfigDto> getAllConfigs(){
        return null;
    }

    public void saveConfig(ApiConfigDto config){
        
    }
}