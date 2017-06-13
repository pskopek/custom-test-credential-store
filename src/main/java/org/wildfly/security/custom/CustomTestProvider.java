/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.wildfly.security.custom;

import java.security.Provider;
import java.util.Collections;

import org.kohsuke.MetaInfServices;
import org.wildfly.security.credential.store.CredentialStore;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@MetaInfServices(Provider.class)
public class CustomTestProvider extends Provider {

    public CustomTestProvider() {
        super("CustomTestProvider", 1.0, "Custom Test Provider - just for testing");
        putService(new Service(this, CredentialStore.CREDENTIAL_STORE_TYPE, CustomTestCredentialStore.CUSTOM_TEST_CREDENTIAL_STORE, CustomTestCredentialStore.class.getName(), Collections.emptyList(), Collections.emptyMap()));
    }

}
