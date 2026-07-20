package com.solaria.auth.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "address")
class Address(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null,

    @Column(nullable = false, length = 2)
    var state: String = "",

    @Column(nullable = false)
    var city: String = "",

    @Column(nullable = false)
    var neighborhood: String = "",

    @Column(name = "zip_code", nullable = false, length = 8)
    var zipCode: String = "",

    @Column(nullable = false)
    var street: String = "",

    @Column(nullable = false, length = 10)
    var number: String = ""
) {
    @OneToOne(mappedBy = "address")
    var geolocalization: Geolocalization? = null

    @OneToMany(mappedBy = "address")
    var profiles: MutableSet<Profile> = mutableSetOf()
}
