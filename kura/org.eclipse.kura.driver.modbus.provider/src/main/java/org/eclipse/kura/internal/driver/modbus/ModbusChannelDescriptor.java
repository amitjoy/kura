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
    private static final ModbusDriverMessages s_message = LocalizationAdapter.adapt(ModbusDriverMessages.class);

    /** {@inheritDoc} */
    @Override
    public Object getDescriptor() {
        final List<Tad> elements = CollectionUtil.newArrayList();

        final Tad unitId = new Tad();
        unitId.setId(s_message.unitId());
        unitId.setName(s_message.unitId());
        unitId.setType(Tscalar.INTEGER);
        unitId.setDefault("1");
        unitId.setMin("1");
        unitId.setMax("247");
        unitId.setDescription(s_message.unitIdDesc());
        unitId.setCardinality(0);
        unitId.setRequired(true);

        elements.add(unitId);

        final Tad primaryTable = new Tad();
        primaryTable.setName(s_message.primaryTable());
        primaryTable.setId(s_message.primaryTable());
        primaryTable.setDescription(s_message.primaryTableDesc());
        primaryTable.setDefault(s_message.holdingRegs());
        primaryTable.setType(Tscalar.STRING);
        primaryTable.setRequired(true);

        final Toption coil = new Toption();
        coil.setValue(s_message.coils());
        coil.setLabel(s_message.coils());
        primaryTable.getOption().add(coil);

        final Toption discreteInput = new Toption();
        discreteInput.setValue(s_message.discreteInputs());
        discreteInput.setLabel(s_message.discreteInputs());
        primaryTable.getOption().add(discreteInput);

        final Toption inputRegister = new Toption();
        inputRegister.setValue(s_message.inputRegs());
        inputRegister.setLabel(s_message.inputRegs());
        primaryTable.getOption().add(inputRegister);

        final Toption holdingRegister = new Toption();
        holdingRegister.setValue(s_message.holdingRegs());
        holdingRegister.setLabel(s_message.holdingRegs());
        primaryTable.getOption().add(holdingRegister);

        elements.add(primaryTable);

        final Tad address = new Tad();
        address.setName(s_message.memoryAddr());
        address.setId(s_message.memoryAddr());
        address.setDescription(s_message.memoryAddrDesc());
        address.setType(Tscalar.INTEGER);
        address.setRequired(true);
        address.setDefault("1");
        address.setMin("1");
        address.setMax("65536");

        elements.add(address);

        return elements;
    }

}
