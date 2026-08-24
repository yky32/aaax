package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import lombok.Getter;

import java.math.RoundingMode;
import java.util.Arrays;

public enum Currency {

    // =========== CurrencyType.FIAT
    JPY("JPY", 0, RoundingMode.DOWN, CurrencyType.FIAT),
    HKD("HKD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CNY("CNY", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    USD("USD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    GBP("GBP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    AUD("AUD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    EUR("EUR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CAD("CAD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CHF("CHF", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SGD("SGD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SEK("SEK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    KRW("KRW", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    NOK("NOK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    NZD("NZD", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    INR("INR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    MXN("MXN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    TWD("TWD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ZAR("ZAR", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BRL("BRL", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    DKK("DKK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PLN("PLN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    THB("THB", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ILS("ILS", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    IDR("IDR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CZK("CZK", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    AED("AED", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    TRY("TRY", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    HUF("HUF", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    CLP("CLP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    SAR("SAR", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PHP("PHP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    MYR("MYR", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    COP("COP", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    RUB("RUB", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    RON("RON", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    PEN("PEN", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BHD("BHD", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    BGN("BGN", 4, RoundingMode.HALF_UP, CurrencyType.FIAT),
    ARS("ARS", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),
    VND("VND", 2, RoundingMode.HALF_UP, CurrencyType.FIAT),

    // =========== CurrencyType.CRYPTO
    BTC("BTC", 8, RoundingMode.HALF_UP, CurrencyType.CRYPTO),
    ETH("ETH", 18, RoundingMode.HALF_UP, CurrencyType.CRYPTO),

    // =========== CurrencyType.LOYALTY_POINT
    LP("LP", 0, RoundingMode.DOWN, CurrencyType.LOYALTY_POINT),


    ALL("ALL", 2, RoundingMode.HALF_UP, CurrencyType.ALL)
    ;


    @Getter
    private final String isoCode;
    @Getter
    private final int decimalPlaces;
    @Getter
    private final RoundingMode roundingMode;
    @Getter
    private final CurrencyType type;

    Currency(String isoCode, int decimalPlaces, RoundingMode roundingMode, CurrencyType type) {
        this.isoCode = isoCode;
        this.decimalPlaces = decimalPlaces;
        this.roundingMode = roundingMode;
        this.type = type;
    }

    public static Currency get(String input) {
        for (Currency value : Currency.values()) {
            if (input.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        String message = String.format("Wrong [%s] value. [%s] not in -> %s", input, input, Arrays.asList(Currency.values()));
        throw new BizException(SystemResponse.PAM0400, message);
    }

}
