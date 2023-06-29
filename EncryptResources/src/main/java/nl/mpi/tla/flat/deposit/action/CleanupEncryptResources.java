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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.StringWriter;
import java.io.PrintWriter;

import net.sf.saxon.s9api.XdmItem;

/**
 * Doorkeeper action to cleanup backup original resources marked for encryption
 *
 * @author  Ibrahim Abdullah <ibrahim.abdullah@mpi.nl>
 * @package Doorkeeper
 */
public class CleanupEncryptResources extends AbstractAction {

    /**
     * Logger instance
     */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CleanupEncryptResources.class.getName());

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

        logger.info("[CleanupEncryptResources] Cleaning up encrypted resources - started");
        Boolean status = context.getFlow().getStatus();

        logger.info("[CleanupEncryptResources] FLOW STATUS = " + status + ", toString = " + (status == null ? "null" : status.toString()));

        String encryptionFilesParam     = this.getParameter("encryption_files", "./encryption");
        String encryptionMetadataParam  = this.getParameter("encryption_metadata", "./metadata/flat_encryption.json");
        String vaultServiceAddressParam = this.getParameter("vault_service_address", "http://vault:8200");
        String authServiceAddressParam  = this.getParameter("auth_service_address", "http://vodapi:3003/auth");

        logger.info("STARTING CLEANUP ENCRYPTION ACTION - encryptionFilesParam: " + encryptionFilesParam + " encryptionMetadataParam: " + encryptionMetadataParam + " vaultServiceAddressParam: " + vaultServiceAddressParam + " authServiceAddressParam" + authServiceAddressParam);

        try {

            EncryptionService encryptionService = new EncryptionService(encryptionFilesParam, encryptionMetadataParam, vaultServiceAddressParam, authServiceAddressParam);

            if (status == null || !status.booleanValue()) {

                // the archiving was not successful, so run restore
                logger.info("[CleanupEncryptResources] Archiving was not successful, run restore");
                encryptionService.restore(context, this);

            } else if (status.booleanValue() == true) {

                // the archiving was successful, clean up
                logger.info("[CleanupEncryptResources] Archiving was successful, run cleanup");
                encryptionService.cleanup(context, this);
            }

            logger.info("FINISHED CLEANUP ENCRYPTION ACTION");

        } catch (Exception e) {

            logger.info("CLEANUP ENCRYPTION ACTION FAILED");
            logger.info(e.toString());

            StringWriter sw = new StringWriter();
            PrintWriter  pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            logger.info(sw.toString());

            if (!(e instanceof DepositException)) {

                // if not deposit exception, throw again to ensure stopping the archiving process
                // and trigger the rollback
                logger.info("[CleanupEncryptResources] Exception that is not a DepositException, throwing again");
                throw new DepositException(e);
            }
        }

        return true;
    }
}
