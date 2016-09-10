/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 *   Amit Kumar Mondal (admin@amitinside.com)
 */
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.asset.provider.BaseChannelDescriptor;
import org.eclipse.kura.configuration.ComponentConfiguration;
import org.eclipse.kura.configuration.ConfigurationService;
import org.eclipse.kura.configuration.Password;
import org.eclipse.kura.configuration.metatype.AD;
import org.eclipse.kura.configuration.metatype.Option;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.Driver;
import org.eclipse.kura.driver.DriverService;
import org.eclipse.kura.web.server.util.GwtServerUtil;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraErrorCode;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtChannelInfo;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtConfigParameter.GwtConfigParameterType;
import org.eclipse.kura.web.shared.model.GwtWiresConfiguration;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtWireService;
import org.eclipse.kura.wire.WireComponent;
import org.eclipse.kura.wire.WireConfiguration;
import org.eclipse.kura.wire.WireEmitter;
import org.eclipse.kura.wire.WireHelperService;
import org.eclipse.kura.wire.WireReceiver;
import org.eclipse.kura.wire.WireService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The class GwtWireServiceImpl implements {@link GwtWireService}
 */
public final class GwtWireServiceImpl extends OsgiRemoteServiceServlet implements GwtWireService {

	/**
	 * Different property related constants
	 */
	private static final String CELL_TYPE = "cellType";
	private static final String CELLS = "cells";
	private static final String CONSUMER = "consumer";
	private static final String DELETE_CELLS = "deleteCells";
	private static final String DEVS_MODEL_ELEMENT = "devs.Atomic";
	private static final String FACTORY_PID = "factoryPid";
	private static final String GRAPH = "graph";
	private static final String ID = "id";
	private static final String JOINT_JS = "jointJs";
	private static final String LABEL = "label";
	private static final String NEW_WIRE = "newWire";
	private static final String PID = "pid";

	private static final String PRODUCER = "producer";

	/** The Logger instance. */
	private static final Logger s_logger = LoggerFactory.getLogger(GwtWireServiceImpl.class);

	/** Serial Version */
	private static final long serialVersionUID = -6577843865830245755L;

	private static final String TYPE = "type";

	/** Wire Service PID Property */
	private static final String WIRE_SERVICE_PID = "org.eclipse.kura.wire.WireService";

	private static Map<String, Object> fillPropertiesFromConfiguration(final GwtConfigComponent config,
			final ComponentConfiguration currentCC) {
		// Build the new properties
		final Map<String, Object> properties = new HashMap<String, Object>();
		final ComponentConfiguration backupCC = currentCC;
		if (backupCC == null) {
			return null;
		}
		final Map<String, Object> backupConfigProp = backupCC.getConfigurationProperties();
		for (final GwtConfigParameter gwtConfigParam : config.getParameters()) {

			Object objValue;

			final Map<String, Object> currentConfigProp = currentCC.getConfigurationProperties();
			final Object currentObjValue = currentConfigProp.get(gwtConfigParam.getName());

			final int cardinality = gwtConfigParam.getCardinality();
			if ((cardinality == 0) || (cardinality == 1) || (cardinality == -1)) {

				final String strValue = gwtConfigParam.getValue();

				if ((currentObjValue instanceof Password) && PLACEHOLDER.equals(strValue)) {
					objValue = currentConfigProp.get(gwtConfigParam.getName());
				} else {
					objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValue);
				}
			} else {

				final String[] strValues = gwtConfigParam.getValues();

				if (currentObjValue instanceof Password[]) {
					final Password[] currentPasswordValue = (Password[]) currentObjValue;
					for (int i = 0; i < strValues.length; i++) {
						if (PLACEHOLDER.equals(strValues[i])) {
							strValues[i] = new String(currentPasswordValue[i].getPassword());
						}
					}
				}

				objValue = GwtServerUtil.getObjectValue(gwtConfigParam, strValues);
			}
			properties.put(gwtConfigParam.getName(), objValue);
		}

