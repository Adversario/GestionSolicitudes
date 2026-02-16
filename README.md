# Gestión de Solicitudes – Desarrollo App Móviles II

Aplicación Android desarrollada como parte del proyecto progresivo del curso, donde se implementaron mejoras avanzandas de rendimiento, debugging, arquitectura y librerías externas.

## Funcionalidades principales

- Registro de solicitudes de servicio

- Edición y eliminación de solicitudes

- Persistencia local con Room

- Sincronización con API REST (Retrofit)

- Simulación de errores de red

- Procesos asincrónicos con Kotlin Coroutines

- Diagnóstico y corrección de Memory Leaks

- Animación de carga con Lottie

- Arquitectura MVVM

## Flujo funcional seleccionado

Sincronización API → Base de datos → UI

Este flujo fue elegido por ser crítico, ya que involucra:

Llamadas a red

Procesamiento de datos

Escritura en base de datos

Actualización de UI

Fue optimizado usando Coroutines para evitar bloqueo del hilo principal.

## Procesos en segundo plano

Se implementaron Kotlin Coroutines con Dispatchers.IO en:

Sincronización de datos desde API

Inserción en Room

Eliminación de registros

Esto garantiza:

UI fluida

No ANR

Mejor experiencia de usuario

## Debugging y manejo de errores

Se aplicaron:

try-catch en operaciones críticas

Logs en Logcat con tags personalizados

Simulación real de error de red

Mensajes controlados mediante UiState

Ejemplo de error simulado:

Unable to resolve host "jsonplaceholder.typicode.com.invalid"

## Diagnóstico de Memory Leaks

Se utilizó LeakCanary para:

Detectar fuga intencional de Activity

Analizar Heap Trace

Confirmar liberación de objetos retenidos

Mensaje confirmado tras corrección:

All retained objects were garbage collected

# Librerías externas integradas

## Retrofit

Para consumo de API REST.

## Lottie

Para animación de carga.

# Arquitectura aplicada

Se implementó patrón MVVM:

UI (Activities)

ViewModel

Repository

Data (Room + Retrofit)

Separación clara de responsabilidades.