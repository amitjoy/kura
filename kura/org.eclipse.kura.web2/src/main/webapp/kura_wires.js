/**
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Eurotech Amit Kumar Mondal (admin@amitinside.com)
 */
var kuraWires = (function() {
	var client = {}; // Holds accessible elements of JS library
	var clientConfig = {}; // Configuration passed from Kura OSGi
	var delCells = []; // Components and Wires to be deleted from OSGi
	// framework on save
	var graph, paper; // JointJS objects
	var initialized = false;
	var xPos = 10;
	var yPos = 10;
	var currentZoomLevel = 1.0;
	var paperScaleMax = 1.5;
	var paperScaleMin = .5;
	var paperScaling = .2;
	var selectedElement, oldSelectedPid;
	var oldCellView;
	var elementsContainerTemp = [];

	/*
	 * / Public functions
	 */
	client.render = function(obj) {
		clientConfig = JSON.parse(obj);
		sse();
		setup();
		regiterFormInputFieldValidation();
	};

	client.getDriver = function(assetPid) {
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			if (!elem.isLink()) {
				if (elem.attributes.label === assetPid) {
					return elem.attributes.driver;
				}
			}
		}
	};

	var removeCellFunc = function(cell) {
		top.jsniMakeUiDirty();
		removeCell(cell);
	};

	/**
	 * Interaction with OSGi Event Admin through Server Sent Events
	 */
	function sse() {
		var eventSource = new EventSource("/sse");
		eventSource.onmessage = function(event) {
			_.each(graph.getElements(), function(c) {
				if (c.attributes.pid === event.data) {
					fireTransition(c);
				}
			});
		};
	}

	function checkForCycleExistence() {
		var visited = [];
		var level = 0;
		var isCycleExists;
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			if ((graph.getPredecessors(elem).length > 0) && !elem.isLink()
					&& hasCycle(elem, visited, level)) {
				isCycleExists = true;
				break;
			}
		}
		if (isCycleExists) {
			top.jsniShowCycleExistenceError();
		}
		return isCycleExists;
	}

	function hasCycle(comp, visited, level) {
		var neighbors = graph.getNeighbors(comp, {
			outbound : true
		}), i;

		if (visited.indexOf(comp.id) > -1)
			return true;
		visited.push(comp.id);

		for (i = 0; i < neighbors.length; i++)
			if (hasCycle(neighbors[i], visited.slice(), ++level))
				return true;

		return false;
	}

	/*
	 * / Initiate JointJS graph
	 */
	function setup() {
		// Setup element events. Cannot be done in ready as we need to wait for
		// GWT entry point to be called
		// Instantiate JointJS graph and paper
		if (!initialized) {
			$("#btn-create-comp").on("click", createNewComponent);
			$("#btn-save-graph").on("click", saveConfig);
			$("#btn-delete-comp").on("click", deleteComponent);
			$("#btn-delete-graph-confirm").on("click", deleteGraph);
			$("#btn-zoom-in").on("click", zoomInPaper);
			$("#btn-zoom-out").on("click", zoomOutPaper);

			initialized = true;

			// Set up custom elements
			setupElements();

			// Setup graph and paper
			graph = new joint.dia.Graph;

			paper = new joint.dia.Paper({
				el : $('#wires-graph'),
				width : '100%',
				height : 400,
				model : graph,
				gridSize : 20,
				snapLinks : true,
				linkPinning : false,
				defaultLink : new joint.shapes.customLink.Element,
				multiLinks : false,
				markAvailable : true,
				restrictTranslate : function(elementView) {
					var parentId = elementView.model.get('parent');
					return parentId && this.model.getCell(parentId).getBBox();
				},
				validateConnection : function(cellViewS, magnetS, cellViewT,
						magnetT, end, linkView) {
					// Prevent linking from input ports.
					if (magnetS && magnetS.getAttribute('type') === 'input')
						return false;
					// Prevent linking from output ports to input ports within
					// one element.
					if (cellViewS === cellViewT)
						return false;
					// Prevent linking to input ports.
					return magnetT && magnetT.getAttribute('type') === 'input';
				}
			});
		}

		graph.off('remove', removeCellFunc);
		// Load a graph if it exists
		if (!$.isEmptyObject(clientConfig.pGraph)) {
			graph.clear();
			try {
				graph.fromJSON(clientConfig.pGraph);
			} catch (err) {
				console.log(err.stack);
			}
		}

		// for any position change of the element, make the UI dirty
		var _elements = graph.getElements();
		for (var i = 0; i < _elements.length; i++) {
			var elem = _elements[i];
			elem.on('change:position', function() {
				top.jsniMakeUiDirty();
			})
		}

		// for any position change of the link, make the UI dirty
		var _links = graph.getLinks();
		for (var i = 0; i < _links.length; i++) {
			var link = _links[i];
			elem.on('change', function() {
				top.jsniMakeUiDirty();
			})
		}

		// If components exist in the framework but not the graph, create UI
		// elements
		if (typeof clientConfig.components != 'undefined') {
			$.each(clientConfig.components, function(index, component) {
				var exists = false;
				$.each(graph.getCells(), function(index, cell) {
					if (cell.attributes.pid === component.pid) {
						exists = true;
					}
				});
				if (!exists) {
					createComponent(component);
				}
			});
		}
		graph.on('change:source change:target', function(link) {
			createWire(link);
			top.jsniMakeUiDirty();
		});
		graph.on('remove', removeCellFunc);

		paper.on('cell:pointerdown', function(cellView, evt, x, y) {
			var pid = cellView.model.attributes.label;
			var factoryPid = cellView.model.attributes.factoryPid;
			selectedElement = cellView.model;
			if (oldCellView != null) {
				// oldCellView.unhighlight();
				oldCellView = null;
			}
			if (typeof cellView !== 'undefined'
					&& typeof cellView.sourceBBox === 'undefined') {
				if (oldSelectedPid !== pid) {
					top.jsniUpdateSelection(pid, factoryPid);
					oldSelectedPid = pid;
					isUpdateSelectionTriggered = true;
				}
				// cellView.highlight();
				oldCellView = cellView;
			}
		});

		paper.on('blank:pointerdown', function(cellView, evt, x, y) {
			top.jsniUpdateSelection("", "");
			selectedElement = "";
			oldSelectedPid = null;
			if (oldCellView != null) {
				// oldCellView.unhighlight();
				oldCellView = null;
			}
		});

		paper.$el.on('mousewheel wheel', onMouseWheel);
	}

	function zoomInPaper() {
		if (currentZoomLevel <= paperScaleMax) {
			currentZoomLevel = currentZoomLevel + paperScaling;
			paper.scale(currentZoomLevel);
		}
	}

	function zoomOutPaper() {
		if (currentZoomLevel >= paperScaleMin) {
			currentZoomLevel = currentZoomLevel - paperScaling;
			paper.scale(currentZoomLevel);
		}
	}

	function onMouseWheel(e) {

		e.preventDefault();
		e = e.originalEvent;

		var delta = Math.max(-1, Math.min(1, (e.wheelDelta || -e.detail))) / 50;
		var offsetX = (e.offsetX || e.clientX - $(this).offset().left); // offsetX

		var offsetY = (e.offsetY || e.clientY - $(this).offset().top); // offsetY

		var p = offsetToLocalPoint(offsetX, offsetY);
		var newScale = V(paper.viewport).scale().sx + delta; // the current

		currentZoomLevel = newScale;
		if (newScale > 0.4 && newScale < 2) {
			paper.setOrigin(0, 0); // reset the previous viewport translation
			paper.scale(newScale, newScale, p.x, p.y);
		}
	}

	function offsetToLocalPoint(x, y) {
		var svgPoint = paper.svg.createSVGPoint();
		svgPoint.x = x;
		svgPoint.y = y;
		// Transform point into the viewport coordinate system.
		var pointTransformed = svgPoint.matrixTransform(paper.viewport.getCTM()
				.inverse());
		return pointTransformed;
	}

	function allPredecessorsVisited(needle, haystack) {
		for (var i = 0; i < needle.length; i++) {
			if (haystack.indexOf(needle[i]) === -1)
				return false;
		}
		return true;
	}

	function fireTransition(t) {

		var inbound = graph.getConnectedLinks(t, {
			inbound : false
		});
		var outbound = graph.getConnectedLinks(t, {
			outbound : true
		});

		var placesBefore = _.map(inbound, function(link) {
			return graph.getCell(link.get('source').id);
		});
		var placesAfter = _.map(outbound, function(link) {
			return graph.getCell(link.get('target').id);
		});

		var isFirable = true;
		_.each(placesBefore, function(p) {
			if (p.get('tokens') === 0)
				isFirable = false;
		});

		if (isFirable) {

			_.each(placesAfter, function(p) {
				var link = _.find(outbound, function(l) {
					return l.get('target').id === p.id;
				});

				link.transition('attrs/.connection/stroke', '#F39C12', {
					duration : 400,
					timingFunction : function(t) {
						return t;
					},
					valueFunction : function(a, b) {

						var ca = parseInt(a.slice(1), 16);
						var cb = parseInt(b.slice(1), 16);
						var ra = ca & 0x0000ff;
						var rd = (cb & 0x0000ff) - ra;
						var ga = ca & 0x00ff00;
						var gd = (cb & 0x00ff00) - ga;
						var ba = ca & 0xff0000;
						var bd = (cb & 0xff0000) - ba;

						return function(t) {

							var scale = t < .5 ? t * 2 : 1 - (2 * (t - .5));
							var r = (ra + rd * scale) & 0x000000ff;
							var g = (ga + gd * scale) & 0x0000ff00;
							var b = (ba + bd * scale) & 0x00ff0000;

							var result = '#'
									+ (1 << 24 | r | g | b).toString(16).slice(
											1);
							if (t === 0) {
								result = '#4b4f6a';
							}
							return result;
						};
					}
				});

				link.transition('attrs/.connection/stroke-width', 8, {
					duration : 400,
					timingFunction : joint.util.timing.linear,
					valueFunction : function(a, b) {
						var d = b - a;
						return function(t) {
							var scale = t < .5 ? t * 2 : 1 - (2 * (t - .5));
							if (t === 0) {
								result = 4;
							} else {
								result = a + d * scale;
							}
							return result;
						};
					}
				});
			});
		}
	}

	/*
	 * / Create a new component
	 */
	function createComponent(comp) {

		if (comp.name === "") {
			comp.name = comp.pid;
		}

		// validate all the existing elements' PIDs with the new element PID. If
		// any of the existing element already has a PID which matches with the
		// PID with the new element then it would show an error modal
		var isFoundExistingElementWithSamePid;
		_.each(elementsContainerTemp, function(c) {
			if (c.name === comp.name) {
				isFoundExistingElementWithSamePid = true;
			}
		});

		if (isFoundExistingElementWithSamePid) {
			top.jsniShowDuplicatePidModal(name);
			return;
		}

		elementsContainerTemp.push(comp);

		// Setup allowed ports based on type
		if (comp.type === 'both') {
			inputPorts = [ '' ];
			outputPorts = [ '' ];
		} else if (comp.type === 'producer') {
			inputPorts = [];
			outputPorts = [ '' ];
		} else {
			inputPorts = [ '' ];
			outputPorts = [];
		}

		attrib = {
			'.label' : {
				text : joint.util.breakText(comp.name, {
					width : 100
				}),
			}
		};

		var rect = new joint.shapes.devs.Atomic({
			position : {
				x : xPos,
				y : yPos
			},
			attrs : attrib,
			inPorts : inputPorts,
			outPorts : outputPorts,
			label : comp.name,
			factoryPid : comp.fPid,
			pid : comp.pid,
			cType : comp.type,
			driver : comp.driver
		});

		graph.addCells([ rect ]);

		/* rounded corners */
		rect.attr({
			'.body' : {
				'rx' : 6,
				'ry' : 6
			}
		});

		rect.on('change:position', function() {
			top.jsniMakeUiDirty();
		})

		/* custom highlighting for ports */
		/*
		 * Custom Highlighting doesn't work efficiently for logical blocks as
		 * the custom highlighting functionality in jointJS creates an overlay
		 * element on top of the selected one. So, such extra element on the
		 * paper which makes us feel that the item is selected is not what we
		 * need. As it creates a problem while dragging the item after select.
		 * The newly created element which is used for highlighting remains at
		 * the same position but the element can be dragged anywhere.
		 */
		var portHighlighter = V('circle', {
			'r' : 14,
			'stroke' : '#ff7e5d',
			'stroke-width' : '6px',
			'fill' : 'transparent',
			'pointer-events' : 'none'
		});

		paper.off('cell:highlight cell:unhighlight').on({
			'cell:highlight' : function(cellView, el, opt) {
				var bbox = V(el).bbox(false, paper.viewport);

				if (opt.connecting) {
					portHighlighter.attr(bbox);
					portHighlighter.translate(bbox.x + 10, bbox.y + 10, {
						absolute : true
					});
					V(paper.viewport).append(portHighlighter);
				}
			},

			'cell:unhighlight' : function(cellView, el, opt) {

				if (opt.connecting) {
					portHighlighter.remove();
				}
			}
		});

		xPos = xPos + 212;
		if (xPos > 500) {
			xPos = 300;
			yPos = 300;
		}

		return rect.attributes.id;
	}

	/*
	 * / Event Functions
	 */
	function saveConfig() {
		newConfig = {
			jointJs : graph,
			deleteCells : delCells
		}
		elementsContainerTemp = [];
		delCells = [];
		if (!checkForCycleExistence()) {
			top.jsniUpdateWireConfig(JSON.stringify(newConfig));
		}
	}

	function createWire(link) {
		if ((typeof link.attributes.source.id != 'undefined')
				&& (typeof link.attributes.target.id != 'undefined')) {
			link.set("producer", link.attributes.source.id);
			link.set("consumer", link.attributes.target.id);
			link.set("newWire", true);
		}
	}

	function deleteComponent() {
		if (selectedElement !== ""
				&& selectedElement.attributes.type === 'devs.Atomic') {
			selectedElement.remove();
			top.jsniUpdateSelection("", "");
		}
	}

	function deleteGraph() {
		_.each(graph.getElements(), function(c) {
			c.remove();
		});
	}

	function regiterFormInputFieldValidation() {
		$("#componentName").alphanum({
			allowSpace : true,
			allowUpper : true,
			allowOtherCharSets : false,
			allowLower : true,
			allowNumeric : true,
			allowNewline : false,
			maxLength : 25,
			forceUpper : false,
			forceLower : false,
			allowLatin : true
		});
	}

	function createNewComponent() {
		var newComp;
		// Determine whether component can be producer, consumer, or both
		fPid = $("#factoryPid").val();
		driverPid = $("#driverPids").val();
		name = $("#componentName").val();

		// validate all the existing elements' PIDs with the new element PID. If
		// any of the existing element already has a PID which matches with the
		// PID with the new element then it would show an error modal
		var isFoundExistingElementWithSamePid;
		_.each(graph.getElements(), function(c) {
			if (c.attributes.pid === name) {
				isFoundExistingElementWithSamePid = true;
			}
		});

		if (isFoundExistingElementWithSamePid) {
			top.jsniShowDuplicatePidModal(name);
			return;
		}

		if ($.inArray(fPid, clientConfig.pFactories) !== -1
				&& $.inArray(fPid, clientConfig.cFactories) !== -1) {
			cType = "both";
		} else if ($.inArray(fPid, clientConfig.pFactories) !== -1) {
			cType = "producer";
		} else {
			cType = "consumer";
		}
		if (name !== '') {
			if (fPid === "org.eclipse.kura.wire.WireAsset") {
				if (driverPid === "--- Select Driver ---") {
					return;
				}
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					driver : driverPid,
					type : cType
				}
			} else {
				newComp = {
					fPid : fPid,
					pid : "none",
					name : name,
					type : cType
				}
			}
			// Create the new component and store information in array
			createComponent(newComp);
			$("#componentName").val('');
			$("#driverPids").val('--- Select Driver ---');
			$("#factoryPid").val('');
			$("#asset-comp-modal").modal('hide');
		}
	}

	function removeCell(cell) {
		// If the cell only exists in JointJS, no need to delete from
		// OSGi framework. For components this is determined by the
		// PID equalling 'none'. For wires, the neWire value will be true.

		// Delete Wire
		if (cell.attributes.type === 'customLink.Element'
				&& !cell.attributes.newWire) {
			delCells.push({
				cellType : 'wire',
				p : cell.attributes.producer,
				c : cell.attributes.consumer
			});
		} else if (cell.attributes.type === 'devs.Atomic'
				&& cell.attributes.pid !== 'none') {
			delCells.push({
				cellType : 'instance',
				pid : cell.attributes.pid
			});
		}
	}

	/*
	 * / Setup Custom Elements
	 */
	function setupElements() {

		joint.shapes.customLink = {};
		joint.shapes.customLink.Element = joint.dia.Link.extend({
			defaults : joint.util.deepSupplement({
				type : 'customLink.Element',
				router : {
					name : 'metro',
					args : {
						startDirections : [ 'right' ],
						endDirections : [ 'left' ]
					}
				},
				connector : {
					name : 'rounded'
				},
				attrs : {
					'.connection' : {
						'stroke' : "#4b4f6a",
						'stroke-width' : 4
					},
					'.marker-target' : {
						d : 'M 10 0 L 0 5 L 10 10 z'
					},
				},
				producer : 'producer',
				consumer : 'consumer',
				newWire : false
			}, joint.dia.Link.prototype.defaults)
		});
	}

	return client;

}());