# MicroservicesAppCloud - Marcos Ruiz Muñoz 
Formación en microservicios con Spring Cloud

## Balanceo de carga con Ribbon

Ejemplo de balanceo de carga.
Se crean dos instancias del microservicio productos (uno en el puerto 8001 y otro en el 9001)
Servicio items (puerto 8002) se conecte a uno de los servicios activos de productos y va a ser Ribbon
quien elija la mejor instancia disponible de productos.

Para añadirlo a Spring, a partir de Spring 2.4 > no es compatible con Ribbon por lo que para usarlo, es necesario
modificar el pom de items para utilizar la versión 2.3.0.RELEASE (por ejemplo) y además, es necesario cambiar 
la versión de spring-cloud a Hoxton.SR12.
<pre>
<version>2.7.4</version> --> <version>2.3.0.RELEASE</version> 

<spring-cloud.version>2021.0.4</spring-cloud.version> -->
<spring-cloud.version>Hoxton.SR12</spring-cloud.version>
</pre>

- Configuración de Ribbon en el properties:
<pre>
products-service.ribbon.listOfServers = localhost:8001,localhost:9001
</pre>

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
eureka.client.register-with-eureka=false
eureka.client.fetch-registry=false
</pre>
Es necesario agregar la dependencia JAXB en el pom.xml si se usa Java >=11 sino no es necesario. 
En la documentación JDK 11 Support: https://cloud.spring.io/spring-cloud-netflix/multi/multi_spring-cloud-eureka-server.html

<pre>
		<dependency>
			<groupId>org.glassfish.jaxb</groupId>
			<artifactId>jaxb-runtime</artifactId>
		</dependency>
</pre>

Con esto ya se ha creado un servidor eureka que manejará a los microservicios (products e items).
Para ir al panel de Eureka, acceder a la ruta asignada en el puerto 8761 (indicado en properties)

### Configurar los microservicios para que Eureka los use

- click derecho al microservicio - spring - edit starters - añadir la dependencia Eureka Discovery
(Cada vez que se queria registrar un cliente en el server de Eureka, es necesario que tenga la dependecia Eureka Discovery)
- Aunque no es necesario, poner la anotación @EnableEurekaClient en el application run class
- Poner en el properties de cada microservicio el servidor de eureka al que se va a conectar. Esto no es necesario si están en la misma máquina, pero por si acaso, ponerlo.

<pre>
eureka.client.service-url.defaultZone = http://localhost:8761/eureka
</pre>

- Quitar las dependencias Ribbon del pom.xml porque Eureka ya tiene esta dependencia implícita. Además, es necesario
eliminar la anotación @RibbonClient(name = "products-service") del app run class. Feign es necesario ya que se necesita como cliente para conectarse a las apis.

- Runear primero el eureka client y luego los demás microservicios.

Al acceder al panel de Eureka, se podrán ver las instancias de microservicios.


## Escalar microservicios con puerto dinámico
La idea es que Spring de forma automática asigne el puerto de los servicios para hacer la aplicación más escalable. Para ello:
- Modificar en el archivo properties de cada microservicio, el server.port = ${PORT:0}
- Añadir una nueva línea de configuración de Eureka para que se asigne una url dinámica al servicio:
<pre>
eureka.instance.instance-id=${spring.application.name}:${spring.application.instance_id:${random.value}}
</pre>

## Hystrix

### Tolerancia a fallos,excepciones con Hystrix
Por ejemplo, cuando alcanza cierto límite de fallos en peticiones en alguna instancia, ya se deja de hacer
solicitudes a esa instancia.
Además, puede reemplazar a esta instancia que falla por otra.
Al igual que pasa con Ribbon, Hystrix es compatible con Spring <=2.3 con Spring >=2.4 se usa Resillence

- Agregar la dependencia
<pre>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
		</dependency>
</pre>

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

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 60000

ribbon:

  ConnectTimeout: 3000
  
  ReadTimeout: 60000
  
</pre>

Que pasándolo al properties de item:
IMPORTANTE: Asegurarse que el tiempo de respuesta de Hystrix sea mayor que ribbon: 20000>13000

<pre>

hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 20000

ribbon.ConnectTimeout: 3000

ribbon.ReadTimeout: 10000
</pre>

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
	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.3.0.RELEASE</version>
		<relativePath/> <!-- lookup parent from repository -->
	</parent>
	<properties>
		<java.version>11</java.version>
		<spring-cloud.version>Hoxton.SR12</spring-cloud.version>
	</properties>
</pre>
- Añadir la dependencia zuul:
<pre>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-zuul</artifactId>
		</dependency>
</pre>

