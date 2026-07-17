import java.io.File
import com.moandjiezana.toml.Toml

fun main() {
    val tomlString = """
[mutes]
enabled = false
"""

    val toml = Toml().read(tomlString)
    println("Value: " + toml.getBoolean("mutes.enabled"))
}
