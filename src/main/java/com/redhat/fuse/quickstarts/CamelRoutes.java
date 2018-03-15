/*
 * Copyright (C) 2018 Red Hat, Inc.
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
package com.redhat.fuse.quickstarts;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestParamType;
import org.springframework.stereotype.Component;

@Component
public class CamelRoutes extends RouteBuilder {

    public void configure() {

        restConfiguration().contextPath("/api");

        rest().get("/")
                .produces("text/plain")
                .route()
                .to("sql:select message from audit_log order by audit_id")
                .convertBodyTo(String.class);

        rest().post("/")
                .param().name("entry").type(RestParamType.query).endParam()
                .produces("text/plain")
                .route()
                .transform().header("entry")
                .to("direct:transaction");

        from("direct:transaction")
                .transacted()
                .transform().simple("${body}")
                .log("Processing {message} = ${body}")
                .setHeader("message", body())
                .to("sql:insert into audit_log (message) values (:#message)")
                .to("jms:outbound?disableReplyTo=true")
                .choice()
                    .when(body().startsWith("fail"))
                        .log("Failing forever with exception")
                        .process(x -> {throw new RuntimeException("Fail");})
                    .otherwise()
                        .log("Message ${body} added")
                .endChoice();


        from("jms:outbound")
                .log("Message sent to outbound: ${body}")
                .setHeader("message", simple("${body}-ok"))
                .to("sql:insert into audit_log (message) values (:#message)");

    }

}
