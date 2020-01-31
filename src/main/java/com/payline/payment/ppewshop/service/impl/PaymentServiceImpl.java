package com.payline.payment.ppewshop.service.impl;

import com.payline.payment.ppewshop.bean.common.*;
import com.payline.payment.ppewshop.bean.configuration.RequestConfiguration;
import com.payline.payment.ppewshop.bean.request.InitDossierRequest;
import com.payline.payment.ppewshop.bean.response.InitDossierResponse;
import com.payline.payment.ppewshop.exception.InvalidDataException;
import com.payline.payment.ppewshop.exception.PluginException;
import com.payline.payment.ppewshop.utils.Constants;
import com.payline.payment.ppewshop.utils.PluginUtils;
import com.payline.payment.ppewshop.utils.http.HttpClient;
import com.payline.pmapi.bean.common.Buyer;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.Order;
import com.payline.pmapi.bean.payment.RequestContext;
import com.payline.pmapi.bean.payment.request.PaymentRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentService;
import org.apache.logging.log4j.Logger;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class PaymentServiceImpl implements PaymentService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentServiceImpl.class);
    private HttpClient client = HttpClient.getInstance();

    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");


    @Override
    public PaymentResponse paymentRequest(PaymentRequest request) {
        try {
            final RequestConfiguration configuration = new RequestConfiguration(
                    request.getContractConfiguration()
                    , request.getEnvironment()
                    , request.getPartnerConfiguration()
            );

            InitDossierRequest initDossierRequest = createInitDossierFromPaymentRequest(request);
            InitDossierResponse initDossierResponse = client.initDossier(configuration, initDossierRequest);

            PaymentResponseRedirect.RedirectionRequest redirectionRequest = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder
                    .aRedirectionRequest()
                    .withUrl(new URL(initDossierResponse.getInitDossierOut().getRedirectionUrl()))
                    .withRequestType(PaymentResponseRedirect.RedirectionRequest.RequestType.GET)
                    .build();

            Map<String, String> requestData = new HashMap<>();
            requestData.put(Constants.RequestContextKeys.PARTNER_TRANSACTION_ID, initDossierResponse.getInitDossierOut().getTransactionId());
            RequestContext context = RequestContext.RequestContextBuilder
                    .aRequestContext()
                    .withRequestData(requestData)
                    .build();

            return PaymentResponseRedirect.PaymentResponseRedirectBuilder
                    .aPaymentResponseRedirect()
                    .withPartnerTransactionId(initDossierResponse.getInitDossierOut().getTransactionId())
                    .withRedirectionRequest(redirectionRequest)
                    .withRequestContext(context)
                    .build();

        } catch (MalformedURLException e) {
            String errorMessage = "Invalid URL format";
            LOGGER.error(errorMessage, e);
            return PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginUtils.truncate(errorMessage, PluginException.ERROR_CODE_MAX_LENGTH))
                    .withFailureCause(FailureCause.INVALID_DATA)
                    .build();
        } catch (PluginException e) {
            return e.toPaymentResponseFailureBuilder().build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            return PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
    }


    InitDossierRequest createInitDossierFromPaymentRequest(PaymentRequest request) {
        MerchantInformation merchantInformation = MerchantInformation.Builder.aMerchantInformation()
                .withMerchantCode(request.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.MERCHANT_CODE).getValue())
                .withDistributorNumber(request.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.DISTRIBUTOR_NUMBER).getValue())
                .withCountryCode(request.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.COUNTRY_CODE).getValue())
                .build();

        MerchantConfiguration merchantConfiguration = MerchantConfiguration.Builder.aMerchantConfiguration()
                .withGuardBackUrl(request.getEnvironment().getNotificationURL())
                .withGuardPushUrl(request.getEnvironment().getRedirectionReturnURL())
                .build();

        Buyer buyer = request.getBuyer();

        if (buyer.getFullName() == null){
            throw new InvalidDataException("paymentRequest.buyer.fullName is required");
        }

        if (buyer.getAddressForType(Buyer.AddressType.BILLING) == null){
            throw new InvalidDataException("paymentRequest.buyer.address(BILLING) is required");
        }

        CustomerInformation customerInformation = CustomerInformation.Builder.aCustomerInformation()
                .withCustomerLanguage(request.getLocale().getISO3Country())
                .withTitle(PluginUtils.getCivilityFromPayline(request.getBuyer().getFullName().getCivility()))
                .withFirstName(request.getBuyer().getFullName().getFirstName())
                .withName(request.getBuyer().getFullName().getLastName())
                .withBirthDate(simpleDateFormat.format(request.getBuyer().getBirthday()))
                .withEmail(request.getBuyer().getEmail())
                .withAddressLine1(request.getBuyer().getAddressForType(Buyer.AddressType.BILLING).getStreet1())
                .withAddressLine2(request.getBuyer().getAddressForType(Buyer.AddressType.BILLING).getStreet2())
                .withCity(request.getBuyer().getAddressForType(Buyer.AddressType.BILLING).getCity())
                .withPostCode(request.getBuyer().getAddressForType(Buyer.AddressType.BILLING).getZipCode())
                .withCellPhoneNumber(request.getBuyer().getPhoneNumberForType(Buyer.PhoneNumberType.CELLULAR))
                .withPrivatePhoneNumber(request.getBuyer().getPhoneNumberForType(Buyer.PhoneNumberType.HOME))
                .withProfessionalPhoneNumber(request.getBuyer().getPhoneNumberForType(Buyer.PhoneNumberType.WORK))
                .build();

        String category = PluginUtils.getGoodsCode(getMainCat(request.getOrder().getItems()));
        OrderInformation orderInformation = OrderInformation.Builder.anOrderInformation()
                .withGoodsCode(category)
                .withPrice(PluginUtils.createStringAmount(request.getAmount().getAmountInSmallestUnit(), request.getAmount().getCurrency()))
                .withFinancialProductType(OrderInformation.CLA)
                .build();

        MerchantOrderReference orderReference = MerchantOrderReference.Builder.aMerchantOrderReference()
                .withMerchantOrderId(request.getTransactionId())
                .withMerchantRef(request.getOrder().getReference())
                .build();

        InitDossierRequest initDossierRequest = new InitDossierRequest();
        initDossierRequest.getInitDossierIn().setMerchantInformation(merchantInformation);
        initDossierRequest.getInitDossierIn().setMerchantConfiguration(merchantConfiguration);
        initDossierRequest.getInitDossierIn().setCustomerInformation(customerInformation);
        initDossierRequest.getInitDossierIn().setOrderInformation(orderInformation);
        initDossierRequest.getInitDossierIn().setOrderReference(orderReference);

        return initDossierRequest;
    }

    /**
     * Get the main category of a list of items
     * The main category is the category having the maximum amount
     *
     * @param items list of Payline item
     * @return category having the maximum amount
     */
    String getMainCat(List<Order.OrderItem> items) {
        Map<String, BigInteger> categories = new HashMap<>();

        for (Order.OrderItem item : items) {
            // get category and amount values
            String cat = item.getCategory();
            BigInteger amount = item.getAmount().getAmountInSmallestUnit()
                    .multiply(BigInteger.valueOf(item.getQuantity()));

            // if category doesn't exists in the Map, create it
            categories.putIfAbsent(cat, BigInteger.ZERO);

            // add item amount to the total amount of this category
            categories.computeIfPresent(cat, (k, v) -> v.add(amount));
        }

        // return the category having the biggest total amount
        Map.Entry<String, BigInteger> maxEntry = Collections.max(categories.entrySet()
                , Comparator.comparing(Map.Entry::getValue));
        return maxEntry.getKey();
    }

}