# MicroservicesAppCloud - Marcos Ruiz Muñoz 
Formación en microservicios con Spring Cloud


## Rest Template y Feign Client
Se usan para que un microservicio utilice otro microservicio.

### 1. Rest Template
- Se necesita crear un bean en el application run que devuelva una instancia Rest Template
<pre><code>
	@Bean(name = "restClient")
	@LoadBalanced //Para que RestTemplate use Balanceo de carga con Ribbon
	public RestTemplate registerRestTemplate() {
		return new RestTemplate();
	}
</code></pre>
- En los service se inyecta esta dependencia y se puede utilizar para hacer los métodos GET,PUT,POST y DELETE
- <b>GET:</b> se utiliza el método getForObject. Cabe destacar que en la URL se puede usar el nombre del microservicio y ya Ribbon (en Eureka o configurandolo en el microservicio) se
encargará de elegir la mejor instancia.

<pre><code>
	@Override
	public Item findById(Long id, Integer quantity) {
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		// Al usar RestTemplate con balanceo de carga, ya no es necesario indicar la URL ya que Ribbon elegirá el puerto mejor disponible
		
		// Ahora hay que elegir el servicio
		//Product product = this.restClient.getForObject("http://localhost:8001/products/{id}", Product.class, pathVariablesMap);
		Product product = this.restClient.getForObject("http://products-service/products/{id}", Product.class, pathVariablesMap);
		
		return new Item(product,quantity);
	}
</code></pre>

- <b>POST:</b>: se utiliza el método exchange indicando la URL, el método y el tipo de objeto que devuelve la petición:

<pre><code>
	@Override
	public Product save(Product product) {
		
		HttpEntity<Product> body = new HttpEntity<Product>(product);
		
		//Con el método exchange se indica la url, el método y el tipo de objeto que se devuelve en el body. Hay que pasarle el body un HttpEntity que sera el objeto producto.
		ResponseEntity<Product> responseEntity = this.restClient.exchange("http://products-service/create", HttpMethod.POST,body,Product.class);
		Product productResponse=responseEntity.getBody();
		return productResponse;
	}
</code></pre>

- <b>PUT:</b>: se utiliza también el método exchange pero hay que agregarle los path variables a través de un HashMap

<pre><code>
	@Override
	public Product update(Product product, Long id) {
		
		HttpEntity<Product> body = new HttpEntity<Product>(product);
		
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		ResponseEntity<Product> responseEntity = this.restClient.exchange("http://products-service/update/{id}", 
				HttpMethod.PUT,body,Product.class, pathVariablesMap);
		return responseEntity.getBody();
	}
</code></pre>

- <b>DELETE:</b>: Utiliza el método delete indicando la url, el código HttpStatus.DELETE y las pathVariables

<pre><code>
	@Override
	public void delete(Long id) {
		Map<String, String> pathVariablesMap = new HashMap<>();
		pathVariablesMap.put("id", id.toString());
		
		this.restClient.delete("http://products-service/delete/{id}", pathVariablesMap);
	}
</code></pre>

### 2. Feign Client

Es mejor usar la biblioteca Feign para crear un cliente que use la api de productos. Es mucho más sencillo, menos código y se consume el microservicio de forma remota mediante
interfaces.
Esto en vez de hacerlo con REST TEMPLATE, se puede hacer mediante interfaces con la bliblioteca Spring Feign
Ir a ItemServiceFeign que usa ClientRestProduct
- Se necesita inyectar la dependencia Open Feign: spring-cloud-starter-openfeign
- En el application run se tendrá que poner la anotación <b>@EnableFeignClients</b>
- En el microservicio que se va a usar otro microservicio, se crea una interfaz que tendrá la anotación
<b>@FeignClient(name = "products-service")</b> indicando el nombre del microservicio que se va a utilizar.
En esta interfaz se tendrá que poner las url handler del controlador del microservicio que se va a utilizar y de esta forma
ya no hay que implementar nada más.

<pre><code>
@FeignClient(name = "products-service") //indicar el nombre especificado en el properties
public interface ClientRestProduct {
	
	@GetMapping("/")
	public ResponseEntity<List<Product>> getAllProducts();
	
	@GetMapping("/{id}")
	public ResponseEntity<Product> getProduct(@PathVariable Long id);

}

</code></pre>

## Balanceo de carga con Ribbon

Ejemplo de balanceo de carga.
Se crean dos instancias del microservicio productos (uno en el puerto 8001 y otro en el 9001)
Servicio items (puerto 8002) se conecte a uno de los servicios activos de productos y va a ser Ribbon
quien elija la mejor instancia disponible de productos.

Para añadirlo a Spring, a partir de Spring 2.4 > no es compatible con Ribbon por lo que para usarlo, es necesario
modificar el pom de items para utilizar la versión 2.3.0.RELEASE (por ejemplo) y además, es necesario cambiar 
la versión de spring-cloud a Hoxton.SR12.
<pre>
<code>
 version 2.7.4 /version  -->  version 2.3.0.RELEASE /version  

<spring-cloud.version>2021.0.4</spring-cloud.version> -->
<spring-cloud.version>Hoxton.SR12</spring-cloud.version>
</code></pre>

- Configuración de Ribbon en el properties:
<pre>
<code>
products-service.ribbon.listOfServers = localhost:8001,localhost:9001
</code></pre>

Ribbon utiliza un algoritmo para ELEGIR la mejor instancia del microservicio para obtener los datos.

### ¿Cómo crear varias instancias de un microservicio en Spring Tool Suite?
- Click derecho al microservicio - Run As - Run Configuration
- En el desplegable, elegir el microservicio debajo de Spring Boot App
- Ir a la pestaña argumentos y dirigirse al cuadro de texto VM arguments
- Colocar: -Dserver.port=9001 y darle a Run
- Después, hay que crear la instancia de por defecto que está en el 8001. Para ello, al mismo tiempo que está levantada la instancia en el 9001,
se quita la línea agregada (-Dserver.port = 9001) del cuadro de texto VM arguments. Se da a Apply - Run.
- De esta forma están ejecutándose 2 instancias de productos uno en el 9001 y otro en el 8001.


## Servidor con EUREKA
Eureka es un servidor para el registro y localización de microservicios, balanceo de carga y tolerancia a fallos. La función de Eureka es registrar las diferentes instancias de microservicios existentes, su localización, estado, metadatos...

Por defecto Euroke se registra así mismo como servidor y como cliente/microservicio. Interesa que se comporte como servidor poniendo en properties:
<pre>
<code>
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
</code></pre>
Es necesario agregar la dependencia JAXB en el pom.xml si se usa Java >=11 sino no es necesario. 
En la documentación JDK 11 Support: https://cloud.spring.io/spring-cloud-netflix/multi/multi_spring-cloud-eureka-server.html

<pre>
<code>
		dependency
			groupId org.glassfish.jaxb /groupId 
			 artifactId jaxb-runtime /artifactId 
		/dependency
</code></pre>

Con esto ya se ha creado un servidor eureka que manejará a los microservicios (products e items).
Para ir al panel de Eureka, acceder a la ruta asignada en el puerto 8761 (indicado en properties)

### Configurar los microservicios para que Eureka los use

- click derecho al microservicio - spring - edit starters - añadir la dependencia Eureka Discovery
(Cada vez que se queria registrar un cliente en el server de Eureka, es necesario que tenga la dependecia Eureka Discovery)
- Aunque no es necesario, poner la anotación @EnableEurekaClient en el application run class
- Poner en el properties de cada microservicio el servidor de eureka al que se va a conectar. Esto no es necesario si están en la misma máquina, pero por si acaso, ponerlo.

<pre>
<code>
eureka.client.service-url.defaultZone = http://localhost:8761/eureka
</code></pre>

- Quitar las dependencias Ribbon del pom.xml porque Eureka ya tiene esta dependencia implícita. Además, es necesario
eliminar la anotación @RibbonClient(name = "products-service") del app run class. Feign es necesario ya que se necesita como cliente para conectarse a las apis.

