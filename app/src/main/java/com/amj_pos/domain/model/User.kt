package com.amj_pos.domain.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "employee", // "owner" or "employee"
    val assigned_branch: String = ""
)
