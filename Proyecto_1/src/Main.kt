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

/**
 * Definitions
 */
data class User(
    val id: Int,
    val friends: MutableList<Int> = mutableListOf(),
    val candidates: MutableList<Int> = mutableListOf() // TODO: Add ClosenessLevel
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

        // Inicializar usuarios con amigos y candidatos // TODO: agregar el ClosenessLevel para cada candidato
        for (user_id in 1..friendsGraph.obtenerNumeroDeVertices()) {
            val friends = getFriends(user_id).toMutableList()
            val candidates = getCandidates(user_id).toMutableList()
            this.users[user_id] = User(user_id, friends, candidates)
        }
    }

    fun getFriends(user_id: Int): List<Int> {
        // Obtener la lista de amigos de un usuario, ordenados por el id del amigo
        return getVecinos(this.friendsGraph, user_id)
    }

    fun getCandidates(user_id: Int): List<Int> {
        // Obtener la lista de candidatos de un usuario, ordenados por el id del candidato
        return getVecinos(this.candidatesGraph, user_id)
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

    fun report() {
        println("INFORME I♥CATS")
        
        val usersWithMoreFriends = getUsersWithMoreFriends()
        val usersWithLessFriends = getUsersWithLessFriends()

        this.printWinners(usersWithMoreFriends, "MAS")
        this.printWinners(usersWithLessFriends, "MENOS")
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
