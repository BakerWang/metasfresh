
# About this document

This file contains the tasks/issues which we implement in metasfresh, in a chronological fashion (latest first)

In this document, we sort of lean on http://www.semanticreleasenotes.org/, 
but since that spec doesn't (yet?) support multiple releases in one file, 
we only adhere to it as far as it's convenient in term of the documents structuring

Meanings of the categories we currently use:
 * it: was developed in the it baseline
 * uat: was developed in the uat baseline (important: this baseline is discontinued!)
 * prod: was deveoped in the prod baseline
 * fix: a bugfix
 * feature: a new feature

Additional notes:
 * The metasfresh source code is hosted at https://github.com/metasfresh/metasfresh
 * The metasfresh website is at http://metasfresh.com/
 * You can also follow us on twitter: @metasfresh

# The actual release notes

## Upcoming Release

## metasfresh ERP 4.2.1
 - 09809 Report direct costing Year Title wrong (103628559355) +it +fixture
 - 09803 Revenue report fix (109269170462) +it +fix
 - 09281 create report for packaging material balance (106483495857) +it +feature
 - 09779 Report C Activity ID swap changes (101962781663) +it +feature
 - 09783 Improve Salesgroups - migration and report (105684868719) +it +feature
 - 09801 Customer Individual Shipment change (104284980744) +it +feature
    * minor change to the alternative shipment jasper
 - 09804 Account-Information: don't show and sum Budget (106521617847) +it +fix
 - 09677 extending the BPartner changes report (107837562286) +it +feature
 - 09502_Support quality based invoicing +it +fix
    * directly refresh/recreate existing invoice candidates when a PP_Order is unclosed; don't wait for it to be closed again
 - 09700 Counter orders with mapped products (100691234288) +it +feature
    * allow counter orders (sames order in one org => purchase order in another) with org-internal products that are mapped against each other
 - 09773 Signature fix in shipment jasper (100363111538) +it +fix
 - 09782 remove note from purchase invoice (109638032503) +it +fix
 - 09788 Show isInfiniteCapacity in Pricelist (106197421720) +it +fix
 - 09785 F4-save does not work anymore in included invoice line tab +it +fix
 - 09780 Sorting of List Reference for PriceListVersion (105389853564) +it +feature
 - 09777 German Translations for Salesgroup (106405148729) +it +fix
    * small changes for Salesgroup Translations
 - 09625 Costing short report incl Budget (105806249331) +it +fix
 - 09710 Report regarding effective prices (107746499502) +uat +fix
    * layout/display fix rergarding the report's page number
 - 09766 VAT codes (107418136945) +uat +feature
    * allow defining different VAT codes for sales and purchase, and matching them with each other
 - 09739 gain and loss during bank transfers in foreign currencies (108136874441) +uat +feature
    * introducing default conversion type that can be specified to be active at a given time
	* currency gain and loss from bank account transfers are computed by comparing default conversion type with the default conversion rate
 - 09110 Make activity mandatory in accounting documents and allow the user to select one on demand (105477200774) +uat +feature
 - 09775 Import GL Journal Number Format Exception (104021981594): +it +feature
    * when importing values into a non-text filed that can't be parsed as number, date etc, then don't fail the whole import
 - minor, unrelated fix: when retrieving dunning levels, order them by +it +fix
    *"DaysAfterDue" to make sure they are dealt with in chronological order.
    *Actually, the order might not matter, but a counterintuitive ordering causes FUD.
 - 09771 Dunning docs mail (102929053917) +it +fix +feature
    * small changes around the dunning jasper

