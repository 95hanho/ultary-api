package me._hanho.ultary.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan(basePackages = "me._hanho.ultary.domain.**.mapper")
public class MyBatisConfig {
}
