package com.msa.hystrix;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    URI uri = URI.create("http://localhost:8080/read");
    Thread.sleep(100000);
    return this.restTemplate.getForObject(uri, String.class);
  }

  public String fallback() {
    return "Basic book...";
  }

}