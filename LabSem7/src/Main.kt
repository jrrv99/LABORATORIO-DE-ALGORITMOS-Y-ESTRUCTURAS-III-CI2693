import ve.usb.libGrafo.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import kotlin.system.exitProcess

val clases=arrayOf("GrafoNoDirigidoCosto","GrafoNoDirigido","GrafoDirigidoCosto","GrafoDirigido")
fun formatDouble(costo: Double)=if(costo%1==0.0) costo.toInt().toString() else costo.toString()
fun formatLista(t:List<Triple<Int,Int,Double>>)=t.joinToString(separator=", "){(a,b,w)->"($a, $b, ${formatDouble(w)})"}

fun seleccionaArchivoTxt(prompt:String):String?{
	val directory = File(".")
	val txtFiles = directory.listFiles{ _, name -> clases.any { name.contains(it) } && name.endsWith(".txt")}?.sortedBy{it.name}
	if (txtFiles != null && txtFiles.isNotEmpty()) {
		val fileChooser = JFileChooser(directory)
		fileChooser.dialogTitle = "Selecciona un archivo TXT"
		fileChooser.isMultiSelectionEnabled = false
		fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
		val options = txtFiles.map{it.name}.toTypedArray()
		val selectedFile = JOptionPane.showInputDialog(
			null,
			prompt,
			"Selector de archivos *.txt",
			JOptionPane.PLAIN_MESSAGE,
			null,
			options,
			options[0]
		)
		return selectedFile  as String?
	}
	return null
}

fun getMSTbyPrim(g: GrafoNoDirigidoCosto): List<Pair<List<Int>, List<Triple<Int, Int, Double>>>> {
	val m: MutableList<Pair<List<Int>, List<Triple<Int, Int, Double>>>> = mutableListOf()
	/*
	SU CODIGO
	*/
	g.adyacentes(1)
	return m
}

fun main(args: Array<String>) {
	val t = seleccionaArchivoTxt("Seleccionar archivo de grafo")?:exitProcess(1)
	val g = GrafoNoDirigidoCosto(t)?:exitProcess(1)
	println("$t $g")
	getMSTbyPrim(g).forEach{println("CC=${it.first} E=[${formatLista(it.second)}] W=${formatDouble(it.second.sumOf{it.third})}")}
 }