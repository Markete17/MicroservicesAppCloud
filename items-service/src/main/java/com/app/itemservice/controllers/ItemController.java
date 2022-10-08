package com.app.itemservice.controllers;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.itemservice.models.Item;
import com.app.itemservice.models.Product;
import com.app.itemservice.models.services.ItemService;
//import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;

@RefreshScope //Usado en el tema del servidor de configuración. Sirve para refrescar el controlador en caso de que cambie alguna configuración
			  //del archivo properties tanto del servidor de configuración como del propio properties del microservicio
			  //En caso de cambio en el Environment, inyecta de nuevo los autowired y el controlador. 
			 // Es necesario añadir la dependencia Spring Boot Actuator
@RestController
//@RequestMapping(path = "/items") Ya no es necesario, se configura en el zuul-server (API Gateway) en properties
public class ItemController {

	@Autowired
	// @Qualifier("serviceRestTemplate") //Usando Rest Template
	@Qualifier("serviceFeign") // Usando Feign
	private ItemService itemService;
	
	//Resillence4J
	@Autowired
	private CircuitBreakerFactory<?, ?> circuitBreakerFactory;
	
	@Value("${myconfig.text}")
	@Autowired
	private String text;
	
	@Autowired
	private Environment environment;
	
	private final Logger logger = LoggerFactory.getLogger(ItemController.class);

	@GetMapping
	public ResponseEntity<List<Item>> getAllItems(@RequestParam(name = "name",required = false) String name, @RequestHeader(name = "token-request",required = false) String token) {
		System.out.println("Name: "+name);
		System.out.println("Token: "+token);
		return new ResponseEntity<>(this.itemService.findAll(), HttpStatus.OK);
	}

	//@HystrixCommand(fallbackMethod = "alternativeMethod") alternativa Resillence4J
	@GetMapping("/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem(@PathVariable Long id, @PathVariable Integer quantity) {
		
		/*Item item = this.itemService.findById(id, quantity);
		if (item != null) {
			return new ResponseEntity<Item>(item, HttpStatus.OK);
		} else {
			return new ResponseEntity<Item>(HttpStatus.NOT_FOUND);
		}*/
		
		/**Probar Resilence4j**/
		return circuitBreakerFactory.create("items").run(() ->new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK),e -> alternativeMethod(id, quantity,e));
	}
	
	/* PROBAR ANOTACION @CircuitBreaker
	 * */
	
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod")
	@GetMapping("/aux/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem2(@PathVariable Long id, @PathVariable Integer quantity) {
		return new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK);
	}
	
	/* PROBAR ANOTACION @TimeLimiter
	 * */
	
	@TimeLimiter(name = "items")//,fallbackMethod = "alternativeMethod2")
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod2") //se puede quitar o combinar con TimeLimiter
	@GetMapping("/aux2/{id}/quantity/{quantity}")
	public CompletableFuture<ResponseEntity<Item>> getItem3(@PathVariable Long id, @PathVariable Integer quantity) {
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK));
	}

	public ResponseEntity<Item> alternativeMethod(@PathVariable Long id, @PathVariable Integer quantity, Throwable e) {
		
		logger.info("Error: "+e.getMessage());
		
		Item item = new Item();
		Product product = new Product();

		product.setId(id);
		product.setName("Sony Camera");
		product.setPrice(500.00);

		item.setProduct(product);
		item.setQuantity(quantity);
		return new ResponseEntity<Item>(item, HttpStatus.OK);
	}
	
	public CompletableFuture<ResponseEntity<Item>> alternativeMethod2(@PathVariable Long id, @PathVariable Integer quantity, Throwable e) {
		
		logger.info("Error: "+e.getMessage());
		
		Item item = new Item();
		Product product = new Product();

		product.setId(id);
		product.setName("Sony Camera");
		product.setPrice(500.00);

		item.setProduct(product);
		item.setQuantity(quantity);
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(item, HttpStatus.OK));
	}
	
	@GetMapping("/get-config")
	public ResponseEntity<?> getConfig(@Value("${server.port}") String port ){
		logger.info(text);
		
		Map<String, String> json = new HashMap<>();
		json.put("text", this.text);
		json.put("port", port);
		
		if(environment.getActiveProfiles().length>0 && environment.getActiveProfiles()[0].equals("dev")) {
			json.put("author.name", environment.getProperty("myconfig.author.name"));
			json.put("author.email", environment.getProperty("myconfig.author.email"));
		}
		
		return new ResponseEntity<Map<String,String>>(json,HttpStatus.OK);
	}

}
