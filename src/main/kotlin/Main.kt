fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) {
            println("fuata: missing command. Available commands: init, add, commit, log.")
            return
        }

        val fuata = Fuata()

        when (args[0]) {
            "init" -> fuata.initialiseRepository()
            "add" -> {
                if (args.size < 2) {
                    println("fuata: missing file name for 'add' command")
                    println("Usage: fuata add <file_name>")
                } else {
                    fuata.add(args[1])
                }
            }

            "commit" -> {
                if (args.size < 2) {
                    println("fuata: missing commit message")
                    println("Usage: fuata commit \"<commit_message>\"")
                } else {
                    fuata.commit(args[1])
                }
            }

            "log" -> fuata.log()
            "create-branch" -> {
                if (args.size < 2) {
                    println("fuata: missing branch name")
                    println("Usage: fuata create-branch <branch_name>")
                } else {
                    fuata.createBranch(args[1])
                }
            }

            "checkout" -> {
                if (args.size < 2) {
                    println("fuata: missing branch name")
                    println("Usage: fuata checkout <branch_name>")
                } else {
                    fuata.checkout(args[1])
                }
            }

            else -> println(
                "fuata: unknown command ${args[0]}. " +
                        "\nAvailable commands: init, log, add, commit, create-branch, checkout"
            )
        }
    } catch (e: Exception) {
        println("fuata: ${e.message ?: "unknown error"}")
    }
}