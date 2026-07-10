package com.solaria.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "company")
data class Company(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var corporateName: String,

    @Column(nullable = false, unique = true)
    var cnpj: String,

    @Column(nullable = false)
    var email: String,

    @OneToMany(mappedBy = "company")
    val users: MutableList<User> = mutableListOf()
)