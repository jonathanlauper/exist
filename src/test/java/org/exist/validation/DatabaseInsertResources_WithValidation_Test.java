/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 20011 The eXist-db Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * $Id: DatabaseInsertResources_NoValidation_Test.java 5986 2007-06-03 15:39:39Z dizzzz $
 */
package org.exist.validation;

import java.io.IOException;
import java.util.Optional;

import org.exist.TestUtils;
import org.exist.collections.Collection;
import org.exist.storage.BrokerPool;
import org.exist.storage.DBBroker;
import org.exist.storage.txn.TransactionManager;
import org.exist.storage.txn.Txn;
import org.exist.test.ExistEmbeddedServer;
import org.exist.util.XMLReaderObjectFactory;
import org.exist.xmldb.XmldbURI;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import static org.exist.util.PropertiesBuilder.propertiesBuilder;

/**
 *  Insert documents for validation tests.
 *
 * @author Dannes Wessels (dizzzz@exist-db.org)
 */
public class DatabaseInsertResources_WithValidation_Test {

    private final static String TEST_COLLECTION = "testValidationInsert";

    private final static String ADMIN_UID = "admin";
    private final static String ADMIN_PWD = "";

    private final static String GUEST_UID = "guest";

    private final static String VALIDATION_HOME_COLLECTION_URI = "/db/" + TEST_COLLECTION + "/" + TestTools.VALIDATION_HOME_COLLECTION;
    
    /**
     * Test for inserting hamlet.xml, while validating using default registered
     * DTD set in system catalog.
     *
     * First the string
     *     <!--!DOCTYPE PLAY PUBLIC "-//PLAY//EN" "play.dtd"-->
     * needs to be modified into
     *     <!DOCTYPE PLAY PUBLIC "-//PLAY//EN" "play.dtd">
     */
    @Test
    public void testValidDocumentSystemCatalog() throws IOException{

        String hamletWithValid = new String(TestUtils.readHamletSampleXml());
        hamletWithValid=hamletWithValid.replaceAll("\\Q<!\\E.*DOCTYPE.*\\Q-->\\E",
            "<!DOCTYPE PLAY PUBLIC \"-//PLAY//EN\" \"play.dtd\">" );

        TestTools.insertDocumentToURL(
                hamletWithValid.getBytes(),
                "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_TMP_COLLECTION + "/hamlet_valid.xml"
        );
    }

    /**
     * Test for inserting hamlet.xml, while validating using default registered
     * DTD set in system catalog.
     *
     * First the string
     *     <!--!DOCTYPE PLAY PUBLIC "-//PLAY//EN" "play.dtd"-->
     * needs to be modified into
     *     <!DOCTYPE PLAY PUBLIC "-//PLAY//EN" "play.dtd">
     *
     * Aditionally all "TITLE" elements are renamed to "INVALIDTITLE"
     */
    @Test
    public void invalidDocumentSystemCatalog() throws IOException{

        String hamletWithInvalid = new String(TestUtils.readHamletSampleXml());
        hamletWithInvalid = hamletWithInvalid.replaceAll("\\Q<!\\E.*DOCTYPE.*\\Q-->\\E",
            "<!DOCTYPE PLAY PUBLIC \"-//PLAY//EN\" \"play.dtd\">" );

        hamletWithInvalid = hamletWithInvalid.replaceAll("TITLE", "INVALIDTITLE" );

        try {
            TestTools.insertDocumentToURL(
                hamletWithInvalid.getBytes(),
                "xmldb:exist://" + VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_TMP_COLLECTION + "/hamlet_invalid.xml"
                
            );
        } catch(IOException ioe) {
            //TODO consider how to get better error handling than matching on exception strings!
            if(!ioe.getCause().getMessage().matches(".*Element type \"INVALIDTITLE\" must be declared.*")){
                throw ioe;
            }
        }
    }

    @ClassRule
    public static final ExistEmbeddedServer existEmbeddedServer = new ExistEmbeddedServer(
        propertiesBuilder()
            .set(XMLReaderObjectFactory.PROPERTY_VALIDATION_MODE, "auto")
            .build(),
        true,
        false);

    @BeforeClass
    public static void startup() throws Exception {
        createTestCollections();
    }

    @AfterClass
    public static void shutdown() throws Exception {
        removeTestCollections();
    }

    private static void createTestCollections() throws Exception {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate(ADMIN_UID, ADMIN_PWD)));
            final Txn txn = transact.beginTransaction()) {


            /** create nessecary collections if they dont exist */
            Collection testCollection = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI));
            testCollection.getPermissions().setOwner(GUEST_UID);
            broker.saveCollection(txn, testCollection);

            Collection col = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI + "/" + TestTools.VALIDATION_TMP_COLLECTION));
            col.getPermissions().setOwner(GUEST_UID);
            broker.saveCollection(txn, col);

            transact.commit(txn);
        }
    }

    private static void removeTestCollections() throws Exception {
        final BrokerPool pool = existEmbeddedServer.getBrokerPool();
        final TransactionManager transact = pool.getTransactionManager();

        try(final DBBroker broker = pool.get(Optional.of(pool.getSecurityManager().authenticate(ADMIN_UID, ADMIN_PWD)));
            final Txn txn = transact.beginTransaction()) {

            Collection testCollection = broker.getOrCreateCollection(txn, XmldbURI.create(VALIDATION_HOME_COLLECTION_URI));
            broker.removeCollection(txn, testCollection);

            transact.commit(txn);
        }
    }
}
