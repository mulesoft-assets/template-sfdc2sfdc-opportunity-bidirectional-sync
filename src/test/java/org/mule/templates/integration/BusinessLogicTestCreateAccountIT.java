/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static org.mule.templates.builders.SfdcObjectBuilder.anAccount;
import static org.mule.templates.builders.SfdcObjectBuilder.anOpportunity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.templates.AbstractTemplatesTestCase;

import com.mulesoft.module.batch.BatchTestHelper;
import com.sforce.soap.partner.SaveResult;

/**
 * The objective of this class is validating the correct behavior of the flows
 * for this Mule Anypoint Template
 * 
 */
@SuppressWarnings("unchecked")
public class BusinessLogicTestCreateAccountIT extends AbstractTemplatesTestCase {

	private static final String A_INBOUND_FLOW_NAME = "triggerSyncFromAFlow";
	private static final String B_INBOUND_FLOW_NAME = "triggerSyncFromBFlow";
	private static final String ANYPOINT_TEMPLATE_NAME = "sfdc2sfdc-bidirectional-opportunity-sync";
	private static final String ACCOUNT_ID_IN_B = "0012000001OolOiAAJ";
	private static final int TIMEOUT_MILLIS = 60;

	private static List<String> accountsCreatedInA = new ArrayList<String>();
	private static List<String> accountsCreatedInB = new ArrayList<String>();
	private static List<String> opportunitiesCreatedInA = new ArrayList<String>();
	private static List<String> opportunitiesCreatedInB = new ArrayList<String>();
	private static SubflowInterceptingChainLifecycleWrapper deleteOpportunityFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteOpportunityFromBFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteAccountFromAFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteAccountFromBFlow;
	
	private SubflowInterceptingChainLifecycleWrapper createAccountInAFlow;
	private SubflowInterceptingChainLifecycleWrapper createAccountInBFlow;
	private SubflowInterceptingChainLifecycleWrapper createOpportunityInAFlow;
	private SubflowInterceptingChainLifecycleWrapper createOpportunityInBFlow;
	private InterceptingChainLifecycleWrapper queryOpportunityFromAFlow;
	private InterceptingChainLifecycleWrapper queryOpportunityFromBFlow;
	private BatchTestHelper batchTestHelper;

	@BeforeClass
	public static void beforeTestClass() {
		System.setProperty("page.size", "1000");

		// Set polling frequency to 10 seconds
		System.setProperty("poll.frequencyMillis", "10000");

		// Set default water-mark expression to current time
		System.clearProperty("watermark.default.expression");
		DateTime now = new DateTime(DateTimeZone.UTC).minusMinutes(1);
		DateTimeFormatter dateFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		System.setProperty("watermark.default.expression", now.toString(dateFormat));

		System.setProperty("trigger.policy", "poll");
		System.setProperty("account.sync.policy", "syncAccount");
	}
	
	@Before
	public void setUp() throws MuleException {
		stopAutomaticPollTriggering();
		getAndInitializeFlows();
		
		batchTestHelper = new BatchTestHelper(muleContext);
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("poll.frequencyMillis");
		System.clearProperty("watermark.default.expression");
		System.clearProperty("account.sync.policy");
		System.clearProperty("trigger.policy");
	}

	@After
	public void tearDown() throws MuleException, Exception {
		
	}

	private void stopAutomaticPollTriggering() throws MuleException {
		stopFlowSchedulers(A_INBOUND_FLOW_NAME);
		stopFlowSchedulers(B_INBOUND_FLOW_NAME);
	}

	private void getAndInitializeFlows() throws InitialisationException {
		// Flow for creating Accounts in sfdc A instance
		createAccountInAFlow = getSubFlow("createAccountInAFlow");
		createAccountInAFlow.initialise();
				
		// Flow for creating Accounts in sfdc B instance
		createAccountInBFlow = getSubFlow("createAccountInBFlow");
		createAccountInBFlow.initialise();
		
		// Flow for creating opportunities in sfdc A instance
		createOpportunityInAFlow = getSubFlow("createOpportunityInAFlow");
		createOpportunityInAFlow.initialise();

		// Flow for creating opportunities in sfdc B instance
		createOpportunityInBFlow = getSubFlow("createOpportunityInBFlow");
		createOpportunityInBFlow.initialise();

		// Flow for deleting opportunities in sfdc A instance
		deleteOpportunityFromAFlow = getSubFlow("deleteOpportunityFromAFlow");
		deleteOpportunityFromAFlow.initialise();

		// Flow for deleting opportunities in sfdc B instance
		deleteOpportunityFromBFlow = getSubFlow("deleteOpportunityFromBFlow");
		deleteOpportunityFromBFlow.initialise();
		
		// Flow for deleting opportunities in sfdc A instance
		deleteAccountFromAFlow = getSubFlow("deleteAccountFromAFlow");
		deleteAccountFromAFlow.initialise();
	
		// Flow for deleting opportunities in sfdc B instance
		deleteAccountFromBFlow = getSubFlow("deleteAccountFromBFlow");
		deleteAccountFromBFlow.initialise();

		// Flow for querying opportunities in sfdc A instance
		queryOpportunityFromAFlow = getSubFlow("queryOpportunityFromAFlow");
		queryOpportunityFromAFlow.initialise();

		// Flow for querying opportunities in sfdc B instance
		queryOpportunityFromBFlow = getSubFlow("queryOpportunityFromBFlow");
		queryOpportunityFromBFlow.initialise();
	}

