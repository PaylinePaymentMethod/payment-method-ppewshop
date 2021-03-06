package com.payline.payment.ppewshop;

import com.payline.payment.ppewshop.utils.Constants;
import com.payline.payment.ppewshop.utils.http.StringResponse;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import com.payline.pmapi.bean.configuration.request.ContractParametersCheckRequest;
import com.payline.pmapi.bean.notification.request.NotificationRequest;
import com.payline.pmapi.bean.payment.*;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.paymentform.request.PaymentFormConfigurationRequest;
import com.payline.pmapi.bean.paymentform.request.PaymentFormLogoRequest;
import com.payline.pmapi.bean.refund.request.RefundRequest;
import com.payline.pmapi.bean.reset.request.ResetRequest;
import org.apache.http.Header;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicStatusLine;
import org.mockito.internal.util.reflection.FieldSetter;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

public class MockUtils {
    private static String TRANSACTIONID = "123456789012345678901";
    private static String PARTNER_TRANSACTIONID = "098765432109876543210";

    public static final String STATUS_CODE = "STATUS_CODE";

    public static final String templateCheckStatusRequest = "<checkStatus xmlns=\"urn:PPEWShopServiceV3\"><checkStatusIn><merchantInformation><merchandCode>1212121212</merchandCode><distributorNumber>2323232323</distributorNumber><countryCode>FRA</countryCode></merchantInformation><transactionId>1234567890</transactionId></checkStatusIn></checkStatus>";
    public static final String templateInitDossierRequest = "<initDossier xmlns=\"urn:PPEWShopServiceV3\"><initDossierIn><merchantInformation><merchandCode>1212121212</merchandCode><distributorNumber>2323232323</distributorNumber><countryCode>FRA</countryCode></merchantInformation><merchantConfiguration><guarPushUrl>urlPush</guarPushUrl><guarBackUrl>urlBack</guarBackUrl></merchantConfiguration><customerInformation><title>MR</title><customerLanguage>FR</customerLanguage><firstName>Test</firstName><name>Test</name><birthDate>1992-08-14</birthDate><email>test.test@cetelem.fr</email><addressLine1>25 Elysées la Défense</addressLine1><addressLine2>Apt 20</addressLine2><city>La défense</city><postCode>92000</postCode><cellPhoneNumber>0172757512</cellPhoneNumber><privatePhoneNumber/><professionalPhoneNumber/></customerInformation><orderInformation><goodsCode>616</goodsCode><price>1000</price><financialProductType>CLA</financialProductType></orderInformation><orderReference/></initDossierIn></initDossier>";

    public static final String templateInitDossierResponse = "<initDossierResponse xmlns=\"urn:PPEWShopServiceV3\">" +
            "<initDossierOut>" +
            "<transactionId>1234567890</transactionId>" +
            "<redirectionUrl>http://redirectionUrl.com</redirectionUrl>" +
            "<warning>" +
            "<warningCode>13008</warningCode>" +
            "<warningDescription>warningDescription</warningDescription>" +
            "</warning>" +
            "</initDossierOut>" +
            "</initDossierResponse>";

    public static final String templateCheckStatusResponse = "<checkStatusResponse xmlns=\"urn:PPEWShopServiceV3\">\n" +
            "    <checkStatusOut>\n" +
            "        <transactionId>1234567890</transactionId>\n" +
            "        <merchandOrderReference>\n" +
            "            <merchandOrderId>32552564</merchandOrderId>\n" +
            "        </merchandOrderReference>\n" +
            "        <statusCode>STATUS_CODE</statusCode>\n" +
            "        <redirectionUrl>http://redirectionUrl.com</redirectionUrl>\n" +
            "        <creditAuthorizationNumber>34600015</creditAuthorizationNumber>\n" +
            "    </checkStatusOut>\n" +
            "</checkStatusResponse>";

    public static final String templateResponseError = "<axis2ns1:PPEWShopServiceException xmlns:axis2ns1=\"urn:PPEWShopServiceV3\">" +
            "<axis2ns1:errorCode>ERROR_CODE</axis2ns1:errorCode>" +
            "<axis2ns1:errorDescription>ERROR_DESCRIPTION</axis2ns1:errorDescription>" +
            "</axis2ns1:PPEWShopServiceException>";

    /**------------------------------------------------------------------------------------------------------------------*/

