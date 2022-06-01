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
package nl.mpi.tla.flat.deposit.action;

import nl.mpi.tla.flat.deposit.action.encryption.EncryptionService;
import nl.mpi.tla.flat.deposit.Context;
import nl.mpi.tla.flat.deposit.DepositException;
import nl.mpi.tla.flat.deposit.util.Saxon;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ListIterator;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.StringWriter;
import java.io.PrintWriter;

import net.sf.saxon.s9api.XdmItem;

/**
 * Doorkeeper action to encrypt resources marked for ingestion
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class EncryptResources extends AbstractAction {

    /**
     * Logger instance
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(EncryptResources.class.getName());

    /**
     * Encrypting resources inside context
     *
     * @param Context context
     *
     * @return boolean
     * @throws DepositException
     */
    @Override
    public boolean perform(Context context) throws DepositException {

        logger.info("STARTING ENCRYPTION ACTION");

        String encryptionFilesParam     = this.getParameter("encryption_files", "./encryption");
        String encryptionMetadataParam  = this.getParameter("encryption_metadata", "./metadata/flat_encryption.json");
        String vaultServiceAddressParam = this.getParameter("vault_service_address", "http://vault:8200");
        String authServiceAddressParam  = this.getParameter("auth_service_address", "http://vodapi:3003/auth");

        logger.info("FLAT ENCRYPTION PARAMS : ENCRYPTION file dir: " + encryptionFilesParam + ", metadata: " + encryptionMetadataParam + ", vault service address: " + vaultServiceAddressParam + ", auth service address: " + authServiceAddressParam);

        try {

            EncryptionService encryptionService = new EncryptionService(encryptionFilesParam, encryptionMetadataParam, vaultServiceAddressParam, authServiceAddressParam);
            encryptionService.encrypt(context, this);

            logger.info("FINISHED ENCRYPTION ACTION");

        } catch (Exception e) {

            logger.info("ENCRYPTION ACTION FAILED");
            logger.info(e.toString());

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            logger.info(sw.toString());

            if (!(e instanceof DepositException)) {

                // if not deposit exception, throw again to ensure stopping the archiving process
                // and trigger the rollback
                throw new DepositException(e);
            }
        }

        return true;
    }

    @Override
    public void rollback(Context context, List<XdmItem> events) {

        logger.info("[rollback.EncryptResources] rollback started");
        for (ListIterator<XdmItem> iter = events.listIterator(events.size());iter.hasPrevious();) {

            XdmItem event = iter.previous();

            try {

                String type = Saxon.xpath2string(event, "@type");

                logger.info("[rollback.EncryptResources] rollback event type=" + type);

                if (type.equals("encryption.restore.original")) {

                    logger.info("[rollback.EncryptResources] rollback event encryption.restore.original detected and running");

                    Path keyFile = Paths.get(Saxon.xpath2string(event, "param[@name='key']/@value"));
                    Path originalFile = Paths.get(Saxon.xpath2string(event, "param[@name='original']/@value"));
                    Path backupFile = Paths.get(Saxon.xpath2string(event, "param[@name='backup']/@value"));
                    Path encryptionFolder = Paths.get(keyFile.toFile().getParentFile().getAbsolutePath());

                    logger.info("[rollback.EncryptResources] encryption folder = " + encryptionFolder);
                    logger.info("[rollback.EncryptResources] keyFile = " + keyFile.toString() + ", exists = " + keyFile.toFile().exists());
                    logger.info("[rollback.EncryptResources] originalFile = " + originalFile.toString() + ", exists = " + originalFile.toFile().exists());
                    logger.info("[rollback.EncryptResources] backupFile = " + backupFile.toString() + ", exists = " + backupFile.toFile().exists());

                    if (keyFile.toFile().exists() && originalFile.toFile().exists() && backupFile.toFile().exists()) {

                        logger.info("[rollback.EncryptResources] rollback action[" + this.getName() + "] event[" + type + "] removing key [" + keyFile.toString() + "], encrypted file [" + originalFile.toString() + "] and restoring original file [" + backupFile.toString() + "]");

                        // clearing all the encryption files
                        Files.deleteIfExists(keyFile);
                        Files.deleteIfExists(originalFile);
                        Files.move(backupFile, originalFile);

                        try {
                            Files.deleteIfExists(encryptionFolder);
                        } catch (DirectoryNotEmptyException e) {

                            logger.info("[rollback.EncryptResources] ERRROR encryptionFolder could not be deleted", e);
                            logger.error("rollback action[" + this.getName() + "] event[" + type + "] encryption folder [" + encryptionFolder.toString() + "] is not empty, skipping deletion");
                        }

                    } else {

                        logger.info("[rollback.EncryptResources] one of the files mentioned above could not be found, check the exists flag next to filename");
                        logger.error("rollback action[" + this.getName() + "] event[" + type + "] failed removing key [" + keyFile.toString() + "], encrypted file [" + originalFile.toString() + "] and restoring original file [" + backupFile.toString() + "]!");
                    }

                } else {

                    logger.info("[rollback.EncryptResources] rollback event type=' + type + '] is not supported");
                    logger.error("rollback action[" + this.getName() + "] rollback unknown event[" + type +"]!");
                }

            } catch (Exception ex) {

                logger.info("[rollback.EncryptResources] rollback failed because of exception: ", ex);

                StringWriter sw = new StringWriter();
                PrintWriter  pw = new PrintWriter(sw);

                ex.printStackTrace(pw);
                logger.info("[rollback.EncryptResources] stacktrace: \n\n " + sw.toString());

                logger.error("rollback action[" + this.getName() + "] event[" + event + "] failed!", ex);
            }
        }
    }
}

