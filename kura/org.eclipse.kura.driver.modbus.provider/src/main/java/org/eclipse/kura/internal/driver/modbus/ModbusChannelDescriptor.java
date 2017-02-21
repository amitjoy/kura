/**
 * Copyright (c) 2016, 2017 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *   Eurotech
 *   Amit Kumar Mondal
 */
package org.eclipse.kura.internal.driver.modbus;

import java.util.List;

import org.eclipse.kura.core.configuration.metatype.Tad;
import org.eclipse.kura.core.configuration.metatype.Toption;
import org.eclipse.kura.core.configuration.metatype.Tscalar;
import org.eclipse.kura.driver.ChannelDescriptor;
import org.eclipse.kura.driver.modbus.localization.ModbusDriverMessages;
import org.eclipse.kura.localization.LocalizationAdapter;
import org.eclipse.kura.util.collection.CollectionUtil;

/**
 * Modbus specific channel descriptor. The descriptor contains the following
 * attribute definition identifiers.
 *
 * <ul>
 * <li>unit.id</li> denotes the Unit to connect to
 * <li>primary.table</li> denotes the primary address space : COILS,
 * DISCRETE_INPUTS, INPUT_REGISTERS, HOLDING_REGISTERS (string representation)
 * <li>memory.address</li> denotes the memory address to perform operation (in
 * integer format)
 * </ul>
 */
public final class ModbusChannelDescriptor implements ChannelDescriptor {

    /** Localization Resource. */
    private static final ModbusDriverMessages message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

    /** {@inheritDoc} */
    @Override
    public Object getDescriptor() {
        final List<Tad> elements = CollectionUtil.newArrayList();

        final Tad unitId = new Tad();
        unitId.setId(message.unitId());
        unitId.setName(message.unitId());
        unitId.setType(Tscalar.INTEGER);
        unitId.setDefault("1");
        unitId.setMin("1");
        unitId.setMax("247");
        unitId.setDescription(message.unitIdDesc());
        unitId.setCardinality(0);
        unitId.setRequired(true);

        elements.add(unitId);

        final Tad primaryTable = new Tad();
        primaryTable.setName(message.primaryTable());
        primaryTable.setId(message.primaryTable());
        primaryTable.setDescription(message.primaryTableDesc());
        primaryTable.setDefault(message.holdingRegs());
        primaryTable.setType(Tscalar.STRING);
        primaryTable.setRequired(true);

        final Toption coil = new Toption();
        coil.setValue(message.coils());
        coil.setLabel(message.coils());
        primaryTable.getOption().add(coil);

        final Toption discreteInput = new Toption();
        discreteInput.setValue(message.discreteInputs());
        discreteInput.setLabel(message.discreteInputs());
        primaryTable.getOption().add(discreteInput);

        final Toption inputRegister = new Toption();
        inputRegister.setValue(message.inputRegs());
        inputRegister.setLabel(message.inputRegs());
        primaryTable.getOption().add(inputRegister);

        final Toption holdingRegister = new Toption();
        holdingRegister.setValue(message.holdingRegs());
        holdingRegister.setLabel(message.holdingRegs());
        primaryTable.getOption().add(holdingRegister);

        elements.add(primaryTable);

        final Tad address = new Tad();
        address.setName(message.memoryAddr());
        address.setId(message.memoryAddr());
        address.setDescription(message.memoryAddrDesc());
        address.setType(Tscalar.INTEGER);
        address.setRequired(true);
        address.setDefault("1");
        address.setMin("1");
        address.setMax("65536");

        elements.add(address);

        return elements;
    }

}
