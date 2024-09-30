# 将Spring Boot Admin和Eureka Server集成在同一个服务上
网上大部分整合spring boot admin的例子都是Eureka Server一个注册中心服务，
Spring Boot Admin作为另一个服务注册到Eureka Server。

现在展示如何将Spring Boot Admin和Eureka Server集成在同一个服务上，步骤如下：
1. 新建spcloud-eureka-spboot-admin-7001模块，新增pom.xml。
```
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<!--继承主工程的 POM-->
	<parent>
		<artifactId>spring-cloud-netflix-demo</artifactId>
		<groupId>net.biancheng.c</groupId>
		<version>1.0.0-SNAPSHOT</version>
	</parent>

	<artifactId>spcloud-eureka-spboot-admin-7001</artifactId>

	<properties>
		<java.version>1.8</java.version>
		<spring-boot-admin.version>2.3.1</spring-boot-admin.version>
		<!-- 指定内置tomcat的版本，避免SBA-UI使用的一些长轮询被关闭后报错 -->
		<tomcat.version>9.0.54</tomcat.version>
	</properties>

	<dependencies>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-web</artifactId>
		</dependency>
		<!--为服务注册中心引入 Eureka Server 的依赖-->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>
		<!-- eureka client依赖 -->
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
		<!-- springboot admin服务端依赖 -->
		<dependency>
			<groupId>de.codecentric</groupId>
			<artifactId>spring-boot-admin-starter-server</artifactId>
		</dependency>
		<!-- 安全认证相关依赖 -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>de.codecentric</groupId>
				<artifactId>spring-boot-admin-dependencies</artifactId>
				<version>${spring-boot-admin.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
				<configuration>
					<excludes>
						<exclude>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</exclude>
					</excludes>
				</configuration>
			</plugin>
		</plugins>
	</build>
</project>
```

2. 新增application.yml。
   重点是eureka.dashboard.enabled设置为false，避免影响到springboot admin首页的访问。
```
server:
  port: 7001

spring:
  application:
    name: micro-service-cloud-eureka-spboot-admin-7001
  # 结合 Spring Security 实现需要用户名和密码登录的安全认证
  security:
    user:
      name: "admin"
      password: "admin"
  # 设置springboot admin的信息，没啥用
#  boot:
#    admin:
#      context-path: /admin # admin界面与eureka分离

eureka:
  dashboard:
    # 禁用eureka服务控制台，整合springboot admin关键配置，否则会影响springboot admin首页的访问
    enabled: false
  client:
    healthcheck:
      enabled: true
    service-url:
      # defaultZone: http://admin:admin@localhost:8761/eureka/
      defaultZone: http://${spring.security.user.name}:${spring.security.user.password}@${eureka.instance.hostname}:${server.port}/eureka/
    # 获取注册信息，这里因为admin需要获取所以设置为true
    # （启动的时候会报错，没关系，后面eureka server启动后就不会报错）
    fetch-registry: true
    # 单实例情况不需要相互注册，这里因为admin端需要被发现所以注册上
    register-with-eureka: true
    # 获取注册信息时间间隔，单位为秒
    registryFetchIntervalSeconds: 5
  instance:
    hostname: localhost
    # 显示访问路径的 ip 地址
    prefer-ip-address: true
    # 向注册中心发送心跳时间间隔，单位为秒
    leaseRenewalIntervalInSeconds: 10
    health-check-url-path: /actuator/health
    metadata-map:
      user.name: ${spring.security.user.name}
      user.password: ${spring.security.user.password}

# 与Spring Boot 2一样，默认情况下，大多数actuator的端口都不会通过http公开
# * 代表公开所有这些端点。对于生产环境，应该仔细选择要公开的端点
# 暴露所有端点信息，不然在页面中无法查看监控数据
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: ALWAYS
```

3. 新增配置类。
```
/**
 * web安全相关配置
 * 包括springboot admin登录相关配置
 * @author bleachin
 */
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private final String adminContextPath; // 对应的配置项：spring.boot.admin.context-path

    public WebSecurityConfig(AdminServerProperties adminServerProperties) {
        this.adminContextPath = adminServerProperties.getContextPath();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // http.httpBasic()
        //         .and().authorizeRequests()
        //         .anyRequest().permitAll()
        //         .and().csrf().disable();

        // 登录成功之后重定向到applications页
        SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(adminContextPath + "/applications");

        http.authorizeRequests()
                // 授予对所有静态资源、登录页面的公共访问权限
                .antMatchers(adminContextPath + "/assets/**").permitAll()
                .antMatchers(adminContextPath + "/login").permitAll()
                // 这里根据你的实际情况暴露对应需要监控的服务
                .antMatchers("/actuator/**").permitAll()
                // 除了以上配置的，其他的请求必须进行身份验证
                .anyRequest().authenticated()
                .and()
                // 配置登录和注销
                .formLogin().loginPage(adminContextPath + "/login").successHandler(successHandler)
                .and()
                .logout().logoutUrl(adminContextPath + "/logout")
                .and()
                // 启用HTTP-Basic支持。这是Spring Boot Admin Client注册所必需的
                .httpBasic()
                .and()
                // 关闭跨站请求伪造，否则登录会失败
                .csrf().disable();

        // System.out.println("adminContextPath：" + adminContextPath);
        // SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
        // successHandler.setTargetUrlParameter("redirectTo");
        // http.authorizeRequests()
        //         .antMatchers(adminContextPath + "/assets/**").permitAll()
        //         .antMatchers(adminContextPath + "/login").permitAll()
        //         .antMatchers(adminContextPath + "/eureka/**").permitAll()
        //         .anyRequest().authenticated()
        //         .and()
        //         .formLogin().loginPage(adminContextPath + "/login").successHandler(successHandler)
        //         .and()
        //         .logout().logoutUrl(adminContextPath + "/logout")
        //         .and()
        //         .httpBasic()
        //         .and()
        //         .csrf()
        //         .ignoringAntMatchers(adminContextPath + "/instances", adminContextPath + "/actuator/**")
        //         .disable();
    }
}
```

4. 新增启动类，加上@EnableEurekaServer服务注册中心注解、admin服务注解，此处不用加@EnableDiscoveryClient‌，加入eureka client依赖会自动开启服务注册。
```
@SpringBootApplication
@EnableEurekaServer
@EnableAdminServer
public class SpCloudEurekaSpbootAdmin7001Application {
	public static void main(String[] args) {
		SpringApplication.run(SpCloudEurekaSpbootAdmin7001Application.class, args);
	}
}
```

5. 测试验证。
   浏览器访问：http://localhost:7001/login
   账号密码：
   admin
   admin
   登录后，界面会跳转到：http://localhost:7001/applications