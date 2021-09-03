/*
 * Copyright (C) 2021 The Language Archive
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.mpi.tla.flat.deposit.action.encryption;

import nl.mpi.tla.flat.deposit.Context;
import nl.mpi.tla.flat.deposit.DepositException;
import nl.mpi.tla.flat.deposit.sip.Resource;
import nl.mpi.tla.flat.deposit.sip.SIPInterface;

import org.slf4j.LoggerFactory;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

/**
 * Service for easy manipulation of resources
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class ResourceService {

    /**
     * Fetch all the resources inside context
     */
    public static Set<Resource> fetchAll(Context context) throws DepositException {

        SIPInterface sip = context.getSIP();
        Set<Resource> resources = sip
            .getResources()
            .stream()
            .filter(ResourceService::allowed)
            .collect(Collectors.toSet());

        return resources;
    }

    /**
     * Check whether resource is allowed to be encrypted (ie @see{Resource.Status} is INSERT or UPDATE)
     */
    public static boolean allowed(Resource resource) {

        return resource.getStatus() == Resource.Status.INSERT ||
               resource.getStatus() == Resource.Status.UPDATE;
    }

    /**
     * transforming outputDir param in flat-deposit.xml into a usable Path object
     */
    public static Path getOutputDir(String outputDirParam) {

        return Paths
            .get(outputDirParam)
            .toFile()
            .getAbsoluteFile()
            .toPath();
    }

    /**
     * Create output directory, return true if created or false if dir already exists
     */
    public static boolean createOutputDir(Path outputDir) {

        try {

            if (Files.exists(outputDir)) {
                return false;
            }

            Files.createDirectories(outputDir);

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * transforming credentials param in flat-deposit.xml into a usable Path object
     */
    public static Path getCredentials(String credentialsParam) {

        return Paths
            .get(credentialsParam)
            .toFile()
            .getAbsoluteFile()
            .toPath();
    }
}