	private static void cleanUpSandboxes() throws MuleException, Exception {
		final List<String> idList = new ArrayList<String>();
		for (String opportunity : opportunitiesCreatedInA) {
			idList.add(opportunity);
		}
		deleteOpportunityFromAFlow.process(getTestEvent(idList,	MessageExchangePattern.REQUEST_RESPONSE));
		
		idList.clear();
		for (String opportunity : opportunitiesCreatedInB) {
			idList.add(opportunity);
		}
		deleteOpportunityFromBFlow.process(getTestEvent(idList,	MessageExchangePattern.REQUEST_RESPONSE));
		
		idList.clear();
		for (String account : accountsCreatedInA) {
			idList.add(account);
		}
		deleteAccountFromAFlow.process(getTestEvent(idList,	MessageExchangePattern.REQUEST_RESPONSE));
		
		idList.clear();
		for (String account : accountsCreatedInB) {
			idList.add(account);
		}
		deleteAccountFromBFlow.process(getTestEvent(idList,	MessageExchangePattern.REQUEST_RESPONSE));
	}

	@Test
	public void testFromBtoAWithAccount() throws MuleException, Exception {
		// Build test account
		Map<String, Object> accountInB = anAccount()
				.with("Name", ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "Test-Account")
				.build();

		// Create account in sand-box and keep track of it for cleaning up afterwards
		String idOfAccountInB = createTestObjectInSfdcSandbox(accountInB, createAccountInBFlow);
		accountsCreatedInA.add(idOfAccountInB);
		
		// Build test opportunities
		Map<String, Object> opportunityInB = anOpportunity()
				.with("Amount", "123456")
				.with("Name", ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "Test-Opportunity")
				.with("AccountId", idOfAccountInB)
				.with("CloseDate", new Date())
				.with("StageName", "Stage One")
				.build();

		// Create opportunity in sand-box and keep track of it for cleaning up afterwards
		opportunitiesCreatedInB.add(createTestObjectInSfdcSandbox(opportunityInB, createOpportunityInBFlow));

		// Execution
		executeWaitAndAssertBatchJob(B_INBOUND_FLOW_NAME);

		// Assertions
		Map<String, String> retrievedOpportunityFromA = (Map<String, String>) queryOpportunity(opportunityInB, queryOpportunityFromAFlow);
		Assert.assertNotNull("There should be some opportunity in org A", retrievedOpportunityFromA);
		
		opportunitiesCreatedInA.add(retrievedOpportunityFromA.get("Id"));
		Assert.assertEquals("Opportunities should be synced",opportunityInB.get("Name"), retrievedOpportunityFromA.get("Name"));
		
		cleanUpSandboxes();
	}
	
	@Test
	public void testFromAtoBWithAccount() throws MuleException, Exception {
		// Build test account
		Map<String, Object> accountInA = anAccount()
				.with("Name", ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "Test-Account")
				.build();

		// Create account in sand-box and keep track of it for cleaning up afterwards
		String idOfAccountInA = createTestObjectInSfdcSandbox(accountInA, createAccountInAFlow);
		accountsCreatedInA.add(idOfAccountInA);

		// Build test opportunity
		Map<String, Object> opportunityInA = anOpportunity()
				.with("Amount", "123456")
				.with("Name", ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis() + "Test-Opportunity")
				.with("AccountId", idOfAccountInA)
				.with("CloseDate", new Date())
				.with("StageName", "Stage One")
				.build();

		// Create opportunity in sand-box and keep track of it for cleaning up afterwards
		opportunitiesCreatedInA.add(createTestObjectInSfdcSandbox(opportunityInA, createOpportunityInAFlow));

		// Execution
		executeWaitAndAssertBatchJob(A_INBOUND_FLOW_NAME);

		// Assertions
		Map<String, String> retrievedOpportunityFromB = (Map<String, String>) queryOpportunity(opportunityInA, queryOpportunityFromBFlow);
		Assert.assertNotNull("There should be some opportunity in org B", retrievedOpportunityFromB);
		
		opportunitiesCreatedInB.add(retrievedOpportunityFromB.get("Id"));
		Assert.assertEquals("Opportunities should be synced",opportunityInA.get("Name"), retrievedOpportunityFromB.get("Name"));
		
		cleanUpSandboxes();
	}

	private Object queryOpportunity(Map<String, Object> opportunity, InterceptingChainLifecycleWrapper queryOpportunityFlow)
			throws MuleException, Exception {
		return queryOpportunityFlow
				.process(getTestEvent(opportunity, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();
	}

	private String createTestObjectInSfdcSandbox(Map<String, Object> object, InterceptingChainLifecycleWrapper createObjectFlow)
			throws MuleException, Exception {
		List<Map<String, Object>> listOfObjects = new ArrayList<Map<String, Object>>();
		listOfObjects.add(object);

		final List<SaveResult> payloadAfterExecution = (List<SaveResult>) createObjectFlow
				.process(getTestEvent(listOfObjects, MessageExchangePattern.REQUEST_RESPONSE))
				.getMessage().getPayload();
		return payloadAfterExecution.get(0).getId();
	}

	private void executeWaitAndAssertBatchJob(String flowConstructName)
			throws Exception {

		// Execute synchronization
		runSchedulersOnce(flowConstructName);

		// Wait for the batch job execution to finish
		batchTestHelper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		batchTestHelper.assertJobWasSuccessful();
	}
	

}
