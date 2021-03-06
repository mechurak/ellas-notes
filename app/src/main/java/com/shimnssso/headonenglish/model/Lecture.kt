package com.shimnssso.headonenglish.model

data class Lecture(
    val date: String,
    val title: String,
    val url: String,
    val rows: List<Row>,
)

data class Row(
    val id: Int,
    val spelling: String,
    val meaning: String,
    val description: String
)

data class DomainCard(
    val date: String,
    val order: Int,

    val text: String,
    val hint: String?,
    val note: String?,
    val memo: String?,
)