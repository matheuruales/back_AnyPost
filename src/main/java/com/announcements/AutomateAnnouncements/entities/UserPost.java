package com.announcements.AutomateAnnouncements.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_posts")
@Getter
@Setter
public class UserPost {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id")
    private UserProfile owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 60)
    private String status;

    @Column(name = "video_url", length = 2048)
    private String videoUrl;

    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ElementCollection
    @CollectionTable(name = "user_post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "tag_value")
    private List<String> tags = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "user_post_targets", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "target_order")
    @Column(name = "target_value")
    private List<String> targetPlatforms = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPostPublication> publications = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = "published";
        }
        if (this.publishedAt == null) {
            this.publishedAt = now;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "published";
        }
    }
}
