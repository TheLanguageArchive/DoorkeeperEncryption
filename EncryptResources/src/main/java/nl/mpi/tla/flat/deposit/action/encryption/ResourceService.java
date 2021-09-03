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
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.LoggerFactory;

/**
 * Service for easy manipulation of resources
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class ResourceService {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ResourceService.class.getName());
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
    public static Path getResourcesDir(String resourcesDirParam) {

        return Paths
            .get(resourcesDirParam)
            .toFile()
            .getAbsoluteFile()
            .toPath();
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

    public static FilesMarked getFilesMarkedForEncryption(String encryptionParam) throws IOException {

        byte[] encoded = Files.readAllBytes(Paths.get(encryptionParam));
        String json    = new String(encoded, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(json, FilesMarked.class);
    }
}
