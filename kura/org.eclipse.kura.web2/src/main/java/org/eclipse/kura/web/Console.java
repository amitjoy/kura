/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com> - Fix possible NPE, cleanup
 *     Amit Kumar Mondal (admin@amitinside.com)
 *******************************************************************************/
package org.eclipse.kura.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import org.eclipse.kura.configuration.ConfigurableComponent;
import org.eclipse.kura.configuration.KuraConfigReadyEvent;
import org.eclipse.kura.crypto.CryptoService;
import org.eclipse.kura.system.SystemService;
import org.eclipse.kura.web.server.GwtCertificatesServiceImpl;
import org.eclipse.kura.web.server.GwtComponentServiceImpl;
import org.eclipse.kura.web.server.GwtDeviceServiceImpl;
import org.eclipse.kura.web.server.GwtNetworkServiceImpl;
import org.eclipse.kura.web.server.GwtPackageServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityServiceImpl;
import org.eclipse.kura.web.server.GwtSecurityTokenServiceImpl;
import org.eclipse.kura.web.server.GwtSettingServiceImpl;
import org.eclipse.kura.web.server.GwtSnapshotServiceImpl;
import org.eclipse.kura.web.server.GwtSslServiceImpl;
import org.eclipse.kura.web.server.GwtStatusServiceImpl;
import org.eclipse.kura.web.server.GwtWireServiceImpl;
import org.eclipse.kura.web.server.servlet.DeviceSnapshotsServlet;
import org.eclipse.kura.web.server.servlet.FileServlet;
import org.eclipse.kura.web.server.servlet.SkinServlet;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Console implements ConfigurableComponent {

    private static final String APP_ROOT = "app.root";

    private static final String CONSOLE_PASSWORD = "console.password.value";
    private static final String CONSOLE_USERNAME = "console.username.value";

    private static String s_aliasRoot;
    private static String s_appRoot;

    private static ComponentContext s_context;
    private static final Logger s_logger = LoggerFactory.getLogger(Console.class);
    private static final String SERVLET_ALIAS_ROOT = "servlet.alias.root";

    private AuthenticationManager authMgr;

    private CryptoService m_cryptoService;
    private EventAdmin m_eventAdmin;

    private HttpService m_httpService;

    private Map<String, Object> m_properties;
    private SystemService m_systemService;

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    protected void activate(final ComponentContext context, final Map<String, Object> properties) {
        try {
            // Check if web interface is enabled.
            final boolean webEnabled = Boolean.parseBoolean((this.m_systemService.getKuraWebEnabled()));

            if (webEnabled) {
                s_logger.info("activate...");

                s_context = context;
                s_aliasRoot = (String) properties.get(SERVLET_ALIAS_ROOT);
                s_appRoot = (String) properties.get(APP_ROOT);
                final String servletRoot = s_aliasRoot;

                this.m_properties = new HashMap<String, Object>();
                final Iterator<String> keys = properties.keySet().iterator();
                while (keys.hasNext()) {
                    final String key = keys.next();
                    final Object value = properties.get(key);
                    this.m_properties.put(key, value);
                }

                final Object pwdProp = properties.get(CONSOLE_PASSWORD);
                char[] propertyPassword = null;
                if (pwdProp instanceof char[]) {
                    propertyPassword = (char[]) properties.get(CONSOLE_PASSWORD);
                } else {
                    propertyPassword = properties.get(CONSOLE_PASSWORD).toString().toCharArray();
                }

                try {
                    propertyPassword = this.m_cryptoService.decryptAes(propertyPassword);
                } catch (final Exception e) {
                }

                final Object value = properties.get(CONSOLE_PASSWORD);
                char[] decryptedPassword = null;
                try {
                    decryptedPassword = this.m_cryptoService.decryptAes(((String) value).toCharArray());
                } catch (final Exception e) {
                    decryptedPassword = value.toString().toCharArray();
                }
                propertyPassword = this.m_cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

                final String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
                this.authMgr = new AuthenticationManager(registeredUsername, propertyPassword);
                this.initHTTPService(this.authMgr, servletRoot);

                final Map<String, Object> props = new HashMap<String, Object>();
                props.put("kura.version", this.m_systemService.getKuraVersion());
                final EventProperties eventProps = new EventProperties(props);
                s_logger.info("postInstalledEvent() :: posting KuraConfigReadyEvent");
                this.m_eventAdmin.postEvent(new Event(KuraConfigReadyEvent.KURA_CONFIG_EVENT_READY_TOPIC, eventProps));
            } else {
                s_logger.info("Web interface disabled in Kura properties file.");
            }
        } catch (final Throwable t) {
            s_logger.warn("Error Registering Web Resources", t);
        }

    }

    protected void deactivate(final BundleContext context) {
        s_logger.info("deactivate...");

        s_context = null;

        this.unregisterServlet();
    }

    private void initHTTPService(final AuthenticationManager authMgr, final String servletRoot)
            throws NamespaceException, ServletException {
        // Initialize HttpService

        final HttpContext httpCtx = new SecureBasicHttpContext(this.m_httpService.createDefaultHttpContext(), authMgr);
        this.m_httpService.registerResources("/", "www", httpCtx);
        this.m_httpService.registerResources(s_appRoot, "www/denali.html", httpCtx);
        this.m_httpService.registerResources(s_aliasRoot, "www" + s_aliasRoot, httpCtx);

        this.m_httpService.registerServlet(servletRoot + "/xsrf", new GwtSecurityTokenServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/status", new GwtStatusServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/device", new GwtDeviceServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/network", new GwtNetworkServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/component", new GwtComponentServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/package", new GwtPackageServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/snapshot", new GwtSnapshotServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/setting", new GwtSettingServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/certificate", new GwtCertificatesServiceImpl(), null,
                httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/security", new GwtSecurityServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/file", new FileServlet(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/device_snapshots", new DeviceSnapshotsServlet(), null,
                httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/skin", new SkinServlet(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/ssl", new GwtSslServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet(servletRoot + "/wires", new GwtWireServiceImpl(), null, httpCtx);
        this.m_httpService.registerServlet("/sse", new EventHandlerServlet(), null, httpCtx);
    }

    public void setCryptoService(final CryptoService cryptoService) {
        this.m_cryptoService = cryptoService;
    }

    public void setEventAdminService(final EventAdmin eventAdmin) {
        this.m_eventAdmin = eventAdmin;
    }

    public void setHttpService(final HttpService httpService) {
        this.m_httpService = httpService;
    }

    public void setSystemService(final SystemService systemService) {
        this.m_systemService = systemService;
    }

    private void unregisterServlet() {
        final String servletRoot = s_aliasRoot;
        this.m_httpService.unregister("/");
        this.m_httpService.unregister(s_appRoot);
        this.m_httpService.unregister(s_aliasRoot);
        this.m_httpService.unregister(servletRoot + "/status");
        this.m_httpService.unregister(servletRoot + "/device");
        this.m_httpService.unregister(servletRoot + "/network");
        this.m_httpService.unregister(servletRoot + "/component");
        this.m_httpService.unregister(servletRoot + "/package");
        this.m_httpService.unregister(servletRoot + "/snapshot");
        this.m_httpService.unregister(servletRoot + "/setting");
        this.m_httpService.unregister(servletRoot + "/file");
        this.m_httpService.unregister(servletRoot + "/device_snapshots");
        this.m_httpService.unregister(servletRoot + "/skin");
        this.m_httpService.unregister(servletRoot + "/wires");
        this.m_httpService.unregister("/sse");

    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    public void unsetCryptoService(final CryptoService cryptoService) {
        this.m_cryptoService = null;
    }

    public void unsetEventAdminService(final EventAdmin eventAdmin) {
        this.m_eventAdmin = null;
    }

    public void unsetHttpService(final HttpService httpService) {
        this.m_httpService = null;
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    public void unsetSystemService(final SystemService systemService) {
        this.m_systemService = null;
    }

    protected void updated(final Map<String, Object> properties) {

        final boolean webEnabled = Boolean.parseBoolean((this.m_systemService.getKuraWebEnabled()));
        if (!webEnabled) {
            return;
        }

        char[] propertyPassword = null;

        final String registeredUsername = (String) properties.get(CONSOLE_USERNAME);
        this.authMgr.updateUsername(registeredUsername);

        try {
            final Object value = properties.get(CONSOLE_PASSWORD);
            char[] decryptedPassword = null;
            try {
                decryptedPassword = this.m_cryptoService.decryptAes(((String) value).toCharArray());
            } catch (final Exception e) {
                decryptedPassword = value.toString().toCharArray();
            }

            propertyPassword = this.m_cryptoService.sha1Hash(new String(decryptedPassword)).toCharArray();

            this.authMgr.updatePassword(propertyPassword);
        } catch (final Exception e) {
            s_logger.warn("Error Updating Web properties", e);
        }

    }

    public static String getApplicationRoot() {
        return s_appRoot;
    }

    public static BundleContext getBundleContext() {
        return s_context.getBundleContext();
    }

    public static String getServletRoot() {
        return s_aliasRoot;
    }
}
