package com.app.commonservice;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class}) //Con esto se ignora la configuración por defecto de Spring Data Jpa que obliga a que si existe la dependencia Jpa
									// debe haber tambien dependencia h2,mysql,etc. Se puede itilizar esto o usar en el commons la dependencia h2
public class CommonsServiceApplication {

}