/**
 * Mule Anypoint Template
 * Copyright (c) MuleSoft, Inc.
 * All rights reserved.  http://www.mulesoft.com
 */

package org.mule.templates.integration;

import static junit.framework.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.construct.Flow;
import org.mule.processor.chain.InterceptingChainLifecycleWrapper;
import org.mule.processor.chain.SubflowInterceptingChainLifecycleWrapper;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.templates.AbstractTemplatesTestCase;

import com.mulesoft.module.batch.BatchTestHelper;

/**
 * The objective of this class is to validate the correct behavior of the
 * Anypoint Template that make calls to external systems.
 * 
 * @author Vlado Andoga
 */
@SuppressWarnings("unchecked")
public class BusinessLogicPushNotificationIT extends AbstractTemplatesTestCase {
	
	private static final String A_INBOUND_FLOW_NAME = "triggerSyncFromAFlow";
	private static final String B_INBOUND_FLOW_NAME = "triggerSyncFromBFlow";
	private static final String ANYPOINT_TEMPLATE_NAME = "sfdc2sfdc-bidirectional-opportunity-sync";
	private static final int TIMEOUT_MILLIS = 60;
	private static final String ACCOUNT_ID_IN_B = "0012000001AHHlv";
	private static final String SOURCE_SYSTEM = "A";
	private BatchTestHelper helper;
	private Flow triggerPushFlow;
	private InterceptingChainLifecycleWrapper queryOpportunityFromBFlow;
	private static SubflowInterceptingChainLifecycleWrapper deleteOpportunityFromBFlow;
	private List<Map<String, Object>> createdOpportunities = new ArrayList<Map<String, Object>>();
	
	@Rule
	public DynamicPort port = new DynamicPort("http.port");
	
	@BeforeClass
	public static void beforeClass() {
		System.setProperty("trigger.policy", "push");
		System.setProperty("account.sync.policy", "assignDummyAccount");
		System.setProperty("account.id.in.b", ACCOUNT_ID_IN_B);
	}

	@AfterClass
	public static void shutDown() {
		System.clearProperty("trigger.policy");
		System.clearProperty("account.sync.policy");
		System.clearProperty("account.id.in.b");
	}

	@Before
	public void setUp() throws Exception {
		stopAutomaticPollTriggering();
		helper = new BatchTestHelper(muleContext);
		triggerPushFlow = getFlow("triggerPushFlow");
		initialiseSubFlows();
	}

	@After
	public void tearDown() throws Exception {
		cleanUpSandboxesByRemovingTestOpportunities();
	}
	
	/**
	 * Stops polling triggering.
	 * @throws MuleException the flow schedulers stop failed
	 */
	private void stopAutomaticPollTriggering() throws MuleException {
		stopFlowSchedulers(A_INBOUND_FLOW_NAME);
		stopFlowSchedulers(B_INBOUND_FLOW_NAME);
	}
	
	/**
	 * Inits all tests sub-flows.
	 * @throws Exception when initialisation is unsuccessful 
	 */
	private void initialiseSubFlows() throws Exception {		
		queryOpportunityFromBFlow = getSubFlow("queryOpportunityFromBFlow");
		queryOpportunityFromBFlow.initialise();
		
		// Flow for deleting opportunities in sfdc B instance
		deleteOpportunityFromBFlow = getSubFlow("deleteOpportunityFromBFlow");
		deleteOpportunityFromBFlow.initialise();
	}

