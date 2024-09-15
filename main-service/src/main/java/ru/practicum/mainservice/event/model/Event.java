package ru.practicum.mainservice.event.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.mainservice.category.model.Category;
import ru.practicum.mainservice.user.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 2000)
    private String annotation;
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(name = "confirmed_requests")
    private Long confirmedRequests;
    @Column(name = "created_on", nullable = false)
    private LocalDateTime createdOn;
    @Column(nullable = false, length = 7000)
    private String description;
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;
    @Embedded
    private Location location;
    @Column(nullable = false)
    private boolean paid;
    @Builder.Default
    @Column(name = "participant_limit", nullable = false)
    private Long participantLimit = 0L;
    @Column(name = "published_on")
    private LocalDateTime publishedOn;
    @Builder.Default
    @Column(name = "request_moderation", nullable = false)
    private boolean requestModeration = true;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;
    @Column(nullable = false, length = 120)
    private String title;
    private Long views;
}
