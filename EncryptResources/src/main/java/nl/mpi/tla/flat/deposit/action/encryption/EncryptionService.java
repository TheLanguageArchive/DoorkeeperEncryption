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

import nl.mpi.tla.flat.deposit.action.encryption.EncryptionService;
import nl.mpi.tla.flat.deposit.action.encryption.ResourceService;

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
import java.nio.file.StandardCopyOption;
import java.nio.file.DirectoryNotEmptyException;
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
    private Path encryptionFiles;
    private String vaultServiceAddress;
    private String authServiceAddress;

    /**
     * Logger instance
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EncryptionService.class.getName());

    /**
     * Constructor
     *
     * @param String encryptionFilesParam
     * @param String encryptionMetaDataParam
     * @param String vaultServiceAddressParam
     * @param String authServiceAddressParam
     *
     * @return EncryptionService
     * @throws DepositException
     */
    public EncryptionService(String encryptionFilesParam, String encryptionMetadataParam, String vaultServiceAddressParam, String authServiceAddressParam) throws DepositException {

        logger.info("CREATING ENCRYPTION SERVICE - encryptionFiles: " + encryptionFilesParam + " encryptionMetadata: " + encryptionMetadataParam + " vaultServiceAddress: " + vaultServiceAddressParam + " authServiceAddress: " + authServiceAddressParam);

        this.filesMarkedForEncryption = this.getFilesMarkedForEncryption(encryptionMetadataParam);
        this.encryptionFiles          = ResourceService.getEncryptionFilesDir(encryptionFilesParam);
        this.vaultServiceAddress      = vaultServiceAddressParam;
        this.authServiceAddress       = authServiceAddressParam;
        this.manager                  = this.getManager();
    }

    /**
     * Encrypt files marked for encryption through doorkeeper
     *
     * @param Context context
     * @param ActionInterface action
     *
     * @return void
     * @throws DepositException, IOException
     */
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


                // if encryption folder doesn't exists, create it
                // folder used to save the backup file and keyset
                if (!Files.exists(this.encryptionFiles)) {
                    Files.createDirectory(this.encryptionFiles);
                }

                Path keyFile = Paths.get(this.encryptionFiles.toString(), inputFile.getFileName().toString() + ".keyset.json");
                Path outputFile = Paths.get(inputFile.toString() + ".enc");

                this.encryptFile(keyFile.toFile(), inputFile.toFile(), outputFile.toFile());

                Path encryptedFile = outputFile;
                Path originalFile = inputFile;
                Path backupFile = Paths.get(this.encryptionFiles.toString(), originalFile.getFileName().toString() + ".orig");

                // saving original resource to allow for rollback to revert to original if something goes wrong
                context.registerRollbackEvent(action, "encryption.restore.original", "key", keyFile.toString(), "original", originalFile.toString(), "backup", backupFile.toString());

                Files.copy(originalFile, backupFile, StandardCopyOption.COPY_ATTRIBUTES);

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

    /**
     * When encrypting files through doorkeeper, we also create a backup of the original file.
     * This action will clear these backups when successful and restore the original file when doorkeeper fails.
     *
     * @param Context context
     * @param ActionInterface action
     *
     * @return void
     * @throws DepositException, IOException
     */
    public void cleanup(Context context, ActionInterface action) throws DepositException, IOException {

        Set<Resource> resources = ResourceService.fetchAll(context);

        for (Resource res : resources) {

            Path inputFile = res.getPath();

            if (!this.filesMarkedForEncryption.isMarked(inputFile.toFile())) {

                // file isn't marked for encryption
                // no cleanup necessary
                continue;
            }

            try {

                Path originalFile = inputFile;
                Path backupFile = Paths.get(this.encryptionFiles.toString(), originalFile.getFileName().toString() + ".orig");
                Path keyFile = Paths.get(this.encryptionFiles.toString(), originalFile.getFileName().toString() + ".keyset.json");
                Path keyFileDestination = Paths.get(originalFile.toString() + ".keyset.json");

                // cleaning up original file
                Files.deleteIfExists(backupFile);

                // move keyset to location of resource
                Files.move(keyFile, keyFileDestination);

                // delete encryption folder
                try {
                    Files.deleteIfExists(this.encryptionFiles);
                } catch (DirectoryNotEmptyException e) {
                    logger.error("Could not delete encryption folder: " + this.encryptionFiles.toString());
                }

            } catch (NoSuchFileException e) {}

        }
    }

    /**
     * Encrypt a file
     *
     * @param File keyFile
     * @param File inputFile
     * @param File outputFile
     *
     * @return void
     * @throws DepositException
     */
    private void encryptFile(File keyFile, File inputFile, File outputFile) throws DepositException {

        try {

            logger.info("Encrypting file " + inputFile.getName());
            this.manager.encrypt(keyFile, inputFile, outputFile);

        } catch (GeneralSecurityException | IOException e) {

            logger.info("Could not encrypt file " + inputFile.getName());
            logger.info("ERR: " + e.toString());

            throw new DepositException("Could not encrypt file " + inputFile.getName(), e);
        }
    }

    /**
     * StreamingManager Factory
     *
     * @return StreamingManager
     * @throws DepositException
     */
    private StreamingManager getManager() throws DepositException {

        try {

            logger.info("Connection encryption manager for KEK: " + this.kekUri);
            return new StreamingManager(this.kekUri, this.vaultServiceAddress, this.authServiceAddress);

        } catch (GeneralSecurityException e) {

            logger.info("Failed to connect to encryption manager for KEK: " + this.kekUri);
            logger.info(e.toString());

            throw new DepositException("Could not connect to encryption manager");
        }
    }

    /**
     * getting files marked for encryption, if no files are marked, an empty FilesMarked instance is returned
     *
     * @param String encryptionParam
     *
     * @return FilesMarked
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