- Runear primero el eureka client y luego los demás microservicios.

Al acceder al panel de Eureka, se podrán ver las instancias de microservicios.


## Escalar microservicios con puerto dinámico
La idea es que Spring de forma automática asigne el puerto de los servicios para hacer la aplicación más escalable. Para ello:
- Modificar en el archivo properties de cada microservicio, el server.port = ${PORT:0}
- Añadir una nueva línea de configuración de Eureka para que se asigne una url dinámica al servicio:
<pre>
<code>
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
</code></pre>

## Hystrix

### Tolerancia a fallos,excepciones con Hystrix
Por ejemplo, cuando alcanza cierto límite de fallos en peticiones en alguna instancia, ya se deja de hacer
solicitudes a esa instancia.
Además, puede reemplazar a esta instancia que falla por otra.
Al igual que pasa con Ribbon, Hystrix es compatible con Spring <=2.3 con Spring >=2.4 se usa Resillence

- Agregar la dependencia
<pre>
<code>
		dependency
			groupId org.springframework.cloud /groupId 
			 artifactId spring-cloud-starter-netflix-hystrix /artifactId 
		/dependency
</code></pre>

- Agregar en la clase run, la anotación @EnableCircuitBreaker
- Con esto, Hystrix va a manejar los Runtime Exception. Es necesario poner en los métodos de los controladores
de los microservicios la anotación @HystrixCommand(fallbackMethod = "alternativeMethod") para que lo pueda manejar.
- Con la configuración fallbackMethod lo que hace es que si detecta un error en el método, se ejecute un método como alternativa y este método tiene que
tener los mismos parámetros de entrada.  (ver clase controller service-item)
- Un caso de prueba es que si se apaga el microservicio product con el que accedemos desde un cliente Feign con item-service,
al hacer una petición a la api de productos y éste estar apagado, va a ir a este método alternativo y dar 200 OK con la implementación dada.

### Tolerar Timeout con Hystrix
- Ejemplo para provocar un timeout, en un método del controller de producto, poner un Thread.sleep(2000L) -> dormir 2000 milisegundos
- Al comentar el @HystrixCommand del método y lanzar la petición con el timeout, tiene que dar el siguiente error: feign.RetryableException: Read timed out executing GET
- Al descomentar el @HystrixCommand, va a ir por el camino alternativo. 
Pero la idea no es esta, es que espere el tiempo y ejecute el método original. Para ello hay que configurar 
el tiempo de respuesta de Hystrix y Ribbon en el properties:
- Hystrix anida a Ribbon y tiene que tener un tiempo de respuesta mayor a Ribbon. 
https://cloud.spring.io/spring-cloud-static/spring-cloud-netflix/1.3.6.RELEASE/multi/multi__hystrix_timeouts_and_ribbon_clients.html
- Uploading Files through Zuul: https://cloud.spring.io/spring-cloud-netflix/multi/multi__router_and_filter_zuul.html#_uploading_files_through_zuul
Copiar la configuración

<pre>
<code>

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 60000

ribbon:

  ConnectTimeout: 3000
  
  ReadTimeout: 60000
  
</code></pre>

Que pasándolo al properties de item:
IMPORTANTE: Asegurarse que el tiempo de respuesta de Hystrix sea mayor que ribbon: 20000>13000

<pre>
<code>

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000

ribbon.ConnectTimeout: 3000

ribbon.ReadTimeout: 10000
</code></pre>

- Ahora al hacer el ejemplo del timeout de 2 segundos en el método, se debería de esperar esos 2 segundos y ejecutará
el método original.

## Servicio Zuul API Gateway
Para crear una puerta de entrada/puerta de enlace a todos los microservicios.
Va a poner establecer un enrutamiento dinámico para cada microservicio.
Todas las peticiones que pasan por Zuul API Gateway vienen con balanceo de carga y se integra con Ribbon y configurado por defecto.

<b>
Está compuesto por un conjunto de filtros predeterminados y también fitros creados propios. Por ejemplo, el
enrutamiento dinámico que viene de fábrica pero también se pueden implementar filtros nuevos como para dar seguridad
y autorización en vez de con Spring Security en cada microservicio por separado, hacerlo con Zuul para cada microservicio.
</b>

Zuul NO es disponible para versiones >=2.4

- Crear un nuevo proyecto zuul-server que tenga las dependencias: devtools, spring web y eureka client.
- Modificar el pom.xml para poner las versiones anteriores:
<pre>
<code>
	<parent>
		groupId org.springframework.boot /groupId 
		 artifactId spring-boot-starter-parent /artifactId 
		 version 2.3.0.RELEASE /version 
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	 properties 
		<java.version>11</java.version>
		<spring-cloud.version>Hoxton.SR12</spring-cloud.version>
	 /properties 
</code></pre>
- Añadir la dependencia zuul:
<pre>
<code>
		dependency
			groupId org.springframework.cloud /groupId 
			 artifactId spring-cloud-starter-netflix-zuul /artifactId 
		/dependency
</code></pre>

- Configurar el properties del proyecto. Añadiendo la configuración de cliente Eureka y las rutas zuul:
<pre>
<code>
- Configuración Eureka
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

### Configurar las rutas de los microservicios

