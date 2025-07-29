package com.hbs.entitysense.entity;

import com.hbs.entitysense.model.RiskCategory;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "watchlist_entities", schema = "entitysenseschema")
@Data
public class WatchlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;
    private String country;

    @ElementCollection
    @CollectionTable(name = "entity_accounts", joinColumns = @JoinColumn(name = "entity_id"))
    @Column(name = "account")
    private List<String> knownAccounts;

    @Enumerated(EnumType.STRING)
    private RiskCategory riskCategory;

    /**
     * Hibernate‑vector will now handle pgvector VECTOR(768) natively.
     */
    @JdbcTypeCode(SqlTypes.VECTOR)
    @Array(length = 768)
    @Column(columnDefinition = "vector(768)")
    private float[] embedding;

    private LocalDateTime createdAt = LocalDateTime.now();
}
