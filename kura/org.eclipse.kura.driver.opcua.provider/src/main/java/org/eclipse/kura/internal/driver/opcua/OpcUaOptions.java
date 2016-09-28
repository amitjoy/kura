/**
 * Copyright (c) 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.internal.driver.opcua;

import static org.eclipse.kura.Preconditions.checkNull;

import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.localization.resources.OpcUaMessages;
import org.eclipse.kura.util.base.StringUtil;
import org.eclipse.kura.util.base.ThrowableUtil;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.UsernameProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class OpcUaOptions is responsible to provide all the required
 * configurable options for the OPC-UA Driver.<br/>
 * <br/>
 *
 * The different properties to configure a OPC-UA Driver are as follows:
 * <ul>
 * <li>endpoint.ip</li>
 * <li>endpoint.port</li>
 * <li>server.name</li>
 * <li>application.name</li>
 * <li>application.uri</li>
 * <li>certificate.location</li>
 * <li>keystore.client.alias</li>
 * <li>keystore.server.alias</li>
 * <li>keystore.password</li>
 * <li>keystore.type</li> must be one of these : PKCS11, PKCS12, JKS
 * <li>security.policy</li> must be one of these : None, Basic128Rsa15,
 * Basic256, Basic256Sha256
 * <li>username</li>
 * <li>password</li>
 * <li>request.timeout</li>
 * <li>session.timeout</li>
 * </ul>
 */
final class OpcUaOptions {

    /**
     * Configurable Property to set OPC-UA application certificate
     */
    private static final String APPLICATION_CERTIFICATE = "certificate.location";

    /**
     * Configurable Property to set OPC-UA application name
     */
    private static final String APPLICATION_NAME = "application.name";

    /**
     * Configurable Property to set OPC-UA application uri
     */
    private static final String APPLICATION_URI = "application.uri";

    /** OPC-UA Endpoint IP */
    private static final String IP = "endpoint.ip";

    /**
     * Configurable property to set client alias for the keystore
     */
    private static final String KEYSTORE_CLIENT_ALIAS = "keystore.client.alias";

    /**
     * Configurable Property to set keystore password
     */
    private static final String KEYSTORE_PASSWORD = "keystore.password";

    /**
     * Configurable Property to set server alias for the keystore
     */
    private static final String KEYSTORE_SERVER_ALIAS = "keystore.server.alias";

    /**
     * Configurable Property to set keystore type
     */
    private static final String KEYSTORE_TYPE = "keystore.type";

    /**
     * Configurable Property to OPC-UA server password
     */
    private static final String PASSWORD = "password";

    /** OPC-UA Endpoint Port */
    private static final String PORT = "endpoint.port";

    /**
     * Configurable property specifying the request timeout
     */
    private static final String REQUEST_TIMEOUT = "request.timeout";

    /** The Logger instance. */
    private static final Logger s_logger = LoggerFactory.getLogger(OpcUaOptions.class);

    /** Localization Resource. */
    private static final OpcUaMessages s_message = LocalizationAdapter.adapt(OpcUaMessages.class);

    /**
     * Configurable property specifying the Security Policy
     */
    private static final String SECURITY_POLICY = "security.policy";

    /** OPC-UA Server Name */
    private static final String SERVER_NAME = "server.name";

    /**
     * Configurable property specifying the session timeout
     */
    private static final String SESSION_TIMEOUT = "session.timeout";

    /**
     * Configurable Property to set OPC-UA server username
     */
    private static final String USERNAME = "username";

    /** The Crypto Service dependency. */
    private final CryptoService cryptoService;

    /** The properties as associated */
    private final Map<String, Object> properties;

    /**
     * Instantiates a new S7 PLC options.
     *
     * @param properties
     *            the properties
     */
    OpcUaOptions(final Map<String, Object> properties, final CryptoService cryptoService) {
        checkNull(properties, s_message.propertiesNonNull());
        checkNull(cryptoService, s_message.cryptoServiceNonNull());

        this.properties = properties;
        this.cryptoService = cryptoService;
    }

