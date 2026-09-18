package com.drbep.tvplayer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class VodCatalogOrder {
    private static final Pattern DATE = Pattern.compile("^([12][0-9]{3})(?:-([0-9]{2})-([0-9]{2}))?");
    static int dateKey(String value) {
        Matcher m = DATE.matcher(value == null ? "" : value.trim());
        if (!m.find()) return 0;
        int year = Integer.parseInt(m.group(1));
        int month = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        int day = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        if (month > 12 || day > 31) return 0;
        return year * 10000 + month * 100 + day;
    }
    static int dateKey(ChannelItem item) {
        if (item == null) return 0;
        int date = dateKey(item.vodReleaseDate);
        if (date == 0) date = dateKey(item.vodYear);
        if (date == 0) date = dateKey(item.daznStart);
        if (date == 0 && "SkyShowtime".equals(item.platformName)) {
            Matcher year = Pattern.compile("\\(([12][0-9]{3})\\)$").matcher(item.name.trim());
            if (year.find()) date = dateKey(year.group(1));
        }
        return date;
    }
    static void newest(List<ChannelItem> items) {
        items.sort((a, b) -> {
            int date = Integer.compare(dateKey(b), dateKey(a));
            if (date != 0) return date;
            int title = a.name.compareToIgnoreCase(b.name);
            return title != 0 ? title : a.id.compareTo(b.id);
        });
    }
}