#### Products
zuul.routes.products.service-id=products-service
zuul.routes.products.path=/api/products/**

#### Items
zuul.routes.items.service-id=items-service
zuul.routes.items.path=/api/items/**
</code></pre>

- Al runear los microservicios, hay que runear el proyecto zuul el último.
- Al hacer las peticiones, es necesario hacerlas en el puerto de zuul (8090 o el que se haya asignado)


### Zuul Filtros HTTP
Hay tres tipos de filtros en zuul: 
1. <b>PRE: </b>Se ejecuta antes de que la request sea enrutada. Principalmente en este filtro se utiliza para asignar datos, atributos para usarlo en los otros filtros. Para pasar los datos al request.
2. <b>POST: </b>Se ejecuta después de que la request haya sido enrutada. Se usa para modificar la respuesta
3. <b>ROUTE: </b>Se ejecuta durante el enrutado de la request. Aquí se resuelve la ruta. Se usa para la comunicación con el microservicio. Utiliza por defecto RibbonRoutingFilter que utiliza Ribbon y Hystrix.


1. Configurar filtro PRE
Crear un package en el proyecto zuul-server que se llame filters para incorporar estos 3 filtros.
Crear una clase en este paquete @Component PreTimeElapsedFilter.class extends ZuulFilter (Pre tiempo transcurrido Filter).
Implementar los métodos @Override:
- shouldFilter(): para validar si se ejecuta el método run o no
- run(): aqui va la lógica del filtro. Por ejemplo, obtener el tiempo de inicio y meterlo en la request.
- filterType() -> siempre va a devolver return "pre" porque es de tipo PRE
- filterOrder() -> por defecto, return 1

2. Configurar filtro POST
Crear una clase en este paquete @Component PostTimeElapsedFilter.class extends ZuulFilter (Pre tiempo transcurrido Filter).
Implementar los métodos @Override:
- shouldFilter(): para validar si se ejecuta el método run o no
- run(): aqui va la lógica del filtro. Por ejemplo, tomar el tiempo de inicio iniciado en PRE,  tomar el final y calcular el tiempo transcurrido y mostrarlo por consola.
- filterType() -> siempre va a devolver return "post" porque es de tipo PRO
- filterOrder() -> por defecto, return 1

Lo implementado en el run, se podrá ver por la consola. Para ello, ir al BootDashboard de spring, seleccionar el proyecto zuul-server click derecho y open console. Aquí indicará lo implementado (ver clases en zuul-server project.
<pre>
<code>
c.a.z.filters.PostTimeElapsedFilter : Enter to POST
c.a.z.filters.PostTimeElapsedFilter : Time elapsed: 0.548 seconds.
c.a.z.filters.PostTimeElapsedFilter : Time elapsed: 0.548 ms.
</code></pre>

### Zuul Configurar TimeOuts
Con la configuración Hystrix anterior, en zuul no vale. Es necesario configurar los timeouts en Zuul con las nuevas rutas.
Con la configuración anterior (HystrixCommand + lo del properties) para zuul sigue siendo un Timeout -> error: Gateway Timeout.

Para ello, se copia la configuración de Hystrix y se copia tanto en items-service como en zuul-service (ambos)
<pre>
<code>
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 10000
ribbon.ConnectTimeout: 3000
ribbon.ReadTimeout: 60000
</code></pre>

Cabe indicar que si solo se pone en zuul-server y se descomenta en item-server, no va a esperar y entrará en la ruta alternativa.
Por eso, es necesario ponerlo en ambos proyectos para que espere los 2 segundos y ejecute el método original y no el alternativo.

## Spring Cloud API Gateway (Alternativa a Zuul)
 Viene a reemplazar Zuul. Es un servidor de enrutamiento dinámico con filtros, seguridad, autorización, etc.
 
 - Existen dos Gateway: Zuul Netflix (mantenimiento pero aún se usa) y Spring Cloud Gateway (se recomienda y se usa para programación reactiva)
 - Puerta de enlace, acceso centralizado
 - Enrutamiento dinámico de los microservicios
 - Balanceo de carga (Ribbon o Load Balancer)
 - Maneja filtros propios
 - Permite extender funcionalidades,crear propios filtros.
 - En el API Gateway se implementa la seguridad y así no hace falta implementar la seguridad en todos los microservicios.
 
 Se crea un nuevo proyecto y se añaden las dependencias <b>Eureka Client, DevTools y Gateway.</b>
 Al igual que Zuul, se pueden configurar las rutas de los microservicios. Se puede hacer tanto en el archivo
 properties como en uno nuevo .yml. En este ejemplo se va a crear un nuevo archivo yml.
 
 <pre>
 <code>
 spring:
  application:
    name: gateway-service-server
  cloud:
    gateway:
      routes:
      # PRODUCTS
      - id: products-service
        ## Lb para incorporar LoadBalancer (balanceo de carga)
        uri: lb://products-service
        predicates:
          - Path=/api/products/**
        filters:
          ## Esto porque está formado por 2 prefijos /api/products
          - StripPrefix=2
      # ITEMS
      - id: items-service
        uri: lb://items-service
        predicates:
          - Path=/api/items/**
        filters:
          - StripPrefix=2
server:
  port: 8090
  
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka
 </code></pre>
 
 Con un archivo properties:
 <pre>
 <code>
 ## Configuracion con properties

spring.application.name=zuul-service-server
server.port=8090

# Configuración Eureka
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

#Configuración Spring API Gateway
spring.cloud.gateway.routes[0].id=products-service
spring.cloud.gateway.routes[0].uri=lb://products-service
spring.cloud.gateway.routes[0].predicates=Path=/api/products/**
spring.cloud.gateway.routes[0].filters=StripPrefix=2
 
spring.cloud.gateway.routes[1].id=items-service
spring.cloud.gateway.routes[1].uri=lb://items-service
spring.cloud.gateway.routes[1].predicates=Path=/api/items/**
spring.cloud.gateway.routes[1].filters=StripPrefix=2
 </code></pre>
 
 <b>Con esto, se estaría usando Spring API Gateway en vez de Zuul. Tiene una configuración similar y la diferencia es que con Spring Gateway no se tiene que poner 
 ninguna anotación en la clase app run y con Zuul se tiene que poner la anotación @EnableZuulProxy.
 Además, Spring Gateway al igual que Zuul, tiene balanceo de carga de manera implícita.
 </b>
 
 ### Filtros con Spring Cloud Gateway
 Cada request que pase por el Spring Cloud Gateway se van a ejecutar estos filtros. Al igual que Zuul, puede tener filtros PRE,POST y ROUTE.
 - Crear un paquete llamado filters dentro del paquete principal.
 - Se crea una clase de ejemplo de filter llamado @Component GlobalFilterExample implements GlobalFilter. Y se implementan
 el método Override filter.
 - Para implementar el filtro PRE y POST en el método filter, se necesita programación reactiva con la función then. (Ver clase  GlobalFilterExample)
 
 <pre>
 <code>
 	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		
		//Lo de antes del return es el filter PRE y lo de después del den es el filtro POST
		logger.info("Execute PRE Filter");
		
		
 		return chain.filter(exchange).then(Mono.fromRunnable(() ->{
			logger.info("Execute POST Filter");
			// Ejemplo para añadir una cookie a la respuesta
			exchange.getResponse().getCookies().add("color", ResponseCookie.from("color", "red").build());
			// Ejemplo para transformar la respuesta en Texto plano
			exchange.getResponse().getHeaders().setContentType(MediaType.TEXT_PLAIN);
		}));
	}
 </code></pre>
 
 Modificando la respuesta es sencillo pero para modificar la request tiene algunas restricciones.
 Se necesita usar la función mutable para hacer la request modificable y con la función headers se pueden añadir tokens al header.
 <pre>
 <code>
 		/*MODIFICAR LA REQUEST*/
		exchange.getRequest().mutate().headers(h -> {
			h.add("token", "123456");
		});
 </code></pre>
 En el POST, se puede modificar esta request que se editó en el PRE:
 <pre>
 <code>
 			Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("token"))
			.ifPresent(value -> exchange.getResponse().getHeaders().add("token", value));
 </code></pre>
 
 Para aplicar un orden a las clases que implementan filtros es necesario que la clase @Component GlobalFilterExample implements Order
 e implementar el método Override getOrder().
 
 <pre>
 <code>
 	@Override
	public int getOrder() {
		return 1;
	}
 </code></pre>
 
 ### Gateway Filter Factory
 Otra manera de crear filtros mucho más personalizable.
 - Crear un nuevo packete dentro de filters llamado filters.factory.
 - Crear la clase pero no con un nombre cualquiera: <Name>GatewayFilterFactory por ejemplo, 
 @Component ExampleGatewayFilterFactory extends AbstractGatewayFilterFactory<ExampleGatewayFilterFactory.ConfigurationFilter>
 - Como se comprueba, maneja un genérico como clase de configuración que hay que crearlo como clase interna:
 <pre>
 <code>
 @Component
public class ExampleGatewayFilterFactory extends AbstractGatewayFilterFactory<ExampleGatewayFilterFactory.ConfigurationFilter> {