    /**
     * Returns the OPC-UA Application Certificate
     *
     * @return the OPC-UA Application Certificate
     */
    String getApplicationCertificate() {
        String applicationCert = null;
        final Object certificate = this.properties.get(APPLICATION_CERTIFICATE);
        if ((this.properties != null) && this.properties.containsKey(APPLICATION_CERTIFICATE)
                && (certificate != null)) {
            applicationCert = certificate.toString();
        }
        return applicationCert;
    }

    /**
     * Returns the OPC-UA Application Name
     *
     * @return the OPC-UA Application Name
     */
    String getApplicationName() {
        String applicationName = null;
        final Object appName = this.properties.get(APPLICATION_NAME);
        if ((this.properties != null) && this.properties.containsKey(APPLICATION_NAME) && (appName != null)) {
            applicationName = appName.toString();
        }
        return applicationName;
    }

    /**
     * Returns the OPC-UA Application URI
     *
     * @return the OPC-UA Application URI
     */
    String getApplicationUri() {
        String applicationUri = null;
        final Object appUri = this.properties.get(APPLICATION_URI);
        if ((this.properties != null) && this.properties.containsKey(APPLICATION_URI) && (appUri != null)) {
            applicationUri = appUri.toString();
        }
        return applicationUri;
    }

    /**
     * Returns the OPC-UA Identity Provider
     *
     * @return the OPC-UA Identity Provider
     */
    IdentityProvider getIdentityProvider() {
        IdentityProvider identityProvider;
        final String username = this.getUsername();
        final String password = this.getPassword();
        if (StringUtil.isNullOrEmpty(username) && StringUtil.isNullOrEmpty(password)) {
            identityProvider = new AnonymousProvider();
        } else {
            identityProvider = new UsernameProvider(username, password);
        }
        return identityProvider;
    }

    /**
     * Returns the OPC-UA Endpoint IP
     *
     * @return the OPC-UA Endpoint IP
     */
    String getIp() {
        String ipAddress = null;
        final Object ip = this.properties.get(IP);
        if ((this.properties != null) && this.properties.containsKey(IP) && (ip != null)) {
            ipAddress = ip.toString();
        }
        return ipAddress;
    }

    /**
     * Returns the Keystore Client Alias
     *
     * @return the Keystore Client Alias
     */
    String getKeystoreClientAlias() {
        String clientAlias = null;
        final Object alias = this.properties.get(KEYSTORE_CLIENT_ALIAS);
        if ((this.properties != null) && this.properties.containsKey(KEYSTORE_CLIENT_ALIAS) && (alias != null)) {
            clientAlias = alias.toString();
        }
        return clientAlias;
    }

    /**
     * Returns the Keystore Password
     *
     * @return the Keystore Password
     */
    String getKeystorePassword() {
        String password = null;
        Password decryptedPassword = null;
        final Object keystorePass = this.properties.get(KEYSTORE_PASSWORD);
        if ((this.properties != null) && this.properties.containsKey(KEYSTORE_PASSWORD) && (keystorePass != null)) {
            try {
                decryptedPassword = new Password(this.cryptoService.decryptAes(keystorePass.toString().toCharArray()));
                password = new String(decryptedPassword.getPassword());
            } catch (final KuraException e) {
                s_logger.error(ThrowableUtil.stackTraceAsString(e));
            }
        }
        return password;
    }

    /**
     * Returns the Keystore Server Alias
     *
     * @return the Keystore Server Alias
     */
    String getKeystoreServerAlias() {
        String serverAlias = null;
        final Object keystoreServerAlias = this.properties.get(KEYSTORE_SERVER_ALIAS);
        if ((this.properties != null) && this.properties.containsKey(KEYSTORE_SERVER_ALIAS)
                && (keystoreServerAlias != null)) {
            serverAlias = keystoreServerAlias.toString();
        }
        return serverAlias;
    }

