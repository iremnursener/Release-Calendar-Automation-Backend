package com.example.ReleaseRadar.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000") // frontend adresin
                        .allowedMethods("GET", "POST", "PUT", "DELETE")
                        .allowedHeaders("*")
                        .allowCredentials(true); // kimlik bilgieri gönderilecek mi cookies
            }
        };
    }
}
//Frontend React (http://localhost:3000) ve backend Spring Boot (http://localhost:8080) ise:
//React uygulamasından API'ye istek atıldığında (fetch, axios) tarayıcı bu isteği CORS olarak görür.
//Eğer bu yapılandırma yoksa tarayıcı "CORS policy hatası" verir.
//Bu CorsConfig sayesinde her yerden gelen istekler kabul edilir.