		// Force kura.service.pid into properties, if originally present
		if (backupConfigProp.get("kura.service.pid") != null) {
			properties.put("kura.service.pid", backupConfigProp.get("kura.service.pid"));
		}
		return properties;
	}

	/**
	 * Returns the formatted component string required for JS
	 *
	 * @param pid
	 *            the PID to parse
	 * @return the formatted string
	 * @throws GwtKuraException
	 */
	private static String getComponentString(final String pid) throws GwtKuraException {
		final StringBuilder result = new StringBuilder();

		final BundleContext ctx = FrameworkUtil.getBundle(GwtWireServiceImpl.class).getBundleContext();
		final Collection<ServiceReference<WireComponent>> refs = ServiceLocator.getInstance()
				.getServiceReferences(WireComponent.class, null);
		for (final ServiceReference<WireComponent> ref : refs) {
			if (ref.getProperty(ConfigurationService.KURA_SERVICE_PID).equals(pid)) {
				final String fPid = (String) ref.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
				final WireComponent comp = ctx.getService(ref);
				String compType;
				if ((comp instanceof WireEmitter) && (comp instanceof WireReceiver)) {
					compType = "both";
				} else if (comp instanceof WireEmitter) {
					compType = "producer";
				} else {
					compType = "consumer";
				}
				result.append(fPid).append("|").append(pid).append("|").append(pid).append("|").append(compType);
				return result.toString();
			}
		}
		s_logger.error("Could not find WireComponent for pid {}", pid);
		throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
	}

	private GwtChannelInfo getChannelFromProperties(final int channelIndex, final GwtConfigComponent descriptor,
			final GwtConfigComponent asset) {
		final GwtChannelInfo ci = new GwtChannelInfo();
		String indexPrefix = String.valueOf(channelIndex) + ".CH.";
		ci.setName(asset.getParameter(indexPrefix + "name").getValue());
		ci.setId(String.valueOf(channelIndex));
		ci.setType(asset.getParameter(indexPrefix + "type").getValue());
		ci.setValueType(asset.getParameter(indexPrefix + "value.type").getValue());
		indexPrefix += "DRIVER.";
		for (final GwtConfigParameter param : descriptor.getParameters()) {
			ci.set(param.getName(), asset.getParameter(indexPrefix + param.getName()).getValue());
		}

		return ci;
	}

	/** {@inheritDoc} */
	@Override
	public List<String> getDriverInstances(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);
		final Collection<ServiceReference<Driver>> refs = ServiceLocator.getInstance()
				.getServiceReferences(Driver.class, null);
		final List<String> drivers = new ArrayList<String>();
		for (final ServiceReference<Driver> ref : refs) {
			drivers.add(String.valueOf(ref.getProperty("kura.service.pid")));
		}
		return drivers;
	}

	@Override
	public GwtConfigComponent getGwtBaseChannelDescriptor(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		final BaseChannelDescriptor bcd = new BaseChannelDescriptor();
		try {
			@SuppressWarnings("unchecked")
			final List<AD> params = (List<AD>) bcd.getDescriptor();

			final GwtConfigComponent gwtConfig = new GwtConfigComponent();
			gwtConfig.setComponentId("BaseChannelDescriptor");

			final List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
			gwtConfig.setParameters(gwtParams);
			for (final AD ad : params) {
				final GwtConfigParameter gwtParam = new GwtConfigParameter();
				gwtParam.setId(ad.getId());
				gwtParam.setName(ad.getName());
				gwtParam.setDescription(ad.getDescription());
				gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
				gwtParam.setRequired(ad.isRequired());
				gwtParam.setCardinality(ad.getCardinality());
				if ((ad.getOption() != null) && !ad.getOption().isEmpty()) {
					final Map<String, String> options = new HashMap<String, String>();
					for (final Option option : ad.getOption()) {
						options.put(option.getLabel(), option.getValue());
					}
					gwtParam.setOptions(options);
				}
				gwtParam.setMin(ad.getMin());
				gwtParam.setMax(ad.getMax());
				gwtParam.setDefault(ad.getDefault());

				gwtParams.add(gwtParam);
			}
			return gwtConfig;
		} catch (final Exception ex) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		}
	}

	@Override
	public GwtConfigComponent getGwtChannelDescriptor(final GwtXSRFToken xsrfToken, final String driverPid)
			throws GwtKuraException {
		final DriverService driverService = ServiceLocator.getInstance().getService(DriverService.class);

		final Driver d = driverService.getDriver(driverPid);
		final ChannelDescriptor cd = d.getChannelDescriptor();
		try {
			@SuppressWarnings("unchecked")
			final List<AD> params = (List<AD>) cd.getDescriptor();

			final GwtConfigComponent gwtConfig = new GwtConfigComponent();
			gwtConfig.setComponentId(driverPid);

			final List<GwtConfigParameter> gwtParams = new ArrayList<GwtConfigParameter>();
			gwtConfig.setParameters(gwtParams);
			for (final AD ad : params) {
				final GwtConfigParameter gwtParam = new GwtConfigParameter();
				gwtParam.setId(ad.getId());
				gwtParam.setName(ad.getName());
				gwtParam.setDescription(ad.getDescription());
				gwtParam.setType(GwtConfigParameterType.valueOf(ad.getType().name()));
				gwtParam.setRequired(ad.isRequired());
				gwtParam.setCardinality(ad.getCardinality());
				if ((ad.getOption() != null) && !ad.getOption().isEmpty()) {
					final Map<String, String> options = new HashMap<String, String>();
					for (final Option option : ad.getOption()) {
						options.put(option.getLabel(), option.getValue());
					}
					gwtParam.setOptions(options);
				}
				gwtParam.setMin(ad.getMin());
				gwtParam.setMax(ad.getMax());
				gwtParam.setDefault(ad.getDefault());

				gwtParams.add(gwtParam);
			}
			return gwtConfig;
		} catch (final Exception ex) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR);
		}

	}

	@Override
	public List<GwtChannelInfo> getGwtChannels(final GwtXSRFToken xsrfToken, final GwtConfigComponent descriptor,
			final GwtConfigComponent asset) throws GwtKuraException {

		final List<GwtChannelInfo> result = new ArrayList<GwtChannelInfo>();

		final Set<Integer> channelIndexes = new HashSet<Integer>();
		for (final GwtConfigParameter param : asset.getParameters()) {
			if (param.getName().endsWith("CH.name")) {
				final String[] tokens = param.getName().split("\\.");
				channelIndexes.add(Integer.parseInt(tokens[0]));
			}
		}

		for (final Integer index : channelIndexes) {
			final GwtChannelInfo ci = this.getChannelFromProperties(index, descriptor, asset);
			result.add(ci);
		}

		return result;
	}

	/** {@inheritDoc} */
	@Override
	public GwtWiresConfiguration getWiresConfiguration(final GwtXSRFToken xsrfToken) throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);
		return this.getWiresConfigurationInternal();
	}

	private GwtWiresConfiguration getWiresConfigurationInternal() throws GwtKuraException {
		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);
		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		final Set<WireConfiguration> wireConfigurations = wireService.getWireConfigurations();
		final List<String> wireEmitterFactoryPids = new ArrayList<String>();
		final List<String> wireReceiverFactoryPids = new ArrayList<String>();
		final List<String> wireComponents = new ArrayList<String>();

		GwtServerUtil.fillFactoriesLists(wireEmitterFactoryPids, wireReceiverFactoryPids);

		String sGraph = null;
		// Get Graph JSON from WireService
		try {
			final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			sGraph = (String) wsProps.get(GRAPH);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}

		// create the JSON for the Wires Configuration
		final JSONObject wireConfig = new JSONObject();
		int i = 0;
		for (final WireConfiguration wireConfiguration : wireConfigurations) {
			final String emitterPid = wireConfiguration.getEmitterPid();
			final String receiverPid = wireConfiguration.getReceiverPid();
			wireComponents.add(getComponentString(emitterPid));
			wireComponents.add(getComponentString(receiverPid));

			final JSONObject wireConf = new JSONObject();
			try {
				wireConf.put("p", emitterPid);
				wireConf.put("c", receiverPid);
				wireConfig.put(String.valueOf(++i), wireConf);
			} catch (final JSONException exception) {
				throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
			}
		}
		final GwtWiresConfiguration configuration = new GwtWiresConfiguration();
		configuration.getWireEmitterFactoryPids().addAll(wireEmitterFactoryPids);
		configuration.getWireReceiverFactoryPids().addAll(wireReceiverFactoryPids);
		configuration.getWireComponents().addAll(wireComponents);
		configuration.setWiresConfigurationJson(wireConfig.toString());
		configuration.setGraph(sGraph == null ? "{}" : sGraph);
		return configuration;
	}

	/** {@inheritDoc} */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public GwtWiresConfiguration updateWireConfiguration(final GwtXSRFToken xsrfToken,
			final String newJsonConfiguration, final Map<String, GwtConfigComponent> configurations)
			throws GwtKuraException {
		this.checkXSRFToken(xsrfToken);

		final Map<String, String> idToPid = new HashMap<String, String>();

		JSONObject jObj = null; // JSON object containing wires configuration
		JSONObject jGraph = null; // JSON object containing graph configuration
		JSONArray jCells = null; // JSON array of cells within JointJS graph
		JSONArray jDelCells = null; // JSON array of cells to be deleted

		final WireService wireService = ServiceLocator.getInstance().getService(WireService.class);
		final WireHelperService wireHelperService = ServiceLocator.getInstance().getService(WireHelperService.class);
		final ConfigurationService configService = ServiceLocator.getInstance().getService(ConfigurationService.class);

		try {
			jObj = new JSONObject(newJsonConfiguration);
			jDelCells = jObj.getJSONArray(DELETE_CELLS);
			jGraph = jObj.getJSONObject(JOINT_JS);
			jCells = jGraph.getJSONArray(CELLS);

			// Delete wires
			// if you are wondering why we need to delete first, then you must
			// know that there can be situations where the graph is deleted but
			// not yet saved and the components are not removed from OSGi and
			// the user at the same time, creates a graph with new components
			// with the same names as given to the previously delete components.
			// That's the reason, we need to delete first and the recreate it.
			for (int i = 0; i < jDelCells.length(); i++) {
				final JSONObject jsonObject = jDelCells.getJSONObject(i);
				final String deleteCells = jsonObject.getString(CELL_TYPE);
				String producerPid = null;
				String consumerPid = null;
				if ("wire".equalsIgnoreCase(deleteCells)) {
					// delete wires must rely on the previous config saved in
					// the Wire Service properties
					try {
						final Map<String, Object> wsProps = configService.getComponentConfiguration(WIRE_SERVICE_PID)
								.getConfigurationProperties();
						final String graph = (String) wsProps.get(GRAPH);

						if (graph != null) {
							final JSONObject oldGraph = new JSONObject(graph);
							final JSONArray oldArray = oldGraph.getJSONArray("cells");
							for (int k = 0; k < oldArray.length(); k++) {
								final JSONObject oldJson = oldArray.getJSONObject(k);
								if (jsonObject.getString("p").equalsIgnoreCase(oldJson.getString(ID))) {
									producerPid = oldJson.getString(PID);
								}
								if (jsonObject.getString("c").equalsIgnoreCase(oldJson.getString(ID))) {
									consumerPid = oldJson.getString(PID);
								}
							}
						}
					} catch (final KuraException exception) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
					} catch (final JSONException exception) {
						throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
					}
					if ((producerPid != null) && (consumerPid != null)) {
						s_logger.info(
								"Deleting Wire: Producer PID -> " + producerPid + " | Consumer PID -> " + consumerPid);
						final WireConfiguration wireConfiguration = new WireConfiguration(producerPid, consumerPid,
								null);
						wireService.deleteWireConfiguration(wireConfiguration);
					}
				}
			}

			// Delete Wire Component instances
			for (int i = 0; i < jDelCells.length(); i++) {
				final JSONObject jsonObject = jDelCells.getJSONObject(i);
				if ("instance".equalsIgnoreCase(jsonObject.getString(CELL_TYPE))) {
					final String componentPid = jsonObject.getString(PID);
					// just to ensure the map doesn't explode :P
					if (configurations.containsKey(componentPid)) {
						configurations.remove(componentPid);
					}
					// check if the wire component is present as there can be a
					// situation where wire component is already deleted but the
					// JSON still has stale data
					final String servicePid = wireHelperService.getServicePid(componentPid);
					if (servicePid != null) {
						s_logger.info("Deleting Wire Component: PID -> " + componentPid);
						configService.deleteFactoryConfiguration(componentPid, false);
						for (final Iterator<Map.Entry<String, GwtConfigComponent>> it = configurations.entrySet()
								.iterator(); it.hasNext();) {
							final Map.Entry<String, GwtConfigComponent> entry = it.next();
							if (entry.getKey().equals(componentPid)) {
								s_logger.debug("Deleting Wire Component Configuration of: PID -> " + componentPid
										+ " from the configuration map");
								it.remove();
							}
						}
					}
				}
			}

			// Create new Wire Component instances
			for (int i = 0; i < jCells.length(); i++) {
				final JSONObject jsonObject = jCells.getJSONObject(i);
				if (DEVS_MODEL_ELEMENT.equalsIgnoreCase(jsonObject.getString(TYPE))) {
					String elementPid = jsonObject.getString(PID);
					if ("none".equalsIgnoreCase(elementPid)) {
						final String elementFactoryPid = jsonObject.getString(FACTORY_PID);
						final String elementLabel = jsonObject.getString(LABEL);
						s_logger.info("Creating new component: Factory PID -> " + elementFactoryPid + " | PID -> "
								+ elementLabel);
						elementPid = elementLabel;
						Map<String, Object> properties = null;
						String driver = null;
						try {
							driver = jsonObject.getString("driver");
						} catch (final JSONException ex) {
							// do nothing
						}
						if (driver != null) {
							properties = new HashMap<String, Object>();
							properties.put("asset.desc", "Sample Asset");
							properties.put("driver.pid", driver);
						}
						configService.createFactoryConfiguration(elementFactoryPid, elementPid, properties, false);
						jsonObject.put(PID, elementPid);
					}
					final String elementId = jsonObject.getString(ID);
					idToPid.put(elementId, elementPid);
				}
			}
			jGraph.put(CELLS, jCells);

			// Create new wires
			for (int i = 0; i < jCells.length(); i++) {
				if ("customLink.Element".equalsIgnoreCase(jCells.getJSONObject(i).getString(TYPE))
						&& jCells.getJSONObject(i).getBoolean(NEW_WIRE)) {
					final String prod = idToPid.get(jCells.getJSONObject(i).getString(PRODUCER));
					final String cons = idToPid.get(jCells.getJSONObject(i).getString(CONSUMER));
					s_logger.info("Creating new wire: Producer PID -> " + prod + " | Consumer PID -> " + cons);
					s_logger.info("Service Pid for Producer before tracker: {}", wireHelperService.getServicePid(prod));
					s_logger.info("Service Pid for Consumer before tracker: {}", wireHelperService.getServicePid(cons));

					// track and wait for the producer
					final String pPid = wireHelperService.getServicePid(prod);
					final BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
					String filterString = "(" + Constants.SERVICE_PID + "=" + pPid + ")";
					Filter filter = bundleContext.createFilter(filterString);
					final ServiceTracker producerTracker = new ServiceTracker(bundleContext, filter, null);
					producerTracker.open();
					producerTracker.waitForService(5000);
					producerTracker.close();

					// track and wait for the consumer
					final String cPid = wireHelperService.getServicePid(cons);
					filterString = "(" + Constants.SERVICE_PID + "=" + cPid + ")";
					filter = bundleContext.createFilter(filterString);
					final ServiceTracker consumerTracker = new ServiceTracker(bundleContext, filter, null);
					consumerTracker.open();
					consumerTracker.waitForService(5000);
					consumerTracker.close();

					wireService.createWireConfiguration(prod, cons);
					jCells.getJSONObject(i).put(NEW_WIRE, false);
				}
			}

			// Update the configuration for all the changes tracked in Wires
			// Composer
			for (final String pid : configurations.keySet()) {
				final GwtConfigComponent config = configurations.get(pid);
				if (config != null) {
					final ComponentConfiguration currentConf = configService.getComponentConfiguration(pid);
					final Map<String, Object> props = fillPropertiesFromConfiguration(config, currentConf);
					if (props != null) {
						configService.updateConfiguration(pid, props, false);
					}
				}
			}
			final Map<String, Object> props = configService.getComponentConfiguration(WIRE_SERVICE_PID)
					.getConfigurationProperties();
			props.put(GRAPH, jGraph.toString());
			configService.updateConfiguration(WIRE_SERVICE_PID, props, true);
			configurations.clear();
		} catch (final JSONException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final KuraException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final InterruptedException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		} catch (final InvalidSyntaxException exception) {
			throw new GwtKuraException(GwtKuraErrorCode.INTERNAL_ERROR, exception);
		}
		return this.getWiresConfigurationInternal();
	}

}
