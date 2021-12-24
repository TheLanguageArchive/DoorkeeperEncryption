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

import nl.mpi.tla.flat.deposit.action.encryption.ResourceService;
import nl.mpi.tla.flat.deposit.action.encryption.EncryptionService;
import nl.mpi.tla.flat.deposit.Context;
import nl.mpi.tla.flat.deposit.DepositException;
import nl.mpi.tla.flat.deposit.sip.Resource;
import nl.mpi.tla.flat.deposit.sip.SIPInterface;
import nl.mpi.tla.flat.deposit.util.Saxon;
import nl.mpi.tla.encryption.hcvault.HcVaultClient;

import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import org.apache.commons.codec.digest.DigestUtils;

import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmValue;

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
     */
    @Override
    public boolean perform(Context context) throws DepositException {

        logger.info("STARTING ENCRYPTION ACTION");

        String encryptionParam = this.getParameter("encryption", "./metadata/flat_encryption.json");
        String credentialsParam  = this.getParameter("credentials");

        logger.info("FLAT ENCRYPTION PARAMS : ENCRYPTION: " + encryptionParam + ", CREDENTIALS: " + credentialsParam);

        try {

            EncryptionService encryptionService = new EncryptionService(encryptionParam, credentialsParam);
            encryptionService.encrypt(context, this);

            logger.info("FINISHED ENCRYPTION ACTION");

        } catch (Exception e) {

            logger.info("ENCRYPTION ACTION FAILED");
            logger.info(e.toString());
        }

        return true;
    }

    @Override
    public void rollback(Context context, List<XdmItem> events) {

        for (ListIterator<XdmItem> iter = events.listIterator(events.size());iter.hasPrevious();) {

            XdmItem event = iter.previous();

            try {

                String type = Saxon.xpath2string(event, "@type");

                if (type.equals("encryption.restore.original")) {

                    Path keyFile = Paths.get(Saxon.xpath2string(event, "param[@name='key']/@value"));
                    Path originalFile = Paths.get(Saxon.xpath2string(event, "param[@name='original']/@value"));
                    Path backupFile = Paths.get(Saxon.xpath2string(event, "param[@name='backup']/@value"));

                    if (keyFile.toFile().exists() && originalFile.toFile().exists() && backupFile.toFile().exists()) {

                        logger.debug("rollback action[" + this.getName() + "] event[" + type + "] removing removing key [" + keyFile.toString() + "], encrypted file [" + originalFile.toString() + "] and restoring original file [" + backupFile.toString() + "]");

                        Files.deleteIfExists(keyFile);
                        Files.deleteIfExists(originalFile);
                        Files.move(backupFile, originalFile);

                    } else {
                        logger.error("rollback action[" + this.getName() + "] event[" + type + "] failed removing key [" + keyFile.toString() + "], encrypted file [" + originalFile.toString() + "] and restoring original file [" + backupFile.toString() + "]!");
                    }

                } else {
                    logger.error("rollback action[" + this.getName() + "] rollback unknown event[" + type +"]!");
                }

            } catch (Exception ex) {
                logger.error("rollback action[" + this.getName() + "] event[" + event + "] failed!", ex);
            }
        }
    }
}