	/**
	 * In test, we are creating new SOAP message to create/update an existing contact. Contact first name is always generated
	 * to ensure, that flow correctly updates contact in the Saleforce. 
	 * @throws Exception when flow error occurred
	 */
	@Test
	public void testMainFlow() throws Exception {
		// Execution
		String name = buildUniqueName();
		MuleMessage message = new DefaultMuleMessage(buildRequest(name), muleContext);
		MuleEvent testEvent = getTestEvent(message, MessageExchangePattern.REQUEST_RESPONSE);
		testEvent.setFlowVariable("sourceSystem", SOURCE_SYSTEM);
		triggerPushFlow.process(testEvent);
		
		helper.awaitJobTermination(TIMEOUT_MILLIS * 1000, 500);
		helper.assertJobWasSuccessful();

		Map<String, Object> opportunityToRetrieveByName = new HashMap<String, Object>();
		opportunityToRetrieveByName.put("Name", name);

		MuleEvent event = queryOpportunityFromBFlow.process(getTestEvent(opportunityToRetrieveByName, MessageExchangePattern.REQUEST_RESPONSE));

		Map<String, Object> payload = (Map<String, Object>) event.getMessage().getPayload();
		
		// Track created records for a cleanup.
		Map<String, Object> createdOpportunity = new HashMap<String, Object>();
		createdOpportunity.put("Id", payload.get("Id"));
		createdOpportunity.put("Name", payload.get("Name"));
		createdOpportunities.add(createdOpportunity);

		// Assertions
		assertEquals("The user should have been sync and new name must match", name, payload.get("Name"));
	}

	/**
	 * Builds the soap request as a string
	 * @param name the name
	 * @return a soap message as string
	 */
	private String buildRequest(String name){
		StringBuilder request = new StringBuilder();
		request.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		request.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
		request.append("<soapenv:Body>");
		request.append("  <notifications xmlns=\"http://soap.sforce.com/2005/09/outbound\">");
		request.append("   <OrganizationId>00Dd0000000dtDqEAI</OrganizationId>");
		request.append("   <ActionId>04kd0000000PCgvAAG</ActionId>");
		request.append("   <SessionId xsi:nil=\"true\"/>");
		request.append("   <EnterpriseUrl>https://na14.salesforce.com/services/Soap/c/30.0/00Dd0000000dtDq</EnterpriseUrl>");
		request.append("   <PartnerUrl>https://na14.salesforce.com/services/Soap/u/30.0/00Dd0000000dtDq</PartnerUrl>");
		request.append("   <Notification>");
		request.append("     <Id>04l2000000KFmjJAAT</Id>");
		request.append("     <sObject xsi:type=\"sf:Opportunity\" xmlns:sf=\"urn:sobject.enterprise.soap.sforce.com\">");
		request.append("       <sf:Id>0062000000ZZpewAAD</sf:Id>");
		request.append("       <sf:AccountId>0012000001AOHpxAAH</sf:AccountId>");
		request.append("       <sf:Amount>12.0</sf:Amount>");
		request.append("       <sf:CloseDate>2011-08-04</sf:CloseDate>");
		request.append("       <sf:Description>description</sf:Description>");
		request.append("       <sf:LastModifiedDate>2014-08-20T11:16:04.000Z</sf:LastModifiedDate>");
		request.append("       <sf:Name>" + name + "</sf:Name>");
		request.append("       <sf:Probability>50.0</sf:Probability>");
		request.append("       <sf:StageName>Value Proposition</sf:StageName>");
		request.append("       <sf:Type>Existing Customer - Replacement</sf:Type>");
		request.append("     </sObject>");
		request.append("   </Notification>");
		request.append("  </notifications>");
		request.append(" </soapenv:Body>");
		request.append("</soapenv:Envelope>");
		return request.toString();
	}
	
	/**
	 * Builds unique name based on current time stamp.
	 * @return a unique name as string
	 */
	private String buildUniqueName() {
		return ANYPOINT_TEMPLATE_NAME + "-" + System.currentTimeMillis();
	}
	
	/**
	 * Deletes data created by the tests.
	 * @throws Exception when an error occurred during clean up.
	 */
	private void cleanUpSandboxesByRemovingTestOpportunities()
			throws MuleException, Exception {
		List<Object> idList = new ArrayList<Object>();
		for (Map<String, Object> c : createdOpportunities) {
			idList.add(c.get("Id"));
		}

		deleteOpportunityFromBFlow.process(getTestEvent(idList,	MessageExchangePattern.REQUEST_RESPONSE));
	}
	
}
