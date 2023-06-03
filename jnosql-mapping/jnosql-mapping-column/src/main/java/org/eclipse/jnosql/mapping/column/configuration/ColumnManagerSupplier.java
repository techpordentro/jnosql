/*
 *  Copyright (c) 2022 Contributors to the Eclipse Foundation
 *   All rights reserved. This program and the accompanying materials
 *   are made available under the terms of the Eclipse Public License v1.0
 *   and Apache License v2.0 which accompanies this distribution.
 *   The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *   and the Apache License v2.0 is available at http://www.opensource.org/licenses/apache2.0.php.
 *
 *   You may elect to redistribute this code under either of these licenses.
 *
 *   Contributors:
 *
 *   Otavio Santana
 */
package org.eclipse.jnosql.mapping.column.configuration;

import jakarta.data.exceptions.MappingException;
import org.eclipse.jnosql.communication.Settings;
import org.eclipse.jnosql.communication.column.ColumnConfiguration;
import org.eclipse.jnosql.communication.column.ColumnManager;
import org.eclipse.jnosql.communication.column.ColumnManagerFactory;
import org.eclipse.jnosql.mapping.config.MicroProfileSettings;
import org.eclipse.jnosql.mapping.reflection.Reflections;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.jnosql.mapping.config.MappingConfigurations.COLUMN_DATABASE;
import static org.eclipse.jnosql.mapping.config.MappingConfigurations.COLUMN_PROVIDER;

@ApplicationScoped
class ColumnManagerSupplier implements Supplier<ColumnManager> {

    private static final Logger LOGGER = Logger.getLogger(ColumnManagerSupplier.class.getName());

    @Override
    @Produces
    @ApplicationScoped
    public ColumnManager get() {
        Settings settings = MicroProfileSettings.INSTANCE;

        ColumnConfiguration configuration = settings.get(COLUMN_PROVIDER, Class.class)
                .filter(ColumnConfiguration.class::isAssignableFrom)
                .map(c -> {
                    final Reflections reflections = CDI.current().select(Reflections.class).get();
                    return (ColumnConfiguration) reflections.newInstance(c);
                }).orElseGet(ColumnConfiguration::getConfiguration);

        ColumnManagerFactory managerFactory = configuration.apply(settings);

        Optional<String> database = settings.get(COLUMN_DATABASE, String.class);
        String db = database.orElseThrow(() -> new MappingException("Please, inform the database filling up the property "
                + COLUMN_DATABASE.get()));
        ColumnManager manager = managerFactory.apply(db);

        LOGGER.log(Level.FINEST, "Starting  a ColumnManager instance using Eclipse MicroProfile Config," +
                " database name: " + db);
        return manager;
    }

    public void close(@Disposes ColumnManager manager) {
        LOGGER.log(Level.FINEST, "Closing ColumnManager resource, database name: " + manager.name());
        manager.close();
    }
}
