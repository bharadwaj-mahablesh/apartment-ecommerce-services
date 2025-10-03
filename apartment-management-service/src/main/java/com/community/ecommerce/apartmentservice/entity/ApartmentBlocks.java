package com.community.ecommerce.apartmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "apartment_blocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentBlocks {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_blocks_seq")
    @SequenceGenerator(name = "apartment_blocks_seq", sequenceName = "apartment_blocks_id_seq")
    private Long id;

    @Column(name = "block_name")
    private String blockName;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
