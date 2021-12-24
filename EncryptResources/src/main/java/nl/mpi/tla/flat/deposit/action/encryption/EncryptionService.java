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

import nl.mpi.tla.flat.deposit.action.ActionInterface;
import nl.mpi.tla.flat.deposit.Context;
import nl.mpi.tla.flat.deposit.DepositException;
import nl.mpi.tla.flat.deposit.sip.Resource;
import nl.mpi.tla.encryption.StreamingManager;

import org.slf4j.LoggerFactory;

import java.util.Set;
import java.io.File;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.nio.file.Files;

import java.security.GeneralSecurityException;


/**
 * Service for easy manipulation of resources
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class EncryptionService  {

    private String kekUri = "hcvault://flat_mpi";
    private StreamingManager manager;
    private FilesMarked filesMarkedForEncryption;
    private Path credentials;

    /**
     * Logger instance
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EncryptionService.class.getName());

    public EncryptionService(String encryptionParam, String credentialsParam) throws DepositException {

        logger.info("CREATING ENCRYPTION SERVICE");

        this.filesMarkedForEncryption = this.getFilesMarkedForEncryption(encryptionParam);
        this.credentials              = this.getCredentials(credentialsParam);
        this.manager                  = this.getManager();
    }

    public void encrypt(Context context, ActionInterface action) throws DepositException, IOException {

        Set<Resource> resources = ResourceService.fetchAll(context);

        for (Resource res : resources) {

            Path inputFile = res.getPath();

            logger.info("SHOULD FILE: " + inputFile + " BE ENCRYPTED?");

            if (!this.filesMarkedForEncryption.isMarked(inputFile.toFile())) {

                logger.info("FILE: " + inputFile + " SHOULD NOT BE ENCRYPTED");
                continue;
            }

            logger.info("ENCRYPTING FILE: " + inputFile);

            try {

                Path keyFile = Paths.get(inputFile.toFile().getAbsolutePath() + ".keyset.json");
                Path outputFile = Paths.get(inputFile.toFile().getAbsolutePath() + ".enc");

                this.encryptFile(keyFile.toFile(), inputFile.toFile(), outputFile.toFile());

                Path encryptedFile = outputFile;
                Path originalFile = inputFile;
                Path backupFile = Paths.get(originalFile.toFile().getAbsolutePath() + ".orig");

                // saving original resource to allow for rollback to revert to original if something goes wrong
                context.registerRollbackEvent(action, "encryption.restore.original", "key", keyFile.toString(), "original", originalFile.toString(), "backup", backupFile.toString());

                Files.copy(originalFile, backupFile);

                // replacing original resource with encrypted one
                Files.deleteIfExists(originalFile);
                Files.move(encryptedFile, originalFile);

                logger.info("ENCRYPTED FILE: " + originalFile + ", KEY FILE: " + keyFile);
                logger.info("FILE WAS SUCCESSFULLY ENCRYPTED: " + outputFile);

                // cleaning up by removing encrypted output file
                Files.deleteIfExists(outputFile);

            } catch (NoSuchFileException e) {}

        }
    }

    public void encryptFile(File keyFile, File inputFile, File outputFile) {

        try {

            logger.info("Encrypting file " + inputFile.getName());
            this.manager.encrypt(keyFile, inputFile, outputFile);

        } catch (GeneralSecurityException | IOException e) {

            logger.info("Could not encrypt file " + inputFile.getName());
            logger.info("ERR: " + e.toString());
        }
    }

    private StreamingManager getManager() throws DepositException {

        try {

            logger.info("Connection encryption manager for KEK: " + this.kekUri);
            return new StreamingManager(this.kekUri, this.credentials.toString());

        } catch (GeneralSecurityException e) {

            logger.info("Failed to connect to encryption manager for KEK: " + this.kekUri);
            logger.info(e.toString());

            throw new DepositException("Could not connect to encryption manager");
        }
    }

    /**
     * Get credentials
     */
    private Path getCredentials(String credentialsParam) throws DepositException {

        Path credentials = ResourceService.getCredentials(credentialsParam);
        logger.info("ENCRYPTION CREDENTIALS PARAMETER: " + credentialsParam);

        return credentials;
    }

    /**
     * getting files marked for encryption
     */
    private FilesMarked getFilesMarkedForEncryption(String encryptionParam) {

        logger.info("GETTING FILES MARKED FOR ENCRYPTION");

        try {
            return ResourceService.getFilesMarkedForEncryption(encryptionParam);
        } catch (IOException e) {
            logger.info("NO FILES MARKED FOR ENCRYPTION");
        }

        return new FilesMarked();
    }
}
