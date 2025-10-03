package com.community.ecommerce.apartmentservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "apartment_contacts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentContacts {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "apartment_contacts_seq")
    @SequenceGenerator(name = "apartment_contacts_seq", sequenceName = "apartment_contacts_id_seq")
    private Long id;

    @Column(name = "contact_name")
    private String contactName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone_number")
    private String contactPhoneNumber;

    @ManyToOne
    @JoinColumn(name = "apartment_id")
    private Apartment apartment;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;
}
