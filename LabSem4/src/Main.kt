import ve.usb.libGrafo.*
import Jama.Matrix
import java.io.File
import kotlin.reflect.full.primaryConstructor

fun getGrafo(rutaArchivo: String): Grafo? { // para leer el grafo de un archivo. De acuerdo al nombre en el archivo se crea el tipo de grafo
    val clases = arrayOf("GrafoNoDirigidoCosto","GrafoNoDirigido","GrafoDirigidoCosto","GrafoDirigido")
    for (tipoGrafo in clases) {
        if (rutaArchivo.contains(tipoGrafo)) {
            return try {
                val clase = Class.forName("ve.usb.libGrafo.${tipoGrafo}").kotlin
                val constructor = clase.constructors.find{it.parameters.size == 1 && it.parameters[0].type.classifier == String::class}
                val instancia = constructor?.call(rutaArchivo) as Grafo
                // feedback
                val V = instancia.obtenerNumeroDeVertices()
                val E = instancia.obtenerNumeroDeLados()
                for(i in 1..V) println("${File(rutaArchivo).name} $tipoGrafo V=$V E=$E $i:${instancia.adyacentes(i).map{it.elOtroVertice(i)}}")
                instancia
            } catch (e: Exception) {
                println("Error al instanciar la clase: ${e.message}")
                null
            }
        }
    }
    return null
}

fun getMatrizDeAdyacencia(g: Grafo): Matrix {
    var n = g.obtenerNumeroDeVertices()
    var A = Matrix(n, n) // El constructor por defecto inicializa en ceros
    
    for (i in 1..n) {
        for (adyacente in g.adyacentes(i)) {
            A.set(i - 1, adyacente.elOtroVertice(i) - 1, 1.0)
        }
    }
    
    return A
}

fun getMatrizDeAlcance(A: Matrix): Matrix {
    val R = A.copy()
    val n = R.rowDimension
    
    for (k in 0 until n) {
        for (i in 0 until n) {
            for (j in 0 until n) {
                if (R.get(i, j) == 0.0 && R.get(i, k) == 1.0 && R.get(k, j) == 1.0) {
                    R.set(i, j, 1.0)
                }
            }
        }
    }
    
    return R
}

fun getComponentesConexas(g: Grafo): List<List<Int>> {
    val n = g.obtenerNumeroDeVertices()
    val A = getMatrizDeAdyacencia(g) // construir la matriz de adyacencia (A)
    val R = getMatrizDeAlcance(A) // construir la matriz de alcance (R)
    print("\nMatriz de adyacencia (A) $n x $n");
    A.print(0, 0);
    print("\nMatriz de alcance (R) $n x $n");
    R.print(0, 0) // feedback
    
    val esNoDirigido = g is GrafoNoDirigido || g is GrafoNoDirigidoCosto
    val visited = BooleanArray(n) // El constructor por defecto inicializa en False
    val components = mutableListOf<List<Int>>()

    for (i in 0 until n) {
        if (!visited[i]) {
            val component = mutableListOf<Int>()

            if (esNoDirigido) {
                // Para grafos no dirigidos, solo agregamos los vértices conectados a i
                for (j in 0 until n) {
                    if (R.get(i, j) == 1.0) {
                        component.add(j + 1) // Los vértices se guardan de 1 a n, no de 0 a n-1
                        visited[j] = true
                    }
                }
            } else {
                // Para grafos dirigidos, necesitamos verificar caminos en ambas direcciones (CFC)
                component.add(i+1)
                visited[i] = true
                for (j in 0 until n) {
                    if (R.get(i, j) == 1.0 && R.get(j, i) == 1.0 && i!=j) {
                        component.add(j + 1) // Los vértices se guardan de 1 a n, no de 0 a n-1
                        visited[j] = true
                    }
                }
            }

            components.add(component)
        }
    }

    return components
}

fun main(args: Array<String>) {
    File(".").listFiles{_,name->name.endsWith(".txt")}?.sortedBy{it.name}?.forEach{file->
        getGrafo(file.absolutePath)?.let{grafo->
            print("Componentes conexas: ${getComponentesConexas(grafo)}\n\n${"*".repeat(120)}\n")
        }?: print("No se pudo crear la instancia del grafo para el archivo ${file.name}.")
    }
 }