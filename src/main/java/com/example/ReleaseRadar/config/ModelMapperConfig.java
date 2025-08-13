package com.example.ReleaseRadar.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Java'da nesneler arasında otomatik dönüşüm (object mapping) yapmak için oluşturulmuştur.
// Entity → DTO,
//DTO → Entity dönüşümlerini
//kolaylaştırmak için oluşturulur.


@Configuration
//Bu sınıfın bir konfigürasyon sınıfı olduğunu belirtir. Yani içinde Spring konteynerine özel @Bean tanımlamaları yapılır.
public class ModelMapperConfig {


    @Bean
    //Bu metotun dönüş değerini Spring konteynerine bir bean olarak ekler. Yani artık bu ModelMapper objesi, tüm projede @Autowired ile kullanılabilir.
    public ModelMapper modelMapper() {

        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE)
                .setMatchingStrategy(MatchingStrategies.STANDARD);


        return modelMapper;
    }


}