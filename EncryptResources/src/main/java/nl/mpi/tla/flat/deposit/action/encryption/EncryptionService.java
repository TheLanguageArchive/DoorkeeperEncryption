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

import nl.mpi.tla.encryption.Manager;

import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.security.GeneralSecurityException;

/**
 * Service for easy manipulation of resources
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class EncryptionService {

    private String kekUri = "hcvault://flat_mpi";
    private Manager manager;
    private Path credentials;
    private Path outputDir;

    /**
     * Logger instance
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EncryptionService.class.getName());

    public EncryptionService(String credentialsParam, String outputDirParam) throws DepositException {

        logger.debug("CREATING ENCRYPTION SERVICE");

        this.credentials = this.getCredentials(credentialsParam);
        this.outputDir   = this.createOutputDir(outputDirParam);
        this.manager     = this.getManager();
    }

    public void encrypt(Context context) throws DepositException {

        Set<Resource> resources = ResourceService.fetchAll(context);

        for (Resource res : resources) {

            logger.debug("ENCRYPTING FILE: " + res.getFile().getName());

            Path path = res.getPath();
            Path filename = path.getFileName();
            File keyFile = this.outputDir.resolve(filename + "-keyset.json").toFile();
            File outputFile = this.outputDir.resolve(filename + ".enc").toFile();

            this.encryptFile(keyFile, path.toFile(), outputFile);

            logger.debug("FILE WAS SUCCESSFULLY ENCRYPTED: " + res.getFile().getName());
        }
    }

    public void encryptFile(File keyFile, File inputFile, File outputFile) {

        try {

            logger.debug("Encrypting file " + inputFile.getName());
            this.manager.encrypt(keyFile, inputFile, outputFile);

        } catch (GeneralSecurityException | IOException e) {
            logger.debug("Could not encrypt file " + inputFile.getName());
        }
    }

    private Manager getManager() throws DepositException {

        try {

            logger.debug("Connection encryption manager for KEK: " + this.kekUri);
            return new Manager(this.kekUri, this.credentials.toString());

        } catch (GeneralSecurityException e) {

            logger.debug("Failed to connect to encryption manager for KEK: " + this.kekUri);
            logger.debug(e.toString());

            throw new DepositException("Could not connect to encryption manager");
        }
    }

    /**
     * Get credentials
     */
    private Path getCredentials(String credentialsParam) throws DepositException {

        Path credentials = ResourceService.getCredentials(credentialsParam);
        logger.debug("ENCRYPTION CREDENTIALS PARAMETER: " + credentialsParam);

        return credentials;
    }

    /**
     * Create output directory where the encrypted resources are stored
     */
    private Path createOutputDir(String outputDirParam) throws DepositException {

        Path outputDir = ResourceService.getOutputDir(outputDirParam);
        boolean created = ResourceService.createOutputDir(outputDir);

        logger.debug("ENCRYPTION OUTPUT DIR PARAMETER: " + outputDirParam);
        logger.debug("ENCRYPTION OUTPUT DIR CREATED: " + (true == created ? "YES" : "NO"));

        if (false == created) {
            throw new DepositException("ENCRYPTION OUTPUT DIR COULD NOT BE CREATED");
        }

        return outputDir;
    }
}