	public class ConfigurationFilter {

	}
}
 </code></pre>
 - Implementar el método Override apply(), es decir, aplicar el filtro con la clase de configuración personalizada.
 Es igual que el otro pero mucho más configurable porque las propiedades son dinámicas:
 <pre>
 <code>
 	@Override
	public GatewayFilter apply(ConfigurationFilter config) {
		
		//** ZONA PRE **\\

		logger.info("Execute PRE in filter factory: "+config.message);
		
		return (exchange,chain) ->{
			return chain.filter(exchange).then(Mono.fromRunnable(() ->{
				
				//** ZONA POST **\\
				logger.info("Execute POST in filter factory: "+config.message);
				
				Optional.ofNullable(config.cookieValue).ifPresent(cookie ->{
					exchange.getResponse().addCookie(ResponseCookie.from(config.cookieName, cookie).build());
				});
			}));
		};
	}
 </code></pre>
 
 - Es mucho más configurables porque en el properties o yml, se puede añadir estos filtros. Por ejemplo,
 si solo se quiere que esto se ejecute para el microservicio products, se pone en el properties del products y en items no.
 Es necesario que en el name se ponga el prefijo que se ha puesto en <name>GatewayFilterFactory y en args, los argumentos 
 de la clase interna ESTÁTICA de configuración. <b>Para que funcione y se vincule correctamente es necesario que la clase interna de configuración sea estática y que implemente los getters & setters (ver clase Configuration)</b>
 <pre>
 <code>
         filters:
          - StripPrefix=2
          - name: Example
            args:
              message: "Hello! This is a message"
              cookieName: "user"
              cookieValue: "Marcos"
 </code></pre>
 También se puede poner de forma compacta:
 <pre>
 <code>
         filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, Marcos
 </code></pre>
 Pero para este ejemplo último, se necesita añadir el orden de los objetos y el nombre de la clase. Es decir, implementar los métodos override:
 <pre>
 <code>
 	@Override
	public String name() {
		return "Cookie";
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("message", "cookieName", "cookieValue");
	}
 </code></pre>
 
 ### Filtros de fábrica en Spring Cloud Gateway
 Estos son algunos filtros que se usan de fábrica en Spring Gateway y que se pueden añadir directamente al archivo properties:
 - AddRequestHeader-> para modificar la cabecera o añadir parámetros no existentes de la request. Ej: - AddRequestHeader=token-request, 123456
 - AddResponseHeader-> para modificar la cabecera o añadir parámetros no existentes de la response. Ej:  - AddResponseHeader=token-response, 12345678
 - AddRequestParameter-> Se añade un parámetro a la request. Ej: AddRequestParameter=name, Marcos
 
  Luego estos parámetros se pueden usar en un controller
 
 <pre>
 <code>
 	@GetMapping
	public ResponseEntity<List<Item>> getAllItems(@RequestParam(name = "name",required = false) String name, @RequestHeader(name = "token-request",required = false) String token) {
		System.out.println("Name: "+name);
		System.out.println("Token: "+token);
		return new ResponseEntity<>(this.itemService.findAll(), HttpStatus.OK);
	}
 </code></pre>
 
 - Para modificar se usa el prefijo Set y se usa para parámetros ya existentes de las cabeceras. Ej: - SetResponseHeader=Content-Type, text/plain
 - Todos los filtros de fábrica se encuentran en: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gatewayfilter-factories
 
 
 ### Predicates de fábrica en Spring Cloud Gateway
 Los predicates son reglas del request. Por ejemplo, la regla Path que para ejecutarse cierto microservicio, necesita que tenga una ruta específica difinida en Path.
 <pre>
 <code>
         predicates:
          - Path=/api/products/**
 </code></pre>
 <b>Pero hay muchos más:</b>
 
 <pre>
 <code>
        predicates:
          - Path=/api/products/**
          # Que el header lleve un parámetro token y tiene que ser un digito(marcado con \d+)
          - Header= token, \d+ 
          #- Header= Content-Type,application/json 
          # Que solo permitan GET y POST
          - Method=GET, POST
          # Envia una Query? en la url con el parámetro color y valor verde
          # - Query=color, green para un color en específico
          - Query=color
          # Envia cookies al ejecutar esta URL
          - Cookie=color, blue
 </code></pre>
 
 Si no se cumplen todas estas reglas, da el siguiente error: 404 Not Found.
 Hay muchos más predicates que se pueden encontrar en: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gateway-request-predicates-factories
 
## Resilence4J Circuit Breaker
Viene a reemplazar a Hystrix. Muchas veces en un ecosistema de microservicios la comunicación puede fallar, puede que tarde demasiado en responder
o que el servicio arroje aguna excepción o simplemente que el servicio no esté en disponible.
La solución es implementar el patrón Circuit Breaker con Resilence4J.

Resilence4J es una librería para trabajar la resilencia y tolerancia a fallos e implementa el patrón cortocircuito, diseñada con programación funcional.
Estados del Circuit Breaker:
1. Cerrado: cuando no hay fallos.
2. Abierto. Cuando la tasa de fallos/tiempo de espera están por encima del umbral.
3. Semi abierto: Se hacen pruebas aqui y después de un tiempo de espera si la tasa de fallas está por encima del umbral vuelve a abierto y sino sigue al estado cerrado.

Parámetros del Circuit Breaker (POR DEFECTO):
- slidingWindowSize(100) -> muestreo estádistico con 100 pruebas y si es mayor al umbral, cortocircuito
- failureRateThreshold(50) -> porcentaje de fallas. Si de 100 pruebas fallan 50, cortocircuito
- waitDurationInOpenState(60000ms) -> tiempo que pertenece en abierto y no recibe más peticiones
- permittedNumberOfCallsInHalfOpenState(10) -> nº peticiones de prueba permitido de llamadas en estado semi abierto.
- slowCallRateThreshold(100) -> si de 100 llamadas, las 100 son lentas -> cortocircuito
- slowCallDurationThreshold(60000) -> si se tarda más de 1 minuto -> cortocircuito

Para usar Resilence4J, lo primero actualizar el pom.xml a la ultima versión de Spring y Spring cloud ya que Hystrix usaba Spring<=2.3.
<pre>
<code>
	!-- Para usar Hystrix
	 version 2.3.0.RELEASE /version> --!
	 version 2.7.4 /version 
		
	 properties 
		java.version 11 /java.version
		!--Para usar Hystrix
		
		spring-cloud.version Hoxton.SR12 /spring-cloud.version --!
		spring-cloud.version 2021.0.4 /spring-cloud.version
	 /properties 
</code></pre>

Y en la clase principal, quitar el @EnableCircuitBreaker que usaba Hystrix:
<pre>
<code>
//@EnableCircuitBreaker // Para usar Hystrix para la tolerancia a fallos y timeouts
</code></pre>

Ahora solo falta añadir las dependencias.
<pre>
<code>
		dependency
			groupId org.springframework.cloud /groupId 
			 artifactId spring-cloud-starter-bootstrap /artifactId 
		/dependency
		dependency
			groupId org.springframework.cloud /groupId 
			 artifactId spring-cloud-starter-circuitbreaker-resilience4j /artifactId 
		/dependency
</code></pre>

Spring Cloud boostrap no tiene nada que ver con Resilence4J pero
se usará para implementar un archivo de configuración y añadir el parámetro en properties.

<pre>
<code>
spring.config.import=optional:configserver:
</code></pre>

Ahora para cualquier Controller, se puede usar el objeto @Autowired private CircuitBreakerFactory circuitBreakerFactory;
en alguna requestmapping y además a la vez poner el método alternativo.
<pre>
<code>
	@GetMapping("/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem(@PathVariable Long id, @PathVariable Integer quantity) {
		/**Probar Resilence4j**/
		return circuitBreakerFactory.create("items").run(() ->new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK),e -> alternativeMethod(id, quantity,e));
	}
</code></pre>

Con el circuitbreaker y los parámetros por defecto. De 100 peticiones, si por ejemplo se hacen 55 peticiones erroneas a esta URL y 45 peticiones correctas, superará el umbral y entrará en estado cerradao.
Aquí aunque se realicen peticiones correctas, irá al método alternativo. Estára el estado semiabierto realizando con 10 pruebas de límite. Si supera el umbral del 50% de fallos, volverá al estado abierto, sino al cerrado.

### Cambiar parámetros que vienen por defecto del CircuitBreaker de Resillence4J
Existen dos formas, mediante el properties o mediante una clase Bean.

1. Mediante una clase Bean en un @Configuration
	<pre>
	<code>
	@Configuration public class AppConfig
	
	@Bean
	public Customizer<Resilience4JCircuitBreakerFactory> defaultCustomizer(){
		return factory -> factory.configureDefault(id->{
			return new Resilience4JConfigBuilder(id)
					.circuitBreakerConfig(CircuitBreakerConfig.custom()
							.slidingWindowSize(10) //por defecto es 100
							.failureRateThreshold(50) //por defecto es 50% tambien
							.waitDurationInOpenState(Duration.ofSeconds(10)) // por defecto es 60000ms
							.permittedNumberOfCallsInHalfOpenState(5) //por defecto son 10
							.build()) //si no es customizado no se necesita el build
					/*TIMEOUTS por defecto*/
					.timeLimiterConfig(TimeLimiterConfig.ofDefaults())
					.build();
		});
	}
	</code></pre>
	
