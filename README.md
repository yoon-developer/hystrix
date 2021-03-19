Spring Cloud Netflix Hystrix
==========

# 1. build.gradle
> 버전  
- spring cloud: Hoxton.SR10
- spring-cloud-starter-netflix-hystrix: 2.2.7
- spring-cloud-starter-netflix-hystrix-dashboard: 2.2.7
- spring-boot-starter-actuator: 2.3.9

> dependencies 추가
- org.springframework.cloud:spring-cloud-starter-netflix-hystrix
- org.springframework.cloud:spring-cloud-starter-netflix-hystrix-dashboard
- org.springframework.boot:spring-boot-starter-actuator

```text
ext {
	set('springCloudVersion', "Hoxton.SR10")
}

dependencies {
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-hystrix'
	implementation 'org.springframework.cloud:spring-cloud-starter-netflix-hystrix-dashboard'
	implementation 'org.springframework.boot:spring-boot-starter-actuator'
	testImplementation('org.springframework.boot:spring-boot-starter-test') {
		exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
	}
}

dependencyManagement {
	imports {
		mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
	}
}
```

# 2. Application.yml

> 서버 포트 설정
```yaml
server:
  port: 80
```

> hystrix 설정
- hysrix.dashboard.proxy-stream-allow-list: 대시보드 stream allow list 작성
- hystrix.command.default: 전역 설정
- hystrix.command.book: 특정 commandKey에 대한 설정

```yaml
hystrix:
  dashboard:
    proxy-stream-allow-list: localhost
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 10000
    book:
      execution.isolation.thread.timeoutInMilliseconds: 1000
```

> actuator 설정
- management.endpoints.web.exposure.include: 활성화하고 싶은 endpoint 작성

```yaml
management:
  endpoints:
    web:
      exposure:
        include: '*'
```

# 3. Code

> @EnableHystrix, @EnableHystrixDashboard 추가
```java
@EnableHystrix
@EnableHystrixDashboard
@SpringBootApplication
public class HystrixApplication {

  public static void main(String[] args) {
    SpringApplication.run(HystrixApplication.class, args);
  }

}
```

> hystrix

```java
@RestController
public class BookController {

  @Autowired
  private BookService bookService;

  @Bean
  public RestTemplate rest(RestTemplateBuilder builder) {
    return builder.build();
  }

  @RequestMapping("/book")
  public String readBook() throws InterruptedException {
    return bookService.getBook();
  }

  @RequestMapping("/read")
  public String read() {
    return "Hystrix...";
  }

}
```

- Service 에 Hystrix 설정을 해야 함
- commandKey: 따로 설정하지 않을 경우는 메서드명 으로 설정. 서킷오픈시 같은 Key 값을 가지는 메서드 들은 같이 통계가 매겨짐
- HystrixProperty: 메드에 Hystrix 설정 (application.yml 파일에 작성 가능)
- fallbackMethod: fallback 메서드 지정서

```java
@Service
public class BookService {

  private final RestTemplate restTemplate;

  public BookService(RestTemplate rest) {
    this.restTemplate = rest;
  }

  @HystrixCommand(commandKey = "book", fallbackMethod = "fallback",
      commandProperties = {
          @HystrixProperty(name = "circuitBreaker.errorThresholdPercentage", value = "5"),
          @HystrixProperty(name = "circuitBreaker.requestVolumeThreshold", value = "3"),
          @HystrixProperty(name = "circuitBreaker.sleepWindowInMilliseconds", value = "10000")})

  public String getBook() throws InterruptedException {
    URI uri = URI.create("http://localhost/read");
//    Thread.sleep(100000);
    return this.restTemplate.getForObject(uri, String.class);
  }

  public String fallback() {
    return "Basic book...";
  }

}
```

# 4. Test

- 대시보드 URL: http://{hostname}:{port}/hystrix
- Single Hystrix App: https://{hostname}:{port}/actuator/hystrix.stream

> TEST [1]
```text
1. [GET] http://localhost/read
```

> Test [2]
```text
1. BookService > getBook
Thread.sleep(100000) 주석 해제

2. [GET] http://localhost/read
```

# 5. 추가 설명

## 5.1. Configuration
참조: https://github.com/Netflix/Hystrix/wiki/Configuration#execution.isolation.strategy
