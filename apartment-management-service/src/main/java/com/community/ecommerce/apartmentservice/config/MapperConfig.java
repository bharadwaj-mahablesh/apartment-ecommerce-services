package com.community.ecommerce.apartmentservice.config;

import com.community.ecommerce.apartmentservice.mapper.ApartmentMapper;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapperConfig {

    @Bean
    public ApartmentMapper apartmentMapper() {
        return Mappers.getMapper(ApartmentMapper.class);
    }
}