#### Timeouts con Resilence4J
Se puede configurar también en el customizer la propiedad timeLimiterConfig(TimeLimiterConfig.ofDefaults()) pero en vez de que sea por defecto,
personalizarla:
<pre>
<code>
					.timeLimiterConfig(TimeLimiterConfig.custom()
							.timeoutDuration(Duration.ofSeconds(6L)) /*6 segundos se demora (por defecto es 1)*/
							.build())
</code></pre>

#### Llamadas lentas con Resilence4J
Se configura también en el customizer de la propiedad circuitBreakerConfig.
<pre><code>
		.slowCallRateThreshold(50)//por defecto es 100%
		.slowCallDurationThreshold(Duration.ofSeconds(2L)) //por defecto 60000ms
</code></pre>
Ahora toda llamada mayor de 2 seg se registra como llamada lenta.
Cabe destacar que primero ocurre el timeout antes que la llamada lenta por lo que el tiempo de la llamada lenta tendra que ser menor.
A diferencia de los timeouts, estas llamadas lentas se van a ejecutar como 200 OK pero se registará como llamada lenta que si se supera el 50% del umbral establecido, entrara en cortocircuito.

2. Modificando el application.properties

Se crea un nombre de configuracion y se le asigna a la instancia creada en el circuitBreakerFactory (return circuitBreakerFactory.create("items").run(()) en este caso para items.

<pre><code>
resilience4j:
  circuitbreaker:
    configs:
      defaultConfigItems:
        sliding-window-size: 6
        failure-rate-threshold: 50
        wait-duration-in-open-state: 20s
        permitted-number-of-calls-in-half-open-state: 4
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
    instances:
      items:
        base-config: defaultConfigItems
  ## Configurar Timeout
  timelimiter:
    configs:
     defaultConfigItemsTimeout:
        timeout-duration: 6s
    instances:
      items:
        base-config: defaultConfigItemsTimeout
</code></pre>

en properties:

<pre><code>
resilience4j.circuitbreaker.configs.defaultConfigItems.sliding-window-size=6
resilience4j.circuitbreaker.configs.defaultConfigItems.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.defaultConfigItems.wait-duration-in-open-state=20s
resilience4j.circuitbreaker.configs.defaultConfigItems.permitted-number-of-calls-in-half-open-state=4
resilience4j.circuitbreaker.configs.defaultConfigItems.slow-call-rate-threshold=50
resilience4j.circuitbreaker.configs.defaultConfigItems.slow-call-duration-threshold=2s
resilience4j.circuitbreaker.instances.items.base-config=defaultConfigItems
 
resilience4j.timelimiter.configs.defaultConfigItemsTimeout.timeout-duration=2s
resilience4j.timelimiter.instances.items.base-config=defaultConfigItemsTimeout
</code></pre>

### Anotacion @CircuitBreaker
En vez de usar circuitBreakerFactory se puede usar la anotacion encima del método del controller.

<pre><code>
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod")
	@GetMapping("/aux/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem2(@PathVariable Long id, @PathVariable Integer quantity) {
		return new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK);
	}
</code></pre>

Esta configuración de "items" tiene que estar en el archivo properties

### Anotacion @TimeLimiter
La funcionalidad es la misma al @CircuitBreaker. Aqui la diferencia es que continua con la ejecución y no hace cortocircuito porque no contabiliza los tiempos ni los estados.
Solo contabiliza los timeouts y en CircuitBreakers se contabilizan las excepciones y llamadas lentas.
Llamada futura asincrona. Cabe destacar que el método alternativo tambien tiene que devolver un CompletableFuture.

<pre><code>
	@TimeLimiter(name = "items",fallbackMethod = "alternativeMethod2")
	@GetMapping("/aux2/{id}/quantity/{quantity}")
	public CompletableFuture<ResponseEntity<Item>> getItem3(@PathVariable Long id, @PathVariable Integer quantity) {
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK));
	}
</code></pre>

También se puede combinar con @CircuitBreaker pero si se combina es necesario quitar el fallbackMethod del TimeLimiter para que el CircuitBreaker haga la toleracion de fallos.
<pre><code>
	@TimeLimiter(name = "items")//,fallbackMethod = "alternativeMethod2")
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod2") //se puede quitar o combinar con TimeLimiter
	@GetMapping("/aux2/{id}/quantity/{quantity}")
	public CompletableFuture<ResponseEntity<Item>> getItem3(@PathVariable Long id, @PathVariable Integer quantity) {
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK));
	}
</code></pre>

### Resilience4J en el API Gateway
- Añadir la dependencia Resilience4J en el pom.xml de gateway-server. Pero a diferencia de antes,
se tiene que anotar como reactiva:
<pre><code>
		dependency
			groupId org.springframework.cloud /groupId 
			 artifactId spring-cloud-starter-circuitbreaker-reactor-resilience4j /artifactId 
		/dependency
</code></pre>
- Colocar la configuracion de Resilience4J en el archivo properties del API Gateway. En este caso
se va a crear la configuracion "products"
<pre><code>

resilience4j:
  circuitbreaker:
    configs:
      defaultConfigProducts:
        sliding-window-size: 6
        failure-rate-threshold: 50
        wait-duration-in-open-state: 20s
        permitted-number-of-calls-in-half-open-state: 4
        slow-call-rate-threshold: 50
        slow-call-duration-threshold: 2s
    instances:
      products:
        base-config: defaultConfigProducts
  timelimiter:
    configs:
     defaultConfigProductsTimeout:
        timeout-duration: 2s
    instances:
      products:
        base-config: defaultConfigProductsTimeout
</code></pre>

- Ahora, para añadirlo al Gateway de products, es necesario colocar la configuración products como filtro.
El nombre es por defecto CircuitBreaker:

<pre><code>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - CircuitBreaker=products
</code></pre>

-Pero con esta configuración no entra en cortocircuito en las excepciones. Hay que hacer otra configuración:
<pre><code>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - name: CircuitBreaker
            args:
              name: products
              statusCodes: 500,404
</code></pre>

- Para crear métodos alternativos en la API Gateway lo que hay que hacer es añadir otro argumento llamado <b>fallbackUri</b>
En esta Uri se tiene que indicar otro microservicio que no sea el propiertario de este filtro ya que éste estará en cortocircuito y seguirá
sin estar disponible para hacer método alternativo

<pre><code>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - name: CircuitBreaker
            args:
              name: products
              statusCodes: 500,404
              fallbackUri: forward:/api/items/2/quantity/3
</code></pre>

## Servidor de configuración con Spring Cloud Config Server

Es un proyecto que va a tener las configuraciones de todos los microservicios de la aplicación.

- Se crea el nuevo proyecto que tendrá las dependencias DevTools y Config Server de Spring Cloud Config.
- La application run tendrá la anotación <b>@EnableConfigServer</b>
- En properties, se necesita crear un repositorio para tener la configuración, para ello, en properties se debe poner
la url del respositorio.(ver properties config-server)
<pre><code>
spring.cloud.config.server.git.uri=file:///XXXX/Desktop/config
</code></pre>
- Crear una carpeta en escritorio config y dentro git init. A partir de aqui se va a escribir la configuración:
- Toda esta configuración de este proyecto se va a sobrescribir. Es decir si encuentra configuración en el properties que sea igual en este proyecto que en 
otros microservicios, se va a sustituir y la demás configuración va a quedar como está.
Por ejemplo, en el servicio items establecido en el puerto 8002, con la siguiente línea se va sustituir por el puerto 8005
<pre><code>
..\Escritorio\config> echo server.port=8005 > items-service.properties
</code></pre>

Con esto se creará el archivo items-service.properties con el server.port a 8005. 
Seguir configurando el items-service.properties:

<pre><code>
server.port=8005 
myconfig.text=Configurando entorno Desarrollo
</code></pre>

### Conectar el servidor de configuración con el microservicio.
Antes que nada, probar la configuración hasta ahora de items-service. Se levanta el proyecto config-server y en postman
realizar la petición: localhost:8888/items-service para ver mediante una petición GET la configuración que se ha escrito en el fichero.

