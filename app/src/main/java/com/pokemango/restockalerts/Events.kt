package com.pokemango.restockalerts

object Events {
    @Volatile var onHistory: (() -> Unit)? = null
    @Volatile var onToken: (() -> Unit)? = null
}
