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

import com.arjuna.ats.jbossatx.jta.RecoveryManagerService;
import com.redhat.fuse.quickstarts.crash.DummyXAResourceRecovery;
import org.apache.camel.component.jms.JmsComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import javax.jms.ConnectionFactory;

@SpringBootApplication
public class Application {

	private static final Logger LOG = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

    @Bean(name = "jms-component")
    public JmsComponent jmsComponent(ConnectionFactory xaJmsConnectionFactory, PlatformTransactionManager jtaTransactionManager) {
	    JmsComponent jms = new JmsComponent();
	    jms.setConnectionFactory(xaJmsConnectionFactory);
	    jms.setTransactionManager(jtaTransactionManager);
	    jms.setTransacted(true);

	    return jms;
    }

	/**
	 * Dummy xa resource recovery to simulate a crash before final commit.
	 *
	 * This is (obviously) not needed in production and must be removed.
	 */
	@Component
	static class ApplicationCrashConfiguration implements ApplicationListener<ApplicationReadyEvent> {

		@Autowired
		private RecoveryManagerService recoveryManagerService;

		@Override
		public void onApplicationEvent(ApplicationReadyEvent applicationReadyEvent) {
			LOG.warn("Adding DummyXAResourceRecovery to recovery manager service");
			DummyXAResourceRecovery dummyRecovery = new DummyXAResourceRecovery();
			recoveryManagerService.addXAResourceRecovery(dummyRecovery);
		}

	}

}
