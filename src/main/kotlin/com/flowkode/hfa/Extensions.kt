package com.flowkode.hfa

fun List<String>?.firstOrEmpty(): String {
    return if (isNullOrEmpty()) "" else this[0]
}