- Configurar el properties del proyecto. Añadiendo la configuración de cliente Eureka y las rutas zuul:
<pre>
- Configuración Eureka
eureka.client.service-url.defaultZone = http://localhost:8761/eureka

### Configurar las rutas de los microservicios

#### Products
zuul.routes.products.service-id=products-service
zuul.routes.products.path=/api/products/**

#### Items
zuul.routes.items.service-id=items-service
zuul.routes.items.path=/api/items/**
</pre>

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
c.a.z.filters.PostTimeElapsedFilter : Enter to POST
c.a.z.filters.PostTimeElapsedFilter : Time elapsed: 0.548 seconds.
c.a.z.filters.PostTimeElapsedFilter : Time elapsed: 0.548 ms.
</pre>

### Zuul Configurar TimeOuts
Con la configuración Hystrix anterior, en zuul no vale. Es necesario configurar los timeouts en Zuul con las nuevas rutas.
Con la configuración anterior (HystrixCommand + lo del properties) para zuul sigue siendo un Timeout -> error: Gateway Timeout.

Para ello, se copia la configuración de Hystrix y se copia tanto en items-service como en zuul-service (ambos)
<pre>
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds: 10000
ribbon.ConnectTimeout: 3000
ribbon.ReadTimeout: 60000
</pre>

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
 </pre>
 
 Con un archivo properties:
 <pre>
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
 </pre>
 
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
 </pre>
 
 Modificando la respuesta es sencillo pero para modificar la request tiene algunas restricciones.
 Se necesita usar la función mutable para hacer la request modificable y con la función headers se pueden añadir tokens al header.
 <pre>
 		/*MODIFICAR LA REQUEST*/
		exchange.getRequest().mutate().headers(h -> {
			h.add("token", "123456");
		});
 </pre>
 En el POST, se puede modificar esta request que se editó en el PRE:
 <pre>
 			Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("token"))
			.ifPresent(value -> exchange.getResponse().getHeaders().add("token", value));
 </pre>
 
 Para aplicar un orden a las clases que implementan filtros es necesario que la clase @Component GlobalFilterExample implements Order
 e implementar el método Override getOrder().
 
 <pre>
 	@Override
	public int getOrder() {
		return 1;
	}
 </pre>
 
 ### Gateway Filter Factory
 Otra manera de crear filtros mucho más personalizable.
 - Crear un nuevo packete dentro de filters llamado filters.factory.
 - Crear la clase pero no con un nombre cualquiera: <Name>GatewayFilterFactory por ejemplo, 
 @Component ExampleGatewayFilterFactory extends AbstractGatewayFilterFactory<ExampleGatewayFilterFactory.ConfigurationFilter>
 - Como se comprueba, maneja un genérico como clase de configuración que hay que crearlo como clase interna:
 <pre>
 @Component
public class ExampleGatewayFilterFactory extends AbstractGatewayFilterFactory<ExampleGatewayFilterFactory.ConfigurationFilter> {