La respuesta dará la configuración:
<pre><code>
{
    "name": "items-service",
    "profiles": [
        "default"
    ],
    "label": null,
    "version": "bd55976518b7a0b28636d928a2afabfb48bd7c99",
    "state": null,
    "propertySources": [
        {
            "name": "file://C:/Users/marco/Escritorio/config/file:C:\\Users\\marco\\Escritorio\\config\\items-service.properties",
            "source": {
                "server.port": "8005 ",
                "myconfig.text": "Configurando entorno Desarrollo"
            }
        }
    ]
}
</code></pre>

- Ahora, para vincular esta configuración al microservicio items es necesario añadir a este microservicio items 
una nueva dependencia <b>Config Client</b>

<pre><code>
dependency = spring-cloud-starter-config
</code></pre>

-Para vincular items-service con config-server, se tiene que crear en resources un archivo llamado boostrap.properties que tenga 
la uri del servidor de configuración.

<pre><code>
spring.application.name=items-service
spring.cloud.config.uri=http://localhost:8888
</code></pre>

Va a cargar primero boostrap.properties que el properties o yml del microservicio

- Para usar la configuración del bootstrap.properties en el microservicio. Como es un @ClientConfig, se 
puede inyectar la anotación @Value.

Se puede usar como global o tambien como parámetro.

<pre><code>
@RestController
public class ItemController {
	
	@Value("${myconfig.text}")
	@Autowired
	private String text;
	
		@GetMapping("/get-config")
		public ResponseEntity<?> getConfig(@Value("${server.port}") String port ){
		logger.info(text);
		
		Map<String, String> json = new HashMap<>();
		json.put("text", this.text);
		json.put("port", port);
		return new ResponseEntity<Map<String,String>>(json,HttpStatus.OK);
	}

}
</code></pre

-Para probarlo, se levanta el servidor de configuración primero y al hacer el GET en Postman, se puede ver 
como ha cambiado el puerto:localhost:8005/get-config


<pre><code>
{
    "port": "8005",
    "text": "Configurando entorno Desarrollo"
}
</code></pre>


### Configurando los entornos en repositorio de configuración
En la carpeta Config, se crean dos nuevos archivos y tienen que llevar el siguiente formato:
<b>[nombreMicroservicio]-[entorno].properties</b>

Para desarollo: items-service-dev.properties
<pre><code>
server.port=8005
myconfig.text=Configurando entorno Desarrollo
myconfig.author.name=Marcos
myconfig.author.email=marcos@marcos.com
</code></pre>

Para producción: items-service-prod.properties
<pre><code>
server.port=8007
myconfig.text=Configurando entorno Producción
myconfig.author.name=Marcos
myconfig.author.email=marcos@marcos.com
</code></pre>

Y se tienen que hacer los commits de este repositorio antes de probar.

- Para configurarlo en Spring, se necesita modificar el bootstrap.properties de items-service o del microservicio que se ha añadido la configuración
en el repositorio config.

<pre><code>
spring.application.name=items-service
spring.cloud.config.uri=http://localhost:8888
spring.profiles.active=dev
</code></pre>

Para leer estos valores en el controller, se puede usar también springframework.core.env.Environment en vez de la
anotación @Value

<pre><code>
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
</code></pre>

Y al hacer la petición localhost:8005/get-config, se puede comprobar como al estar en el entorno de desarrollo,
se va a dar la siguiente respuesta.

<pre><code>
{
    "author.name": "Marcos",
    "port": "8005",
    "author.email": "marcos@marcos.com",
    "text": "Configurando entorno Desarrollo"
}
</code></pre>

Ahora al hacer la petición del servidor de configuración, se puede poner el entorno localhost:8888/items-service/dev y devolverá las configuraciones
establecidas. Se puede apreciar como además del entorno dev, también tiene la configuración de por defecto y si tienen las mismas propiedades, sobrescribe sobre el de por defecto.

<pre><code>
{
    "name": "items-service",
    "profiles": [
        "dev"
    ],
    "label": null,
    "version": "284fbf162f959122205f1df332d6461fef60d09f",
    "state": null,
    "propertySources": [
        {
            "name": "file:///C:/Users/marco/Escritorio/config/file:C:\\Users\\marco\\Escritorio\\config\\items-service-dev.properties",
            "source": {
                "server.port": "8005",
                "myconfig.text": "Configurando entorno Desarrollo",
                "myconfig.author.name": "Marcos",
                "myconfig.author.email": "marcos@marcos.com"
            }
        },
        {
            "name": "file:///C:/Users/marco/Escritorio/config/file:C:\\Users\\marco\\Escritorio\\config\\items-service.properties",
            "source": {
                "server.port": "8005",
                "myconfig.text": "Configurando entorno default"
            }
        }
    ]
}
</code></pre>


### Actualizar cambios de la configuración en un microservicio con @RefreshScope
Sirve para refrescar el controlador o bean que se use en caso de que cambie alguna configuración
del archivo properties tanto del servidor de configuración como del propio properties del microservicio
En caso de cambio en el Environment, inyecta de nuevo los autowired y el controlador. 
Es necesario añadir la dependencia Spring Boot Actuator en el microservicio que se quiera utilizar.

<pre><code>
@RefreshScope
@RestController
public class ItemController {
...
}
</code></pre>

También se necesita modificar el bootstrap.properties para indicar el uso del RefreshScope
<pre><code>
spring.application.name=items-service
spring.cloud.config.uri=http://localhost:8888
spring.profiles.active=dev
#Para que actualice con @RefreshScope los controladores en caso de cambio en la configuración
management.endpoints.web.exposure.include=*
</code></pre>

- Ahora al modificar mediante un commit el archivo de configuración del servidor:

items-service-dev.properties
<pre><code>
server.port=8005
myconfig.text=Configurando entorno Desarrollo ...
myconfig.author.name=Jhon
myconfig.author.email=jhon@jhon.com
</code></pre>

git add *
git commit ....

Al hacer una petición POST del actuator: <b>localhost:8005/actuator/refresh </b>
Indicará en la respuesta que se han cambiado correctamente los campos:

<b>200 OK</b>
<pre><code>

[
    "config.client.version",
    "myconfig.text"
]
</code></pre>

<b>Esto se puede hacer con configuraciones propias pero no se podrá actualizar en tiempo real configuraciones
del servidor como server.port</b>


## Biblioteca Commons para reutilzar código en microservicios
La case Items y products son muy parecidos. Se puede crear un microservicio a parte para reutilizar código.
- Crear un nuevo proyecto commons-service con dependencias Jpa
- Quitar el proyecto main run ya que es un proyecto de librería y no se va a ejecutar.
- Quitar en el pom.xml el plugin maven en build
- Deshabilitar el autoconfiguración del DataSource que viene por defecto en SpringBoot: 

<pre><code>
@SpringBootApplication
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
public class CommonsServiceApplication {

}
<b>También desactivar en service-items. OTRA Solución es usar en el commons-service la dependencia H2</b>

</code></pre>

- Crear el jar: ir al directorio y ejecutar mvn install: ...\commons-service>mvnw.cmd install
- Ir al pom.xml del common y copiar el groupId,artifactId y version para pegarlo como dependencia
en los microservicios que se vayan a usar:

<pre><code>
		dependency
			groupId com.app.commonservice /groupId
			artifactId commons-service /artifactId
			version 0.0.1-SNAPSHOT /version
		/dependency
</code></pre>

- Ahora importar el Product del paquete commons:import com.app.commonservice.models.entities.Product en todas las clases donde se use.
- Utilizar la anotacion @EntityScan ya que Product no está en el paquete raíz y no lo encuentra. Con esta anotación obliga a escanear en el package commons:

<pre><code>
@EnableEurekaClient
@SpringBootApplication
@EntityScan({"com.app.commonservice.models.entities"})
public class ProductsServiceApplication {
...
}
</code></pre>

## Spring Cloud Security: OAuth2 y JWT

### Introducción a JWT (JSON Web Token)

