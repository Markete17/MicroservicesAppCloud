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