	public class ConfigurationFilter {

	}
}
 </pre>
 - Implementar el método Override apply(), es decir, aplicar el filtro con la clase de configuración personalizada.
 Es igual que el otro pero mucho más configurable porque las propiedades son dinámicas:
 <pre>
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
 </pre>
 
 - Es mucho más configurables porque en el properties o yml, se puede añadir estos filtros. Por ejemplo,
 si solo se quiere que esto se ejecute para el microservicio products, se pone en el properties del products y en items no.
 Es necesario que en el name se ponga el prefijo que se ha puesto en <name>GatewayFilterFactory y en args, los argumentos 
 de la clase interna ESTÁTICA de configuración. <b>Para que funcione y se vincule correctamente es necesario que la clase interna de configuración sea estática y que implemente los getters & setters (ver clase Configuration)</b>
 <pre>
         filters:
          - StripPrefix=2
          - name: Example
            args:
              message: "Hello! This is a message"
              cookieName: "user"
              cookieValue: "Marcos"
 </pre>
 También se puede poner de forma compacta:
 <pre>
         filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, Marcos
 </pre>
 Pero para este ejemplo último, se necesita añadir el orden de los objetos y el nombre de la clase. Es decir, implementar los métodos override:
 <pre>
 	@Override
	public String name() {
		return "Cookie";
	}

	@Override
	public List<String> shortcutFieldOrder() {
		return Arrays.asList("message", "cookieName", "cookieValue");
	}
 </pre>
 
 ### Filtros de fábrica en Spring Cloud Gateway
 Estos son algunos filtros que se usan de fábrica en Spring Gateway y que se pueden añadir directamente al archivo properties:
 - AddRequestHeader-> para modificar la cabecera o añadir parámetros no existentes de la request. Ej: - AddRequestHeader=token-request, 123456
 - AddResponseHeader-> para modificar la cabecera o añadir parámetros no existentes de la response. Ej:  - AddResponseHeader=token-response, 12345678
 - AddRequestParameter-> Se añade un parámetro a la request. Ej: AddRequestParameter=name, Marcos
 
  Luego estos parámetros se pueden usar en un controller
 
 <pre>
 	@GetMapping
	public ResponseEntity<List<Item>> getAllItems(@RequestParam(name = "name",required = false) String name, @RequestHeader(name = "token-request",required = false) String token) {
		System.out.println("Name: "+name);
		System.out.println("Token: "+token);
		return new ResponseEntity<>(this.itemService.findAll(), HttpStatus.OK);
	}
 </pre>
 
 - Para modificar se usa el prefijo Set y se usa para parámetros ya existentes de las cabeceras. Ej: - SetResponseHeader=Content-Type, text/plain
 - Todos los filtros de fábrica se encuentran en: https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/#gatewayfilter-factories
 
 
 ### Predicates de fábrica en Spring Cloud Gateway
 Los predicates son reglas del request. Por ejemplo, la regla Path que para ejecutarse cierto microservicio, necesita que tenga una ruta específica difinida en Path.
 <pre>
         predicates:
          - Path=/api/products/**
 </pre>
 <b>Pero hay muchos más:</b>
 
 <pre>
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
 </pre>
 
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
	<!-- Para usar Hystrix
	<version>2.3.0.RELEASE</version>-->
	<version>2.7.4</version>
		
	<properties>
		<java.version>11</java.version>
		<!--Para usar Hystrix
		<spring-cloud.version>Hoxton.SR12</spring-cloud.version>-->
		<spring-cloud.version>2021.0.4</spring-cloud.version>
	</properties>
</pre>

Y en la clase principal, quitar el @EnableCircuitBreaker que usaba Hystrix:
<pre>
//@EnableCircuitBreaker // Para usar Hystrix para la tolerancia a fallos y timeouts
</pre>

Ahora solo falta añadir las dependencias.
<pre>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-bootstrap</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
		</dependency>
</pre>

Spring Cloud boostrap no tiene nada que ver con Resilence4J pero
se usará para implementar un archivo de configuración y añadir el parámetro en properties.

<pre>
spring.config.import=optional:configserver:
</pre>

Ahora para cualquier Controller, se puede usar el objeto @Autowired private CircuitBreakerFactory circuitBreakerFactory;
en alguna requestmapping y además a la vez poner el método alternativo.
<pre>
	@GetMapping("/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem(@PathVariable Long id, @PathVariable Integer quantity) {
		/**Probar Resilence4j**/
		return circuitBreakerFactory.create("items").run(() ->new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK),e -> alternativeMethod(id, quantity,e));
	}
</pre>

Con el circuitbreaker y los parámetros por defecto. De 100 peticiones, si por ejemplo se hacen 55 peticiones erroneas a esta URL y 45 peticiones correctas, superará el umbral y entrará en estado cerradao.
Aquí aunque se realicen peticiones correctas, irá al método alternativo. Estára el estado semiabierto realizando con 10 pruebas de límite. Si supera el umbral del 50% de fallos, volverá al estado abierto, sino al cerrado.

### Cambiar parámetros que vienen por defecto del CircuitBreaker de Resillence4J
Existen dos formas, mediante el properties o mediante una clase Bean.

1. Mediante una clase Bean en un @Configuration
	<pre>
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
	</pre>
	
#### Timeouts con Resilence4J
Se puede configurar también en el customizer la propiedad timeLimiterConfig(TimeLimiterConfig.ofDefaults()) pero en vez de que sea por defecto,
personalizarla:
<pre>
					.timeLimiterConfig(TimeLimiterConfig.custom()
							.timeoutDuration(Duration.ofSeconds(6L)) /*6 segundos se demora (por defecto es 1)*/
							.build())
</pre>

#### Llamadas lentas con Resilence4J
Se configura también en el customizer de la propiedad circuitBreakerConfig.
<pre>
		.slowCallRateThreshold(50)//por defecto es 100%
		.slowCallDurationThreshold(Duration.ofSeconds(2L)) //por defecto 60000ms
</pre>
Ahora toda llamada mayor de 2 seg se registra como llamada lenta.
Cabe destacar que primero ocurre el timeout antes que la llamada lenta por lo que el tiempo de la llamada lenta tendra que ser menor.
A diferencia de los timeouts, estas llamadas lentas se van a ejecutar como 200 OK pero se registará como llamada lenta que si se supera el 50% del umbral establecido, entrara en cortocircuito.

