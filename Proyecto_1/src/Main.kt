import ve.usb.libGrafo.*
import Jama.Matrix
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import java.util.ArrayDeque
import kotlin.reflect.full.primaryConstructor
import kotlin.system.exitProcess

/**
 * Utils
 */
fun getVecinos(g: Grafo, u: Int): List<Int> { // Obtener la lista de vecinos de un nodo, ordenados por el # del nodo
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

data class Candidate(
    val id: Int,
    val proximity: Int
)

/**
 * Definitions
 */
data class User(
    val id: Int,
    val friends: MutableList<Int> = mutableListOf(),
    val candidates: MutableList<Candidate> = mutableListOf() // TODO: Add ProximityLevel
)

class ILoveCatsNetwork(
    val friendsFilePath: String,
    val candidatesFilePath: String
) {
    val users = mutableMapOf<Int, User>()
    var friendsGraph: GrafoNoDirigido
    var candidatesGraph: GrafoNoDirigido

    init {
        friendsGraph = GrafoNoDirigido(friendsFilePath)
        candidatesGraph = GrafoNoDirigido(candidatesFilePath)

        // Inicializar usuarios con amigos y candidatos // TODO: agregar el ProximityLevel para cada candidato
        for (user_id in 1..friendsGraph.obtenerNumeroDeVertices()) {
            val friends = getFriends(user_id).toMutableList()
            val candidates = getCandidates(user_id)
            this.users[user_id] = User(user_id, friends, candidates)
        }
    }

    fun getFriends(user_id: Int): List<Int> {
        // Obtener la lista de amigos de un usuario, ordenados por el id del amigo
        return getVecinos(this.friendsGraph, user_id)
    }

    fun getCandidates(user_id: Int): MutableList<Candidate> {
        // Obtener la lista de candidatos de un usuario, ordenados por el id del candidato
        val candidates = getVecinos(this.candidatesGraph, user_id)
        val candidatesWithClosseness = mutableListOf<Candidate>()

        for (candidate in candidates) {
            candidatesWithClosseness.add(Candidate(candidate, this.getProximityLevel(user_id, candidate)))
        }

        return candidatesWithClosseness
    }

    fun getUsersWithMoreFriends(): List<User> {
        // Encontrar el máximo número de amigos entre todos los usuarios
        val maxAmigos = users.values.map { it.friends.size }.max() ?: 0

        // Filtrar todos los usuarios que tengan el número máximo de amigos y ordenar por ID
        return users.values
            .filter { it.friends.size == maxAmigos }
            .sortedBy { it.id } // Ordenar de menor a mayor por ID
    }

    fun getUsersWithLessFriends(): List<User> {
        // Encontrar el menor número de amigos entre todos los usuarios
        val minAmigos = users.values.map { it.friends.size }.min() ?: 0

        // Filtrar todos los usuarios que tengan el número menor de amigos y ordenar por ID
        return users.values
            .filter { it.friends.size == minAmigos }
            .sortedBy { it.id } // Ordenar de menor a mayor por ID
    }

    fun printWinners(winners: List<User>, category: String) {
        println("\tUSUARIOS CON $category AMIGOS=${winners.size}")
        
        for ((index, user) in winners.withIndex()) {
            println("\t\t${index+1}:${user.id}:${user.friends.size}:${user.friends}")
        }
    }

    fun getCommunities(): List<List<Int>> {
        val communities = getComponentesConexas(this.friendsGraph)

        // Ordenar cada comunidad individualmente de mayor a menor por la cantidad de amigos, y luego por ID en caso de empate
        val sortedCommunities = communities.map { community ->
            community.sortedWith(
                compareByDescending<Int> { this.users[it]?.friends?.size ?: 0 } // Ordenar de mayor a menor por cantidad de amigos
                    .thenBy { it } // En caso de empate, ordenar de menor a mayor por ID
            )
        }

        // Devolver las comunidades ordenadas por tamaño, de mayor a menor
        return sortedCommunities.sortedByDescending { it.size }
    }

    fun printCommunities() {
        val communities = this.getCommunities()
        println("\tCOMUNIDADES DE AMIGOS=${communities.size}")

        for ((c_index, community) in communities.withIndex()) {
            println("\t\tCOMUNIDAD ${c_index + 1}")

            for ((u_index, user_id) in community.withIndex()) {
                val user: User = this.users[user_id] as User

                println("\t\t\t${u_index + 1}:${user.id}:${user.friends.size}:${user.friends}")
            }
        }

    }

    fun getProximityLevel(user: Int, candidato: Int): Int {
        if (user == candidato) return Int.MAX_VALUE

        val visitado = mutableListOf<Int>()
        val cola = ArrayDeque<Pair<Int, Int>>() // (user, level)

        cola.add(Pair(user, 1))
        visitado.add(user)

        while (cola.isNotEmpty()) {
            val (currentUser, level) = cola.removeFirst()

            if (currentUser == candidato) return level

            // Usar una lista vacía si no hay amigos para el usuario actual
            val friends = this.users[currentUser]?.friends ?: mutableListOf()

            friends.forEach { friend ->
                if (friend !in visitado) {
                    visitado.add(friend)
                    cola.add(Pair(friend, level + 1))
                }
            }
        }

        return Int.MAX_VALUE
    }

    fun printClossenessByUser() {
        println("\tLISTA DE <<CANDIDATOS A AMIGOS>> POR USUARIO")

        for ((id, user) in this.users) {
            println("\t\tUSUARIO $id")

            for ((index, candidate) in user.candidates.withIndex()) {
                println("\t\t\t${index + 1}:${candidate.id}:${if (candidate.proximity == Int.MAX_VALUE) "∞" else candidate.proximity.toString()}")
            }
        }
    }

    fun report() {
        println("INFORME I♥CATS")
        
        val usersWithMoreFriends = getUsersWithMoreFriends()
        val usersWithLessFriends = getUsersWithLessFriends()

        this.printWinners(usersWithMoreFriends, "MAS")
        this.printWinners(usersWithLessFriends, "MENOS")

        this.printCommunities()

        this.printClossenessByUser()
    }
}

fun main(args: Array<String>) {
    fun seleccionaArchivoTxt(prompt: String): String? {
        val directory = File(".")
        val txtFiles = directory.listFiles { _, name -> name.endsWith(".txt") }?.sortedBy { it.name }
        if (txtFiles != null && txtFiles.isNotEmpty()) {
            val fileChooser = JFileChooser(directory)
            fileChooser.dialogTitle = "Selecciona un archivo TXT"
            fileChooser.isMultiSelectionEnabled = false
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            val options = txtFiles.map { it.name }.toTypedArray()
            val selectedFile = JOptionPane.showInputDialog(
                null, prompt, "Selector de archivos *.txt", JOptionPane.PLAIN_MESSAGE, null, options, options[0]
            )
            return selectedFile as String?
        }
        return null
    }

    val amigosTxt = seleccionaArchivoTxt("Seleccionar archivo de amigos.txt")
    if (amigosTxt == null) exitProcess(1)
    val candidatosTxt = seleccionaArchivoTxt("Seleccionar archivo de candidatos.txt")
    if (candidatosTxt == null) exitProcess(1)

    println("amigos=$amigosTxt\ncandidatos=$candidatosTxt")

    /**
     * Crear e inicializar la red
     */
    val network = ILoveCatsNetwork(amigosTxt, candidatosTxt)
    network.report();

    
}
