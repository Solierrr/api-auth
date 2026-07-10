package com.solaria.auth.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "position")
data class Position(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var name: String,

    @OneToMany(mappedBy = "position")
    val users: MutableList<User> = mutableListOf(),

    @OneToMany(mappedBy = "position")
    val permissions: MutableList<PositionPermission> = mutableListOf()
)