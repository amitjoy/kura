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
 *     Jens Reimann <jreimann@redhat.com> Fixes and cleanups
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Icon;
import org.eclipse.kura.configuration.metatype.OCD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.KuraExceptionHandler;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.wire.WireHelperService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class GwtComponentServiceImpl extends OsgiRemoteServiceServlet implements GwtComponentService {
    private static final long serialVersionUID = -4176701819112753800L;

    public List<GwtConfigComponent> findComponentConfigurations(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            // sort the list alphabetically by service name
            Collections.sort(configs, new Comparator<ComponentConfiguration>() {
                @Override
                public int compare(ComponentConfiguration arg0, ComponentConfiguration arg1) {
                    String name0;
                    int start = arg0.getPid().lastIndexOf('.');
                    int substringIndex = start + 1;
                    if (start != -1 && substringIndex < arg0.getPid().length()) {
                        name0 = arg0.getPid().substring(substringIndex);
                    } else {
                        name0 = arg0.getPid();
                    }

                    String name1;
                    start = arg1.getPid().lastIndexOf('.');
                    substringIndex = start + 1;
                    if (start != -1 && substringIndex < arg1.getPid().length()) {
                        name1 = arg1.getPid().substring(substringIndex);
                    } else {
                        name1 = arg1.getPid();
                    }
                    return name0.compareTo(name1);
                }
            });
            for (ComponentConfiguration config : configs) {

                // ignore items we want to hide
                if (config.getPid().endsWith("SystemPropertiesService") ||
                        config.getPid().endsWith("NetworkAdminService") ||
                        config.getPid().endsWith("NetworkConfigurationService") ||
                        config.getPid().endsWith("SslManagerService") ||
                        config.getPid().endsWith("FirewallConfigurationService") ||
                        config.getPid().endsWith("WireService")) {
                    continue;
                }

                OCD ocd = config.getDefinition();
                if (ocd != null) {

					GwtConfigComponent gwtConfig = createConfigFromConfiguration(config);
					gwtConfigs.add(gwtConfig);
				}
            }
        } catch (Throwable t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;
    }

    public List<GwtConfigComponent> findComponentConfiguration(GwtXSRFToken xsrfToken) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        List<GwtConfigComponent> gwtConfigs = new ArrayList<GwtConfigComponent>();
        try {

            List<ComponentConfiguration> configs = cs.getComponentConfigurations();
            // sort the list alphabetically by service name
            Collections.sort(configs, new Comparator<ComponentConfiguration>() {
                @Override
                public int compare(ComponentConfiguration arg0, ComponentConfiguration arg1) {
                    String name0;
                    int start = arg0.getPid().lastIndexOf('.');
                    int substringIndex = start + 1;
                    if (start != -1 && substringIndex < arg0.getPid().length()) {
                        name0 = arg0.getPid().substring(substringIndex);
                    } else {
                        name0 = arg0.getPid();
                    }

                    String name1;
                    start = arg1.getPid().lastIndexOf('.');
                    substringIndex = start + 1;
                    if (start != -1 && substringIndex < arg1.getPid().length()) {
                        name1 = arg1.getPid().substring(substringIndex);
                    } else {
                        name1 = arg1.getPid();
                    }
                    return name0.compareTo(name1);
                }
            });
            for (ComponentConfiguration config : configs) {

                // ignore items we want to hide
                if (!config.getPid().endsWith("CommandCloudApp")) {
                    continue;
                }

                OCD ocd = config.getDefinition();
                if (ocd != null) {

                    GwtConfigComponent gwtConfig = new GwtConfigComponent();
                    // gwtConfig.setComponentId(ocd.getId());
                    gwtConfig.setComponentId(config.getPid());

                    Map<String, Object> props = config.getConfigurationProperties();
                    if (props != null && props.get("service.factoryPid") != null) {
                        String pid = stripPidPrefix(config.getPid());
                        gwtConfig.setComponentName(pid);
                    } else {
                        gwtConfig.setComponentName(ocd.getName());
                    }

                    gwtConfig.setComponentDescription(ocd.getDescription());
                    if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
                        Icon icon = ocd.getIcon().get(0);
                        gwtConfig.setComponentIcon(icon.getResource());
                    }

                    List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
                    gwtConfig.setParameters(gwtParams);
                    for (AD ad : ocd.getAD()) {

                        GwtConfigParameter gwtParam = new GwtConfigParameter();
                        gwtParam.setId(ad.getId());
                        gwtParam.setName(ad.getName());
                        gwtParam.setDescription(ad.getDescription());
                        gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
                        gwtParam.setRequired(ad.isRequired());
                        gwtParam.setCardinality(ad.getCardinality());
                        if (ad.getOption() != null && !ad.getOption().isEmpty()) {
                            Map<String, String> options = new HashMap<String, String>();
                            for (Option option : ad.getOption()) {
                                options.put(option.getLabel(), option.getValue());
                            }
                            gwtParam.setOptions(options);
                        }
                        gwtParam.setMin(ad.getMin());
                        gwtParam.setMax(ad.getMax());
                        if (config.getConfigurationProperties() != null) {

                            // handle the value based on the cardinality of the
                            // attribute
                            int cardinality = ad.getCardinality();
                            Object value = config.getConfigurationProperties().get(ad.getId());
                            if (value != null) {
                                if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
                                    gwtParam.setValue(String.valueOf(value));
                                } else {
                                    // this could be an array value
                                    if (value instanceof Object[]) {
                                        Object[] objValues = (Object[]) value;
                                        List<String> strValues = new ArrayList<String>();
                                        for (Object v : objValues) {
                                            if (v != null) {
                                                strValues.add(String.valueOf(v));
                                            }
                                        }
                                        gwtParam.setValues(strValues.toArray(new String[] {}));
                                    }
                                }
                            }
                            gwtParams.add(gwtParam);
                        }
                    }
                    gwtConfigs.add(gwtConfig);
                }
            }
        } catch (Throwable t) {
            KuraExceptionHandler.handle(t);
        }
        return gwtConfigs;
    }

    public void updateComponentConfiguration(GwtXSRFToken xsrfToken, GwtConfigComponent gwtCompConfig) throws GwtKuraException {
        checkXSRFToken(xsrfToken);
        ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
        try {

            // Build the new properties
            Map<String, Object> properties = new HashMap<String, Object>();
            ComponentConfiguration backupCC = cs.getComponentConfiguration(gwtCompConfig.getComponentId());
            Map<String, Object> backupConfigProp = backupCC.getConfigurationProperties();
            for (GwtConfigParameter gwtConfigParam : gwtCompConfig.getParameters()) {

                Object objValue = null;

                ComponentConfiguration currentCC = cs.getComponentConfiguration(gwtCompConfig.getComponentId());
                Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
                Object currentObjValue = currentConfigProp.get(gwtConfigParam.getId());

                int cardinality = gwtConfigParam.getCardinality();
                if (cardinality == 0 || cardinality == 1 || cardinality == -1) {

                    String strValue = gwtConfigParam.getValue();

                    if ((currentObjValue instanceof Password) && PLACEHOLDER.equals(strValue)) {
                        objValue = currentConfigProp.get(gwtConfigParam.getId());
                    } else {
                        objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValue);
                    }
                } else {

                    String[] strValues = gwtConfigParam.getValues();

                    if (currentObjValue instanceof Password[]) {
                        Password[] currentPasswordValue = (Password[]) currentObjValue;
                        for (int i = 0; i < strValues.length; i++) {
                            if (PLACEHOLDER.equals(strValues[i])) {
                                strValues[i] = new String(currentPasswordValue[i].getPassword());
                            }
                        }
                    }

                    objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValues);
                }
                properties.put(gwtConfigParam.getId(), objValue);
            }

            // Force kura.service.pid into properties, if originally present
            if (backupConfigProp.get("kura.service.pid") != null) {
                properties.put("kura.service.pid", backupConfigProp.get("kura.service.pid"));
            }
            //
            // apply them
            cs.updateConfiguration(gwtCompConfig.getComponentId(), properties);
        } catch (Throwable t) {
            KuraExceptionHandler.handle(t);
        }
    }
    
    @Override
	public List<String> getFactoryComponents(GwtXSRFToken xsrfToken) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
		//finding all wire components to remove from any list as these factory instances 
		// are only shown in Kura Wires UI
		List<String> wireEmitterFpids = new ArrayList<String>();
		List<String> wireReceiverFpids = new ArrayList<String>();
		GwtServerUtil.fillFactoriesLists(wireEmitterFpids, wireReceiverFpids);
		final List<String> onlyProducers = new ArrayList<String>(wireEmitterFpids);
		final List<String> onlyConsumers = new ArrayList<String>(wireReceiverFpids);
		final List<String> both = new LinkedList<String>();
		for (final String dto : wireEmitterFpids) {
			if (wireReceiverFpids.contains(dto)) {
				both.add(dto);
			}
		}
		onlyProducers.removeAll(both);
		onlyConsumers.removeAll(both);
		List<String> allWireComponents = new ArrayList<String>(onlyProducers);
		allWireComponents.addAll(onlyConsumers);
		allWireComponents.addAll(both);
		List<String> result = new ArrayList<String>();
		result.addAll(cs.getFactoryComponentPids());
		result.removeAll(allWireComponents);
		return result;
	}

	@Override
	public void createFactoryComponent(GwtXSRFToken xsrfToken, String factoryPid, String pid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
		try {
			cs.createFactoryConfiguration(factoryPid, pid, null, true);
		} catch (KuraException e) {
			throw new GwtKuraException("A component with the same name already exists!");
		}
	}

	@Override
	public void deleteFactoryConfiguration(GwtXSRFToken xsrfToken, String pid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
		
		try {
			cs.deleteFactoryConfiguration(pid, true);
		} catch (KuraException e) {
			throw new GwtKuraException("Could not delete component configuration!");
		}
	}

	@Override
	public GwtConfigComponent findComponentConfigurationFromPid(GwtXSRFToken xsrfToken, String pid, String factoryPid) throws GwtKuraException {
		checkXSRFToken(xsrfToken);
		ConfigurationService cs = ServiceLocator.getInstance().getService(ConfigurationService.class);
		
		try {
			ComponentConfiguration conf = cs.getComponentConfiguration(pid);
			if(conf == null){
				conf = cs.getDefaultComponentConfiguration(factoryPid);
				if(conf != null){
					conf.getConfigurationProperties().put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
				}
				if(conf != null && conf.getDefinition()==null){
				   String tempName = String.valueOf(System.currentTimeMillis());
				   cs.createFactoryConfiguration(factoryPid, tempName, null, false);
				   try{
					   final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
					   String filterString = "("+ConfigurationService.KURA_SERVICE_PID+"=" + tempName + ")";
				       Filter filter = bundleContext.createFilter(filterString);
				       final ServiceTracker tempTracker = new ServiceTracker(bundleContext, filter, null);
				       tempTracker.open();
				       tempTracker.waitForService(5000);
				       tempTracker.close();
				       conf = cs.getComponentConfiguration(tempName);
				   }catch(Exception ex){
					   throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, ex);
				   }finally{
					   cs.deleteFactoryConfiguration(tempName, false);
				   }
			   }
			
			}
			GwtConfigComponent comp = createConfigFromConfiguration(conf); 
					
			return comp;
			
		} catch (KuraException e) {
			throw new GwtKuraException("Could not retrieve component configuration!");
		}
	}
	
	private GwtConfigComponent createConfigFromConfiguration(ComponentConfiguration config) throws GwtKuraException{
		WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
		OCD ocd = config.getDefinition();

		GwtConfigComponent gwtConfig = null;
		if(ocd != null){
			gwtConfig = new GwtConfigComponent();
			//gwtConfig.setComponentId(ocd.getId());
			gwtConfig.setComponentId(config.getPid());
			
			Map<String, Object> props = config.getConfigurationProperties();
			
			if(props != null && props.get("driver.pid") != null){
				gwtConfig.set("driver.pid", props.get("driver.pid"));
			}
			
			if (props != null && props.get(ConfigurationAdmin.SERVICE_FACTORYPID) != null){
				String pid = GwtServerUtil.stripPidPrefix(config.getPid());
				gwtConfig.setComponentName(pid);
				gwtConfig.setFactoryComponent(true);
				gwtConfig.setFactoryPid(String.valueOf(props.get(ConfigurationAdmin.SERVICE_FACTORYPID)));
				//check if the PID is assigned to a Wire Component
				gwtConfig.setWireComponent(wireHelperService.getServicePid(pid) != null);
			}else{
				gwtConfig.setComponentName(ocd.getName());
				gwtConfig.setFactoryComponent(false);
				gwtConfig.setWireComponent(false);
			}
			
			gwtConfig.setComponentDescription(ocd.getDescription());
			if (ocd.getIcon() != null && !ocd.getIcon().isEmpty()) {
				Icon icon = ocd.getIcon().get(0);
				gwtConfig.setComponentIcon(icon.getResource());
			}
	
			List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
			gwtConfig.setParameters(gwtParams);
			for (AD ad : ocd.getAD()) {
				GwtConfigParameter gwtParam = new GwtConfigParameter();
				gwtParam.setId(ad.getId());
				gwtParam.setName(ad.getName());
				gwtParam.setDescription(ad.getDescription());
				gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
				gwtParam.setRequired(ad.isRequired());
				gwtParam.setCardinality(ad.getCardinality());
				if (ad.getOption() != null && !ad.getOption().isEmpty()) {
					Map<String, String> options = new HashMap<String, String>();
					for (Option option : ad.getOption()) {
						options.put(option.getLabel(), option.getValue());
					}
					gwtParam.setOptions(options);
				}
				gwtParam.setMin(ad.getMin());
				gwtParam.setMax(ad.getMax());
				if (config.getConfigurationProperties() != null) {
	
					// handle the value based on the cardinality of the attribute
					int cardinality = ad.getCardinality();
					Object value = config.getConfigurationProperties().get(ad.getId());
					if (value != null) {
						if (cardinality == 0 || cardinality == 1 || cardinality == -1) {
							if(gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)){
								gwtParam.setValue(PLACEHOLDER);
							} else {
								gwtParam.setValue(String.valueOf(value));
							}
						}
						else {
							// this could be an array value
							if (value instanceof Object[]) {
								Object[] objValues = (Object[]) value;
								List<String> strValues = new ArrayList<String>();
								for (Object v : objValues) {
									if (v != null) {
										if(gwtParam.getType().equals(GwtConfigParameterType.PASSWORD)){
											strValues.add(PLACEHOLDER);
										} else {
											strValues.add(String.valueOf(v));
										}
									}
								}
								gwtParam.setValues(strValues.toArray( new String[]{}));
							}
						}
					}
					gwtParams.add(gwtParam);
				}
			}
		}
		
		return gwtConfig;
			
	}

    private String stripPidPrefix(String pid) {
        int start = pid.lastIndexOf('.');
        if (start < 0) {
            return pid;
        } else {
            int begin = start + 1;
            if (begin < pid.length()) {
                return pid.substring(begin);
            } else {
                return pid;
            }
        }
    }
}
