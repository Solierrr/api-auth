package com.solaria.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.util.UUID

@Entity
@Table(name = "geolocalization")
class Geolocalization(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fk_address", nullable = false, unique = true)
    var address: Address? = null,

    @Column(nullable = false, precision = 10, scale = 7)
    var latitude: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 7)
    var longitude: BigDecimal = BigDecimal.ZERO
)
