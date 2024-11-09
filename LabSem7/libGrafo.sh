#!/bin/bash

# Compilar los archivos Kotlin en el directorio ve/usb/libGrafo/
kotlinc -d . ve/usb/libGrafo/*.kt

# Crear el archivo JAR en la carpeta libs
jar cf libs/libGrafo.jar -C . .

# Eliminar el directorio META-INF
rm -rf META-INF
