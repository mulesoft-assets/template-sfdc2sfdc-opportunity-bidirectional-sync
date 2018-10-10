
# Anypoint Template: Salesforce and Salesforce Opportunity Bidirectional Sync

# License Agreement
This template is subject to the conditions of the 
<a href="https://s3.amazonaws.com/templates-examples/AnypointTemplateLicense.pdf">MuleSoft License Agreement</a>.
Review the terms of the license before downloading and using this template. You can use this template for free 
with the Mule Enterprise Edition, CloudHub, or as a trial in Anypoint Studio.

# Use Case
As a Salesforce admin, I want to have my opportunities synchronized between two different Salesforce organizations

## Template overview 

Let's say we want to keep Salesforce instance *A* synchronized with Salesforce instance *B*. Then, the integration behavior can be summarized just with the following steps:

1. Ask Salesforce *A*:
> *Which changes have there been since the last time I got in touch with you?*

2. For each of the updates fetched in the previous step (1.), ask Salesforce *B*:
> *Does the update received from A should be applied?*

3. If Salesforce answer for the previous question (2.) is *Yes*, then *upsert* (create or update depending each particular case) B with the belonging change

4. Repeat previous steps (1. to 3.) the other way around (using *B* as source instance and *A* as the target one)

 Repeat *ad infinitum*:

5. Ask Salesforce *A*:
> *Which changes have there been since the question I've made in the step 1.?*

And so on...
 
The question for recent changes since a certain moment in nothing but a scheduler with a watermark using object store.

As implemented, this Anypoint Template also leverages [Outbound messaging](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging.htm)
The integration could be also triggered by HTTP inbound connector defined in the flow that is going to trigger the application and executing the batch job with received message from Salesforce source instance.
Outbound messaging in Salesforce allows you to specify that changes to fields within Salesforce can cause messages with field values to be sent to designated external servers.
Outbound messaging is part of the workflow rule functionality in Salesforce. Workflow rules watch for specific kinds of field changes and trigger automatic Salesforce actions in this case sending opportunities as an outbound message to Mule HTTP listener,
which will then further process this message and create Opportunity in target Salesforce organization.

# Considerations

To make this Anypoint Template run, there are certain preconditions that must be considered. All of them deal with the preparations in both, that must be made in order for all to run smoothly. **Failing to do so could lead to unexpected behavior of the template.**



## Salesforce Considerations

Here's what you need to know about Salesforce to get this template to work.

### FAQ

- Where can I check that the field configuration for my Salesforce instance is the right one? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=checking_field_accessibility_for_a_particular_field.htm&language=en_US">Salesforce: Checking Field Accessibility for a Particular Field</a>
- Can I modify the Field Access Settings? How? See: <a href="https://help.salesforce.com/HTViewHelpDoc?id=modifying_field_access_settings.htm&language=en_US">Salesforce: Modifying Field Access Settings</a>

### As a Data Source

If the user who configured the template for the source system does not have at least *read only* permissions for the fields that are fetched, then an *InvalidFieldFault* API fault displays.

```
java.lang.RuntimeException: [InvalidFieldFault [ApiQueryFault [ApiFault  exceptionCode='INVALID_FIELD'
exceptionMessage='
Account.Phone, Account.Rating, Account.RecordTypeId, Account.ShippingCity
^
ERROR at Row:1:Column:486
No such column 'RecordTypeId' on entity 'Account'. If you are attempting to use a custom field, be sure to append the '__c' after the custom field name. Reference your WSDL or the describe call for the appropriate names.'
]
row='1'
column='486'
]
]
```

### As a Data Destination

There are no considerations with using Salesforce as a data destination.









# Run it!
Simple steps to get Salesforce and Salesforce Opportunity Bidirectional Sync running.


## Running On Premises
In this section we help you run your template on your computer.


### Where to Download Anypoint Studio and the Mule Runtime
If you are a newcomer to Mule, here is where to get the tools.

