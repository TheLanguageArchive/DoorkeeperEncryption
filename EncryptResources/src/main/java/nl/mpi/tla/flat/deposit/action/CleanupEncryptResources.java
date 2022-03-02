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
     */
    @Override
    public boolean perform(Context context) throws DepositException {

        Boolean status = context.getFlow().getStatus();

        if (status == null || !status.booleanValue()) {

            // the archiving was not successful, so no cleanup is needed
            return true;
        }

        logger.info("STARTING CLEANUP ENCRYPTION ACTION");

        String encryptionFilesParam       = this.getParameter("encryption_files", "./encryption");
        String encryptionMetadataParam    = this.getParameter("encryption_metadata", "./metadata/flat_encryption.json");
        String encryptionCredentialsParam = this.getParameter("encryption_credentials");

        try {

            EncryptionService encryptionService = new EncryptionService(encryptionFilesParam, encryptionMetadataParam, encryptionCredentialsParam);
            encryptionService.cleanup(context, this);

            logger.info("FINISHED CLEANUP ENCRYPTION ACTION");

        } catch (Exception e) {

            logger.info("CLEANUP ENCRYPTION ACTION FAILED");
            logger.info(e.toString());
        }

        return true;
    }
}
