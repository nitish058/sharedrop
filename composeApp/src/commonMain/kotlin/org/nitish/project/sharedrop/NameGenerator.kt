package org.nitish.project.sharedrop

object NameGenerator {
    private val adjectives = listOf("Cosmic", "Silent", "Neon", "Frozen", "Thunder", "Iron")
    private val animals = listOf("Panda", "Falcon", "Wolf", "Bear", "Fox", "Hawk", "Tiger")

    fun generate(): String {
        return "${adjectives.random()} ${animals.random()}"
    }
}