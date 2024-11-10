import ve.usb.libGrafo.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import kotlin.system.exitProcess
import java.util.PriorityQueue
import Jama.Matrix

val clases = arrayOf("GrafoNoDirigidoCosto", "GrafoNoDirigido", "GrafoDirigidoCosto", "GrafoDirigido")
fun formatDouble(costo: Double) = if (costo % 1 == 0.0) costo.toInt().toString() else costo.toString()
fun formatLista(t: List<Triple<Int, Int, Double>>) = t.joinToString(separator = ", ") { (a, b, w) -> "($a, $b, ${formatDouble(w)})" }

fun seleccionaArchivoTxt(prompt: String): String? {
    val directory = File(".")
    val txtFiles = directory.listFiles { _, name -> clases.any { name.contains(it) } && name.endsWith(".txt") }?.sortedBy { it.name }
    if (txtFiles != null && txtFiles.isNotEmpty()) {
        val fileChooser = JFileChooser(directory)
        fileChooser.dialogTitle = "Selecciona un archivo TXT"
        fileChooser.isMultiSelectionEnabled = false
        fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
        val options = txtFiles.map { it.name }.toTypedArray()
        val selectedFile = JOptionPane.showInputDialog(
            null,
            prompt,
            "Selector de archivos *.txt",
            JOptionPane.PLAIN_MESSAGE,
            null,
            options,
            options[0]
        )
        return selectedFile as String?
    }
    return null
}

/**
 * UTILS
 */
fun getVecinos(g: Grafo, u: Int): List<Int> { // obtener la lista de vecinos a un nodo, ordenados por el # del nodo
    return g.adyacentes(u).map { it.elOtroVertice(u) }.sorted()
}

fun getMatrizDeAdyacencia(g: Grafo): Matrix {
    var n = g.obtenerNumeroDeVertices()
    var A = Matrix(n,n) // el constructor por defecto inicializa en ceros
    val esNoDirigido = g is GrafoNoDirigido || g is GrafoNoDirigidoCosto
    for(i in 1..n){
        for(j in getVecinos(g,i)){
            A.set(i-1,j-1,1.0)
            if(esNoDirigido) A.set(j-1,i-1,1.0)
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

fun getComponentesConexas(g: Grafo): List<List<Int>> { //Construir una lista de lista de los rótulos (Int) de los nodos con las componentes conexas
    val n = g.obtenerNumeroDeVertices()
    val A = getMatrizDeAdyacencia(g) // construir la matriz de adyacencia (A)
    val R = getMatrizDeAlcance(A) // construir la matriz de alcance (R)
    // Utilizar la matriz C (copia de R) para extraer las componentes conexas
    val C = R.copy()
    val esNoDirigido = g is GrafoNoDirigido || g is GrafoNoDirigidoCosto
    val visitado = BooleanArray(n) // El constructor por defecto inicializa en False
    val componentes = mutableListOf<List<Int>>()
    for (i in 0 until n) {
        C.set(i,i,1.0)
        if (!visitado[i]) {
            var componente = mutableListOf<Int>()
            for (j in 0 until n) {
                val criterio = if(esNoDirigido) (C.get(i,j) == 1.0 && !visitado[j]) else (C.get(i,j) == 1.0 && C.get(j,i) == 1.0)
                if (criterio) {
                    componente.add(j+1) // OJO, la matrices comienzan en cero mientras los grafos en uno
                    visitado[j] = true
                }
            }
            componentes.add(componente)
        }
    }
    return componentes
}

fun MSTPrimByCC(grafo: GrafoNoDirigidoCosto, componente: List<Int>): List<AristaCosto> {
    // Calcula el MST con el algoritmo de Prim a partir de una Component Conexa del grafo
    val mst = mutableListOf<AristaCosto>() // Lista para almacenar las aristas del MST
    val visited = mutableSetOf<Int>() // Conjunto para marcar los nodos visitados
    val priorityQueue = PriorityQueue<AristaCosto>() // Cola de prioridad para las aristas de menor a mayor costo

    // Elegir un nodo inicial arbitrario de la componente
    val startNode = componente.random()
    visited.add(startNode)

    // Agregar las aristas del nodo inicial a la cola de prioridad
    priorityQueue.addAll(grafo.adyacentes(startNode) as List<AristaCosto>)

    // Mientras la cola no esté vacía y no hayamos visitado todos los nodos de la componente
    while (priorityQueue.isNotEmpty() && visited.size < componente.size) {
        val edge = priorityQueue.poll() // Extraer la arista de menor costo
        
        // Determinar el nuevo nodo que se visitará
        val newNode = if (edge.a in visited) edge.b else edge.a

        // Si el nuevo nodo no ha sido visitado, añadimos la arista al MST
        if (newNode !in visited) {
            visited.add(newNode)
            mst.add(edge)

            // Añadir las aristas del nuevo nodo a la cola de prioridad
            for (nextEdge in grafo.adyacentes(newNode) as List<AristaCosto>) {
                val otherNode = if (nextEdge.a == newNode) nextEdge.b else nextEdge.a
                if (otherNode !in visited) {
                    priorityQueue.add(nextEdge)
                }
            }
        }
    }

    return mst // Devuelve las aristas que forman el MST de la componente
}

fun getMSTbyPrim(g: GrafoNoDirigidoCosto): List<Pair<List<Int>, List<Triple<Int, Int, Double>>>> {
    val mstComponents = mutableListOf<Pair<List<Int>, List<Triple<Int, Int, Double>>>>()

    val componentesConexas = getComponentesConexas(g)

    for (componente in componentesConexas) {
        // Obtener el MST de la componente
        val mstAristas = MSTPrimByCC(g, componente)
        // Formatear las aristas del MST en triples para el resultado final
        val mstTriples = mstAristas.map { Triple(it.a, it.b, it.costo) }
            .sortedWith(compareBy({ it.third }, { it.first }, { it.second }))
        mstComponents.add(Pair(componente, mstTriples))
    }

    return mstComponents
}

fun main(args: Array<String>) {
    val archivoGrafo = seleccionaArchivoTxt("Seleccionar archivo de grafo") ?: exitProcess(1)
    val grafo = GrafoNoDirigidoCosto(archivoGrafo)
    println("$archivoGrafo $grafo")
    val mstComponents = getMSTbyPrim(grafo)

    mstComponents.forEach { component ->
        val totalWeight = component.second.sumByDouble { it.third } // Suma manual de los pesos
        println("CC=${component.first} E=[${formatLista(component.second)}] W=${formatDouble(totalWeight)}")
    }
}