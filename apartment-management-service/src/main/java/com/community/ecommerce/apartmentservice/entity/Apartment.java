package com.community.ecommerce.apartmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "apartments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Apartment {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_seq")
    @SequenceGenerator(name = "apartment_seq", sequenceName = "apartment_id_seq")
    private Long id;

    @Column(name = "apartment_name")
    private String apartmentName;

    @Embedded
    private ApartmentAddress address;

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ApartmentContacts> apartmentContacts = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "apartment", cascade = CascadeType.ALL)
    @Builder.Default
    private List<ApartmentBlocks> apartmentBlocks = new java.util.ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

}
