#!/bin/bash

# Compilar Main.kt con las dependencias de Jama.jar y libGrafo.jar
kotlinc -d src -cp "libs/Jama.jar:libs/libGrafo.jar" src/Main.kt

# Eliminar el directorio META-INF
rm -rf src/META-INF

# Ejecutar el archivo compilado con las dependencias
kotlin -cp "src:libs/Jama.jar:libs/libGrafo.jar" MainKt
