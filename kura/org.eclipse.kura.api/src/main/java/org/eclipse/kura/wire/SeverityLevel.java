/*******************************************************************************
 * Copyright (c) 2017 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Amit Kumar Mondal
 *
 *******************************************************************************/
package org.eclipse.kura.wire;

/**
 * This {@link SeverityLevel} interface is essentially used to incorporate extensible
 * design of the enumeration types that can be associated with {@link WireField}s.
 *
 * @see Severity
 * @see WireField
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 */
@FunctionalInterface
public interface SeverityLevel {

    /**
     * Returns the name of this enumeration constant, exactly as declared in its
     * enumeration declaration.
     *
     * @return the name of this enumeration constant
     */
    public String name();

}
