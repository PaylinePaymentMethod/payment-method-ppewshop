package com.payline.payment.ppewshop.service.impl;

import com.payline.payment.ppewshop.bean.common.CheckStatusIn;
import com.payline.payment.ppewshop.bean.common.CheckStatusOut;
import com.payline.payment.ppewshop.bean.common.MerchantInformation;
import com.payline.payment.ppewshop.bean.configuration.RequestConfiguration;
import com.payline.payment.ppewshop.bean.request.CheckStatusRequest;
import com.payline.payment.ppewshop.bean.response.CheckStatusResponse;
import com.payline.payment.ppewshop.exception.PluginException;
import com.payline.payment.ppewshop.service.HttpService;
import com.payline.payment.ppewshop.utils.Constants;
import com.payline.payment.ppewshop.utils.PluginUtils;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.payment.request.RedirectionPaymentRequest;
import com.payline.pmapi.bean.payment.request.TransactionStatusRequest;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.Email;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import com.payline.pmapi.logger.LogManager;
import com.payline.pmapi.service.PaymentWithRedirectionService;
import org.apache.logging.log4j.Logger;

import java.net.MalformedURLException;
import java.net.URL;

public class PaymentWithRedirectionServiceImpl implements PaymentWithRedirectionService {
    private static final Logger LOGGER = LogManager.getLogger(PaymentWithRedirectionServiceImpl.class);

    private HttpService httpService = HttpService.getInstance();

    @Override
    public PaymentResponse finalizeRedirectionPayment(RedirectionPaymentRequest request) {
        RequestConfiguration configuration = new RequestConfiguration(
                request.getContractConfiguration()
                , request.getEnvironment()
                , request.getPartnerConfiguration()
        );

        String partnerTransactionId = request.getRequestContext().getRequestData().get(Constants.RequestContextKeys.PARTNER_TRANSACTION_ID);
        String buyerPaymentId = request.getBuyer().getEmail();
        return retrieveTransactionStatus(configuration, partnerTransactionId, buyerPaymentId);
    }

    @Override
    public PaymentResponse handleSessionExpired(TransactionStatusRequest request) {
        RequestConfiguration configuration = new RequestConfiguration(
                request.getContractConfiguration()
                , request.getEnvironment()
                , request.getPartnerConfiguration()
        );

        String partnerTransactionId = request.getTransactionId();
        String buyerPaymentId = request.getBuyer().getEmail();
        return retrieveTransactionStatus(configuration, partnerTransactionId, buyerPaymentId);

    }

    PaymentResponse retrieveTransactionStatus(RequestConfiguration configuration, String transactionId, String email) {
        PaymentResponse paymentResponse;

        try {
            CheckStatusRequest checkStatusRequest = createCheckStatusRequest(configuration, transactionId);
            CheckStatusResponse checkStatusResponse = httpService.checkStatus(configuration, checkStatusRequest);

            CheckStatusOut.StatusCode statusCode = checkStatusResponse.getCheckStatusOut().getStatusCode();
            switch (statusCode) {
                case A:
                case E:
                    paymentResponse = createPaymentResponseSuccess(transactionId
                            , statusCode
                            , email
                            , checkStatusResponse.getCheckStatusOut().getCreditAuthorizationNumber());
                    break;
                case I:
                    paymentResponse = createResponseRedirect(transactionId
                            , statusCode
                            , checkStatusResponse.getCheckStatusOut().getRedirectionUrl());
                    break;
                case C:
                    paymentResponse = createPaymentResponseFailure(transactionId, statusCode.name(), FailureCause.CANCEL);
                    break;
                case R:
                    paymentResponse = createPaymentResponseFailure(transactionId, statusCode.name(), FailureCause.REFUSED);
                    break;
                default:
                    paymentResponse = createPaymentResponseFailure(transactionId, statusCode.name(), FailureCause.INVALID_DATA);
                    break;
            }

        } catch (MalformedURLException e) {
            String errorMessage = "Invalid URL format";
            LOGGER.error(errorMessage, e);
            paymentResponse = createPaymentResponseFailure(transactionId
                    , PluginUtils.truncate(errorMessage, PluginException.ERROR_CODE_MAX_LENGTH)
                    , FailureCause.INVALID_DATA);
        } catch (PluginException e) {
            paymentResponse = e.toPaymentResponseFailureBuilder().build();
        } catch (RuntimeException e) {
            LOGGER.error("Unexpected plugin error", e);
            paymentResponse = PaymentResponseFailure.PaymentResponseFailureBuilder
                    .aPaymentResponseFailure()
                    .withErrorCode(PluginException.runtimeErrorCode(e))
                    .withFailureCause(FailureCause.INTERNAL_ERROR)
                    .build();
        }
        return paymentResponse;
    }

    private CheckStatusRequest createCheckStatusRequest(RequestConfiguration configuration, String transactionId) {
        MerchantInformation merchantInformation = MerchantInformation.Builder
                .aMerchantInformation()
                .withMerchantCode(configuration.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.MERCHANT_CODE).getValue())
                .withDistributorNumber(configuration.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.DISTRIBUTOR_NUMBER).getValue())
                .withCountryCode(configuration.getContractConfiguration().getProperty(Constants.ContractConfigurationKeys.COUNTRY_CODE).getValue())
                .build();

        CheckStatusIn checkStatusIn = CheckStatusIn.Builder
                .aCheckStatusIn()
                .withTransactionId(transactionId)
                .withMerchantInformation(merchantInformation)
                .build();

        return new CheckStatusRequest(checkStatusIn);
    }

    private PaymentResponseSuccess createPaymentResponseSuccess(String partnerTransactionId, CheckStatusOut.StatusCode statusCode, String email, String additionalData) {
        return PaymentResponseSuccess.PaymentResponseSuccessBuilder
                .aPaymentResponseSuccess()
                .withPartnerTransactionId(partnerTransactionId)
                .withTransactionDetails(Email.EmailBuilder.anEmail().withEmail(email).build())
                .withTransactionAdditionalData(additionalData)
                .withStatusCode(statusCode.name())
                .build();
    }

    private PaymentResponseRedirect createResponseRedirect(String partnerTransactionId, CheckStatusOut.StatusCode  statusCode, String url) throws MalformedURLException {
        PaymentResponseRedirect.RedirectionRequest redirectionRequest = PaymentResponseRedirect.RedirectionRequest.RedirectionRequestBuilder
                .aRedirectionRequest()
                .withUrl(new URL(url))
                .withRequestType(PaymentResponseRedirect.RedirectionRequest.RequestType.GET)
                .build();

        return PaymentResponseRedirect.PaymentResponseRedirectBuilder
                .aPaymentResponseRedirect()
                .withPartnerTransactionId(partnerTransactionId)
                .withStatusCode(statusCode.name())
                .withRedirectionRequest(redirectionRequest)
                .build();
    }

    private PaymentResponseFailure createPaymentResponseFailure(String partnerTransactionId, String statusCode, FailureCause cause) {
        return PaymentResponseFailure.PaymentResponseFailureBuilder
                .aPaymentResponseFailure()
                .withPartnerTransactionId(partnerTransactionId)
                .withErrorCode(statusCode)
                .withFailureCause(cause)
                .build();
    }
}
