package com.example.demo.calendar;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

/** 일정 한 건을 전달한다. 종일 일정의 시작·종료 시간은 null이다. */
public record CalendarEvent(String id, String title, EventType type,
                            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
                            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime startTime,
                            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm") LocalTime endTime,
                            String description, String sourceLink) {}
