# MicroservicesAppCloud - Marcos Ruiz Muñoz 
Formación en microservicios con Spring Cloud

**Índice**   
1. [Rest Template y Feign Client](#id1)
2. [Balanceo de carga con Ribbon](#id2)
3. [Servidor Eureka](#id3)
4. [Escalar microservicios con puerto dinámico](#id4)
5. [Hystrix](#id5)
6. [Servidor Zuul API Gateway](#id6)
7. [Spring Cloud API Gateway (Alternativa a Zuul)](#id7)
8. [Resilence4J Circuit Breaker](#id8)
9. [Servidor de configuración con Spring Cloud Config Server](#id9)
10. [Biblioteca Commons para reutilzar código en microservicios](#id10)
11. [Spring Cloud Security: OAuth2 y JWT](#id11)
12. [Spring Cloud Security con Spring Cloud Gateway](#id12)
13. [Migrar base de datos MySQL](#id13)
14. [Crear base de datos PostgreSQL](#id14)
15. [Trazabilidad distribuida con Spring Cloud Seuth y Zipkin](#id15)
16. [Desplegando microservicios en Docker](#id16)

## Rest Template y Feign Client<a name="id1"></a>
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

## Balanceo de carga con Ribbon<a name="id2"></a>

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


## Servidor con EUREKA <a name="id3"></a>
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


## Escalar microservicios con puerto dinámico <a name="id4"></a>
La idea es que Spring de forma automática asigne el puerto de los servicios para hacer la aplicación más escalable. Para ello:
- Modificar en el archivo properties de cada microservicio, el server.port = ${PORT:0}
- Añadir una nueva línea de configuración de Eureka para que se asigne una url dinámica al servicio:
<pre>
<code>
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
</code></pre>

## Hystrix <a name="id5"></a>

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

## Servicio Zuul API Gateway <a name="id6"></a>
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

## Spring Cloud API Gateway (Alternativa a Zuul) <a name="id7"></a>
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
 
## Resilence4J Circuit Breaker<a name="id8"></a>
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
Esto es porque a partir de la 2.4>= requiere poner esto
para tener el servidor de configuración.
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

## Servidor de configuración con Spring Cloud Config Server <a name="id9"></a>

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


## Biblioteca Commons para reutilzar código en microservicios <a name="id10"></a>
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

## Spring Cloud Security: OAuth2 y JWT <a name="id11"></a>

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

- <b>Dependencias Eureka Client, Spring Web, Dev Tools, OpenFeign y agregar dependencias manuales de Spring Security:
Se va a utilizar Spring Security Oauth 2.3.8.RELEASE porque desde la 2.4>= se quita el servidor de autorización para crear el token y se tendría
que llamar a librerías externas a Spring Boot/Spring Security</b>

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

- <b>Agregar la dependencia Commons para utilizar las clases User y Role.</b>
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

- <b>Configurar el properties:</b>

<pre><code>
spring.application.name = oauth-server
server.port=9100

# Eureka Configuration
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

# Para el servidor de configuraciones
spring.config.import=optional:configserver:
</code></pre>

- <b>Crear Feign Client</b>

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

- <b>Implementar la clase de Login con UserDetailsService y Feign Client</b>

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
		
		try {
			
		User user = this.userFeignClient.findByUsername(username);

		List<GrantedAuthority> authorities = user.getRoles().stream().map(
				role -> new SimpleGrantedAuthority(role.getName())
				)
				.peek(authority -> logger.info("Role: "+authority.getAuthority()))
				.collect(Collectors.toList());
		
		logger.info("Authenticated User: "+username);
		
		return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(), user.getEnabled(),
				true, true, true, authorities);
		} catch (FeignException e) {
				logger.error("Error: username "+username+" not found.");
				throw new UsernameNotFoundException("Error: username "+username+" not found.");
		}
	}
}
</code></pre>

- <b>Añadir Spring Security Config y registrar UserDetailService</b>

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

- <b>Crear el @Configuration AuthorizationServerConfig</b>

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

- <b>Añadir información adicional al Token</b>
Primero crear una clase <b>@Component InfoAdditionalToken</b> que implemente la interfaz <b>TokenEnhancer.</b>
Se implementa el método @Override <b>enhance()</b>
La variable accessToken es el token al que se le va a añadir la información y authentication es
la variable que presenta al usuario autenticado.
accesToken necesita ser casteado a DefaultOAuth2AccessToken para acceder al método setAdditionalInformation().

<pre><code>
	@Override
	public OAuth2AccessToken enhance(OAuth2AccessToken accessToken, OAuth2Authentication authentication) {
		Map<String, Object> info = new HashMap<>();
		
		User user = this.userService.findByUsername(authentication.getName());
		info.put("firstName", user.getFirstName());
		info.put("lastName", user.getLastName());
		info.put("email", user.getEmail());
		
		((DefaultOAuth2AccessToken) accessToken).setAdditionalInformation(info);
		return accessToken;
	}
</code></pre>


Después, en el método configure del AuthorizationServerConfig, agregar el Token Enhancer Chain al token que le agregará la 
información

<pre><code>
	@Override
	public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {

		// Para añadir información adicional al token
		TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
		tokenEnhancerChain.setTokenEnhancers(Arrays.asList(infoAdditionalToken,accesTokenConverter()));
		// ----
		endpoints
		.authenticationManager(this.authenticationManager)
		.tokenStore(tokenStore())
		.accessTokenConverter(accesTokenConverter())
		.tokenEnhancer(tokenEnhancerChain);
	}
</code></pre>

- <b>Configurando Zuul como servidor de Recurso (Resource Server)</b>
Primero se tienen que añadir las dependencias jaxb, oauth2 y jwt al microservicio zuul-server.
Crear una clase que tendrá la configuración del servidor de configuración de recursos (Resource Server).
Aquí se administraran y protegerán las rutas con el método configure:

<pre><code>
@Configuration
@EnableResourceServer
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
		resources.tokenStore(tokenStore());
	}

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/api/security/oauth/token").permitAll()
		.antMatchers(HttpMethod.GET,"/api/products/","api/items/","/api/users/users/").permitAll()
		.antMatchers(HttpMethod.GET,"/api/products/{id}",
				"/api/items/{id}/quantity/{quantity}",
				"/api/users/users/{id}")
				.hasAnyRole("ADMIN","USER")
		.antMatchers("/api/products/**","/api/items/**","api/users/**").hasRole("ADMIN")
		.anyRequest().authenticated();
	}

}

</code></pre>

- <b>Crear una configuración para el servidor de configuración de OAuth</b>

Ir al repositorio Git donde está la configuración y crear un nuevo archivo llamado application.properties que tendrá
la consiguración del cliente y la firma del token:

<pre><code>
config.security.oauth.client.id=postmanapp
config.security.oauth.client.secret=12345
config.security.oauth.jwt.key=2A281F235A1A61369C76AE1DAFA3A
</code></pre>

No olvidarse de hacer commit al repositorio de configuración.

Ahora es necesario añadir la <b>dependencia Config Client</b> tanto a oauth-server como a zuul-server para que puedan usar
el servidor de configuración.

Al implementar la dependencia Config Client, estos microservicios tienen que tener el archivo bootstrap.properties:

Para oauth-server:

<pre><code>
spring.application.name=oauth-server
spring.cloud.config.uri=http://localhost:8888
management.endpoints.web.exposure.include=*
</code></pre>

Para zuul-server:

<pre><code>
spring.application.name=zuul-service-server
spring.cloud.config.uri=http://localhost:8888
management.endpoints.web.exposure.include=*
</code></pre>

Ahora en AuthorizationServerConfig se puede usar Environment para acceder a estas variables del repositorio 
del servidor de configuracion y también añadir @RefreshScope

<pre><code>
clients.inMemory()
.withClient(this.env.getProperty("config.security.oauth.client.id"))
.secret(this.passwordEncoder.encode(this.env.getProperty("config.security.oauth.client.secret")))

tokenConverter.setSigningKey(this.env.getProperty("config.security.oauth.jwt.key"));
</code></pre>

Y en Zuul-Server igual se puede hacer también con @Value y @RefreshScope

	@Value("${config.security.oauth.jwt.key}")
	private String jwtKey;
	
	tokenConverter.setSigningKey(jwtKey);
	
- <b>Configurar Cors en la aplicación</b>

CORS es un mecanismo que utiliza las cabeceras HTTP para permitir que una aplicación cliente que está en otro
servidor al backend tenga los permisos de acceder a los recursos o endpoints.
Para esto se tendrá que añadir que configurar el cors en el ResourceServerConfig y añadirlo al http para habilitar cors en la aplicación:

<pre><code>

		....
		.anyRequest().authenticated()
		.and().cors().configurationSource(corsConfigurationSource());


	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedOrigin("*"); // Con el asterisco se permite cualquier origen, si solo se quiere permitir una lista de origenes, es el método setAllowedOrigins("http://localhost:4090",...)
		corsConfiguration.setAllowedMethods(Arrays.asList("POST","GET","PUT","DELETE","OPTIONS")); // OPTIONS es importante ya que Oauth2 lo utiliza
		corsConfiguration.setAllowCredentials(true);
		corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
		
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		
		source.registerCorsConfiguration("/**", corsConfiguration); // ** para aplicarlo a todas las rutas
		return source;
	}
	
	/**
	 * Para configurarlo a nivel global como un filtro
	 */
	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter(){
		FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<CorsFilter>(new CorsFilter(corsConfigurationSource()));
		bean.setOrder(Ordered.HIGHEST_PRECEDENCE); //MAXIMA Prioridad
		return bean;
	}
</code></pre>

- <b>Manejar eventos de éxito y fracaso en la autenticación </b>

Crear un paquete events en el server-oauth que se encargará de manejar estos eventos.
Crear una clase que implemente la interfaz <b>AuthenticationEventPublisher </b> que implementará dos métodos.
Uno para cuando el login sea correcto y otro para cuando sea erróneo:

<pre><code>
@Component
public class AuthenticationSuccessErrorHandler implements AuthenticationEventPublisher {

	private Logger logger = LoggerFactory.getLogger(AuthenticationSuccessErrorHandler.class);
	
	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {
		UserDetails user = (UserDetails) authentication.getPrincipal();
		
		String successMessage = "Success Login: "+ user.getUsername();
		System.out.println(successMessage);
		logger.info(successMessage);
	}

	@Override
	public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
		String errorMessage = "Login Error: "+exception.getMessage();
		System.out.println(errorMessage);
		logger.info(errorMessage);
	}

}
</code></pre>

- <b>Implementar evento 3 intentos login</b>

Primero, agregar a la entity User el parámetro attemps para indicar los intentos de logueo del usuario.
No olvidarse de que cada vez que se actualiza el commons, hay que generar el jar: mvnw clean install.

Agregar en el cliente Feign Usuario el método update

<pre><code>
	@PutMapping("/users/{id}")
	public User update(@RequestBody User user, @PathVariable Long id);
</code></pre>

Si falla el login, incrementar el contador y actualizar usuario. Si supera 3 intentos, deshabilita el usuario.
<pre><code>
	@Override
	public void publishAuthenticationFailure(AuthenticationException exception, Authentication authentication) {
		....
		try {
			User user = this.userService.findByUsername(authentication.getName());
			if (user.getAttempts() == null) {
				user.setAttempts(0);
			}
			user.setAttempts(user.getAttempts()+1);
			
			
			if(user.getAttempts()>=3) {
				user.setEnabled(false);
			}
			
			this.userService.update(user, user.getId());
			
		} catch (FeignException e) {
			logger.error(String.format("User %s does not exist", authentication.getName()));
		}
	}
</code></pre>
Si acierta el login, se reinicia el contador:
<pre><code>
	@Override
	public void publishAuthenticationSuccess(Authentication authentication) {
		
		.....
		
		User loginUser = this.userService.findByUsername(authentication.getName());
		if(loginUser.getAttempts() != null && loginUser.getAttempts() > 0) {
			loginUser.setAttempts(0);
			this.userService.update(loginUser, loginUser.getId());
		}
	}
</code></pre>

## Spring Cloud Security con Spring Cloud Gateway <a name="id12"></a>

### Configurando el servidor Spring Cloud Gateway
-Agregar 3 nuevas dependencias al microservicio gateway-server: Spring Security, Cloud Bootstrap y Config Client.
Actualizar el Cloud bootstrap añadiendo el sufijo bootstrap:
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bootstrap</artifactId>
		</dependency>

En el properties de gateway-server, añadir dos nuevas entradas url. Una a la seguridad y otra a los usuarios:

<pre><code>
      - id: oauth-server
        uri: lb://oauth-server
        predicates:
          - Path=/api/security/**
        filters:
          - StripPrefix=2
      - id: users-service
        uri: lb://users-service
        predicates:
          - Path=/api/users/**
        filters:
          - StripPrefix=2
</code></pre>

Segundo, agregar para habilitar el servidor de configuración. Esto es porque a partir de la 2.4>= requiere poner esto
para tener el servidor de configuración.

spring:
  config:
    import: 'optional:configserver:'
	
Tercero, al tener un servidor de configuración, agregar el archivo bootstrap.properties:

<pre><code>
spring.application.name=gateway-service-server
spring.cloud.config.uri=http://localhost:8888
</code></pre>

Por último, agregar la dependencia JJWT: https://github.com/jwtk/jjwt en el pom.xml del api-gateway
<b>Si da algún tipo de error con la versión de Java >=11, agregar la dependencia JAXB</b>
		<dependency>
			<groupId>org.glassfish.jaxb</groupId>
			<artifactId>jaxb-runtime</artifactId>
		</dependency>
Es una librería de JWT para Java.


<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.11.5</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.11.5</version>
    <scope>runtime</scope>
</dependency>

### Implementar la clase Security Config


Crear un paquete security y crear una clase SecurityConfig que tenga la anotación @EnableWebFluxSecurity.
Esta clase va a tener un método bean que devolverá un SecurityWebFilterChain que autorizará las rutas:

<pre><code>
@EnableWebFluxSecurity
public class SpringSecurityConfig {
	
	@Bean
	public SecurityWebFilterChain configure(ServerHttpSecurity http) {
		return http.authorizeExchange()
				.pathMatchers("/api/security/oauth/**").permitAll()
				.pathMatchers(HttpMethod.GET,
						"/api/products/",
						"/api/items/",
						"/api/users/users",
						"/api/items/{id}/quantity/{quantity}",
						"/api/products/{id}").permitAll()
				.pathMatchers(HttpMethod.GET,"/api/users/users/{id}").hasAnyRole("ADMIN","USER")
				.pathMatchers("/api/products/**","/api/items/**","/api/users/users/**").hasRole("ADMIN")
				.anyExchange()
				.authenticated()
				.and()
				.csrf().disable()
				.build();
		
	}
}

</code></pre>

### [Extra] Introducción a la programación reactiva Web Flux

La programación reactiva está orientada a flujo de datos similar a las listas y arreglos pero de manera asíncrona y con programación funcional usando expresiones
lambda, nos permite mediante operadores transformando este flujo hasta un resultado final.

Características:
- Inmutable
- Asíncrono
- Cancelable
- Orientado a evento

Tipos:
- Mono [0..1] un solo elemento
- Flux [0..N] varios elementos

### Componente Authentication Manager Reactive

Para que sea más robusta la secret key, es mejor encriptarlo en base 64 en el método accesTokenConverter auth-server y zuul-server
tokenConverter.setSigningKey(Base64.getEncoder().encodeToString(this.env.getProperty("config.security.oauth.jwt.key").getBytes()));

Se crea el componente Authentication Manager Reactive que implementará la interfaz <b>ReactiveAuthenticationManager</b>

<pre><code>
@Component
public class AuthenticationManagerJwt implements ReactiveAuthenticationManager {
	
	@Value("{config.security.oauth.jwt.key}")
	private String keyJwt;

	@Override
	@SuppressWarnings("unchecked")
	public Mono<Authentication> authenticate(Authentication authentication) {
		//just lo que hace es convertir un objeto normal en uno reactivo
		// en este caso getCredentials() devolverá el token y se convertira en un objeto reactivo
		return Mono.just(authentication.getCredentials().toString())
				.map(token ->{
					SecretKey key = Keys.hmacShaKeyFor(Base64.getEncoder().encode(keyJwt.getBytes()));
					return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
				})
				.map(claims -> {
					String username = claims.get("user_name", String.class);	
					List<String> roles = claims.get("authorities", List.class);
					Collection<GrantedAuthority> authorities = roles.stream().map(SimpleGrantedAuthority::new)
							.collect(Collectors.toList());
					return new UsernamePasswordAuthenticationToken(username, null, authorities);
					
				});
	}
}
</code></pre>

Además es necesario crear la clase JwtAuthenticationFilter que implementa la interfaz WebFilter
<pre><code>
@Component
public class JwtAuthenticationFilter implements WebFilter {
	
	@Autowired
	private ReactiveAuthenticationManager authenticationManager;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
				.filter(authHeader -> authHeader.startsWith("Bearer "))
				.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
				.map(token -> token.replace("Bearer ", ""))
				.flatMap(token ->authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(null, token)))
				.flatMap(authentication -> chain.filter(exchange).contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication)));
	}

}
</code></pre>

### Registrar filtro JwtAuthenticationFilter en SpringSecurity

Se añade la línea en SecurityWebFilterChain.
.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)


<pre><code>
@EnableWebFluxSecurity // Habilitar la seguridad en Web Flux. Es una clase que no implementa nada
//Con esta anotación, se va a tener una anotación Bean para configurar las rutas de seguridad.
public class SpringSecurityConfig {
	
	@Autowired
	private JwtAuthenticationFilter authenticationFilter;
	
	@Bean
	public SecurityWebFilterChain configure(ServerHttpSecurity http) {
		return http.authorizeExchange()
				.pathMatchers("/api/security/oauth/**").permitAll()
				.pathMatchers(HttpMethod.GET,
						"/api/products/",
						"/api/items/",
						"/api/users/users",
						"/api/items/{id}/quantity/{quantity}",
						"/api/products/{id}").permitAll()
				.pathMatchers(HttpMethod.GET,"/api/users/users/{id}").hasAnyRole("ADMIN","USER")
				.pathMatchers("/api/products/**","/api/items/**","/api/users/users/**").hasRole("ADMIN")
				.anyExchange()
				.authenticated()
				.and()
				.addFilterAt(authenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
				.csrf().disable()
				.build();
		
	}

}
</code></pre>


## Migrar base de datos MySQL <a name="id13"></a>

1. Instalación de MySQL community: https://dev.mysql.com/downloads/windows/installer/8.0.html
2. Crear esquema de base de datos en MySQL workbench
3. Agregar la dependencia MySQL Driver.
4. Modificar el application.properties para vincular mysql.

Para buscar la zona horaria: https://en.wikipedia.org/wiki/List_of_tz_database_time_zones
<pre><code>
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/db_springboot_cloud?serverTimezone=Europe/Madrid
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=create

# Debugear Hibernate
logging.level.org.hibernate.SQL= debug
</code></pre>

5. Agregar la dependencia Spring Cloud Starter bootstrap
<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>

6. Añadir la configuración al servidor de configuración.

- Crear en el repositorio de configuración un nuevo archivo: products-service-dev.properties que tendrá lo del punto 4.
No olvidar hacer el commit del repositorio.

- Agregar la dependencia Config Client. No olvidarse de añadir spring.config.import=configserver: al properties de products.

- Crear un bootstrap.properties en products-service.

<dependency>
spring.application.name=products-service
spring.cloud.config.uri=http://localhost:8888
spring.profiles.active=dev
</dependency>

## Crear base de datos PostgreSQL <a name="id14"></a>

MySQL es una base de datos puramente relacional, mientras que PostgreSQL es una base de datos relacional 
de objetos. Esto significa que PostgreSQL ofrece tipos de datos más sofisticados y permite que los objetos 
hereden propiedades. Por otro lado, también hace que sea más complejo trabajar con PostgreSQL.
El usuario por defecto es postgres y en MySQL es root.

- Instalación aqui: https://www.enterprisedb.com/downloads/postgres-postgresql-downloads
- Abrir pgAdmin
- Crear base de datos en PostgreSQL
- Inyectar la dependencia PostgreSQL Driver en el microservicio.
- Modificar el import porque hay algunos cambios respecto a MySQL. Quitar las comillas del nombre de las tablas. En los booleanos en PostgreSQL es true o false y en MySQL es con números.
- Añadir la configuración en el servidor de configuración. Primero, inyectar en el microservicio la dependencia
<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bootstrap</artifactId>
</dependency>
Añadir también la dependencia Spring Config Client.
Después, agregar en el properties: spring.config.import=configserver:

Crear el archivo users-service-dev.properties en el servidor de configuración que tendrá la configuración PostgreSQL:
Crear el archivo bootstrap.properties en el microservicio:

<pre><code>
spring.application.name=users-service
spring.cloud.config.uri=http://localhost:8888
spring.profiles.active=dev
</code></pre>

## Trazabilidad distribuida con Spring Cloud Seuth y Zipkin <a name="id15"></a>

<b>1. Spring Cloud Sleuth</b> es una dependencia que nos provee una solución de trazado distribuido para Spring Cloud.
Permite identificar la petición completa de un microservicio, como un todo, y en cada llamada individual a otros microservicios.
Si un microservicio falla, se podría utilizar esta traza para ver más rapido el problema.

<b>TraceId: </b>identificador asociado a la petición que viaja entre los microservicios
<b>SpanId: </b>identificador de la unidad de trabajo de cada llamada a un microservicios.
Entonces una traza está formado por un conjunto de spam.
Ej: INFO[auth-server,8ad45a1c4d583805,f85ds3a2d6a2f5,false]
INFO[Microservicio,traceId,spanId,parametro exportacion a Zipkin]

<b>Atributos Annotation: </b> mide los tiempos de entrada y salida de cada petición, latencia y salud de los servicios:
- cs(Client Sent): el cliente inicia una petición
- sr(Server Received): El servidor recibe y procesa la petición: latencia = tiempo_sr - tiempo_cs
- ss(Server Sent): La respuesta es enviada al servicio cliente: tºprocesamiento peticion = tiempo_ss - tiempo_sr
- cr(Client Received): El cliente recibe la respuesta del servidor: tºtotal traza = tiempo_cr - tiempo_cs


<b>2. Servidor Zipkin</b>
- Servidor para guardar las trazas y monitorización
- Integra las funcionalidades de Spring Cloud Sleuth
- Interfaz gráfica para visualizar el árbol de llamada de cada traza
- Su objeto es consultar la salud del ecosistema

<b>Implementando Spring Cloud Sleuth</b>
- Agregar la dependencia Sleuth a los microservicios que se quiera obtener las trazas.
- Ahora por ejemplo, si se hace una petición en POSTMAN del Login, la trazabilidad muestra lo siguiente:

INFO [zuul-service-server,663577dc1c01997c,663577dc1c01997c,true] POST request routed to http://localhost:8090/api/security/oauth/token 
WARN [zuul-service-server,663577dc1c01997c,663577dc1c01997c,true] The Hystrix timeout of 10000ms for the command oauth-server is set lower than the combination of the Ribbon read and connect timeout, 126000ms.
INFO [zuul-service-server,663577dc1c01997c,663577dc1c01997c,true] Enter to POST
INFO [zuul-service-server,663577dc1c01997c,663577dc1c01997c,true] Time elapsed: 0.273 seconds.
INFO [zuul-service-server,663577dc1c01997c,663577dc1c01997c,true] Time elapsed: 0.273 ms.

Esta petición tiene el traceId= 663577dc1c01997c y en los demás microservicios en la consola tendrá este traceId y el spainId de la tarea que estén ejecutando como acceso a base de datos.

<b>Obteniendo y despleando Zipkin Server y Zipkin UI</b>
- Instalandolo: https://zipkin.io/pages/quickstart.html en Quickstart - Java - latest release
- Ejecutarlo: java -jar java -jar zipkin-server-2.23.19-exec.jar
- Acceder: http://localhost:9411/zipkin/
- Conectar los microservicios con zipkin. Añadir la dependencia Zipkin Client
- En cada microservicio, añadir la configuración Sleuth y Zipkin en el properties:

<pre><code>
spring:
  application:
    name: items-service
  # Configuración sleuth y zipkin
  sleuth:
    sampler:
      probability: 1.0 # 100% que la envie siempre
  zipkin:
    base-url: http://localhost:9411/ #opcional porque por defecto es esta ruta
</code></pre>

<pre><code>
# Configuracion Sleuth/Zipkin
spring.sleuth.sampler.probability=1.0 #Para establecer que siempre muestre la traza 100%
</code></pre>

Al hacer una petición en postman, se pueden visualizar las trazas en Zipkin. 
En el buscador, se puede buscar la ruta mediante http.path=/oauth/token
O buscar por microservicios con serviceName=xxx
o por error: Error=400

- Agregar información a la traza.

Inyectar @Autowired Tracer de Brave y usarlo por ejemplo, en un catch cuando no encuentra al usuario:

<pre><code>
catch (FeignException e) {
				logger.error("Error: username "+username+" not found.");
				this.tracer.currentSpan().tag("error", "Error: username "+username+" not found." + e.getMessage());
				throw new UsernameNotFoundException("Error: username "+username+" not found.");
		}
</code></pre>

#### Instalar Broker Rabbit MQ - Consumidor de Trazas

Para que se envíen las trazas por RabbitMQ.
https://www.rabbitmq.com/#getstarted

- Conectar los microservicios con RabbitMQ. Agregar la dependencia Spring for RabbitMQ.
- Crear un documento cmd para configurar las variables de entorno para que Rabbit pueda consumir las trazas de Zipkin.
zipkin.cmd
<pre><code>
@echo off
set RABBIT_ADDRESSES=localhost:5672
java -jar ./zipkin-server-2.23.19-exec.jar
</code></pre>

- Acceder a Rabbit: localhost:15672 y en connections se comprueba que está conectado a Zipkin
- Añadir la configuración en el properties de los microservicios que se desea obtener sus trazas en Rabbit:

<pre><code>
spring.zipkin.sender.type=rabbit
</code></pre>

y asegurarse de que cada microservicio tenga las dependencias:

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-amqp</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-sleuth-zipkin</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-sleuth</artifactId>
		</dependency>
		
#### Configurando MySQL Storage en Zipkin Server

- Acceder a https://github.com/openzipkin/zipkin/tree/master/zipkin-server#mysql-storage
- Editar zipkin.cmd poniendo la nueva variable

<pre><code>
@echo off
set RABBIT_ADDRESSES=localhost:5672
set STORAGE_TYPE=mysql
set MYSQL_USER=zipkin
set MYSQL_PASS=zipkin
java -jar ./zipkin-server-2.23.19-exec.jar
</code></pre>

-Crear una nueva base de datos en MySQL Workbench charset=utf8 y utf8 bin
-Ir a user y privilegios y añadir el Login Name=zipkin, Standard, localhost, y las contraseñas. 
Luego ir a la pestaña Schema Privilegies-Add Entry y seleccionar la base de datos Zipkin con privilegios de DELETE,EXECUTE,INSERT,SELECT,SHOW,VIEW,UPDATE
-Con use zipkin se usa la base de datos zipkin creada.
-Crear las tablas usando use zipkin arriba del todo: https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/mysql-v1/src/main/resources/mysql.sql
-Ir a Administración-Usuarios y Privilegios-Elegir usuario Zipkin y añadirle todos los privilegios SELECT,UPDATE,DELETE,etc.

## Desplegando microservicios en Docker<a name="id16"></a>

### Creando un DockerFile para el microservicio de configuración.

1. Acceder al microservicio:

<pre><code>
cd ..../server-config
</code></pre>

2. Generar el jar

<pre><code>
.\mvnw clean package
</code></pre>

3. Generar el DockerFile
Crear un archivo File llamado DockerFile dentro del paquete raíz del microservicio que tenga lo siguiente:

<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 8888
ADD ./target/config-server-0.0.1-SNAPSHOT.jar config-server.jar
ENTRYPOINT ["java","-jar","config-server.jar"]
</code></pre>

FROM debe seleccionar una imagen de DockerHub: https://hub.docker.com/_/openjdk
EXPOSE debe seleccionar el puerto en el que se va a ejecutar
ADD debe seleccionar el archivo que se quiere ejecutar
ENTRYPOINT debe seleccionar el comando a ejecutar para el archivo del ADD

4. Construyendo la imagen Docker

Ir con el cmd al archivo raiz del microservicio y ejecutar el comando <b>docker build -t config-server:v1 .</b>
Con la -t se añade una tag como la version 1 (v1) y el espacio y punto final es importante añadirlo. 
Con docker images se puede comprobar si la imagen se ha generado bien. Debería aparecer config-server y openjdk ejecutándose.

5. Crear el contenedor de la imagen config-server
El contenedor es una instancia de la imagen.
Es necesario crear una red al contenedor con el comando: <b>docker network create springcloud</b>
Con el comando <b>docker run -p 8888:8888 --name config-server --network springcloud config-server:v1</b> (primer numero para acceder desde el local y el otro numero es el puerto interno, el que viene en el properties)

6. Ver los contenedores publicados:
Con el comando <b>docker container ls</b> o <b>docker ps -a</b>

### Creando un DockerFile para el microservicio de Eureka

Acceder a la ruta del microservicio con la consola.

1. Generar el jar con .\mvnw clean package

2. El archivo DockerFile para el microservicio eureka-server sería el siguiente:
<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 8761
ADD ./target/eureka-server-0.0.1-SNAPSHOT.jar eureka-server.jar
ENTRYPOINT ["java","-jar","eureka-server.jar"]
</code></pre>

3. Construir la imagen Docker
Acceder al directorio raíz del microservicio y con el comando <b>docker build -t eureka-server:v1 .</b>

4. Crear el contenedor de esta imagen

Con el comando <b>docker run -p 8761:8761 --name eureka-server --network springcloud eureka-server:v1</b>
Con el comando <b>docker logs -f <id container></b>

### Instalar imagen MySQL con Docker

1. Ir a DockerHub: https://hub.docker.com/_/mysql y ejecutar el comando <b>docker pull mysql:8</b>
2. Crear el contenedor de la imagen: <b>docker run -p 3306:3306 --name mysql8 -network springcloud -e MYSQL_ROOT-PASSWORD=1234 -e MYSQL_DATABASE=db:springboot_cloud -d mysql:8</b>

-d para que se ejecute en background
Con -e se añaden las variables de entorno del contenedor. En este caso se utiliza la base de datos con nombre db:springboot_cloud y el usuario root con contraseña 1234
Ahora en MySQL Workbench se puede acceder a esta conexión yendo a: New Connection:
- Connection Name: docker mysql
- Hostname: localhost port 3306
- Username: root

Y al connectarse se puede apreciar que se crea la base de datos db_springboot_cloud

### Instalar imagen PostgreSQL con Docker

1. Ir a DockerHub: https://hub.docker.com/_/postgres y ejecutar el comando <b>docker pull postgres:12-alpine</b>
2. Crear el contenedor de la imagen: <b>docker run -p 5432:5432 --name postgres12 -network springcloud -e POSTGRES-PASSWORD=1234 -e POSTGRES_DB=db:springboot_cloud -d postgres:12-alpine</b>

### Cambiar el repositorio de configuración para habilitar la base de datos con Docker

- Primero ir al repositorio git de configuración y editar items-service.properties y products-service.properties
- Con el comando docker ps se pueden ver los contenedores ejecutándose y fijarse en la columna name de los contenedores porque 
hay que modificar la línea spring.datasource.url cambiando localhost por el nombre del contenedor.

Para MySQL: products-service-dev.properties

<pre><code>
spring.datasource.url=jdbc:mysql://mysql8:3306/db_springboot_cloud?serverTimezone=Europe/Madrid
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=create

# Debugear Hibernate
logging.level.org.hibernate.SQL= debug
</code></pre>

Para PostgreSQL: users-service-dev.properties

<pre><code>
# MySQL Configuration
spring.datasource.url=jdbc:postgresql://postgres12:5432/db_springboot_cloud
spring.datasource.username=postgres
spring.datasource.password=1234
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQL95Dialect
spring.jpa.hibernate.ddl-auto=create
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true

# Debugear Hibernate
logging.level.org.hibernate.SQL= debug
</code></pre>

Por último, hacer el commit del repositorio y el push.

### Configurando URL de Eureka y Server Config en microservicios

Ejecutar docker ps para ver los contenedores levantados y ver la columna name para el servicio de eureka.
Ir al application.properties de products-service y cambiar la url de eureka cambiando el localhost por el nombre del contenedor:

<pre><code>
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

Cambia por 

eureka.client.service-url.defaultZone = http://eureka-server:8761/eureka
</code></pre>

Hacer lo mismo en cada microservicio.

Como ahora hay varias imagenes e instancias es recomendable descomentar las líneas de configuración de Hystrix para items-service y zuul-server

<pre><code>
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 60000
ribbon.ConnectTimeout: 9000
ribbon.ReadTimeout: 30000
</code></pre>

Ahora es necesario cambiar el bootstrap.properties para cambiar el localhost por el nombre del contenedor del servidor de configuración:
<pre><code>
spring.application.name=users-service
spring.cloud.config.uri=http://config-server:8888
spring.profiles.active=dev
</code></pre>

### Crear DockerFile para products-service

Acceder a la ruta del microservicio con la consola.

1. Crear el jar con .\mvnw clean package -DskipTests
Se aplica el -DskipTests para evitar que se conecte a mysql

2. Crear el DockerFile

<pre><code>
FROM openjdk:11
VOLUME /tmp
ADD ./target/products-service-0.0.1-SNAPSHOT.jar products-service.jar
ENTRYPOINT ["java","-jar","products-service.jar"]
</code></pre>

3. Construir la imagen Docker

Con el comando <b>docker build -t products-service:v1 .</b>

4. Crear el contenedor de esta imagen

Como el puerto es aleatorio, se coloca -P y sin valor

Con el comando <b>docker run -P --name products-service --network springcloud products-service:v1</b>

### Crear DockerFile para zuul-server

Acceder a la ruta del microservicio con la consola.

1. Crear el jar con .\mvnw clean package -DskipTests
Se aplica el -DskipTests para evitar que se conecte a eureka y al servidor de configuración

2. Crear el DockerFile

<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 8090
ADD ./target/zuul-server-0.0.1-SNAPSHOT.jar zuul-server.jar
ENTRYPOINT ["java","-jar","zuul-server.jar"]
</code></pre>

3. Construir la imagen Docker
Acceder al directorio raíz del microservicio y con el comando <b>docker build -t zuul-server:v1 .</b>

4. Crear el contenedor de esta imagen

Como el puerto es aleatorio, se coloca -P y sin valor

Con el comando <b>docker run -p 8090:8090 --name products-service --network springcloud products-service:v1</b>

### Crear DockerFile para users-service

Acceder a la ruta del microservicio con la consola.

1. Crear el jar con .\mvnw clean package -DskipTests
Se aplica el -DskipTests para evitar que se conecte a mysql

2. Crear el DockerFile

<pre><code>
FROM openjdk:11
VOLUME /tmp
ADD ./target/users-service-0.0.1-SNAPSHOT.jar users-service.jar
ENTRYPOINT ["java","-jar","users-service.jar"]
</code></pre>

3. Construir la imagen Docker

Con el comando <b>docker build -t users-service:v1 .</b>

4. Crear el contenedor de esta imagen

Como el puerto es aleatorio, se coloca -P y sin valor

Con el comando <b>docker run -P --name users-service --network springcloud users-service:v1</b>

### Crear DockerFile para oauth-server

Acceder a la ruta del microservicio con la consola.

1. Crear el jar con .\mvnw clean package -DskipTests
Se aplica el -DskipTests para evitar que se conecte a mysql

2. Crear el DockerFile

<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 9100
ADD ./target/oauth-server-0.0.1-SNAPSHOT.jar oauth-server.jar
ENTRYPOINT ["java","-jar","eureka-server.jar"]
</code></pre>

3. Construir la imagen Docker

Con el comando <b>docker build -t oauth-server:v1 .</b>

4. Crear el contenedor de esta imagen

Con el comando <b>docker run -p --name oauth-server --network springcloud oauth-server:v1</b>

### Crear DockerFile para items-service

Acceder a la ruta del microservicio con la consola.

1. Crear el jar con .\mvnw clean package -DskipTests
Se aplica el -DskipTests para evitar que se conecte a mysql

2. Crear el DockerFile

<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 8002
ADD ./target/items-service-0.0.1-SNAPSHOT.jar items-service.jar
ENTRYPOINT ["java","-jar","items-service.jar"]
</code></pre>

3. Construir la imagen Docker

Con el comando <b>docker build -t items-service:v1 .</b>

4. Crear el contenedor de esta imagen

Con el comando <b>docker run -p --name items-service --network springcloud items-service:v1</b>


### Crear varias instancias para products-service con Docker

Con el comando <b>docker run -P --name products-service2 --network springcloud products-service:v1</b>
Se va a levantar otra instancia de productos.
Con docker ps se puede apreciar que habrá dos instancias
Para borrar la instancia: <b>docker rm <id container></b>

### Crear una imagen para RabbitMQ para Rabbit

- Ir al DockerHub: https://hub.docker.com/_/rabbitmq
- Ejecutar el comando <b>docker pull rabbitmq:3.8-management-alpine</b>
- Comprobar que esté instalada con docker images
- Crear un contenedor para esta imagen con el comando:
<pre><code>
docker run -p 15672:15672 -p 5672:5672 --name rabbitmq38 --network springcloud -d rabbitmq:3.8-management-alpine
</code></pre>

### Configurando esquema DDL de zipkin para mysql

1. Conectarse a MySQL Workbench a la conexión de Docker
2. Crear una base de datos para zipkin llamada "zipkin" con configuración utf8 y utf8_general_ci
3. Crear un usuario en Administración - Usuarios y Privilegios - Agregar una cuenta - Login Name: zipkin y Password: zipkin
4. Crear las tablas usando use zipkin arriba del todo: https://github.com/openzipkin/zipkin/blob/master/zipkin-storage/mysql-v1/src/main/resources/mysql.sql
5. Ir a Administración-Usuarios y Privilegios-Elegir usuario Zipkin y añadirle todos los privilegios SELECT,UPDATE,DELETE,etc.

### Crear un DockerFile para zipkin

1. Crear una carpeta en el workspace del proyecto llamado zipkin-server en el que se va a añadir el jar zipkin.
2. Crear el archivo DockerFile en esta carpeta.

<pre><code>
FROM openjdk:11
VOLUME /tmp
EXPOSE 9411
ADD ./zipkin-server-2.23.19-exec.jar zipkin-server.jar
ENTRYPOINT ["java","-jar","zipkin-server.jar"]
</code></pre>

3. Crear la imagen Zipkin.

Ir al terminal, al directorio zipkin-server y ejecutar:
<pre><code>
docker build -t zipkin-server:v1
</code></pre>

4. Construir el contenedor

Teniendo el archivo zipkin.cmd:
<pre><code>
@echo off
set RABBIT_ADDRESSES=localhost:5672
set STORAGE_TYPE=mysql
set MYSQL_USER=zipkin
set MYSQL_PASS=zipkin
java -jar ./zipkin-server-2.23.19-exec.jar
</code></pre>

Se construye el contenedor con estas variables de entorno pero sustituyendo localhost por el nombre de los contenedores.
<pre><code>
docker run -p 9411:9411 --name zipkin-server --network springcloud -e RABBIT_ADDRESSES=rabbitmq38:5672 -e STORAGE_TYPE=mysql -e  MYSQL_USER=zipkin -e set MYSQL_PASS=zipkin zipkin-server:v1
</code></pre>

### Configurar nombre host de RabbitMQ

La configuración de Zipkin en los properties de los microservicios se necesita cambiar el localhost por el nombre del contenedor.

<pre><code>
  zipkin:
    base-url: http://zipkin-server:9411/ #Para docker
  # Para Docker
  rabbitmq: 
    host: rabbitmq38
</code></pre>

<pre><code>
spring.zipkin.base-url=http://zipkin-server:9411/
spring.rabbitmq.host=rabbitmq38
</code></pre>

Pero para no tener que volver a generar los jar para cada microservicio se va a crear un nuevo archivo en el repositorio de configuración
Editando el application.properties:

<pre><code>
config.security.oauth.client.id=postmanapp
config.security.oauth.client.secret=12345
config.security.oauth.jwt.key=2A281F235A1A61369C76AE1DAFA3A

spring.zipkin.base-url=http://zipkin-server:9411/
spring.rabbitmq.host=rabbitmq38
</code></pre>

No olvidarse de commit y push.

Con docker restar <id contenedor> se reiniciaran los contenedores de los microservicios para que adquieran esta nueva configuración.

### Despliegue de contenedores con docker-compose

En vez de ejecutar el docker-run se puede crear este archivo para ejecutar los contenedores.

1. Ir al directorio raíz de la aplicación y crear un nuevo directorio llamado docker-compose
2. Crear un archivo docker-compose.yml con el siguiente contenido:

<pre><code>
version: '3.7'
services:
  config-server:
    image: config-server:v1
    ports:
      - "8888:8888"
    restart: always
    networks:
      - springcloud
  eureka-server:
    image: eureka-server:v1
    ports:
      - "8761:8761"
    restart: always
    networks:
      - springcloud
  mysql8:
    image: mysql:8
    ports:
      - "3306:3306"
    restart: always
    networks:
      - springcloud
    environment: 
      MYSQL_DATABASE: db_springboot_cloud
      MYSQL_ROOT_PASSWORD: 1234
  postgres12:
    image: postgres:12-alpine
    ports:
      - "5432:5432"
    restart: always
    networks:
      - springcloud
    environment: 
      POSTGRES_DB: db_springboot_cloud
      POSTGRES_PASSWORD: 1234
  products-service:
    image: products-service:v1
    restart: always
    networks:
      - springcloud
    depends_on: 
      - config-server
      - eureka-server
      - mysql8
  items-service:
    image: items-service:v1
    ports:
      - "8002:8002"
      - "8005:8005"
      - "8007:8007"
    restart: always
    networks:
      - springcloud
    depends_on: 
      - config-server
      - eureka-server
      - products-service
   users-service:
    image: users-service:v1
    restart: always
    networks:
      - springcloud
    depends_on: 
      - config-server
      - eureka-server
      - postgres12
   oauth-server:
    image: oauth-server:v1
    ports:
      - "9100:9100"
    restart: always
    networks:
      - springcloud
    depends_on: 
      - config-server
      - eureka-server
      - users-service
  zuul-server:
    image: zuul-server:v1
    ports:
      - "8090:8090"
    restart: always
    networks:
      - springcloud
    depends_on: 
      - config-server
      - eureka-server
      - products-service
      - items-service
      - users-service
      - oauth-server
  rabbitmq38:
    image: rabbitmq:3.8-management-alpine
    ports:
      - "15672:15672"
      - "5672:5672"
    restart: always
    networks:
      - springcloud
  zipkin-server:
    image: zipkin-server:v1
    ports:
      - "9411:9411"
    restart: always
    networks:
      - springcloud
    depends_on: 
      - rabbitmq38
      - mysql8
    environment: 
      RABBIT_ADDRESSES: microservicios-rabbitmq38:5672
      STORAGE_TYPE: mysql
      MYSQL_USER: zipkin
      MYSQL_PASS: zipkin
      MYSQL_HOST: mysql8
networks:
  springcloud:
</code></pre>

3. Ir al directorio docker-compose y levantar con el comando docker-compose up


### LISTA DE COMANDOS DOCKER

<pre><code>
======================== config-server

.\mvnw clean package
 
docker build -t config-server:v1 .
docker network create spring-microservicios
docker run -p 8888:8888 --name config-server --network springcloud config-server:v1


======================== servicio-eureka-server

.\mvnw clean package
 
docker build -t servicio-eureka-server:v1 .
docker run -p 8761:8761 --name servicio-eureka-server --network springcloud servicio-eureka-server:v1
======================== mysql

docker pull mysql:8
docker run -p 3306:3306 --name microservicios-mysql8 --network springcloud -e MYSQL_ROOT_PASSWORD=sasa -e MYSQL_DATABASE=db_springboot_cloud -d mysql:8
docker logs -f microservicios-mysql8


======================== postgresql

docker pull postgres:12-alpine
docker run -p 5432:5432 --name microservicios-postgres12 --network springcloud -e POSTGRES_PASSWORD=sasa -e POSTGRES_DB=db_springboot_cloud -d postgres:12-alpine
docker logs -f microservicios-postgres12


======================== springboot-servicio-productos

.\mvnw clean package -DskipTests
 
docker build -t servicio-productos:v1 .
docker run -P --network springcloud servicio-productos:v1


======================== springboot-servicio-zuul-server

.\mvnw clean package -DskipTests
 
docker build -t servicio-zuul-server:v1 .
docker run -p 8090:8090 --network springcloud servicio-zuul-server:v1


======================== springboot-servicio-usuarios

.\mvnw clean package -DskipTests
 
docker build -t servicio-usuarios:v1 .
docker run -P --network springcloud servicio-usuarios:v1


======================== springboot-servicio-oauth

.\mvnw clean package -DskipTests
 
docker build -t servicio-oauth:v1 .
docker run -p 9100:9100 --network springcloud servicio-oauth:v1


======================== springboot-servicio-item

.\mvnw clean package -DskipTests
 
docker build -t servicio-items:v1 .
docker run -p 8002:8002 -p 8005:8005 -p 8007:8007 --network springcloud servicio-items:v1


======================== rabbitmq

docker pull rabbitmq:3.8-management-alpine
docker run -p 15672:15672 -p 5672:5672 --name microservicios-rabbitmq38 --network springcloud -d rabbitmq:3.8-management-alpine
 
docker logs -f microservicios-rabbitmq38


======================== zipkin

docker build -t zipkin-server:v1 .
docker run -p 9411:9411 --name zipkin-server --network springcloud -e RABBIT_ADDRESSES=microservicios-rabbitmq38:5672 -e STORAGE_TYPE=mysql -e MYSQL_USER=zipkin -e MYSQL_PASS=zipkin -e MYSQL_HOST=microservicios-mysql8 zipkin-server:v1
docker logs -f zipkin-server


======================== Otros comandos

detener y eliminar todos los contenedores:

docker stop $(docker ps -q)
docker rm $(docker ps -a -q)


eliminar todas las imagenes:

docker rmi $(docker images -a -q)

</code></pre>