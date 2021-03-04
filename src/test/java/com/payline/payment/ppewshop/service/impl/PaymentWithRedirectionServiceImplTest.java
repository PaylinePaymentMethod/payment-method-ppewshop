package com.payline.payment.ppewshop.service.impl;

import com.payline.payment.ppewshop.MockUtils;
import com.payline.payment.ppewshop.bean.common.CheckStatusOut;
import com.payline.payment.ppewshop.bean.configuration.RequestConfiguration;
import com.payline.payment.ppewshop.bean.response.CheckStatusResponse;
import com.payline.payment.ppewshop.bean.response.PpewShopResponseKO;
import com.payline.payment.ppewshop.exception.PluginException;
import com.payline.payment.ppewshop.service.HttpService;
import com.payline.pmapi.bean.common.FailureCause;
import com.payline.pmapi.bean.common.OnHoldCause;
import com.payline.pmapi.bean.payment.response.PaymentResponse;
import com.payline.pmapi.bean.payment.response.buyerpaymentidentifier.impl.Email;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseFailure;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseOnHold;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseRedirect;
import com.payline.pmapi.bean.payment.response.impl.PaymentResponseSuccess;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

class PaymentWithRedirectionServiceImplTest {

    @InjectMocks
    private PaymentWithRedirectionServiceImpl service = new PaymentWithRedirectionServiceImpl();

    @Mock
    private HttpService httpService = HttpService.getInstance();


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }


    private static Stream<Arguments> statusCode_set() {
        return Stream.of(
                Arguments.of(CheckStatusOut.StatusCode.A, PaymentResponseSuccess.class, null),
                Arguments.of(CheckStatusOut.StatusCode.E, PaymentResponseSuccess.class, null),
                Arguments.of(CheckStatusOut.StatusCode.I, PaymentResponseRedirect.class, null),
                Arguments.of(CheckStatusOut.StatusCode.R, PaymentResponseFailure.class, FailureCause.REFUSED),
                Arguments.of(CheckStatusOut.StatusCode.C, PaymentResponseFailure.class, FailureCause.CANCEL)
        );
    }

    @ParameterizedTest
    @MethodSource("statusCode_set")
    void retrieveTransactionStatus(CheckStatusOut.StatusCode statusCode, Class responseClass, FailureCause cause) throws Exception {
        String xmlOK = MockUtils.templateCheckStatusResponse
                .replace(MockUtils.STATUS_CODE, statusCode.name());

        CheckStatusResponse checkStatusResponse = CheckStatusResponse.fromXml(xmlOK);
        Mockito.doReturn(checkStatusResponse).when(httpService).checkStatus(any(), any());

        RequestConfiguration configuration = new RequestConfiguration(
                MockUtils.aContractConfiguration()
                , MockUtils.anEnvironment()
                , MockUtils.aPartnerConfiguration()
        );

        String transactionId = "1";
        String email = "an.email@foo.bar";

        PaymentResponse response = service.retrieveTransactionStatus(configuration, transactionId, email);
        Assertions.assertEquals(responseClass, response.getClass());

        if (responseClass.equals(PaymentResponseSuccess.class)) {
            PaymentResponseSuccess responseSuccess = (PaymentResponseSuccess) response;
            Assertions.assertEquals(transactionId, responseSuccess.getPartnerTransactionId());
            Assertions.assertEquals(Email.class, responseSuccess.getTransactionDetails().getClass());
            Assertions.assertEquals("34600015", responseSuccess.getTransactionAdditionalData());

        } else if (responseClass.equals(PaymentResponseOnHold.class)) {
            PaymentResponseOnHold responseOnHold = (PaymentResponseOnHold) response;
            Assertions.assertEquals(transactionId, responseOnHold.getPartnerTransactionId());
            Assertions.assertEquals(OnHoldCause.ASYNC_RETRY, responseOnHold.getOnHoldCause());

        } else if (responseClass.equals(PaymentResponseRedirect.class)) {
            PaymentResponseRedirect responseRedirect = (PaymentResponseRedirect) response;
            Assertions.assertEquals(transactionId, responseRedirect.getPartnerTransactionId());
            Assertions.assertEquals(new URL("http://redirectionUrl.com"), responseRedirect.getRedirectionRequest().getUrl());

        } else {
            PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;
            Assertions.assertEquals(cause, responseFailure.getFailureCause());
            Assertions.assertEquals(statusCode.name(), responseFailure.getErrorCode());

        }
    }

    @Test
    void retrieveTransactionStatusUrlException() {
        // init Mock
        String xml = MockUtils.templateCheckStatusResponse.replace("http://redirectionUrl.com", "aMalformedUrl").replace(MockUtils.STATUS_CODE, "I");
        CheckStatusResponse checkStatusResponse = CheckStatusResponse.fromXml(xml);
        Mockito.doReturn(checkStatusResponse).when(httpService).checkStatus(Mockito.any(), Mockito.any());


        RequestConfiguration configuration = new RequestConfiguration(
                MockUtils.aContractConfiguration()
                , MockUtils.anEnvironment()
                , MockUtils.aPartnerConfiguration()
        );

        String transactionId = "1";
        String email = "an.email@foo.bar";

        PaymentResponse response = service.retrieveTransactionStatus(configuration, transactionId, email);

        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
        PaymentResponseFailure responseFailure = (PaymentResponseFailure) response;
        Assertions.assertEquals(FailureCause.INVALID_DATA, responseFailure.getFailureCause());
    }


    @Test
    void retrieveTransactionStatusException() {
        PluginException exception = new PluginException(PpewShopResponseKO.ErrorCode.CODE_21999.code, FailureCause.INVALID_DATA);
        Mockito.doThrow(exception).when(httpService).checkStatus(any(), any());

        RequestConfiguration configuration = new RequestConfiguration(
                MockUtils.aContractConfiguration()
                , MockUtils.anEnvironment()
                , MockUtils.aPartnerConfiguration()
        );

        String transactionId = "1";
        String email = "an.email@foo.bar";

        PaymentResponse response = service.retrieveTransactionStatus(configuration, transactionId, email);
        Assertions.assertEquals(PaymentResponseFailure.class, response.getClass());
    }

}