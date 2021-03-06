/*
 * Copyright (c) 2015-2018 Open Baton (http://openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.nfvo.api.swagger;

import static com.google.common.base.Predicates.or;
import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.PathSelectors.regex;

import com.google.common.base.Predicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.ParameterBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.ModelRef;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/** Created by rvl on 30.01.2017. */
@EnableSwagger2
@Configuration
@Profile("swagger")
public class SwaggerConfig {

  @Bean
  public Docket myApi() {
    return new Docket(DocumentationType.SWAGGER_2)
        .apiInfo(apiInfo())
        .select()
        .apis(RequestHandlerSelectors.any())
        .paths(paths())
        .build()
        .globalOperationParameters(
            newArrayList(
                new ParameterBuilder()
                    .name("Authorization: Bearer")
                    .description("Authorization Token")
                    .modelRef(new ModelRef("string"))
                    .parameterType("header")
                    .required(true)
                    .build()));
  }

  private Predicate<String> paths() {
    return or(
        regex(""),
        regex("/api/v1/vnf-descriptors.*"),
        regex("/api/v1/ns-records.*"),
        regex("/api/v1/datacenters.*"),
        regex("/api/v1/vnf-packages.*"),
        regex("/api/v1/csar-.*"),
        regex("/api/v1/users.*"),
        regex("/api/v1/plugins.*"),
        regex("/api/v1/projects.*"),
        regex("/api/v1/keys.*"),
        regex("/api/v1/vnfmanagers.*"),
        regex("/api/v1/ns-descriptors.*"),
        regex("/api/v1/wizzard.*"));
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder()
        .title("NFVO REST API")
        .version("3.2.x")
        .description(
            "Below is a detailed description of the REST API of the OpenBaton Network  Function  Virtualization  Orchestrator"
                + "(NFVO).      \n"
                + "To  send  REST  requests  to  the  NFVO  you  first  have  to  get  a  token  which  you  then  have  to  pass  in  the"
                + " header of every request. You can retrieve a token by executing this curl request: \n "
                + "curl -v -u openbatonOSClient:secret -X POST http://localhost:8080/oauth/token -H \"Accept:application/json\" -d \"username=admin&password=openbaton&grant_type=password\"")
        .build();
  }
}
