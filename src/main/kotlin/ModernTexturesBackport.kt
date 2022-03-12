import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Files

fun main() {
    val matches = parseFileMatches(File("""L:\Work-ProgrammerArt\programmer_art.matches"""))

    val newZip = File("items-backport.zip")
    ZipUtil.createEmpty(newZip)

    var nonExisting = 0
    val oneDotEighteen = File("""D:\Software\MultiMC\libraries\com\mojang\minecraft\1.18\minecraft-1.18-client.jar""")
    FileSystems.newFileSystem(oneDotEighteen.toPath()).use { mcFs ->
        FileSystems.newFileSystem(newZip.toPath()).use { newFs ->
            matches.forEach { match ->
                val pathInOneDotEighteen = mcFs.getPath(match.oldFile)
                if (Files.exists(pathInOneDotEighteen)) {
                    val pathInOneDotEight = newFs.getPath(match.newFile)
                    Files.createDirectories(pathInOneDotEight.parent)
                    Files.copy(pathInOneDotEighteen, pathInOneDotEight)
                } else {
                    nonExisting++
                    println("File not found: $pathInOneDotEighteen")
                }
            }
        }
    }

    println("Non-existing: $nonExisting")
}

fun parseFileMatches(matchFile: File): List<FileMatch> {

    val fileMatches = mutableListOf<FileMatch>()
    val lines = matchFile.readLines()
    for (line in lines) {
        val split = line.split("->")
        if (split.size != 2) {
            throw IllegalArgumentException("Invalid line: $line")
        }
        val oldFile = split[0]
        val newFile = split[1]
        fileMatches.add(FileMatch(oldFile, newFile))
    }

    return fileMatches
}

data class FileMatch(
    val oldFile: String,
    val newFile: String
)