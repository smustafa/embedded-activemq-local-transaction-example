package com.example.order.util;

import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class LocalDateTimeAdapter
        extends XmlAdapter<String, LocalDateTime> {

    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
            .appendPattern("YYYY-MM-dd")
            .appendLiteral("T")
            .appendPattern("HH:mm:ss.SSS")
            .appendOffset("+HH:MM", "+00:00") // set 'noOffsetText' to desired '+00:00'
            .toFormatter();

    public LocalDateTime unmarshal(String inputDateTime) {
        return StringUtils.isNotBlank(inputDateTime) ? DateTimeFormatter.ISO_DATE_TIME.parse(inputDateTime, LocalDateTime::from) : null;
    }

    public String marshal(LocalDateTime inputDateTime) {
        return inputDateTime != null ? dateTimeFormatter.format(ZonedDateTime.of(inputDateTime, ZoneId.systemDefault())) : null;
    }
}