+ [Download Anypoint Studio](https://www.mulesoft.com/platform/studio)
+ [Download Mule runtime](https://www.mulesoft.com/lp/dl/mule-esb-enterprise)


### Importing a Template into Studio
In Studio, click the Exchange X icon in the upper left of the taskbar, log in with your
Anypoint Platform credentials, search for the template, and click **Open**.


### Running on Studio
After you import your template into Anypoint Studio, follow these steps to run it:

+ Locate the properties file `mule.dev.properties`, in src/main/resources.
+ Complete all the properties required as per the examples in the "Properties to Configure" section.
+ Right click the template project folder.
+ Hover your mouse over `Run as`
+ Click `Mule Application (configure)`
+ Inside the dialog, select Environment and set the variable `mule.env` to the value `dev`
+ Click `Run`


### Running on Mule Standalone
Complete all properties in one of the property files, for example in mule.prod.properties and run your app with the corresponding environment variable. To follow the example, this is `mule.env=prod`. 


## Running on CloudHub
While creating your application on CloudHub (or you can do it later as a next step), go to Runtime Manager > Manage Application > Properties to set the environment variables listed in "Properties to Configure" as well as the **mule.env**.
Once your app is all set and started, you will need to define Salesforce outbound messaging and a simple workflow rule. [This article will show you how to accomplish this](https://www.salesforce.com/us/developer/docs/api/Content/sforce_api_om_outboundmessaging_setting_up.htm)
The most important setting here is the `Endpoint URL` which needs to point to your application running on CloudbHub, eg. `http://yourapp.cloudhub.io:80/?source=value`. Value for source parameter could be `A` for source outbound messaging for organization A or `B` for organization B. Additionally, try to add just few fields to the `Fields to Send` to keep it simple for begin.
Once this all is done every time when you will make a change on Opportunity in source Salesforce org. This Opportunity will be sent as a SOAP message to the Http endpoint of running application in Cloudhub.

### Deploying your Anypoint Template on CloudHub
Studio provides an easy way to deploy your template directly to CloudHub, for the specific steps to do so check this


## Properties to Configure
To use this template, configure properties (credentials, configurations, etc.) in the properties file or in CloudHub from Runtime Manager > Manage Application > Properties. The sections that follow list example values.
### Application Configuration
**HTTP Connector configuration**
+ http.port `9090`

**Batch Aggregator configuration**
+ page.size `200`

**Scheduler configuration**
+ scheduler.frequency `10000`
+ scheduler.startDelay `0`
This are the miliseconds (also different time units can be used) that will run between two different checks for updates in Salesforce

+ watermark.default.expression `2015-08-25T11:00:00.000Z`  
This property is an important one, as it configures what should be the start point of the synchronization. The date format accepted in Salesforce Query Language is either *YYYY-MM-DDThh:mm:ss+hh:mm* or you can use Constants. [More information about Dates in Salesforce](http://www.salesforce.com/us/developer/docs/officetoolkit/Content/sforce_api_calls_soql_select_dateformats.htm)

**Trigger policy(push, poll)**
+ trigger.policy `poll`
This property defines, which policy should be used for synchronization. When the push policy is selected, the HTTP inbound connector is used for Salesforce's outbound messaging and polling mechanism is ignored.

**Account sync policy(empty value, syncAccount)**
+ account.sync.policy ``
If the account.sync.policy property has no value assigned, the contact will be just moved over without a parent account.
If the syncAccount policy is syncAccount then the Opportunity will be created with an account with the same name as in the source instance. 

**SalesForce Connector configuration for company A**
+ sfdc.a.username `sfdc.a.user@mail.com`
+ sfdc.a.password `passworda`
+ sfdc.a.securityToken `avsfwCUl7apQs56Xq2AKi3X`
+ sfdc.a.url `https://login.salesforce.com/services/Soap/u/40.0`
+ sfdc.a.integration.user.id `A0ed000BO9T`

	**Note:** To find out the correct *sfdc.a.integration.user.id* value, please, refer to example project **Salesforce Data Retrieval** in [Anypoint Exchange](http://www.mulesoft.org/documentation/display/current/Anypoint+Exchange).

**SalesForce Connector configuration for company B**
+ sfdc.b.username `sfdc.b.user@mail.com`
+ sfdc.b.password `passwordb`
+ sfdc.b.securityToken `ces56arl7apQs56XTddf34X`
+ sfdc.b.url `https://login.salesforce.com/services/Soap/u/40.0`
+ sfdc.b.integration.user.id `B0ed000BO9T`

	**Note:** To find out the correct *sfdc.b.integration.user.id* value, please, refer to example project **Salesforce Data Retrieval** in [Anypoint Exchange](http://www.mulesoft.org/documentation/display/current/Anypoint+Exchange).

# API Calls
Salesforce imposes limits on the number of API Calls that can be made. Therefore calculating this amount may be an important factor to consider. The Anypoint Template calls to the API can be calculated using the formula:

***1 + X + X / ${page.size}***

Being ***X*** the number of Opportunities to be synchronized on each run. 

The division by ***${page.size}*** is because, by default, Opportunities are gathered in groups of ${page.size} for each Upsert API Call in the commit step. Also consider that this calls are executed repeatedly every polling cycle.	

For instance if 10 records are fetched from origin instance, then 12 api calls will be made (1 + 10 + 1).

When the outbound messaging is enabled in Salesforce and template trigger policy is push, specify Saleforce source organization eg. http://yourapp.cloudhub.io:80/?source=A as URL query parameter
Also consider that all required fields of Opportunity in Salesforce should be added for the outbound messaging.


# Customize It!
This brief guide intends to give a high level idea of how this template is built and how you can change it according to your needs.
As Mule applications are based on XML files, this page describes the XML files used with this template.

More files are available such as test classes and Mule application files, but to keep it simple, we focus on these XML files:

* config.xml
* businessLogic.xml
* endpoints.xml
* errorHandling.xml


## config.xml
Configuration for connectors and configuration properties are set in this file. Even change the configuration here, all parameters that can be modified are in properties file, which is the recommended place to make your changes. However if you want to do core changes to the logic, you need to modify this file.

In the Studio visual editor, the properties are on the *Global Element* tab.


## businessLogic.xml
This file holds the functional aspect of the template (points 2. to 4. described in the [template overview](#templateoverview)). Its main component is a [*Batch job*][8], and it includes *steps* for both executing the synchronization from Salesforce instance A to Salesforce instance B, and the other way around.



## endpoints.xml
This file should contain every inbound and outbound endpoint of your integration app. It is intended to contain the application API.
In this particular template, this file contains a couple flows that query salesforce for updates using watermark as mentioned before.



## errorHandling.xml
This is the right place to handle how your integration reacts depending on the different exceptions. 
This file provides error handling that is referenced by the main flow in the business logic.