    /**
     * Generate a valid {@link Environment}.
     */
    public static Environment anEnvironment() {
        return new Environment("http://notificationURL.com",
                "http://redirectionURL.com",
                "http://redirectionCancelURL.com",
                true);
    }
    /**------------------------------------------------------------------------------------------------------------------*/
    /**
     * Generate a valid {@link PartnerConfiguration}.
     */
    public static PartnerConfiguration aPartnerConfiguration() {
        Map<String, String> partnerConfigurationMap = new HashMap<>();
        partnerConfigurationMap.put(Constants.PartnerConfigurationKeys.URL, "https://recette-webpartners-cetelem-net.neuges.org/PPEWShop/services/PPEWShopServiceV3");
        Map<String, String> sensitiveConfigurationMap = new HashMap<>();
        return new PartnerConfiguration(partnerConfigurationMap, sensitiveConfigurationMap);
    }
    /**------------------------------------------------------------------------------------------------------------------*/


    /**
     * Generate a valid {@link PaymentFormConfigurationRequest}.
     */
    public static PaymentFormConfigurationRequest aPaymentFormConfigurationRequest() {
        return aPaymentFormConfigurationRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link PaymentFormConfigurationRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static PaymentFormConfigurationRequest.PaymentFormConfigurationRequestBuilder aPaymentFormConfigurationRequestBuilder() {
        return PaymentFormConfigurationRequest.PaymentFormConfigurationRequestBuilder.aPaymentFormConfigurationRequest()
                .withAmount(aPaylineAmount())
                .withBuyer(aBuyer())
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withLocale(Locale.FRANCE)
                .withOrder(aPaylineOrder())
                .withPartnerConfiguration(aPartnerConfiguration());
    }

    /**
     * Generate a valid {@link PaymentFormLogoRequest}.
     */
    public static PaymentFormLogoRequest aPaymentFormLogoRequest() {
        return PaymentFormLogoRequest.PaymentFormLogoRequestBuilder.aPaymentFormLogoRequest()
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withPartnerConfiguration(aPartnerConfiguration())
                .withLocale(Locale.getDefault())
                .build();
    }

    /**
     * Generate a valid, but not complete, {@link Order}
     */
    public static Order aPaylineOrder() {
        List<Order.OrderItem> items = new ArrayList<>();

        items.add(Order.OrderItem.OrderItemBuilder
                .anOrderItem()
                .withReference("foo")
                .withAmount(aPaylineAmount())
                .withQuantity(1L)
                .withCategory("1") // Informatique
                .build());

        return Order.OrderBuilder.anOrder()
                .withDate(new Date())
                .withAmount(aPaylineAmount())
                .withItems(items)
                .withReference("ref-20191105153749")
                .build();
    }

    /**
     * Generate a valid Payline Amount.
     */
    public static com.payline.pmapi.bean.common.Amount aPaylineAmount() {
        return aPaylineAmount(200000);
    }

    public static com.payline.pmapi.bean.common.Amount aPaylineAmount(int amount) {
        return new com.payline.pmapi.bean.common.Amount(BigInteger.valueOf(amount), Currency.getInstance("EUR"));
    }

    /**
     * @return a valid user agent.
     */
    public static String aUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:67.0) Gecko/20100101 Firefox/67.0";
    }

    /**
     * Generate a valid {@link Browser}.
     */
    public static Browser aBrowser() {
        return Browser.BrowserBuilder.aBrowser()
                .withLocale(Locale.getDefault())
                .withIp("192.168.0.1")
                .withUserAgent(aUserAgent())
                .build();
    }

    /**
     * Generate a valid {@link Buyer}.
     */
    public static Buyer aBuyer() {
        try {
            return Buyer.BuyerBuilder.aBuyer()
                    .withFullName(new Buyer.FullName("Marie", "Durand", "1"))
                    .withBirthday(new SimpleDateFormat("dd-MM-yyyy").parse("01-01-2020"))
                    .withAddresses(addresses())
                    .withPhoneNumbers(phoneNumbers())
                    .withEmail("foo@bar.baz")
                    .build();
        } catch (ParseException e) {
            // should never append
            return null;
        }
    }

    public static Map<Buyer.AddressType, Buyer.Address> addresses() {
        Map<Buyer.AddressType, Buyer.Address> addresses = new HashMap<>();
        addresses.put(Buyer.AddressType.BILLING, anAddress());
        addresses.put(Buyer.AddressType.DELIVERY, anAddress());

        return addresses;
    }

    public static Buyer.Address anAddress() {
        return Buyer.Address.AddressBuilder
                .anAddress()
                .withStreet1("street1")
                .withStreet2("street2")
                .withCity("City")
                .withZipCode("75000")
                .withState("France")
                .build();
    }

    public static Map<Buyer.PhoneNumberType, String> phoneNumbers() {
        Map<Buyer.PhoneNumberType, String> phoneNumbers = new HashMap<>();
        phoneNumbers.put(Buyer.PhoneNumberType.HOME, "0612345678");
        phoneNumbers.put(Buyer.PhoneNumberType.WORK, "0712345678");
        phoneNumbers.put(Buyer.PhoneNumberType.CELLULAR, "0612345678");
        phoneNumbers.put(Buyer.PhoneNumberType.BILLING, "0612345678");

        return phoneNumbers;
    }

    /**
     * Generate a valid {@link PaymentFormContext}.
     */
    public static PaymentFormContext aPaymentFormContext() {
        Map<String, String> paymentFormParameter = new HashMap<>();

        return PaymentFormContext.PaymentFormContextBuilder.aPaymentFormContext()
                .withPaymentFormParameter(paymentFormParameter)
                .withSensitivePaymentFormParameter(new HashMap<>())
                .build();
    }

    /**------------------------------------------------------------------------------------------------------------------*/
    /**
     * Generate a valid {@link ContractParametersCheckRequest}.
     */
    public static ContractParametersCheckRequest aContractParametersCheckRequest() {
        return aContractParametersCheckRequestBuilder().build();
    }
    /**------------------------------------------------------------------------------------------------------------------*/
    /**
     * Generate a builder for a valid {@link ContractParametersCheckRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static ContractParametersCheckRequest.CheckRequestBuilder aContractParametersCheckRequestBuilder() {
        return ContractParametersCheckRequest.CheckRequestBuilder.aCheckRequest()
                .withAccountInfo(anAccountInfo())
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withLocale(Locale.getDefault())
                .withPartnerConfiguration(aPartnerConfiguration());
    }

    /**
     * Generate a valid {@link PaymentRequest}.
     */
    public static PaymentRequest aPaylinePaymentRequest() {
        return aPaylinePaymentRequestBuilder().build();
    }

    /**
     * Generate a builder for a valid {@link PaymentRequest}.
     * This way, some attributes may be overridden to match specific test needs.
     */
    public static PaymentRequest.Builder aPaylinePaymentRequestBuilder() {
        return PaymentRequest.builder()
                .withAmount(aPaylineAmount())
                .withBrowser(aBrowser())
                .withBuyer(aBuyer())
                .withCaptureNow(true)
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withLocale(Locale.FRANCE)
                .withOrder(aPaylineOrder())
                .withPartnerConfiguration(aPartnerConfiguration())
                .withPaymentFormContext(aPaymentFormContext())
                .withSoftDescriptor("softDescriptor")
                .withTransactionId(TRANSACTIONID);
    }

    public static RefundRequest aPaylineRefundRequest() {
        return aPaylineRefundRequestBuilder().build();
    }

    public static RefundRequest.RefundRequestBuilder aPaylineRefundRequestBuilder() {
        return RefundRequest.RefundRequestBuilder.aRefundRequest()
                .withAmount(aPaylineAmount())
                .withOrder(aPaylineOrder())
                .withBuyer(aBuyer())
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withTransactionId(TRANSACTIONID)
                .withPartnerTransactionId(PARTNER_TRANSACTIONID)
                .withPartnerConfiguration(aPartnerConfiguration());
    }


    public static ResetRequest aPaylineResetRequest() {
        return aPaylineResetRequestBuilder().build();
    }

    public static ResetRequest.ResetRequestBuilder aPaylineResetRequestBuilder() {
        return ResetRequest.ResetRequestBuilder.aResetRequest()
                .withAmount(aPaylineAmount())
                .withOrder(aPaylineOrder())
                .withBuyer(aBuyer())
                .withContractConfiguration(aContractConfiguration())
                .withEnvironment(anEnvironment())
                .withTransactionId(TRANSACTIONID)
                .withPartnerTransactionId(PARTNER_TRANSACTIONID)
                .withPartnerConfiguration(aPartnerConfiguration());
    }

    public static NotificationRequest aPaylineNotificationRequest() {
        return aPaylineNotificationRequestBuilder().build();
    }

    public static NotificationRequest.NotificationRequestBuilder aPaylineNotificationRequestBuilder() {
        return NotificationRequest.NotificationRequestBuilder.aNotificationRequest()
                .withHeaderInfos(new HashMap<>())
                .withPathInfo("transactionDeId=1234567890123")
                .withHttpMethod("POST")
                .withContractConfiguration(aContractConfiguration())
                .withPartnerConfiguration(aPartnerConfiguration())
                .withContent(new ByteArrayInputStream("".getBytes()))
                .withEnvironment(anEnvironment());
    }


    /**------------------------------------------------------------------------------------------------------------------*/
    /**
     * Generate a valid accountInfo, an attribute of a {@link ContractParametersCheckRequest} instance.
     */
    public static Map<String, String> anAccountInfo() {
        return anAccountInfo(aContractConfiguration());
    }
    /**------------------------------------------------------------------------------------------------------------------*/

    /**
     * Generate a valid accountInfo, an attribute of a {@link ContractParametersCheckRequest} instance,
     * from the given {@link ContractConfiguration}.
     *
     * @param contractConfiguration The model object from which the properties will be copied
     */
    public static Map<String, String> anAccountInfo(ContractConfiguration contractConfiguration) {
        Map<String, String> accountInfo = new HashMap<>();
        for (Map.Entry<String, ContractProperty> entry : contractConfiguration.getContractProperties().entrySet()) {
            accountInfo.put(entry.getKey(), entry.getValue().getValue());
        }
        return accountInfo;
    }

    /**
     * Generate a valid {@link ContractConfiguration}.
     */
    public static ContractConfiguration aContractConfiguration() {
        Map<String, ContractProperty> contractProperties = new HashMap<>();
        contractProperties.put(Constants.ContractConfigurationKeys.MERCHANT_CODE, new ContractProperty("3550937738"));
        contractProperties.put(Constants.ContractConfigurationKeys.DISTRIBUTOR_NUMBER, new ContractProperty("1000828996"));
        contractProperties.put(Constants.ContractConfigurationKeys.COUNTRY_CODE, new ContractProperty("FRA"));

        return new ContractConfiguration("PPEWShop", contractProperties);
    }


    /**
     * Moch a StringResponse with the given elements.
     *
     * @param statusCode    The HTTP status code (ex: 200, 403)
     * @param statusMessage The HTTP status message (ex: "OK", "Forbidden")
     * @param content       The response content as a string
     * @param headers       The response headers
     * @return A mocked StringResponse
     */
    public static StringResponse mockStringResponse(int statusCode, String statusMessage, String content, Map<String, String> headers) {
        StringResponse response = new StringResponse();

        try {
            if (content != null && !content.isEmpty()) {
                FieldSetter.setField(response, StringResponse.class.getDeclaredField("content"), content);
            }
            if (headers != null && headers.size() > 0) {
                FieldSetter.setField(response, StringResponse.class.getDeclaredField("headers"), headers);
            }
            if (statusCode >= 100 && statusCode < 600) {
                FieldSetter.setField(response, StringResponse.class.getDeclaredField("statusCode"), statusCode);
            }
            if (statusMessage != null && !statusMessage.isEmpty()) {
                FieldSetter.setField(response, StringResponse.class.getDeclaredField("statusMessage"), statusMessage);
            }
        } catch (NoSuchFieldException e) {
            // This would happen in a testing context: spare the exception throw, the test case will probably fail anyway
            return null;
        }

        return response;
    }

    /**
     * Mock an HTTP Response with the given elements.
     *
     * @param statusCode The status code (ex: 200)
     * @param statusMessage The status message (ex: "OK")
     * @param content The response content/body
     * @return A mocked HTTP response
     */
    public static CloseableHttpResponse mockHttpResponse(int statusCode, String statusMessage, String content, Header[] headers){
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn( new BasicStatusLine( new ProtocolVersion("HTTP", 1, 1), statusCode, statusMessage) )
                .when( response ).getStatusLine();
        doReturn( new StringEntity( content, StandardCharsets.UTF_8 ) ).when( response ).getEntity();
        if( headers != null && headers.length >= 1 ){
            doReturn( headers ).when( response ).getAllHeaders();
        } else {
            doReturn( new Header[]{} ).when( response ).getAllHeaders();
        }
        return response;
    }

}
