package com.aaax.core.constant.enu;

import com.aaax.core.exception.BizException;
import com.aaax.core.response.SystemResponse;
import lombok.Getter;

@Getter
public enum AreaCodeCountryMapping {
    AC("247"), AD("376"), AE("971"), AF("093"), AG("268"), AI("264"), AL("355"), AM("374"),
    AO("244"), AR("054"), AS("684"), AT("043"), AU("061"), AW("297"), AX("358"), AZ("994"),
    BA("387"), BB("246"), BD("880"), BE("032"), BF("226"), BG("359"), BH("973"), BI("257"),
    BJ("229"), BL("590"), BM("441"), BN("673"), BO("591"), BQ("599"), BR("055"), BS("242"),
    BT("975"), BW("267"), BY("375"), BZ("501"), CA("001"), CC("061"), CD("243"), CF("236"),
    CG("242"), CH("041"), CI("225"), CK("682"), CL("056"), CM("237"), CN("086"), CO("057"),
    CR("506"), CU("053"), CV("238"), CW("599"), CX("061"), CY("357"), CZ("420"), DE("049"),
    DJ("253"), DK("045"), DM("767"), DO("809"), DZ("213"), EC("593"), EE("372"), EG("020"),
    EH("212"), ER("291"), ES("034"), ET("251"), FI("358"), FJ("679"), FK("500"), FM("691"),
    FO("298"), FR("033"), GA("241"), GB("044"), GD("473"), GE("995"), GF("594"), GG("044"),
    GH("233"), GI("350"), GL("299"), GM("220"), GN("224"), GP("590"), GQ("240"), GR("030"),
    GT("502"), GU("671"), GW("245"), GY("592"), HK("852"), HN("504"), HR("385"), HT("509"),
    HU("036"), ID("062"), IE("353"), IL("972"), IM("044"), IN("091"), IO("246"), IQ("964"),
    IR("098"), IS("354"), IT("039"), JE("044"), JM("876"), JO("962"), JP("081"), KE("254"),
    KG("996"), KH("855"), KI("686"), KM("269"), KN("869"), KP("850"), KR("082"), KW("965"),
    KY("345"), KZ("007"), LA("856"), LB("961"), LC("758"), LI("423"), LK("094"), LR("231"),
    LS("266"), LT("370"), LU("352"), LV("371"), LY("218"), MA("212"), MC("377"), MD("373"),
    ME("382"), MF("590"), MG("261"), MH("692"), MK("389"), ML("223"), MM("095"), MN("976"),
    MO("853"), MP("670"), MQ("596"), MR("222"), MS("664"), MT("356"), MU("230"), MV("960"),
    MW("265"), MX("052"), MY("060"), MZ("258"), NA("264"), NC("687"), NE("227"), NF("672"),
    NG("234"), NI("505"), NL("031"), NO("047"), NP("977"), NR("674"), NU("683"), NZ("064"),
    OM("968"), PA("507"), PE("051"), PF("689"), PG("675"), PH("063"), PK("092"), PL("048"),
    PM("508"), PR("787"), PS("970"), PT("351"), PW("680"), PY("595"), QA("974"), RE("262"),
    RO("040"), RS("381"), RU("007"), RW("250"), SA("966"), SB("677"), SC("248"), SD("249"),
    SE("046"), SG("065"), SH("290"), SI("386"), SJ("047"), SK("421"), SL("232"), SM("378"),
    SN("221"), SO("252"), SR("597"), SS("211"), ST("239"), SV("503"), SX("721"), SY("963"),
    SZ("268"), TA("290"), TC("649"), TD("235"), TG("228"), TH("066"), TJ("992"), TK("690"),
    TL("670"), TM("993"), TN("216"), TO("676"), TR("090"), TT("868"), TV("688"), TW("886"),
    TZ("255"), UA("380"), UG("256"), US("001"), UY("598"), UZ("998"), VA("039"), VC("784"),
    VE("058"), VG("284"), VI("340"), VN("084"), VU("678"), WF("681"), WS("685"), XK("383"),
    YE("967"), YT("262"), ZA("027"), ZM("260"), ZW("263");

    private final String areaCode;

    AreaCodeCountryMapping(String areaCode) {
        this.areaCode = areaCode;
    }

    public static AreaCodeCountryMapping get(String areaCode, String country) {
        String message = String.format("Wrong [%s] value. mismatch [%s]", areaCode, country);
        for (AreaCodeCountryMapping value : AreaCodeCountryMapping.values()) {
            if (country.equalsIgnoreCase(value.name())){
                if (!value.areaCode.equals(areaCode)) {
                    throw new BizException(SystemResponse.PAM0400, message);
                }
                return value;
            }
        }
        throw new BizException(SystemResponse.PAM0400, message);
    }
}
