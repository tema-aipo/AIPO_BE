package com.aipo.backend.domain.ipo.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "ipo_schedule",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ipo_schedule_stock_type",
                        columnNames = {"stock_id", "schedule_type"}
                )
        }
)
public class IpoSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private IpoStock stock;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    private ScheduleType scheduleType;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static IpoSchedule create(IpoStock stock, ScheduleType scheduleType, LocalDate scheduleDate, String note) {
        IpoSchedule schedule = new IpoSchedule();
        schedule.stock = stock;
        schedule.scheduleType = scheduleType;
        schedule.scheduleDate = scheduleDate;
        schedule.note = note;
        schedule.createdAt = LocalDateTime.now();
        schedule.updatedAt = schedule.createdAt;
        return schedule;
    }

    public void updateDate(LocalDate scheduleDate, String note) {
        this.scheduleDate = scheduleDate;
        this.note = note;
        this.updatedAt = LocalDateTime.now();
    }
}