## it-S16_05-20160202
 - fixed the default location of the client properties file from <user.home>/.metas-fresh to <user.home>/.metasfresh +it +fix
 - 09741 Problems with HU labels for split HUs (104680331233) +uat +fix
 - 09765 Process to manually re-open C_PAySelection records that were already prepared (108508031142) +uat +feature
 - 09745 alternative jasper shipment document without ADR but explicit GMAA-values (107947997555) +uat +feature
 - 09726 Deep-copy support for AD Roles (106651676304) +uat +feature
    * we now also copy user-role assignements to the target role
 - 09625 Costing short report incl Budget (105806249331) +uat +feature
 - 09767 DBMoreThenOneRecordsFoundException when retrieving from picking slot queue (105944016827) +uat +fix
 - 09710 Report regarding effective prices (107746499502)
 - 09704 Migration ADempiere to metasfresh (100169279454) +it +feature
    * making hardcoded endcustomer-feature configurable for all metasfresh users
 - 09752 system creates two printing queue items for gernic reports (107420055849) +it +fix
 - 09764 servicemix update (102943200308): the esb bundles now use servmicemix-6.1 +uat +feature
    *also switching everything from activemq-5.7 to activemq-5.12.1
    *ActiveMQJMSEndpoint now needs to provide username and password to the jms broker
    *commenting out the sniffer plugin because we can now build against java 1.7
    *moving org.adempiere.event to de.metas.event
    *moving EqualsBuilder and HashcodeBuilder from base to util
	* removing javax.mail from printing.esb, instead using guava to encode base64
    * removing javax.mail from de.metas.util
    * further build fixes and clean-ups
 - 09746_Change of activity report adaptions +fix +uat
    * fixing a / by zero issue
 - 09756 Record from foreign org in material receipt (POS) (105735084840) +fix +uat
    * applying user's access rules in all terminal windows
 - 09203_avoid setting IT staff as sales rep in orders that were generated from EDI +fix +uat
    * refined the logic with new specs that came out during QA
 - 09618 order-checkup printing problems (106933593952): +fix +uat
    * server/core changes: 
      * allowing an ITrxListener to deactivate itself in case it does not want to be called more than once 
    * printing client changes
      * using guava to decode the base64, got rid of javax.mail
      * Http endpoint: storing the received data in a file that's deleted once the print worked if the print failed, the file's content can be inspected
      * parent-pom: managing the guava version to be used (=>18.0)
      * cleaned up the printlcient's code
- 09761 Do research and improve logging of the stand alone printing client (104599264471) +feature +uat
    * adding a more usable JUL properties file that includes instructions.
    * minor changes (removing customer specifics, fixing a log level in the ESB to avoid log file flooding)
 - 09618 Bestellkontrolle Druck Probleme (106933593952): allowing an ITrxListener to deactivate itself in case it does not want to be called more than once +fix +uat
 - 09744_Dunning Report and UI changes +uat
 - 09668_Change quality based invoicing for fresh products +uat
 - 09203_avoid setting IT staff as sales rep in orders that were generated from EDI +uat +fix
 - 09743_option to show bugdet data in the balance report  +uat
 - 09746_Change of activity report adaptions +uat
 - 09670_Daily Lot for material storing +uat
 - 09671_accounting_always book discount onto actNo 150 +uat
 - 09704_Migration ADempiere nach metasfresh +fix
    * adjusting the included tab size in the windows order (sales+purchase), shipment and invoice (sales+purchase) to 800, 
    * additional fixes
 - 09748_Menu search box working weird +fix
 - 09709_metasfresh web (early prototyping, nothing to use yet)
 - 09733_Problems with Sepa-XML import into Mammut +uat
 - 09694_Fact Acct Summary incremental updates +uat
 - 09502_Support quality based invoicing +uat +fix
 - 09690_Report change of activity +uat +fix
 - 09726_Deep-copy support for AD_Roles +uat
 - 09722_Changes to the costing report +uat
 - 09717_Costprice is 0 on multiple selection in wareneingang pos +uat +fix
 - 09564_Report all not-completed Documents +uat 
 - 09718_Revenuereport Excel-Export broken +uat
 - 09687_DD order Copy with details +uat