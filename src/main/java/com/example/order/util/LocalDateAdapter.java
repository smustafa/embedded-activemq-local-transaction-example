package com.example.order.util;

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class LocalDateAdapter
        extends XmlAdapter<String, LocalDate> {

    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern("YYYY-MM-dd")
            .appendOffset("+HH:MM", "+00:00") // set 'noOffsetText' to desired '+00:00'
            .toFormatter();

    public LocalDate unmarshal(String inputDate) {
        return StringUtils.isNotBlank(inputDate) ? DateTimeFormatter.ISO_DATE.parse(inputDate, LocalDate::from) : null;
    }

    public String marshal(LocalDate inputDate) {
        return inputDate != null ? dateTimeFormatter.format(ZonedDateTime.of(inputDate, LocalTime.MIDNIGHT, ZoneId.systemDefault())) : null;
    }
}
