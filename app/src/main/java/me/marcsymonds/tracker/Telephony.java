package me.marcsymonds.tracker;

import android.content.Context;
import java.util.ArrayList;

/**
 * Created by Marc on 28/02/2017.
 */

public class Telephony {
    private static ArrayList<CountryCallingCode> mCCC = new ArrayList<>();

    public static void initialise(Context context) {
        String[] cccs = context.getResources().getStringArray(R.array.country_calling_codes);

        mCCC.clear();
        for (String ccc : cccs) {
            mCCC.add(new CountryCallingCode(ccc));
        }
    }

    public static boolean sameNumbers(String num1, String num2, String countryCode) {
        if (num1.equals(num2)) {
            return true;
        }
        else if (num1.startsWith("+") && !num2.startsWith("+")) {
            return checkCountryCode(num1, num2, countryCode);
        }
        else if (!num1.startsWith("+") && num2.startsWith("+")) {
            return checkCountryCode(num2, num1, countryCode);
        }

        return false;
    }

    private static boolean checkCountryCode(String withCC, String withoutCC, String countryCode) {
        String test;

        if (!countryCode.startsWith("+")) {
            countryCode = "+" + countryCode;
        }

        if (withoutCC.startsWith("0")) {
            test = countryCode + withoutCC.substring(1);
        }
        else {
            test = countryCode + withoutCC;
        }

        return test.equals(withCC);
    }

    private static boolean checkCountryCodes(String withCC, String withoutCC) {
        String base, test;

        if (withoutCC.startsWith("0")) {
            base = withoutCC.substring(1);
        }
        else {
            base = withoutCC;
        }

        for (CountryCallingCode cc : mCCC) {
            test = "+" + cc.getCode() + base;
            if (test.equals(withCC)) {
                return true;
            }
        }

        return false;
    }

    public static class CountryCallingCode {
        private String mCode;
        private String mName;

        CountryCallingCode(String data) {
            String[] values = data.split("|");
            mCode = values[0];
            mName = values[1];
        }

        public String getCode() {
            return mCode;
        }

        public String getName() {
            return mName;
        }
    }
}
