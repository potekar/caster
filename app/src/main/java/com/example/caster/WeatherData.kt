package com.example.caster

data class WeatherResponse(
    val weather: List<Weather>,
    val main: Main,
    val wind: Wind,
    val name: String
)

data class Weather(
    val description: String,
    val main: String
)

data class Main(
    val temp: Double,
    val humidity: Int,
    val pressure: Int
)

data class Wind(
    val speed: Double
) 