El usuario envía un código alfanumérico al servidor. El servidor se encarga de descifrar y validar el código comprobando si existe en el sistema y qué roles tiene.
En este token puede ir información no sensible como el nombre, los roles, email, pero nunca la contraseña o tarjetas de crédito.
Este estandar permite descodificar este código y generar este código a partir de una clave secreta con clave pública y privada.
A partir de esta clave secreta se va a generar el token y se podrá comprobar si ha sido manipulado.

Características:
- El token es muy compacto y pequeño que va dentro de las cabeceras Http y almacena gran cantidad de información del usuario sin tener que realizar consultas al sevidor
- Un problema es que el token se puede decodificar pero al firmarlo con clave secreta se va a comprobar si el token es manipulado.
- El tiempo de caducidad del token por defecto es ilimitado por eso hay que configurar este tiempo.
- Cada vez que se quiere acceder a un recurso de la api protegido, hay que enviar el token. ç
- Analizar Jwt. Tiene 3 partes separadas por un punto. El header, la data o payload y la parte de seguridad.

En el header se obtiene información sobre el algoritmo y el tipo.
{
  "alg": "HS256",
  "typ": "JWT"
}

El payload estarían los datos.
{
  "sub": "1234567890",
  "name": "John Doe",
  "iat": 1516239022
}

La parte de seguridad que verifica la firma. Require un código secreto de 256 bits. Sirve para 
verificar que este token no ha sido manipulado gracias a la clave secreta.
HMACSHA256(
  base64UrlEncode(header) + "." +
  base64UrlEncode(payload),
  
your-256-bit-secret

) 

### Introducción a OAuth2
Spring Security provee características de seguridad para aplicaciones Java. Maneja componentes de Autenticación
y Autorización(control acceso).

OAuth2 es un framework de autorización que permiten a las aplicaciones de terceros (angular,react,etc) autenticarse en el servidor
sin tener que compartir información en el acceso como las credenciales.
Se compone de dos partes:
1. <b>Autorization Server:</b> es el servidor de autorización. Se encarga de realizar la autenticación del usuario. Si
es válida, retorna un token. Y con este token el usuario acceder a los recursos.

<b>POST: /auth/token </b>

<b>Header:</b>
Authorization: Base64(client_id:client_secret)
Content-Type: application/x-www.form-urlencoded

<b>Body</b>
grant_type = password
username = user
password = 12345

<b>Authorization Server return:</b>

{
"access_token": "dadsdsa54da5d4saf45"
"token_type": "bearer"
"refresh_token": "dadsdsa54da5d4saf45"
"expires_in": 3589
"scope": "read write"
"jti": "58d4adsa-eads584-bf0-dsae54ad"
}

2. <b>Resource server:</b> servidor de recursos. Se encarga de administrar los permisos y accesos a las url/endpoints del backend.

<b>GET: /api/items/products </b>

<b>Header:</b>
Authorization: Authorization Bearer acces_token

<b>Resource Server return</b>
Output in JSON


### Autenticación con Spring Cloud Security

#### 1. Crear el microservicio para usuarios con dependencias: DevTools, H2, Jpa, Eureka Client, Spring Web
#### 2. Configurar el properties del microservicio:

<pre><code>
spring.application.name=users-service
server.port=${PORT:0}

# h2 console properties
spring.h2.console.path=/h2-console
spring.h2.console.settings.trace=false
spring.h2.console.settings.web-allow-others=false
spring.datasource.url=jdbc:h2:mem:testdb

# Eureka Configuration
eureka.client.service-url.defaultZone = http://localhost:8761/eureka
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}

# Debugear JPA
logging.level.org.hibernate,SQL = debug
</code></pre>

#### 3. Crear las clases entity: Users y Roles

- Clase User:

<pre><code>
@Entity
@Table(name = "users")
public class User implements Serializable {

	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 20)
	private String username;

	@Column(length = 60)
	private String password;

	private Boolean enabled;

	private String firstName;

	private String lastName;

	@Column(unique = true, length = 100)
	private String email;
	
	@ManyToMany(fetch = FetchType.LAZY) 
	@JoinTable(
	 name = "users_roles", 
	 joinColumns = @JoinColumn(name="user_id"), 
	 inverseJoinColumns = @JoinColumn(name="role_id"), 
	 uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id","role_id"})}
	)
	private List<Role> roles;
	
	... getters and setters	
}
</code></pre>

- Clase Role:

<pre><code>
@Entity
@Table(name = "roles")
public class Role implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, length = 30)
	private String name;
	
	... getters and setters
}
</code></pre>

#### 4. Crear DAOS/Repository con Spring Rest Repository
Aquí se pueden revisar Query Methods:
https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.query-creation

Se puede utilizar la dependencia Spring Web Rest Repository que lo que hace es exportar el repositorio a un
path para que haga el CRUD sin tener que crear el controller y la clase Service.

<pre><code>
@RepositoryRestResource(path = "users") //Dependencia SpringWeb Rest Repositories
public interface UserDAO extends PagingAndSortingRepository<User, Long> {
	public User findByUsername(String username);
	
	@Query("select u from User u where u.username=?1 and u.email=?2") //utilizando jpa HQL
	//@Query(value = "select * from users u where u.username=?1 and u.email=?2", nativeQuery = true)
	public User getByUsernameAndEmail(String username, String email);
}

</code></pre>
Con esto ya se pueden hacer peticiones con POSTMAN a este repositorio


### 5. Vincular esta ruta al api-gateway de Zuul Server

Añadir en el application properties: 