    /**
     * Returns the Keystore Type
     *
     * @return the Keystore Type
     */
    String getKeystoreType() {
        String keystoreType = null;
        final Object type = this.properties.get(KEYSTORE_TYPE);
        if ((this.properties != null) && this.properties.containsKey(KEYSTORE_TYPE) && (type != null)) {
            keystoreType = type.toString();
        }
        return keystoreType;
    }

    /**
     * Returns the OPC-UA Password
     *
     * @return the OPC-UA Password
     */
    String getPassword() {
        final Object pass = this.properties.get(PASSWORD);
        Password decryptedPassword = null;
        String password = null;
        if ((this.properties != null) && this.properties.containsKey(PASSWORD) && (pass != null)) {
            try {
                decryptedPassword = new Password(this.cryptoService.decryptAes(pass.toString().toCharArray()));
                password = new String(decryptedPassword.getPassword());
            } catch (final KuraException e) {
                s_logger.error(ThrowableUtil.stackTraceAsString(e));
            }
        }
        return password;
    }

    /**
     * Returns OPC-UA Endpoint Port
     *
     * @return the OPC-UA Endpoint Port
     */
    int getPort() {
        int port = 0;
        final Object endpointPort = this.properties.get(PORT);
        if ((this.properties != null) && this.properties.containsKey(PORT) && (endpointPort != null)) {
            port = Integer.valueOf(endpointPort.toString());
        }
        return port;
    }

    /**
     * Returns OPC-UA Request Timeout
     *
     * @return the OPC-UA Request Timeout
     */
    long getRequestTimeout() {
        long requestTimeout = 0;
        final Object reqTimeout = this.properties.get(REQUEST_TIMEOUT);
        if ((this.properties != null) && this.properties.containsKey(REQUEST_TIMEOUT) && (reqTimeout != null)) {
            requestTimeout = Long.valueOf(reqTimeout.toString());
        }
        return requestTimeout * 1000;
    }

    /**
     * Returns the Security Policy
     *
     * @return the Security Policy
     */
    SecurityPolicy getSecurityPolicy() {
        int securityPolicy = 0;
        final Object policy = this.properties.get(SECURITY_POLICY);
        if ((this.properties != null) && this.properties.containsKey(SECURITY_POLICY) && (policy != null)) {
            securityPolicy = Integer.parseInt(policy.toString());
        }
        switch (securityPolicy) {
        case 1:
            return SecurityPolicy.Basic128Rsa15;
        case 2:
            return SecurityPolicy.Basic256;
        case 3:
            return SecurityPolicy.Basic256Sha256;
        default:
            return SecurityPolicy.None;
        }
    }

    /**
     * Returns the OPC-UA Server Name
     *
     * @return the OPC-UA Server Name
     */
    String getServerName() {
        String serverName = null;
        final Object name = this.properties.get(SERVER_NAME);
        if ((this.properties != null) && this.properties.containsKey(SERVER_NAME) && (name != null)) {
            serverName = name.toString();
        }
        return serverName;
    }

    /**
     * Returns OPC-UA Session Timeout (in milliseconds)
     *
     * @return the OPC-UA Session Timeout (in milliseconds)
     */
    long getSessionTimeout() {
        long sessionTimeout = 0;
        final Object timeout = this.properties.get(SESSION_TIMEOUT);
        if ((this.properties != null) && this.properties.containsKey(SESSION_TIMEOUT) && (timeout != null)) {
            sessionTimeout = Long.valueOf(timeout.toString());
        }
        return sessionTimeout * 1000;
    }

    /**
     * Returns the OPC-UA Username
     *
     * @return the OPC-UA Username
     */
    String getUsername() {
        String username = null;
        final Object name = this.properties.get(USERNAME);
        if ((this.properties != null) && this.properties.containsKey(USERNAME) && (name != null)) {
            username = name.toString();
        }
        return username;
    }

}
