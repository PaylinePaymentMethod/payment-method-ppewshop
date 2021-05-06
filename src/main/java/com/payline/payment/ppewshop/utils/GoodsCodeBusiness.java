package com.payline.payment.ppewshop.utils;

import java.util.HashMap;
import java.util.Map;

public class GoodsCodeBusiness {
    static Map<String, String> goodsCodes = new HashMap<>();

    static {
        goodsCodes.put("1", "625");
        goodsCodes.put("100010001", "625");
        goodsCodes.put("100010002", "626");
        goodsCodes.put("100010003", "626");
        goodsCodes.put("2", "610");
        goodsCodes.put("20001", "611");
        goodsCodes.put("200010001", "624");
        goodsCodes.put("200010002", "611");
        goodsCodes.put("200010003", "615");
        goodsCodes.put("200010004", "623");
        goodsCodes.put("200010005", "623");
        goodsCodes.put("200010006", "622");
        goodsCodes.put("200010007", "613");
        goodsCodes.put("3", "621");
        goodsCodes.put("4", "320");
        goodsCodes.put("40001", "322");
        goodsCodes.put("400010001", "326");
        goodsCodes.put("400010002", "327");
        goodsCodes.put("40002", "320");
        goodsCodes.put("400020001", "323");
        goodsCodes.put("400020002", "324");
        goodsCodes.put("400020003", "329");
        goodsCodes.put("40003", "328");
        goodsCodes.put("400030001", "328");
        goodsCodes.put("400030002", "328");
        goodsCodes.put("5", "663");
        goodsCodes.put("50001", "913");
        goodsCodes.put("500010001", "733");
        goodsCodes.put("50002", "913");
        goodsCodes.put("50003", "663");
        goodsCodes.put("500030001", "737");
        goodsCodes.put("50004", "663");
        goodsCodes.put("500040001", "738");
        goodsCodes.put("500040002", "739");
        goodsCodes.put("500040003", "740");
        goodsCodes.put("500040004", "741");
        goodsCodes.put("599990001", "941");
        goodsCodes.put("599990002", "912");
        goodsCodes.put("6", "640");
        goodsCodes.put("7", "660");
        goodsCodes.put("8", "730");
        goodsCodes.put("9", "660");
        goodsCodes.put("10", "660");
        goodsCodes.put("11", "660");
        goodsCodes.put("110001", "742");
        goodsCodes.put("12", "330");
        goodsCodes.put("120001", "331");
        goodsCodes.put("1200010001", "334");
        goodsCodes.put("1200010002", "336");
        goodsCodes.put("1200010003", "337");
        goodsCodes.put("1200010004", "337");
        goodsCodes.put("120002", "332");
        goodsCodes.put("120003", "339");
        goodsCodes.put("1200030001", "338");
        goodsCodes.put("120004", "341");
        goodsCodes.put("120005", "340");
        goodsCodes.put("120006", "343");
        goodsCodes.put("120007", "342");
        goodsCodes.put("120008", "330");
        goodsCodes.put("1200080001", "660");
        goodsCodes.put("13", "000");
        goodsCodes.put("14", "660");
        goodsCodes.put("15", "660");
        goodsCodes.put("16", "000");
        goodsCodes.put("17", "000");
        goodsCodes.put("170001", "000");
        goodsCodes.put("170002", "000");
        goodsCodes.put("18", "660");
        goodsCodes.put("19", "660");
        goodsCodes.put("20", "631");
        goodsCodes.put("21", "000");
        goodsCodes.put("22", "000");
        goodsCodes.put("23", "000");
        goodsCodes.put("24", "858");
        goodsCodes.put("240001", "858");
        goodsCodes.put("2400010001", "855");
        goodsCodes.put("2400010002", "857");
        goodsCodes.put("25", "650");
        goodsCodes.put("26", "620");
    }


    private GoodsCodeBusiness() {
    }

    private static class Holder {
        private static final GoodsCodeBusiness instance = new GoodsCodeBusiness();
    }


    public static GoodsCodeBusiness getInstance() {
        return GoodsCodeBusiness.Holder.instance;
    }
    // --- Singleton Holder pattern + initialization END




    /**
     * Return a PPEW category mapped from Payline category
     * 000 codes are for non mappable category
     *
     * @param cat Payline category to convert
     * @return the PPWE goods code
     */
    public String getGoodsCode(String cat) {
        String goodsCode = goodsCodes.get(cat);
        if (goodsCode == null) goodsCode = "000";

        return goodsCode;
    }
}
