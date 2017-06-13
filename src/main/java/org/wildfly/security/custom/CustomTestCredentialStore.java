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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.wildfly.common.Assert;
import org.wildfly.security.credential.Credential;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.credential.store.CredentialStoreSpi;
import org.wildfly.security.credential.store.UnsupportedCredentialTypeException;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
public class CustomTestCredentialStore extends CredentialStoreSpi {

    private static final String LOCATION = "location";
    private static final String MODIFIABLE = "modifiable";
    private static final String CREATE = "create";

    private static final List<String> validAttributes = Arrays.asList(LOCATION, MODIFIABLE, CREATE);

    private Properties data = new Properties();
    private Path location;

    public static final String CUSTOM_TEST_CREDENTIAL_STORE = "CustomTestCredentialStore";

    /**
     * Initialize credential store service with given attributes. This procedure should set {@link #initialized} after
     * successful initialization.
     *
     * @param attributes          attributes to used to pass information to credential store service
     * @param protectionParameter the store-wide protection parameter to apply, or {@code null} for none
     * @param providers           providers to be injected into SPI implementation to get custom object instances of various type from, or {@code null} for none
     * @throws CredentialStoreException if initialization fails due to any reason
     */
    @Override
    public void initialize(Map<String, String> attributes, CredentialStore.ProtectionParameter protectionParameter, Provider[] providers) throws CredentialStoreException {
        validateAttribute(attributes, validAttributes);
        final String locationName = attributes.get(LOCATION);
        location = locationName == null ? null : Paths.get(locationName);
        if (location != null) {
            try (FileInputStream is = new FileInputStream(location.toFile())) {
                synchronized (data) {
                    data.load(is);
                }
            } catch (IOException e) {
                // TODO: log the exception
                System.out.println(e.getLocalizedMessage());
            }
        }
        initialized = true;
    }

    /**
     * Flush the credential store contents to storage.  If the credential store does not support or require explicit
     * flushing, this method should do nothing and simply return.
     *
     * @throws CredentialStoreException if the flush fails for some reason.
     */
    @Override
    public void flush() throws CredentialStoreException {
        if (location != null) {
            try (FileOutputStream os = new FileOutputStream(location.toFile())) {
                synchronized (data) {
                    data.store(os, null);
                }
            } catch (IOException e) {
                // TODO: log the exception
                System.out.println(e.getLocalizedMessage());
            }
        }
    }

    /**
     * Check if credential store service supports modification of its store
     *
     * @return {@code true} in case of modification of the store is supported, {@code false} otherwise
     */
    @Override
    public boolean isModifiable() {
        return true;
    }

    /**
     * Store credential to the credential store service under the given alias. If given alias already contains specific credential type type the credential
     * replaces older one. <em>Note:</em> {@link CredentialStoreSpi} supports storing of multiple entries (credential types) per alias.
     * Each must be of different credential type, or differing algorithm, or differing parameters.
     *
     * @param credentialAlias     to store the credential to the store
     * @param credential          instance of {@link Credential} to store
     * @param protectionParameter the protection parameter to apply to the entry, or {@code null} for none
     * @throws CredentialStoreException           when the credential cannot be stored
     * @throws UnsupportedCredentialTypeException when the credentialType is not supported
     */
    @Override
    public void store(String credentialAlias, Credential credential, CredentialStore.ProtectionParameter protectionParameter) throws CredentialStoreException, UnsupportedCredentialTypeException {
        Assert.checkNotNullParam("credentialAlias", credentialAlias);
        Assert.checkNotNullParam("credential", credential);
        if (credential instanceof PasswordCredential) {
            final char[] chars = credential.castAndApply(PasswordCredential.class, c -> c.getPassword().castAndApply(ClearPassword.class, ClearPassword::getPassword));
            synchronized (data) {
                data.setProperty(credentialAlias, new String(chars));
            }
        } else {
            throw new UnsupportedCredentialTypeException(credential.getClass().getCanonicalName());
        }
    }

    /**
     * Retrieve the credential stored in the store under the given alias, matching the given criteria.
     *
     * @param credentialAlias     to find the credential in the store
     * @param credentialType      the credential type class (must not be {@code null})
     * @param credentialAlgorithm the credential algorithm to match, or {@code null} to match any algorithm
     * @param parameterSpec       the parameter specification to match, or {@code null} to match any parameters
     * @param protectionParameter the protection parameter to use to access the entry, or {@code null} for none
     * @return instance of {@link Credential} stored in the store, or {@code null} if the credential is not found
     * @throws CredentialStoreException if the credential cannot be retrieved due to an error
     */
    @Override
    public <C extends Credential> C retrieve(String credentialAlias, Class<C> credentialType, String credentialAlgorithm, AlgorithmParameterSpec parameterSpec, CredentialStore.ProtectionParameter protectionParameter) throws CredentialStoreException {
        String value;
        synchronized (data) {
            value = data.getProperty(credentialAlias);
        }
        return credentialType.cast(new PasswordCredential(ClearPassword.createRaw(ClearPassword.ALGORITHM_CLEAR, value.toCharArray())));
    }

    /**
     * Remove the credentialType with from given alias from the credential store service.
     *
     * @param credentialAlias     alias to remove
     * @param credentialType      the credential type class to match (must not be {@code null})
     * @param credentialAlgorithm the credential algorithm to match, or {@code null} to match all algorithms
     * @param parameterSpec       the credential parameters to match, or {@code null} to match all parameters
     * @throws CredentialStoreException if the credential cannot be removed due to an error
     */
    @Override
    public void remove(String credentialAlias, Class<? extends Credential> credentialType, String credentialAlgorithm, AlgorithmParameterSpec parameterSpec) throws CredentialStoreException {
        synchronized (data) {
            data.remove(credentialAlias);
        }
    }

    /**
     * Returns credential aliases stored in this store as {@code Set<String>}.
     * <p>
     * It is not mandatory to override this method (throws {@link UnsupportedOperationException} by default).
     *
     * @return {@code Set<String>} of all keys stored in this store
     * @throws UnsupportedOperationException when this method is not supported by the underlying credential store
     * @throws CredentialStoreException      if there is any problem with internal store
     */
    @Override
    public Set<String> getAliases() throws UnsupportedOperationException, CredentialStoreException {
        synchronized (data) {
            return data.stringPropertyNames();
        }
    }
}
