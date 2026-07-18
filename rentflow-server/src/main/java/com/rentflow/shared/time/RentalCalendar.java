package com.rentflow.shared.time;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@ConfigurationProperties(prefix = "rentflow.rental")
public class RentalCalendar {
    private ZoneId businessZone = ZoneId.of("Asia/Shanghai");

    public ZoneId businessZone() {
        return businessZone;
    }

    public void setBusinessZone(ZoneId businessZone) {
        this.businessZone = businessZone;
    }

    public LocalDate currentDate(Instant instant) {
        return instant.atZone(businessZone).toLocalDate();
    }
}
