/*
 * jGnash, a personal finance application
 * Copyright (C) 2001-2014 Craig Cavanaugh
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jgnash.text;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jgnash.engine.CommodityNode;
import jgnash.engine.message.Message;
import jgnash.engine.message.MessageBus;
import jgnash.engine.message.MessageChannel;
import jgnash.engine.message.MessageListener;

/**
 * Formats commodities for display
 *
 * @author Craig Cavanaugh
 */
public abstract class CommodityFormat {

    private static final CommodityListener listener;

    private static CommodityFormat fullFormat;

    private static final Map<CommodityNode, DecimalFormat> fullInstanceMap = new HashMap<>();

    private static final Map<CommodityNode, DecimalFormat> simpleInstanceMap = new HashMap<>();

    private static final String[] ESCAPE_CHARS = new String[] { ",", ".", "0", "#", "-", ";", "%" };

    private static final boolean DEBUG = false;

    /**
     * Pre-compiled currency sign pattern
     */
    private static final Pattern CURRENCY_SIGN_PATTERN = Pattern.compile("¤");

    static {
        /*
         * Need to clear any references to CommodityNodes to prevent memory leaks when
         * files are loaded and unload.
         */
        listener = new CommodityListener();

        MessageBus.getInstance().registerListener(listener, MessageChannel.COMMODITY, MessageChannel.SYSTEM);
    }

    public static String getShortNumberPattern(final CommodityNode node) {
        DecimalFormat format = (DecimalFormat) getShortNumberFormat(node);
        String pattern = format.toPattern();

        return CURRENCY_SIGN_PATTERN.matcher(pattern).replaceAll("");
    }

    public static String getFullNumberPattern(final CommodityNode node) {

        DecimalFormat format = (DecimalFormat) getFullNumberFormat(node);
        String pattern = format.toPattern();
               
        if (pattern.charAt(0) == '\u00A4') {
            String prefix = node.getPrefix();

            // escape any special characters
            for (String escapeChar : ESCAPE_CHARS) {
                if (prefix.contains(escapeChar)) {
                    prefix = prefix.replace(escapeChar, "'" + escapeChar + "'");
                }
            }

            return pattern.replace("\u00A4", prefix);
        }

        String suffix = node.getSuffix();

        // escape any special characters
        for (String escapeChar : ESCAPE_CHARS) {
            if (suffix.contains(escapeChar)) {
                suffix = suffix.replace(escapeChar, "'" + escapeChar + "'");
            }
        }

        return pattern.replace("\u00A4", suffix);
    }

    public static NumberFormat getShortNumberFormat(final CommodityNode node) {
        DecimalFormat o = simpleInstanceMap.get(node);

        if (o != null) {
            return o;
        }

        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setCurrencySymbol("");
        df.setDecimalFormatSymbols(dfs);
        df.setMaximumFractionDigits(node.getScale());

        // required for some locale
        df.setMinimumFractionDigits(df.getMaximumFractionDigits());

        // for positive suffix padding for fraction alignment
        int negSufLen = df.getNegativeSuffix().length();
        if (negSufLen > 0) {
            char[] pad = new char[negSufLen];
            for (int i = 0; i < negSufLen; i++) {
                pad[i] = ' ';
            }
            df.setPositiveSuffix(new String(pad));
        }

        simpleInstanceMap.put(node, df);

        return df;
    }

    public static NumberFormat getFullNumberFormat(final CommodityNode node) {
        assert node != null;

        DecimalFormat o = fullInstanceMap.get(node);

        if (o != null) {
            return o;
        }

        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();

        if (DEBUG) {
            BigDecimal bd = new BigDecimal("12.34");
            System.out.println("Before");
            System.out.println(df.format(bd));
            System.out.println(df.format(bd.negate()) + '.');
            System.out.println(df.getNegativeSuffix() + '.');
            System.out.println(df.getPositiveSuffix());
        }

        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setCurrencySymbol(node.getPrefix());
        df.setDecimalFormatSymbols(dfs);
        df.setMaximumFractionDigits(node.getScale());

        // required for some locale
        df.setMinimumFractionDigits(df.getMaximumFractionDigits());

        if (node.getSuffix() != null && !node.getSuffix().isEmpty()) {
            df.setPositiveSuffix(node.getSuffix() + df.getPositiveSuffix());
            df.setNegativeSuffix(node.getSuffix() + df.getNegativeSuffix());
        }

        // for positive suffix padding for fraction alignment
        int negSufLen = df.getNegativeSuffix().length();
        int posSufLen = df.getPositiveSuffix().length();

        if (negSufLen > posSufLen) {
            StringBuilder buf = new StringBuilder(df.getPositiveSuffix());
            for (int i = negSufLen - posSufLen; i <= negSufLen; i++) {
                buf.append(' ');
            }
            df.setPositiveSuffix(buf.toString());
        } else if (posSufLen > negSufLen) {
            StringBuilder buf = new StringBuilder(df.getNegativeSuffix());
            for (int i = posSufLen - negSufLen; i <= posSufLen; i++) {
                buf.append(' ');
            }
            df.setNegativeSuffix(buf.toString());
        }

        fullInstanceMap.put(node, df);

        if (DEBUG) {
            BigDecimal bd = new BigDecimal("12.34");
            System.out.println("After");
            System.out.println(df.format(bd) + '~');
            System.out.println(df.format(bd.negate()) + '~');
        }

        return df;
    }

    public static synchronized CommodityFormat getFullFormat() {
        if (fullFormat != null) {
            return fullFormat;
        }
        return fullFormat = new FullFormat();
    }

    private static String getConversion(final String cur1, final String cur2) {
        return cur1 + " > " + cur2;
    }

    public static String getConversion(final CommodityNode cur1, final CommodityNode cur2) {
        return getConversion(cur1.getSymbol(), cur2.getSymbol());
    }

    public abstract String format(final BigDecimal value, final CommodityNode node);

    private static class FullFormat extends CommodityFormat {
        @Override
        public String format(final BigDecimal value, final CommodityNode node) {
            if (value != null && node != null) {
                return getFullNumberFormat(node).format(value.doubleValue());
            }
            return null;
        }
    }

    private static class CommodityListener implements MessageListener {

        @Override
        public void messagePosted(final Message event) {
            switch (event.getEvent()) {
                case FILE_CLOSING:
                case CURRENCY_MODIFY:
                    simpleInstanceMap.clear();
                    fullInstanceMap.clear();
                    break;
                default:
                    break;
            }
        }
    }
}