2. Modificando el application.properties

Se crea un nombre de configuracion y se le asigna a la instancia creada en el circuitBreakerFactory (return circuitBreakerFactory.create("items").run(()) en este caso para items.

<pre>
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
</pre>

en properties:

<pre>
resilience4j.circuitbreaker.configs.defaultConfigItems.sliding-window-size=6
resilience4j.circuitbreaker.configs.defaultConfigItems.failure-rate-threshold=50
resilience4j.circuitbreaker.configs.defaultConfigItems.wait-duration-in-open-state=20s
resilience4j.circuitbreaker.configs.defaultConfigItems.permitted-number-of-calls-in-half-open-state=4
resilience4j.circuitbreaker.configs.defaultConfigItems.slow-call-rate-threshold=50
resilience4j.circuitbreaker.configs.defaultConfigItems.slow-call-duration-threshold=2s
resilience4j.circuitbreaker.instances.items.base-config=defaultConfigItems
 
resilience4j.timelimiter.configs.defaultConfigItemsTimeout.timeout-duration=2s
resilience4j.timelimiter.instances.items.base-config=defaultConfigItemsTimeout
</pre>

### Anotacion @CircuitBreaker
En vez de usar circuitBreakerFactory se puede usar la anotacion encima del método del controller.

<pre>
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod")
	@GetMapping("/aux/{id}/quantity/{quantity}")
	public ResponseEntity<Item> getItem2(@PathVariable Long id, @PathVariable Integer quantity) {
		return new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK);
	}
</pre>

Esta configuración de "items" tiene que estar en el archivo properties

### Anotacion @TimeLimiter
La funcionalidad es la misma al @CircuitBreaker. Aqui la diferencia es que continua con la ejecución y no hace cortocircuito porque no contabiliza los tiempos ni los estados.
Solo contabiliza los timeouts y en CircuitBreakers se contabilizan las excepciones y llamadas lentas.
Llamada futura asincrona. Cabe destacar que el método alternativo tambien tiene que devolver un CompletableFuture.

<pre>
	@TimeLimiter(name = "items",fallbackMethod = "alternativeMethod2")
	@GetMapping("/aux2/{id}/quantity/{quantity}")
	public CompletableFuture<ResponseEntity<Item>> getItem3(@PathVariable Long id, @PathVariable Integer quantity) {
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK));
	}
</pre>

También se puede combinar con @CircuitBreaker pero si se combina es necesario quitar el fallbackMethod del TimeLimiter para que el CircuitBreaker haga la toleracion de fallos.
<pre>
	@TimeLimiter(name = "items")//,fallbackMethod = "alternativeMethod2")
	@CircuitBreaker(name = "items",fallbackMethod = "alternativeMethod2") //se puede quitar o combinar con TimeLimiter
	@GetMapping("/aux2/{id}/quantity/{quantity}")
	public CompletableFuture<ResponseEntity<Item>> getItem3(@PathVariable Long id, @PathVariable Integer quantity) {
		return CompletableFuture.supplyAsync(() -> new ResponseEntity<Item>(this.itemService.findById(id, quantity), HttpStatus.OK));
	}
</pre>

### Resilience4J en el API Gateway
- Añadir la dependencia Resilience4J en el pom.xml de gateway-server. Pero a diferencia de antes,
se tiene que anotar como reactiva:
<pre>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-circuitbreaker-reactor-resilience4j</artifactId>
		</dependency>
</pre>
- Colocar la configuracion de Resilience4J en el archivo properties del API Gateway. En este caso
se va a crear la configuracion "products"
<pre>

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
</pre>

- Ahora, para añadirlo al Gateway de products, es necesario colocar la configuración products como filtro.
El nombre es por defecto CircuitBreaker:

<pre>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - CircuitBreaker=products
</pre>

-Pero con esta configuración no entra en cortocircuito en las excepciones. Hay que hacer otra configuración:
<pre>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - name: CircuitBreaker
            args:
              name: products
              statusCodes: 500,404
</pre>

- Para crear métodos alternativos en la API Gateway lo que hay que hacer es añadir otro argumento llamado <b>fallbackUri</b>
En esta Uri se tiene que indicar otro microservicio que no sea el propiertario de este filtro ya que éste estará en cortocircuito y seguirá
sin estar disponible para hacer método alternativo

<pre>
        filters:
          - StripPrefix=2
          - Cookie=Custom message!, user, markete
          - name: CircuitBreaker
            args:
              name: products
              statusCodes: 500,404
              fallbackUri: forward:/api/items/2/quantity/3
</pre>