<pre><code>
zuul.routes.users.service-id=users-service
zuul.routes.users.path=/api/users/**
</code></pre>


### 6. Vincular los métodos del Repositorio con Spring Rest Repository

Teniendo un repositorio con Spring Rest Repository:

<pre><code>
@RepositoryRestResource(path = "users") //Dependencia SpringWeb Rest Repositories
public interface UserDAO extends PagingAndSortingRepository<User, Long> {
	
	public User findByUsername(String username);
	
	@Query("select u from User u where u.username=?1 and u.email=?2") //utilizando jpa HQL
	//@Query(value = "select * from users u where u.username=?1 and u.email=?2", nativeQuery = true)
	public User getByUsernameAndEmail(String username, String email);
}
</code></pre>

Se pueden hacer consultas en Postman de la siguiente manera:
/api/users/users/search/<metodo>?queryParams
Por ejemplo: http://localhost:8090/api/users/users/search/findByUsername?username=admin

Para editar esta runta, se añade al método la etiqueta <b>@RestResource(path="nombreURL")</b>
y para los parámtros la anotación <b>@Param("<nombre>")</b>

<pre><code>
	@RestResource(path = "search-username")
	public User findByUsername(@Param("name")String username);
</code></pre>

#### También se puede modificar la respuesta.

Para esto, se crea una clase <b>@Configuration</b> que implementa la <b>interfaz RepositoryRestConfigurer.</b>
Se implementa el método <b>@Override configureRepositoryRestConfiguration</b> con el parámetro config que tiene métodos para 
configurar el Spring Rest Repository, por ejemplo, exposeIdFor que muestra las ids de las clases seleccionadas:

<pre><code>
@Configuration
public class RepositoryConfig implements RepositoryRestConfigurer {

	@Override
	public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
		config.exposeIdsFor(User.class,Role.class);
	}

}
</code></pre>

### 7. Crear biblioteca commons para usuarios para usarlas en otros microservicios.

Se copian y pegan las clases Role Y User en el commons-service en el paquete entities y se ejecuta en commons-service
el comando mvnw install para que se ejecute el jar.
Por último se agrega la dependencia del commons-service en el pom.xml de users-service para que se puedan cambiar los imports sin problema.

<pre><code>
		dependency
			groupId com.app.commonservice /groupId
			artifact Idcommons-service /artifactId
			version 0.0.1-SNAPSHOT /version
		</dependency>
</code></pre>

Y por último, no olvidarse de la anotación @EntityScan el el application.run de user-service para que escanee
la entidad User y Role.

### 8. Crear microservicio OAuth2 (Servidor de Autorización)

- Dependencias Eureka Client, Spring Web, Dev Tools, OpenFeign y agregar dependencias manuales de Spring Security:
Se va a utilizar Spring Security Oauth 2.3.8.RELEASE porque desde la 2.4>= se quita el servidor de autorización para crear el token y se tendría
que llamar a librerías externas a Spring Boot/Spring Security

<b>OAuth2:</b>
<p>
		<dependency>
			<groupId>org.springframework.security.oauth</groupId>
			<artifactId>spring-security-oauth2</artifactId>
			<version>2.3.8.RELEASE</version>
		</dependency>
</p>
<b>JWT</b>
<p>
		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-jwt</artifactId>
			<version>1.1.1.RELEASE</version>
		</dependency>
</p>
<b>JAXB: si se usa Java 11</b>
<p>
		<dependency>
			<groupId>org.glassfish.jaxb</groupId>
			<artifactId>jaxb-runtime</artifactId>
		</dependency>
</p>

- Agregar la dependencia Commons para utilizar las clases User y Role.
Como este proyecto no maneja base de datos ni persistencia y la dependencia commons si la usa, va a pedir
una base de datos Jpa.
Se puede incluir la anotacion en el application.run @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class})
o agregar la dependencia con la etiqueta exclusions y la dependencia a excluir:

<p>
		<dependency>
			<groupId>com.app.commonservice</groupId>
			<artifactId>commons-service</artifactId>
			<version>0.0.1-SNAPSHOT</version>
			<exclusions>
				<exclusion>
					<groupId>org.springframework.boot</groupId>
					<artifactId>spring-boot-starter-data-jpa</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
</p>

- Configurar el properties:

<pre><code>
spring.application.name = oauth-server
server.port=9100

# Eureka Configuration
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

# Para el servidor de configuraciones
spring.config.import=optional:configserver:
</code></pre>

- Crear Feign Client

Añadir en el application.run la anotación @EnableFeignClients y ahora crear una interfaz con
la anotación @FeignClient(name = "users-service") con el microservicio a utilizar. 
Se agregan los métodos en la interfaz.

<pre><code>
@FeignClient(name = "users-service")
public interface UserFeignClient {
	
	@GetMapping("/users/search-username")
	public User findByUsername(@RequestParam String username);

}
</code></pre>

- Implementar la clase de Login con UserDetailsService y Feign Client

Esta va a ser la clase que se va a utilizar para el método de login.
Se crea una clase UserService que va a implementar la interfaz de Spring UserDetailService.
Se tendrá que implementar el método <b>@Override loadUserByUsername.</b>
En este método se necesita crear la lista de authorities con la interfaz GrantedAuthority y las intancias
SimpleGrantedAuthority.

<pre><code>
@Service
public class UserService implements UserDetailsService {
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = this.userFeignClient.findByUsername(username);
		
		if(user == null) {
			logger.error("Error: username "+username+" not found.");
			throw new UsernameNotFoundException("Error: username "+username+" not found.");
		}
		
		List<GrantedAuthority> authorities = user.getRoles().stream().map(
				role -> new SimpleGrantedAuthority(role.getName())
				)
				.peek(authority -> logger.info("Role: "+authority.getAuthority()))
				.collect(Collectors.toList());
		
		
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), user.getEnabled(),
				true, true, true, authorities);
	}
}
</code></pre>

- Añadir Spring Security Config y registrar UserDetailService

Se crea una clase que implementa la interfaz WebSecurityConfigurerAdapter que tendrá
2 métodos para sobrescribir. Configure() para indicar el servicio UserService para encontrar el usuario y el password encoder.
El otro el AuthenticationManager para la clase de authenticacion.

<pre><code>
@Configuration
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {
	
	@Autowired
	private UserDetailsService userService; //Spring buscara el bean que implemente esta interfaz, en este caso UserService
	
	@Bean
	public static BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Override
	@Bean
	protected AuthenticationManager authenticationManager() throws Exception {
		return super.authenticationManager();
	}

	@Override
	@Autowired
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.userDetailsService(this.userService).passwordEncoder(passwordEncoder());
	}
}
</code></pre>

<b>WebSecurityConfigurerAdapter está deprecated! </b>
En su lugar, utilizar FilterChain

@Configuration
public class SpringSecurityConfig {
	
	@Bean
	public static BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
	
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests((authz) -> authz
                .anyRequest().authenticated()
            ).build();
        return http.build();
    }
	
}

- Crear el @Configuration AuthorizationServerConfig

Es una clase que extenderá de AuthorizationServerConfigurerAdapter y tendrá 3 métodos:

1. public void configure(AuthorizationServerEndpointsConfigurer endpoints)

Que es el encargado de configurar, almacenar y firmar el token con una clave secreta.

<pre><code>

	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

		endpoints
		.authenticationManager(this.authenticationManager)
		.tokenStore(tokenStore())
		.accessTokenConverter(accesTokenConverter());
	}

	@Bean
	public JwtTokenStore tokenStore() {
		return new JwtTokenStore(accesTokenConverter());
	}

	@Bean
	public JwtAccessTokenConverter accesTokenConverter() {
		JwtAccessTokenConverter tokenConverter = new JwtAccessTokenConverter();
		
		// Firmar el token
		tokenConverter.setSigningKey("key_secret");
		return tokenConverter;
	} 
</code></pre>

2. public void configure(ClientDetailsServiceConfigurer clients)

Se configuran los clientes frontend que van a usar la aplicación, por ejemplo, Postman,React,Angular,etc.
Se encarga de crear una doble autenticación con el estandar Oauth. Es decir, con Auth va tener que autenticarse
con los credenciales del usuario y además, se creará una autenticación con contraseña con el cliente frontend.

<pre><code>
	@Override
	public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
		clients.inMemory()
		.withClient("angularapp") //se asigna un nombre al cliente
		.secret(this.passwordEncoder.encode("12345")) //se asigna una contraseña
		.scopes("read", "write") //se establecen los permisos
		.authorizedGrantTypes("password", "refresh_token")
		.accessTokenValiditySeconds(3600); //Con lo que se hará la autenticación. Con refresh_token para obtener un token de acceso renovado justo antes que caduque el actual
		
		/** REGISTRAR OTRO CLIENTE
		 * .and().
		withClient("reactapp") //se asigna un nombre al cliente
		.secret(this.passwordEncoder.encode("12345")) //se asigna una contraseña
		.scopes("read", "write") //se establecen los permisos
		.authorizedGrantTypes("password", "refresh_token").accessTokenValiditySeconds(3600); 
		**/
	}
</code></pre>

3. public void configure(AuthorizationServerSecurityConfigurer security)
Es el permiso que va a tener los endpoints en el servidor de autorización

<pre><code>
	@Override
	public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
		security.tokenKeyAccess("permitAll()") // permitAll() es un método de Spring Security para generar el token y autenticarse con oauth/login y que sea publico para que todo el mundo se pueda autenticar.
		.checkTokenAccess("isAuthenticated()"); // para validar el token, se llama al método isAuthenticated() que validará que el usuario esté autenticado.
	}
</code></pre>

- Agregar la ruta en el api gateway Zuul Server
Se modifica el <b>archivo properties de zuul-server</b>. Destacar que se tienen que quitar de las cabeceras las cookies para
que funcione la autenticación en el servidor Zuul:

<pre><code>
## Security
zuul.routes.security.service-id=oauth-server
zuul.routes.security.path=/api/security/**
## Quitar de las cabeceras las Cookies para habilitar la autenticaicón con Zuul
zuul.routes.security.sensitive-headers=Cookie, Set-Cookie
</code></pre>

- Login con Postman.

--> Método: POST
--> URL: http://localhost:8090/api/security/oauth/token
--> Authorization/Basic Authorization -> username:frontendapp password:12345
--> Body: x-wwww-form-urlencoded
	-username: admin
	-password: 12345
	-grant_type: password (esto indica el tipo de autenticación)
