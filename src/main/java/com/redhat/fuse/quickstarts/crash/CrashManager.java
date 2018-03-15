/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version
 * 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.redhat.fuse.quickstarts.crash;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.transaction.TransactionManager;

/**
 * This service simulates an outage in the middle of a XA transaction commit.
 * Do not use this service in a real application.
 *
 * @author nicola
 * @since 20/07/2017
 */
@Service
@Transactional
public class CrashManager {

    private TransactionManager transactionManager;

    @Autowired
    public CrashManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void killBeforeCommit() throws Exception {
        // Kill the system right before commit
        transactionManager.getTransaction().enlistResource(new DummyXAResource(true));
    }

}
